package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundAndHapticsEngine
import com.example.audio.SoundEffect
import com.example.data.local.CachedLevelEntity
import com.example.data.local.GameSettings
import com.example.data.local.LocalGameDatabase
import com.example.data.local.MissionEntity
import com.example.data.local.ModeBestEntity
import com.example.data.local.PowerUpInventory
import com.example.data.local.SettingsDataStore
import com.example.data.remote.FirebaseSyncRepository
import com.example.data.remote.LeaderboardEntryDoc
import com.example.data.remote.LevelProgressDoc
import com.example.data.remote.UserProfileDoc
import com.example.game.campaign.CampaignCatalog
import com.example.game.campaign.ShopItem
import com.example.game.campaign.WheelSlice
import com.example.game.physics.GameMode
import com.example.game.physics.GameSessionConfig
import com.example.game.physics.PhysicsUiState
import com.example.game.physics.PlayPhase
import com.example.game.physics.PowerUpType
import com.example.game.physics.TowerPhysicsEngine
import com.example.services.AdManager
import com.example.services.AdMobTestAdapter
import com.example.services.BillingManager
import com.example.services.NetworkMonitor
import com.example.services.PhotoBackdrop
import com.example.services.PhotoShareHelper
import com.example.services.PlayBillingAdapter
import com.example.services.RewardPlacement
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface LeaderboardUiState {
    data object Loading : LeaderboardUiState
    data class Success(val entries: List<LeaderboardEntryDoc>) : LeaderboardUiState
    data class Error(val message: String) : LeaderboardUiState
}

class TumbleTowerViewModel(
    application: Application,
    private val currentUserId: String,
    private val firebaseEnabled: Boolean = true
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    val settingsStore = SettingsDataStore(appContext)
    private val localDb = LocalGameDatabase.getInstance(appContext)
    private val dao = localDb.gameCacheDao()
    private val firebaseRepo: FirebaseSyncRepository? =
        if (firebaseEnabled && FirebaseApp.getApps(appContext).isNotEmpty()) {
            runCatching { FirebaseSyncRepository(appContext) }.getOrNull()
        } else {
            null
        }
    val audioEngine = SoundAndHapticsEngine(appContext)
    val adManager: AdManager = AdMobTestAdapter()
    val billingManager: BillingManager = PlayBillingAdapter()
    private val networkMonitor = NetworkMonitor(appContext)

    private val physicsEngine = TowerPhysicsEngine(
        onSoundEffect = { audioEngine.playSfx(it) },
        onHaptic = { ms, amp -> audioEngine.vibrate(ms, amp) }
    )

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val settings: StateFlow<GameSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), GameSettings())

    val powerUps: StateFlow<PowerUpInventory> = settingsStore.powerUpsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PowerUpInventory())

    val ownedCosmetics: StateFlow<Triple<Set<String>, Set<String>, Set<String>>> =
        settingsStore.ownedCosmeticsFlow
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                Triple(setOf("wood"), setOf("forest"), setOf("dust"))
            )

    val lastSpinDate: StateFlow<String> = settingsStore.lastSpinDateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "")

    private val _userProfile = MutableStateFlow(UserProfileDoc(id = currentUserId, userId = currentUserId))
    val userProfile: StateFlow<UserProfileDoc> = _userProfile.asStateFlow()

    val cachedLevels: StateFlow<List<CachedLevelEntity>> = dao.observeAllLevels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val missions: StateFlow<List<MissionEntity>> = dao.observeMissions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val modeBests: StateFlow<List<ModeBestEntity>> = dao.observeModeBests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedLeaderboardBoard = MutableStateFlow("daily")
    val selectedLeaderboardBoard: StateFlow<String> = _selectedLeaderboardBoard.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val leaderboardState: StateFlow<LeaderboardUiState> = _selectedLeaderboardBoard
        .flatMapLatest { board ->
            val repo = firebaseRepo
            if (repo == null) {
                flowOf(
                    LeaderboardUiState.Error(
                        "Local Offline Mode: Add google-services.json in your build to unlock Global Leaderboards."
                    )
                )
            } else {
                repo.observeLeaderboard(board)
                    .map<List<LeaderboardEntryDoc>, LeaderboardUiState> { LeaderboardUiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Leaderboard observation error", rootError)
                        emit(LeaderboardUiState.Error(rootError.message ?: "Could not load leaderboard"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LeaderboardUiState.Loading)

    private val _physicsState = MutableStateFlow(PhysicsUiState())
    val physicsState: StateFlow<PhysicsUiState> = _physicsState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var physicsLoopJob: Job? = null
    private var sessionStartMillis: Long = 0L
    private var runRecorded = false

    init {
        viewModelScope.launch {
            settings.collect { s ->
                audioEngine.updateSettings(s.soundEnabled, s.musicEnabled, s.hapticsEnabled, s.sfxVolume, s.musicVolume)
            }
        }

        viewModelScope.launch {
            seedDefaultMissionsIfEmpty()
            val repo = firebaseRepo
            if (repo != null && FirebaseApp.getApps(appContext).isNotEmpty()) {
                val authUser = runCatching { Firebase.auth.currentUser }.getOrNull()
                val defaultName = authUser?.displayName?.take(28)?.ifBlank { "SkyBuilder" } ?: "SkyBuilder"
                repo.ensureUserProfile(defaultName)
            }
        }

        val repo = firebaseRepo
        if (repo != null) {
            viewModelScope.launch {
                repo.observeUserProfile(currentUserId)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Profile observer error", firestoreError ?: error)
                    }
                    .collect { doc ->
                        if (doc != null) {
                            _userProfile.value = doc
                            adManager.setAdsRemoved(doc.adsRemoved)
                        }
                    }
            }

            viewModelScope.launch {
                repo.observeLevelProgress(currentUserId)
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Level progress observer error", firestoreError ?: error)
                    }
                    .collect { remoteLevels ->
                        if (remoteLevels.isNotEmpty()) {
                            dao.upsertLevels(remoteLevels.map { it.toEntity() })
                        }
                    }
            }
        }
    }

    private suspend fun seedDefaultMissionsIfEmpty() {
        if (dao.getMissionCount() > 0) return
        val defaults = listOf(
            MissionEntity("m_towers_5", "Tower Architect", "Complete 5 tower runs", 0, 5, 450, 10, false),
            MissionEntity("m_steady_10", "Steady Hands", "Land 10 Steady Hand combos", 0, 10, 600, 15, false),
            MissionEntity("m_height_20", "Sky Scraper", "Reach 20 blocks tall in any mode", 0, 20, 800, 20, false),
            MissionEntity("m_daily_1", "Daily Challenger", "Finish 1 Daily Tower Challenge", 0, 1, 500, 15, false),
            MissionEntity("m_boss_1", "Boss Slayer", "Beat a Boss Tower level", 0, 1, 1000, 30, false)
        )
        dao.upsertMissions(defaults)
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun selectLeaderboardBoard(board: String) {
        _selectedLeaderboardBoard.value = board
    }

    fun startCampaignLevel(levelNumber: Int) {
        val level = CampaignCatalog.getLevel(levelNumber)
        startGameSession(CampaignCatalog.toSessionConfig(level))
    }

    fun startGameMode(mode: GameMode, playerNames: List<String> = listOf("Player 1", "Player 2")) {
        val cfg = when (mode) {
            GameMode.CAMPAIGN -> CampaignCatalog.toSessionConfig(CampaignCatalog.getLevel(highestUnlockedLevel()))
            GameMode.DAILY_CHALLENGE -> CampaignCatalog.dailyChallengeConfig()
            GameMode.TIME_ATTACK -> GameSessionConfig(
                mode = GameMode.TIME_ATTACK,
                worldId = 2,
                targetMoves = 999,
                targetHeight = 25,
                initialLayers = 10,
                gravityMultiplier = 1.0f,
                baseWindStrength = 0.12f
            )
            GameMode.SURVIVAL -> GameSessionConfig(
                mode = GameMode.SURVIVAL,
                worldId = 5,
                targetMoves = 999,
                targetHeight = 28,
                initialLayers = 11,
                gravityMultiplier = 1.05f,
                baseWindStrength = 0.25f
            )
            GameMode.PASS_AND_PLAY -> GameSessionConfig(
                mode = GameMode.PASS_AND_PLAY,
                worldId = 1,
                targetMoves = 999,
                targetHeight = 24,
                playerNames = playerNames.ifEmpty { listOf("Player 1", "Player 2") },
                initialLayers = 10
            )
            GameMode.ENDLESS -> GameSessionConfig(
                mode = GameMode.ENDLESS,
                worldId = 4,
                targetMoves = 999,
                targetHeight = 35,
                initialLayers = 10,
                gravityMultiplier = 0.94f
            )
            GameMode.CLASSIC -> GameSessionConfig(
                mode = GameMode.CLASSIC,
                worldId = 1,
                targetMoves = 999,
                targetHeight = 22,
                initialLayers = 10
            )
        }
        startGameSession(cfg)
    }

    fun startGameSession(cfg: GameSessionConfig) {
        viewModelScope.launch {
            val modeKey = if (cfg.mode == GameMode.CAMPAIGN) "campaign_${cfg.levelNumber}" else cfg.mode.routeKey
            val ghostBest = dao.getModeBest(modeKey)?.bestHeight ?: (cfg.initialLayers + 4)
            runRecorded = false
            sessionStartMillis = System.currentTimeMillis()
            _physicsState.value = physicsEngine.startNewSession(cfg, ghostBest)
            startPhysicsLoop()
        }
    }

    private fun startPhysicsLoop() {
        physicsLoopJob?.cancel()
        physicsLoopJob = viewModelScope.launch {
            var lastNs = System.nanoTime()
            while (isActive) {
                val targetMs = if (settings.value.batteryFriendly30Fps) 33L else 16L
                delay(targetMs)
                val nowNs = System.nanoTime()
                val dt = ((nowNs - lastNs) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
                lastNs = nowNs

                val nextState = physicsEngine.step(dt, settings.value.particlesEnabled)
                _physicsState.value = nextState

                val intensity = ((1f - nextState.stabilityPercent / 100f) * 0.6f +
                    (nextState.towerHeightLayers / 30f).coerceIn(0f, 0.4f))
                audioEngine.setIntensity(intensity)

                if ((nextState.phase == PlayPhase.LEVEL_COMPLETE || nextState.phase == PlayPhase.GAME_OVER) && !runRecorded) {
                    runRecorded = true
                    recordCompletedRun(nextState)
                }
            }
        }
    }

    fun stopPhysicsLoop() {
        physicsLoopJob?.cancel()
        physicsLoopJob = null
    }

    fun dismissBossIntro() {
        audioEngine.playSfx(SoundEffect.UI_CLICK)
        _physicsState.value = physicsEngine.dismissBossIntro()
    }

    fun setPaused(paused: Boolean) {
        audioEngine.playSfx(SoundEffect.UI_CLICK)
        _physicsState.value = physicsEngine.setPaused(paused)
    }

    fun startPullBlock(blockId: Int) {
        _physicsState.value = physicsEngine.startPullBlock(blockId)
    }

    fun updatePullDrag(deltaPull: Float, lateralJitter: Float) {
        _physicsState.value = physicsEngine.updatePullDrag(
            deltaPull = deltaPull,
            lateralJitter = lateralJitter,
            sensitivity = settings.value.controlSensitivity
        )
    }

    fun releasePull() {
        _physicsState.value = physicsEngine.releasePull()
    }

    fun setXray(enabled: Boolean) {
        _physicsState.value = physicsEngine.setXray(enabled)
    }

    fun updatePlacementAim(slot: Int, fineOffsetNorm: Float) {
        _physicsState.value = physicsEngine.updatePlacementAim(slot, fineOffsetNorm)
    }

    fun placeHeldBlock(slot: Int = 1, fineOffsetNorm: Float = 0f) {
        _physicsState.value = physicsEngine.placeHeldBlock(slotChoice = slot, precisionDropOffset = fineOffsetNorm)
    }

    fun usePowerUp(type: PowerUpType) {
        viewModelScope.launch {
            val inv = powerUps.value
            val count = when (type) {
                PowerUpType.FREEZE -> inv.freeze
                PowerUpType.GLUE -> inv.glue
                PowerUpType.UNDO -> inv.undo
                PowerUpType.SHIELD -> inv.shield
                PowerUpType.CRANE -> inv.crane
            }
            if (count <= 0) {
                _toastMessage.value = "Out of ${type.title}! Buy more or watch an ad in Shop."
                return@launch
            }
            val (used, updatedState) = physicsEngine.activatePowerUp(type)
            _physicsState.value = updatedState
            if (used) {
                settingsStore.updatePowerUps { cur ->
                    when (type) {
                        PowerUpType.FREEZE -> cur.copy(freeze = cur.freeze - 1)
                        PowerUpType.GLUE -> cur.copy(glue = cur.glue - 1)
                        PowerUpType.UNDO -> cur.copy(undo = cur.undo - 1)
                        PowerUpType.SHIELD -> cur.copy(shield = cur.shield - 1)
                        PowerUpType.CRANE -> cur.copy(crane = cur.crane - 1)
                    }
                }
            }
        }
    }

    fun watchAdForDoubleCoins() {
        if (_physicsState.value.coinsDoubledByAd) return
        viewModelScope.launch {
            val rewarded = adManager.showRewardedAd(RewardPlacement.DOUBLE_COINS)
            if (rewarded) {
                val bonus = _physicsState.value.coinsEarned
                _physicsState.value = physicsEngine.doubleCoinsWithAd()
                addCurrency(coinsDelta = bonus, gemsDelta = 0)
                _toastMessage.value = "+$bonus Bonus Coins Earned!"
            }
        }
    }

    fun watchAdForRevive() {
        viewModelScope.launch {
            val rewarded = adManager.showRewardedAd(RewardPlacement.REVIVE_TOWER)
            if (rewarded) {
                runRecorded = false
                _physicsState.value = physicsEngine.reviveWithAd()
            }
        }
    }

    fun watchAdForFreePowerUp(type: PowerUpType) {
        viewModelScope.launch {
            val rewarded = adManager.showRewardedAd(RewardPlacement.FREE_POWERUP)
            if (rewarded) {
                settingsStore.updatePowerUps { cur ->
                    when (type) {
                        PowerUpType.FREEZE -> cur.copy(freeze = cur.freeze + 1)
                        PowerUpType.GLUE -> cur.copy(glue = cur.glue + 1)
                        PowerUpType.UNDO -> cur.copy(undo = cur.undo + 1)
                        PowerUpType.SHIELD -> cur.copy(shield = cur.shield + 1)
                        PowerUpType.CRANE -> cur.copy(crane = cur.crane + 1)
                    }
                }
                audioEngine.playSfx(SoundEffect.COIN_DING)
                _toastMessage.value = "+1 ${type.title} unlocked!"
            }
        }
    }

    fun replayCollapse() {
        _physicsState.value = physicsEngine.startCollapseReplay()
    }

    private fun recordCompletedRun(state: PhysicsUiState) {
        viewModelScope.launch {
            val elapsedSec = ((System.currentTimeMillis() - sessionStartMillis) / 1000L).coerceAtLeast(1L)
            val profile = _userProfile.value
            val isSuccess = state.phase == PlayPhase.LEVEL_COMPLETE || state.movesCompleted > 0

            val newCoins = profile.coins + state.coinsEarned
            val gainedXp = (state.score / 40L).coerceAtLeast(15L)
            val totalXp = profile.xpPoints + gainedXp
            val newLevel = (1L + totalXp / 500L).coerceIn(1L, 1000L)
            val newBestHeight = maxOf(profile.bestHeight, state.towerHeightLayers.toLong())
            val newBestScore = maxOf(profile.bestScore, state.score)

            val updatedProfile = profile.copy(
                coins = newCoins,
                xpPoints = totalXp,
                xpLevel = newLevel,
                bestHeight = newBestHeight,
                bestScore = newBestScore,
                totalTowersBuilt = profile.totalTowersBuilt + if (isSuccess) 1L else 0L,
                perfectPulls = profile.perfectPulls + state.perfectPullsCount.toLong(),
                totalPulls = profile.totalPulls + state.totalPullsCount.toLong(),
                playTimeSeconds = profile.playTimeSeconds + elapsedSec
            )
            _userProfile.value = updatedProfile

            // Local Ghost Tower personal best
            val modeKey = if (state.config.mode == GameMode.CAMPAIGN) {
                "campaign_${state.config.levelNumber}"
            } else {
                state.config.mode.routeKey
            }
            val existingModeBest = dao.getModeBest(modeKey)
            dao.upsertModeBest(
                ModeBestEntity(
                    modeKey = modeKey,
                    bestHeight = maxOf(existingModeBest?.bestHeight ?: 0, state.towerHeightLayers),
                    bestScore = maxOf(existingModeBest?.bestScore ?: 0L, state.score)
                )
            )

            // Sync user profile to Firestore
            firebaseRepo?.updateUserProfileStats(
                displayName = updatedProfile.displayName,
                avatarId = updatedProfile.avatarId,
                coins = updatedProfile.coins,
                gems = updatedProfile.gems,
                xpLevel = updatedProfile.xpLevel,
                xpPoints = updatedProfile.xpPoints,
                equippedSkin = updatedProfile.equippedSkin,
                equippedTheme = updatedProfile.equippedTheme,
                equippedEffect = updatedProfile.equippedEffect,
                adsRemoved = updatedProfile.adsRemoved,
                bestHeight = updatedProfile.bestHeight,
                bestScore = updatedProfile.bestScore,
                totalTowersBuilt = updatedProfile.totalTowersBuilt,
                perfectPulls = updatedProfile.perfectPulls,
                totalPulls = updatedProfile.totalPulls,
                playTimeSeconds = updatedProfile.playTimeSeconds,
                dailyStreak = updatedProfile.dailyStreak,
                lastDailyRewardDate = updatedProfile.lastDailyRewardDate
            )

            // Save Campaign level progress
            if (state.config.mode == GameMode.CAMPAIGN && state.phase == PlayPhase.LEVEL_COMPLETE) {
                val lvlNum = state.config.levelNumber
                val entity = CachedLevelEntity(
                    levelNumber = lvlNum,
                    worldId = state.config.worldId,
                    stars = state.starsEarned.coerceAtLeast(1),
                    highScore = state.score,
                    bestHeight = state.towerHeightLayers,
                    completed = true
                )
                dao.upsertLevel(entity)
                firebaseRepo?.saveLevelProgress(
                    levelNumber = lvlNum,
                    worldId = state.config.worldId,
                    stars = entity.stars,
                    highScore = state.score,
                    bestHeight = state.towerHeightLayers,
                    completed = true
                )
            }

            // Submit to Leaderboard (All-Time + Daily + Daily Challenge)
            val acc = if (state.totalPullsCount > 0) {
                ((state.perfectPullsCount * 100) / state.totalPullsCount).coerceIn(0, 100)
            } else 100
            val today = CampaignCatalog.todayDateKey()
            if (state.score > 0L) {
                firebaseRepo?.submitLeaderboardScore(
                    displayName = updatedProfile.displayName,
                    avatarId = updatedProfile.avatarId,
                    boardType = "all_time",
                    dateKey = "all",
                    score = state.score,
                    height = state.towerHeightLayers,
                    accuracyPercent = acc
                )
                firebaseRepo?.submitLeaderboardScore(
                    displayName = updatedProfile.displayName,
                    avatarId = updatedProfile.avatarId,
                    boardType = "daily",
                    dateKey = today,
                    score = state.score,
                    height = state.towerHeightLayers,
                    accuracyPercent = acc
                )
                if (state.config.mode == GameMode.DAILY_CHALLENGE) {
                    firebaseRepo?.submitLeaderboardScore(
                        displayName = updatedProfile.displayName,
                        avatarId = updatedProfile.avatarId,
                        boardType = "daily_challenge",
                        dateKey = today,
                        score = state.score,
                        height = state.towerHeightLayers,
                        accuracyPercent = acc
                    )
                }
            }

            // Update Missions
            updateMissionsAfterRun(state)

            // Frequency-capped Interstitial hook
            adManager.showInterstitialIfReady()
        }
    }

    private suspend fun updateMissionsAfterRun(state: PhysicsUiState) {
        val current = dao.observeMissions()
        val list = missions.value.toMutableList()
        if (list.isEmpty()) return
        val updated = list.map { m ->
            when (m.id) {
                "m_towers_5" -> m.copy(progress = (m.progress + 1).coerceAtMost(m.target))
                "m_steady_10" -> m.copy(progress = (m.progress + state.perfectPullsCount).coerceAtMost(m.target))
                "m_height_20" -> m.copy(progress = maxOf(m.progress, state.towerHeightLayers).coerceAtMost(m.target))
                "m_daily_1" -> if (state.config.mode == GameMode.DAILY_CHALLENGE) m.copy(progress = m.target) else m
                "m_boss_1" -> if (state.phase == PlayPhase.LEVEL_COMPLETE && state.config.levelNumber % 10 == 0) m.copy(progress = m.target) else m
                else -> m
            }
        }
        dao.upsertMissions(updated)
    }

    fun claimMission(mission: MissionEntity) {
        if (mission.claimed || mission.progress < mission.target) return
        viewModelScope.launch {
            dao.upsertMission(mission.copy(claimed = true))
            addCurrency(mission.rewardCoins, mission.rewardGems)
            audioEngine.playSfx(SoundEffect.COIN_DING)
            _toastMessage.value = "Mission Claimed! +${mission.rewardCoins} Coins, +${mission.rewardGems} Gems"
        }
    }

    fun claimDailyReward() {
        val today = CampaignCatalog.todayDateKey()
        val profile = _userProfile.value
        if (profile.lastDailyRewardDate == today) {
            _toastMessage.value = "Today's Daily Reward is already claimed!"
            return
        }
        val nextDayIdx = (profile.dailyStreak.toInt() % 7)
        val tier = CampaignCatalog.dailyRewards[nextDayIdx]
        viewModelScope.launch {
            val nextStreak = profile.dailyStreak + 1L
            val updated = profile.copy(
                coins = profile.coins + tier.coins,
                gems = profile.gems + tier.gems,
                dailyStreak = nextStreak,
                lastDailyRewardDate = today
            )
            _userProfile.value = updated
            syncProfileToCloud(updated)
            audioEngine.playSfx(SoundEffect.LEVEL_WIN)
            _toastMessage.value = "${tier.label} Claimed: +${tier.coins} Coins & +${tier.gems} Gems!"
        }
    }

    fun grantSpinReward(slice: WheelSlice) {
        val today = CampaignCatalog.todayDateKey()
        viewModelScope.launch {
            settingsStore.setLastSpinDate(today)
            addCurrency(slice.coins, slice.gems)
            audioEngine.playSfx(SoundEffect.COIN_DING)
            _toastMessage.value = "Spin Won: ${slice.label}!"
        }
    }

    fun buyOrEquipCosmetic(item: ShopItem) {
        viewModelScope.launch {
            val (skins, themes, effects) = ownedCosmetics.value
            val owned = when (item.category) {
                "skin" -> item.id in skins
                "theme" -> item.id in themes
                else -> item.id in effects
            }
            val profile = _userProfile.value
            if (!owned) {
                if (profile.coins < item.coinPrice || profile.gems < item.gemPrice) {
                    _toastMessage.value = "Not enough Coins or Gems!"
                    return@launch
                }
                settingsStore.unlockCosmetic(item.category, item.id)
                val deducted = profile.copy(
                    coins = profile.coins - item.coinPrice,
                    gems = profile.gems - item.gemPrice,
                    equippedSkin = if (item.category == "skin") item.id else profile.equippedSkin,
                    equippedTheme = if (item.category == "theme") item.id else profile.equippedTheme,
                    equippedEffect = if (item.category == "effect") item.id else profile.equippedEffect
                )
                _userProfile.value = deducted
                syncProfileToCloud(deducted)
                audioEngine.playSfx(SoundEffect.COIN_DING)
                _toastMessage.value = "Unlocked & Equipped ${item.name}!"
            } else {
                val equipped = profile.copy(
                    equippedSkin = if (item.category == "skin") item.id else profile.equippedSkin,
                    equippedTheme = if (item.category == "theme") item.id else profile.equippedTheme,
                    equippedEffect = if (item.category == "effect") item.id else profile.equippedEffect
                )
                _userProfile.value = equipped
                syncProfileToCloud(equipped)
                audioEngine.playSfx(SoundEffect.UI_CLICK)
                _toastMessage.value = "Equipped ${item.name}!"
            }
        }
    }

    fun buyPowerUpWithGems(type: PowerUpType) {
        viewModelScope.launch {
            val profile = _userProfile.value
            if (profile.gems < type.gemCost) {
                _toastMessage.value = "Need ${type.gemCost} Gems!"
                return@launch
            }
            val updated = profile.copy(gems = profile.gems - type.gemCost)
            _userProfile.value = updated
            syncProfileToCloud(updated)
            settingsStore.updatePowerUps { cur ->
                when (type) {
                    PowerUpType.FREEZE -> cur.copy(freeze = cur.freeze + 1)
                    PowerUpType.GLUE -> cur.copy(glue = cur.glue + 1)
                    PowerUpType.UNDO -> cur.copy(undo = cur.undo + 1)
                    PowerUpType.SHIELD -> cur.copy(shield = cur.shield + 1)
                    PowerUpType.CRANE -> cur.copy(crane = cur.crane + 1)
                }
            }
            audioEngine.playSfx(SoundEffect.COIN_DING)
            _toastMessage.value = "+1 ${type.title} purchased!"
        }
    }

    fun purchaseStoreProduct(sku: String) {
        viewModelScope.launch {
            val res = billingManager.purchaseProduct(sku)
            res.onSuccess { prod ->
                val cur = _userProfile.value
                val updated = cur.copy(
                    coins = cur.coins + prod.coinsReward,
                    gems = cur.gems + prod.gemsReward,
                    adsRemoved = cur.adsRemoved || prod.removesAds
                )
                _userProfile.value = updated
                adManager.setAdsRemoved(updated.adsRemoved)
                syncProfileToCloud(updated)
                audioEngine.playSfx(SoundEffect.LEVEL_WIN)
                _toastMessage.value = "Purchased ${prod.title}!"
            }.onFailure {
                _toastMessage.value = it.localizedMessage ?: "Purchase failed"
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            billingManager.restorePurchases()
            _toastMessage.value = "Purchases restored."
        }
    }

    fun updateProfileIdentity(displayName: String, avatarId: String) {
        viewModelScope.launch {
            val safe = displayName.trim().take(40).ifBlank { "SkyBuilder" }
            val updated = _userProfile.value.copy(displayName = safe, avatarId = avatarId)
            _userProfile.value = updated
            syncProfileToCloud(updated)
            _toastMessage.value = "Profile updated!"
        }
    }

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        viewModelScope.launch {
            settingsStore.updateSettings(transform)
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            dao.clearLevels()
            dao.clearModeBests()
            settingsStore.resetAllLocalPrefs()
            val resetProfile = _userProfile.value.copy(
                coins = 500L,
                gems = 25L,
                xpLevel = 1L,
                xpPoints = 0L,
                bestHeight = 0L,
                bestScore = 0L,
                totalTowersBuilt = 0L,
                perfectPulls = 0L,
                totalPulls = 0L,
                dailyStreak = 0L,
                lastDailyRewardDate = ""
            )
            _userProfile.value = resetProfile
            syncProfileToCloud(resetProfile)
            _toastMessage.value = "Progress reset."
        }
    }

    fun highestUnlockedLevel(): Int {
        val completedMax = cachedLevels.value.filter { it.completed }.maxOfOrNull { it.levelNumber } ?: 0
        return (completedMax + 1).coerceIn(1, 210)
    }

    fun captureAndSharePhoto(backdrop: PhotoBackdrop, showWatermark: Boolean) {
        val state = _physicsState.value
        val blocks = if (state.blocks.isNotEmpty()) state.blocks else {
            physicsEngine.startNewSession(GameSessionConfig(GameMode.CLASSIC), 14).blocks
        }
        val bmp: Bitmap = PhotoShareHelper.renderTowerSnapshot(
            blocks = blocks,
            score = state.score,
            height = state.towerHeightLayers,
            backdrop = backdrop,
            showWatermark = showWatermark
        )
        PhotoShareHelper.shareTowerBitmap(
            appContext,
            bmp,
            "Built a ${state.towerHeightLayers}-block tower in Tumble Tower: Skyfall! Score: ${state.score}"
        )
    }

    private suspend fun addCurrency(coinsDelta: Int, gemsDelta: Int) {
        val cur = _userProfile.value
        val updated = cur.copy(
            coins = (cur.coins + coinsDelta).coerceAtLeast(0L),
            gems = (cur.gems + gemsDelta).coerceAtLeast(0L)
        )
        _userProfile.value = updated
        syncProfileToCloud(updated)
    }

    private suspend fun syncProfileToCloud(p: UserProfileDoc) {
        firebaseRepo?.updateUserProfileStats(
            displayName = p.displayName,
            avatarId = p.avatarId,
            coins = p.coins,
            gems = p.gems,
            xpLevel = p.xpLevel,
            xpPoints = p.xpPoints,
            equippedSkin = p.equippedSkin,
            equippedTheme = p.equippedTheme,
            equippedEffect = p.equippedEffect,
            adsRemoved = p.adsRemoved,
            bestHeight = p.bestHeight,
            bestScore = p.bestScore,
            totalTowersBuilt = p.totalTowersBuilt,
            perfectPulls = p.perfectPulls,
            totalPulls = p.totalPulls,
            playTimeSeconds = p.playTimeSeconds,
            dailyStreak = p.dailyStreak,
            lastDailyRewardDate = p.lastDailyRewardDate
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopPhysicsLoop()
        audioEngine.release()
        networkMonitor.unregister()
    }

    private fun LevelProgressDoc.toEntity(): CachedLevelEntity = CachedLevelEntity(
        levelNumber = levelNumber.toInt(),
        worldId = worldId.toInt(),
        stars = stars.toInt(),
        highScore = highScore,
        bestHeight = bestHeight.toInt(),
        completed = completed
    )

    private companion object {
        const val TAG = "TumbleTowerVM"
    }
}
