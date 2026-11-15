package com.example

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.services.RewardPlacement
import com.example.ui.auth.AuthGateScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.CampaignMapScreen
import com.example.ui.screens.DailyRewardsAndMissionsScreen
import com.example.ui.screens.GameModesScreen
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.PhotoModeScreen
import com.example.ui.screens.ProfileStatsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TutorialScreen
import com.example.ui.theme.TumbleTowerTheme
import com.example.viewmodel.TumbleTowerViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

object Routes {
    const val SPLASH = "splash"
    const val TUTORIAL = "tutorial"
    const val HOME = "home"
    const val MODES = "modes"
    const val CAMPAIGN = "campaign"
    const val GAMEPLAY = "gameplay"
    const val PHOTO = "photo"
    const val SHOP = "shop"
    const val REWARDS = "rewards"
    const val LEADERBOARD = "leaderboard"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TumbleTowerRoot()
        }
    }
}

@Composable
private fun TumbleTowerRoot() {
    val context = LocalContext.current
    val firebaseAvailable = remember(context) {
        FirebaseApp.getApps(context).isNotEmpty()
    }
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        TumbleTowerTheme(darkTheme = true) {
            SplashScreen(onFinished = { showSplash = false })
        }
        return
    }

    if (!firebaseAvailable) {
        val app = context.applicationContext as android.app.Application
        val vm: TumbleTowerViewModel = viewModel(
            key = "local_player",
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TumbleTowerViewModel(app, "local_player", firebaseEnabled = false) as T
                }
            }
        )
        AuthenticatedAppContent(vm = vm, currentUserId = "local_player")
        return
    }

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        TumbleTowerTheme(darkTheme = true) {
            AuthGateScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            val app = context.applicationContext as android.app.Application
            val vm: TumbleTowerViewModel = viewModel(
                key = user.uid,
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return TumbleTowerViewModel(app, user.uid, firebaseEnabled = true) as T
                    }
                }
            )
            AuthenticatedAppContent(vm = vm, currentUserId = user.uid)
        }
    }
}

@Composable
private fun AuthenticatedAppContent(
    vm: TumbleTowerViewModel,
    currentUserId: String
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profile by vm.userProfile.collectAsStateWithLifecycle()
    val powerUps by vm.powerUps.collectAsStateWithLifecycle()
    val ownedCosmetics by vm.ownedCosmetics.collectAsStateWithLifecycle()
    val cachedLevels by vm.cachedLevels.collectAsStateWithLifecycle()
    val missions by vm.missions.collectAsStateWithLifecycle()
    val lastSpinDate by vm.lastSpinDate.collectAsStateWithLifecycle()
    val isOnline by vm.isOnline.collectAsStateWithLifecycle()
    val physicsState by vm.physicsState.collectAsStateWithLifecycle()
    val selectedBoard by vm.selectedLeaderboardBoard.collectAsStateWithLifecycle()
    val leaderboardState by vm.leaderboardState.collectAsStateWithLifecycle()
    val toastMessage by vm.toastMessage.collectAsStateWithLifecycle()

    val baseContext = LocalContext.current
    val localizedContext = remember(settings.languageCode, baseContext) {
        createLocalizedContext(baseContext, settings.languageCode)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> vm.audioEngine.pauseAudio()
                Lifecycle.Event.ON_RESUME -> vm.audioEngine.resumeAudio()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            snackbarHostState.showSnackbar(toastMessage!!)
            vm.clearToast()
        }
    }

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val credentialManager = remember(baseContext) {
        runCatching { CredentialManager.create(baseContext) }.getOrNull()
    }
    var showExitDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalContext provides localizedContext) {
        TumbleTowerTheme(darkTheme = settings.darkTheme) {
            Box(modifier = Modifier.fillMaxSize()) {
                val startRoute = if (settings.tutorialCompleted) Routes.HOME else Routes.TUTORIAL

                NavHost(navController = navController, startDestination = startRoute) {
                    composable(Routes.TUTORIAL) {
                        TutorialScreen(
                            selectedLanguage = settings.languageCode,
                            onLanguageSelected = { code ->
                                vm.updateSettings { it.copy(languageCode = code) }
                            },
                            onComplete = {
                                vm.updateSettings { it.copy(tutorialCompleted = true) }
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.TUTORIAL) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.HOME) {
                        BackHandler { showExitDialog = true }
                        HomeScreen(
                            profile = profile,
                            isOnline = isOnline,
                            onPlayQuick = {
                                vm.startCampaignLevel(vm.highestUnlockedLevel())
                                navController.navigate(Routes.GAMEPLAY)
                            },
                            onOpenCampaign = { navController.navigate(Routes.CAMPAIGN) },
                            onStartMode = { mode ->
                                vm.startGameMode(mode)
                                navController.navigate(Routes.GAMEPLAY)
                            },
                            onOpenAllModes = { navController.navigate(Routes.MODES) },
                            onOpenRewards = { navController.navigate(Routes.REWARDS) },
                            onOpenShop = { navController.navigate(Routes.SHOP) },
                            onOpenLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onOpenProfile = { navController.navigate(Routes.PROFILE) }
                        )
                    }

                    composable(Routes.MODES) {
                        GameModesScreen(
                            onBack = { navController.popBackStack() },
                            onOpenCampaign = { navController.navigate(Routes.CAMPAIGN) },
                            onStartMode = { mode, names ->
                                vm.startGameMode(mode, names)
                                navController.navigate(Routes.GAMEPLAY)
                            }
                        )
                    }

                    composable(Routes.CAMPAIGN) {
                        CampaignMapScreen(
                            cachedLevels = cachedLevels,
                            highestUnlocked = vm.highestUnlockedLevel(),
                            onBack = { navController.popBackStack() },
                            onSelectLevel = { lvl ->
                                vm.startCampaignLevel(lvl)
                                navController.navigate(Routes.GAMEPLAY)
                            }
                        )
                    }

                    composable(Routes.GAMEPLAY) {
                        GameplayScreen(
                            state = physicsState,
                            settings = settings,
                            powerUps = powerUps,
                            equippedSkin = profile.equippedSkin,
                            equippedTheme = profile.equippedTheme,
                            totalCoins = profile.coins,
                            onStartPull = vm::startPullBlock,
                            onUpdatePull = vm::updatePullDrag,
                            onReleasePull = vm::releasePull,
                            onUpdatePlacementAim = vm::updatePlacementAim,
                            onPlaceBlock = vm::placeHeldBlock,
                            onUsePowerUp = vm::usePowerUp,
                            onToggleXray = vm::setXray,
                            onDismissBossIntro = vm::dismissBossIntro,
                            onTogglePause = vm::setPaused,
                            onWatchAdDoubleCoins = vm::watchAdForDoubleCoins,
                            onWatchAdRevive = vm::watchAdForRevive,
                            onReplayCollapse = vm::replayCollapse,
                            onOpenPhotoMode = { navController.navigate(Routes.PHOTO) },
                            onNextLevel = {
                                val next = (physicsState.config.levelNumber + 1).coerceAtMost(210)
                                vm.startCampaignLevel(next)
                            },
                            onRetry = { vm.startGameSession(physicsState.config) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onExitHome = {
                                vm.stopPhysicsLoop()
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.PHOTO) {
                        PhotoModeScreen(
                            physicsState = physicsState,
                            onBack = { navController.popBackStack() },
                            onCaptureAndShare = vm::captureAndSharePhoto
                        )
                    }

                    composable(Routes.SHOP) {
                        ShopScreen(
                            profile = profile,
                            powerUps = powerUps,
                            ownedCosmetics = ownedCosmetics,
                            storeCatalog = vm.billingManager.catalog,
                            onBack = { navController.popBackStack() },
                            onBuyOrEquipCosmetic = vm::buyOrEquipCosmetic,
                            onBuyPowerUp = vm::buyPowerUpWithGems,
                            onWatchAdFreePowerUp = vm::watchAdForFreePowerUp,
                            onPurchaseSku = vm::purchaseStoreProduct
                        )
                    }

                    composable(Routes.REWARDS) {
                        DailyRewardsAndMissionsScreen(
                            profile = profile,
                            lastSpinDate = lastSpinDate,
                            missions = missions,
                            onBack = { navController.popBackStack() },
                            onClaimDailyReward = vm::claimDailyReward,
                            onSpinWin = vm::grantSpinReward,
                            onWatchAdFreeSpin = { vm.adManager.showRewardedAd(RewardPlacement.FREE_SPIN) },
                            onClaimMission = vm::claimMission
                        )
                    }

                    composable(Routes.LEADERBOARD) {
                        LeaderboardScreen(
                            selectedBoard = selectedBoard,
                            leaderboardState = leaderboardState,
                            currentUserId = currentUserId,
                            adsRemoved = profile.adsRemoved,
                            onSelectBoard = vm::selectLeaderboardBoard,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.PROFILE) {
                        ProfileStatsScreen(
                            profile = profile,
                            onBack = { navController.popBackStack() },
                            onSaveProfile = vm::updateProfileIdentity,
                            onSignOut = {
                                signOutUser(baseContext, credentialManager, scope)
                            }
                        )
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            adsRemoved = profile.adsRemoved,
                            onBack = { navController.popBackStack() },
                            onUpdateSettings = vm::updateSettings,
                            onLanguageChange = { code ->
                                vm.updateSettings { it.copy(languageCode = code) }
                            },
                            onRestorePurchases = vm::restorePurchases,
                            onResetProgress = vm::resetAllProgress
                        )
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                )

                if (showExitDialog) {
                    val activity = baseContext as? Activity
                    AlertDialog(
                        onDismissRequest = { showExitDialog = false },
                        title = { Text("Exit Tumble Tower?") },
                        text = { Text("Are you sure you want to leave the tower behind?") },
                        confirmButton = {
                            Button(onClick = { activity?.finish() }) { Text("Exit") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExitDialog = false }) { Text("Stay") }
                        }
                    )
                }
            }
        }
    }
}

private fun createLocalizedContext(base: Context, langCode: String): Context {
    return runCatching {
        val locale = Locale.forLanguageTag(langCode.ifBlank { "en" })
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        base.createConfigurationContext(config)
    }.getOrDefault(base)
}
