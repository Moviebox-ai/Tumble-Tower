package com.example.game.physics

import com.example.audio.SoundEffect
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Multi-layer rigid-body structural equilibrium & physics solver for Tumble Tower: Skyfall.
 *
 * Core algorithms:
 * 1. Bottom-to-top & top-to-bottom Cumulative Center-of-Mass (COM) & Support Envelope solver:
 *    computes exact [minSupport, maxSupport] across both alternating X and Z axes at every layer,
 *    accounting for missing blocks, partial pull-out footprints, Ice melting shrinkage, and Magnet fields.
 * 2. Overturning Torque & Hinge Collapse solver:
 *    evaluates critical hinge layer where overhead mass overhang torque exceeds stabilizing resistance,
 *    toppling the upper sub-tower above the hinge and scattering lower blocks on impact.
 * 3. Friction-weighted Drag & Steady Hand impulse model:
 *    overhead weight scales static breakaway resistance; lateral jitter & yanking inject real shear torque.
 * 4. Special block solvers:
 *    Ice melting slip, Rubber damped harmonic bounce, Magnet field attraction, Gold COM weighting,
 *    TNT radial distance blast impulse, and Glass compressive load fracture.
 */
class TowerPhysicsEngine(
    private val onSoundEffect: (SoundEffect) -> Unit = {},
    private val onHaptic: (Long, Int) -> Unit = { _, _ -> }
) {
    private companion object {
        const val SLOT_SPACING = 0.34f
        const val BLOCK_HALF_SPAN = 0.165f
        const val BEAM_HALF_LEN = 0.51f
        const val CRITICAL_TILT_DEG = 13.5f
        const val CRUSH_RATIO = 1.0f
        const val CRUSH_GRACE_SECONDS = 0.45f
        const val DAREDEVIL_MIN_RISK = 0.45f
        const val DAREDEVIL_SURVIVAL_SECONDS = 1.1f
        const val CALM_DROP_MAX_SWAY = 0.20f
        const val ROUGH_DROP_MIN_SWAY = 0.65f
        const val XRAY_DRAIN_PER_SECOND = 9f
        const val FOCUS_REGEN_PER_SECOND = 1.5f
    }

    private data class StructuralAnalysis(
        val comX: Float,
        val comZ: Float,
        val weakestLayer: Int,
        val minMargin: Float,
        val overhangTorqueX: Float,
        val overhangTorqueZ: Float,
        val missingSupportLayer: Int?
    )

    private var rng = Random(System.currentTimeMillis())
    private var blocks = mutableListOf<BlockState>()
    private var particles = mutableListOf<ParticleState>()
    private val historyStack = ArrayDeque<TowerSnapshot>()
    private val replayFrames = mutableListOf<List<BlockState>>()
    private var replayCursor = 0

    var config = GameSessionConfig(GameMode.CLASSIC)
        private set
    var phase = PlayPhase.PLAYING
        private set

    private var nextBlockId = 1
    private var topLayerIndex = 9
    private var nextPlaceSlot = 0
    private var score = 0L
    private var heightBonus = 0L
    private var comboBonus = 0L
    private var coinsEarned = 0
    private var currentCombo = 0
    private var movesCompleted = 0
    private var perfectPulls = 0
    private var totalPulls = 0

    private var stability = 0.94f
    private var towerTiltDeg = 0f
    private var towerTiltZDeg = 0f
    private var angularVel = 0f
    private var angularVelZ = 0f
    private var comX = 0f
    private var weakestLayerIndex = 0
    private var collapseHingeLayer = 0

    private var activeEvent = LiveEventType.NONE
    private var incomingEvent: LiveEventType? = null
    private var eventTimerSec = 10f
    private var eventActiveSec = 0f
    private var windDirection = 1f
    private var quakePhase = 0f
    private var screenShake = 0f

    private var freezeTimer = 0f
    private var shieldArmed = false
    private var glueArmed = false
    private var timeAttackTimer = 90f
    private var collapseSlowMoTimer = 0f
    private var heartbeatCooldown = 0f

    // --- Signature mechanic: readable tower (Stress Vision, Risk Wager, Calm Drop) ---
    private var xrayActive = false
    private var focus = 100f
    private var stressReport: StressReport = StressReport.EMPTY
    private var riskAssessments: Map<Int, PullAssessment> = emptyMap()
    private var riskRefreshTimer = 0f
    private var riskDirty = true
    private var activePullAssessment: PullAssessment? = null
    private var pendingWager: PullAssessment? = null
    private var pendingWagerTimer = 0f
    private var daredevilStreak = 0
    private var daredevilCount = 0
    private var calmDropCount = 0
    private var creakCooldown = 0f
    private val crushTimers = HashMap<Int, Float>()

    private var activePullBlockId: Int? = null
    private var holdingBlockType: BlockType? = null
    private var aimSlot = 1
    private var aimOffsetNorm = 0f
    private var steadyHandPrecision = 1.0f
    private var dragShakeAccumulator = 0f

    private var currentPlayerIndex = 0
    private var ghostBestHeight = 14
    private var statusBanner: String? = null
    private var statusBannerTimer = 0f

    fun startNewSession(newConfig: GameSessionConfig, ghostHeight: Int): PhysicsUiState {
        config = newConfig
        rng = Random(newConfig.seed)
        blocks.clear()
        particles.clear()
        historyStack.clear()
        replayFrames.clear()
        replayCursor = 0
        nextBlockId = 1
        score = 0L
        heightBonus = 0L
        comboBonus = 0L
        coinsEarned = 0
        currentCombo = 0
        movesCompleted = 0
        perfectPulls = 0
        totalPulls = 0
        freezeTimer = 0f
        shieldArmed = false
        glueArmed = false
        timeAttackTimer = 90f
        collapseSlowMoTimer = 0f
        activePullBlockId = null
        holdingBlockType = null
        aimSlot = 1
        aimOffsetNorm = 0f
        steadyHandPrecision = 1.0f
        dragShakeAccumulator = 0f
        currentPlayerIndex = 0
        ghostBestHeight = ghostHeight.coerceAtLeast(newConfig.initialLayers + 2)
        statusBanner = null
        statusBannerTimer = 0f
        xrayActive = false
        focus = 100f
        stressReport = StressReport.EMPTY
        riskAssessments = emptyMap()
        riskRefreshTimer = 0f
        riskDirty = true
        activePullAssessment = null
        pendingWager = null
        pendingWagerTimer = 0f
        daredevilStreak = 0
        daredevilCount = 0
        calmDropCount = 0
        creakCooldown = 0f
        crushTimers.clear()

        activeEvent = LiveEventType.NONE
        incomingEvent = pickNextEvent()
        eventTimerSec = if (newConfig.mode == GameMode.SURVIVAL) 5f else 9f
        eventActiveSec = 0f
        windDirection = if (rng.nextBoolean()) 1f else -1f
        screenShake = 0f

        towerTiltDeg = when (newConfig.bossType) {
            BossType.LEANING_TOWER -> 4.5f
            else -> 0f
        }
        towerTiltZDeg = 0f
        angularVel = 0f
        angularVelZ = 0f
        comX = 0f
        weakestLayerIndex = 0
        collapseHingeLayer = 0
        stability = if (newConfig.bossType == BossType.LEANING_TOWER) 0.72f else 0.94f

        buildInitialTower(newConfig)
        phase = if (newConfig.bossType != BossType.NONE) PlayPhase.BOSS_INTRO else PlayPhase.PLAYING
        return buildUiState()
    }

    private fun buildInitialTower(cfg: GameSessionConfig) {
        val layers = cfg.initialLayers
        topLayerIndex = layers - 1
        nextPlaceSlot = 0
        for (layer in 0 until layers) {
            val spiralOffset = when (cfg.bossType) {
                BossType.SPIRAL_SKYSCRAPER -> sin(layer * 0.55f) * 0.14f
                BossType.LEANING_TOWER -> layer * 0.022f
                else -> 0f
            }
            val isEven = layer % 2 == 0
            val slotsInLayer = if (cfg.bossType == BossType.PYRAMID_OF_DOOM && layer > layers - 3) 2 else 3
            for (slot in 0 until slotsInLayer) {
                val offsetCoord = (slot - (slotsInLayer - 1) / 2f) * SLOT_SPACING + spiralOffset
                val bx = if (isEven) offsetCoord else spiralOffset
                val bz = if (isEven) 0f else offsetCoord
                val type = chooseBlockType(cfg, layer)
                blocks.add(
                    BlockState(
                        id = nextBlockId++,
                        layer = layer,
                        slot = slot,
                        type = type,
                        x = bx,
                        z = bz
                    )
                )
            }
        }
    }

    private fun chooseBlockType(cfg: GameSessionConfig, layer: Int): BlockType {
        if (layer == 0 || layer == cfg.initialLayers - 1) return BlockType.WOOD
        val roll = rng.nextFloat()
        val world = cfg.worldId
        return when {
            cfg.bossType == BossType.PYRAMID_OF_DOOM && roll < 0.25f -> BlockType.GOLD
            cfg.bossType == BossType.PYRAMID_OF_DOOM && roll < 0.42f -> BlockType.BOMB
            world == 1 -> if (roll < 0.76f) BlockType.WOOD else if (roll < 0.88f) BlockType.RUBBER else BlockType.GOLD
            world == 2 -> if (roll < 0.55f) BlockType.WOOD else if (roll < 0.75f) BlockType.GOLD else if (roll < 0.88f) BlockType.GLASS else BlockType.BOMB
            world == 3 -> if (roll < 0.45f) BlockType.ICE else if (roll < 0.75f) BlockType.WOOD else if (roll < 0.88f) BlockType.GLASS else BlockType.MAGNET
            world == 4 -> if (roll < 0.38f) BlockType.MAGNET else if (roll < 0.65f) BlockType.GLASS else if (roll < 0.84f) BlockType.RUBBER else BlockType.GOLD
            else -> if (roll < 0.32f) BlockType.BOMB else if (roll < 0.58f) BlockType.GOLD else if (roll < 0.80f) BlockType.WOOD else BlockType.RUBBER
        }
    }

    fun dismissBossIntro(): PhysicsUiState {
        if (phase == PlayPhase.BOSS_INTRO) {
            phase = PlayPhase.PLAYING
        }
        return buildUiState()
    }

    fun setPaused(paused: Boolean): PhysicsUiState {
        if (paused && (phase == PlayPhase.PLAYING || phase == PlayPhase.HOLDING_TO_PLACE)) {
            phase = PlayPhase.PAUSED
        } else if (!paused && phase == PlayPhase.PAUSED) {
            phase = if (holdingBlockType != null) PlayPhase.HOLDING_TO_PLACE else PlayPhase.PLAYING
        }
        return buildUiState()
    }

    fun startPullBlock(blockId: Int): PhysicsUiState {
        if (phase != PlayPhase.PLAYING) return buildUiState()
        val idx = blocks.indexOfFirst { it.id == blockId && !it.isShattered }
        if (idx < 0) return buildUiState()
        val target = blocks[idx]
        if (target.layer >= topLayerIndex) {
            setBanner("Pull from lower layers, not the top!")
            return buildUiState()
        }
        if (target.isGlued) {
            setBanner("This block is Glued in place!")
            onSoundEffect(SoundEffect.WOOD_KNOCK)
            return buildUiState()
        }

        // Save undo snapshot before disturbing block
        pushSnapshot()
        // Structural what-if: how dangerous is removing this exact block right now?
        activePullAssessment = StructuralSolver.assessPull(blocks, blockId, swayEnergy(), stressReport)
        riskDirty = true
        activePullBlockId = blockId
        dragShakeAccumulator = 0f
        steadyHandPrecision = 1.0f
        val initialDir = if (target.slot == 0) -1f else 1f
        blocks[idx] = target.copy(
            isBeingPulled = true,
            pullProgress = max(target.pullProgress, 0.06f),
            pullDir = initialDir
        )
        onSoundEffect(
            when (target.type) {
                BlockType.ICE -> SoundEffect.ICE_CRACK
                BlockType.RUBBER -> SoundEffect.RUBBER_BOING
                BlockType.MAGNET -> SoundEffect.MAGNET_HUM
                else -> SoundEffect.WOOD_KNOCK
            }
        )
        onHaptic(18L, 70)
        return buildUiState()
    }

    /**
     * Weighted friction + overhead pressure drag solver.
     * Supports directional pulling (positive delta extracts, negative pushes back) and
     * computes real upper-tower shear wobble when yanking a heavy/sticky block violently.
     */
    fun updatePullDrag(deltaPull: Float, lateralJitter: Float, sensitivity: Float): PhysicsUiState {
        val blockId = activePullBlockId ?: return buildUiState()
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx < 0) return buildUiState()
        val b = blocks[idx]

        // Calculate weight pressing down on this block's layer
        val layerCount = blocks.count { it.layer == b.layer && !it.isShattered }.coerceAtLeast(1)
        val overheadMass = blocks.filter { it.layer > b.layer && !it.isShattered }.sumOf { it.type.mass.toDouble() }.toFloat()
        val loadPerBlock = overheadMass / layerCount

        val rainMult = if (activeEvent == LiveEventType.RAIN) 0.65f else 1f
        val effectiveFriction = (b.type.baseFriction * rainMult * (1f - b.meltProgress * 0.45f)).coerceIn(0.15f, 1.0f)
        // Heavier overhead load + higher friction increases static pull resistance
        val pullResistance = (1.18f / (1f + loadPerBlock * effectiveFriction * 0.055f)).coerceIn(0.45f, 1.45f)

        val newProgress = (b.pullProgress + deltaPull * sensitivity * pullResistance).coerceIn(0f, 1.05f)
        val jitterSpeed = abs(lateralJitter)
        // Smoothness penalty grows quickly on sharp lateral shake or rapid yank
        val yankPenalty = if (abs(deltaPull) > 0.14f) (abs(deltaPull) - 0.14f) * 1.4f else 0f
        dragShakeAccumulator = (dragShakeAccumulator * 0.86f + jitterSpeed * 0.014f + yankPenalty).coerceIn(0f, 1f)
        steadyHandPrecision = (1f - dragShakeAccumulator).coerceIn(0.05f, 1f)

        // Friction shear impulse transfers to upper tower & block angle
        val shearCoupling = effectiveFriction * (0.6f + loadPerBlock * 0.04f)
        if (jitterSpeed > 3.5f || abs(deltaPull) > 0.12f) {
            val impulse = ((lateralJitter * 0.011f) + deltaPull * b.pullDir * 1.8f) * shearCoupling
            if (b.layer % 2 == 0) {
                angularVel += impulse.coerceIn(-1.1f, 1.1f)
            } else {
                angularVelZ += impulse.coerceIn(-1.1f, 1.1f)
            }
        }

        // Rubber blocks vibrate slightly when dragged
        val bounce = if (b.type == BlockType.RUBBER) sin(newProgress * 18f) * 0.18f else b.bouncePhase
        blocks[idx] = b.copy(
            pullProgress = newProgress,
            isBeingPulled = true,
            bouncePhase = bounce
        )

        if (newProgress >= 1.0f) {
            completeBlockExtraction(idx)
        }
        return buildUiState()
    }

    fun releasePull(): PhysicsUiState {
        val blockId = activePullBlockId ?: return buildUiState()
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx >= 0) {
            val b = blocks[idx]
            if (b.pullProgress >= 0.72f) {
                completeBlockExtraction(idx)
            } else {
                // Partial pull leaves subtle physical shift if disturbed past 25%, or settles cleanly
                val nudge = if (b.pullProgress > 0.25f) b.pullDir * (b.pullProgress - 0.25f) * 0.08f else 0f
                val isEven = b.layer % 2 == 0
                blocks[idx] = b.copy(
                    isBeingPulled = false,
                    pullProgress = if (b.pullProgress > 0.25f) b.pullProgress * 0.35f else 0f,
                    x = if (isEven) b.x + nudge * 0.35f else b.x + nudge,
                    z = if (isEven) b.z + nudge else b.z + nudge * 0.35f
                )
                activePullBlockId = null
                activePullAssessment = null
                steadyHandPrecision = 1.0f
                riskDirty = true
            }
        } else {
            activePullBlockId = null
            activePullAssessment = null
        }
        return buildUiState()
    }

    private fun completeBlockExtraction(idx: Int) {
        val removed = blocks.removeAt(idx)
        activePullBlockId = null
        holdingBlockType = removed.type
        totalPulls++

        // Immediately prepare top placement slot aim
        val topFull = blocks.count { it.layer == topLayerIndex && !it.isShattered } >= 3
        val targetLayer = if (topFull) topLayerIndex + 1 else topLayerIndex
        val usedSlots = blocks.filter { it.layer == targetLayer && !it.isShattered }.map { it.slot }.toSet()
        aimSlot = (0..2).firstOrNull { it !in usedSlots } ?: nextPlaceSlot
        aimOffsetNorm = 0f

        // Evaluate structural equilibrium immediately after block leaves its layer
        val structure = analyzeTowerStructure()
        if (structure.missingSupportLayer != null) {
            activePullAssessment = null
            collapseHingeLayer = structure.missingSupportLayer
            triggerCollapseOrShield("Layer ${structure.missingSupportLayer + 1} lost all support!")
            return
        }
        if (structure.overhangTorqueX > 0.2f || structure.overhangTorqueZ > 0.2f) {
            angularVel += structure.overhangTorqueX * 1.4f
            angularVelZ += structure.overhangTorqueZ * 1.4f
        }

        // Rubber extraction snaps neighboring layers slightly
        if (removed.type == BlockType.RUBBER) {
            applyLayerBounce(removed.layer + 1, 0.85f)
        }

        // Steady Hand precision check
        if (steadyHandPrecision >= 0.72f) {
            perfectPulls++
            currentCombo++
            val bonus = 50L * currentCombo
            comboBonus += bonus
            score += 120L + bonus
            coinsEarned += removed.type.coinValue + currentCombo * 2
            onSoundEffect(SoundEffect.COMBO_CHIME)
            setBanner("Steady Hand x$currentCombo! Aim & release on top")
        } else {
            currentCombo = 0
            score += 90L
            coinsEarned += removed.type.coinValue
            setBanner("Extracted! Drag to aim & drop on top")
        }

        if (removed.type == BlockType.GOLD) {
            onSoundEffect(SoundEffect.COIN_DING)
        }

        // Risk Wager: surviving a pull the solver rated dangerous pays out, scaled by how deadly it was.
        val wager = activePullAssessment?.takeIf { it.blockId == removed.id }
        activePullAssessment = null
        riskDirty = true
        if (wager != null) {
            if (wager.risk >= DAREDEVIL_MIN_RISK) {
                // Payout is deferred: the tower must still be standing after the crush/settle window.
                pendingWager = wager
                pendingWagerTimer = 0f
            } else if (wager.risk < StructuralSolver.SAFE_BELOW) {
                daredevilStreak = 0
            }
        }
        if (steadyHandPrecision >= 0.72f) focus = (focus + 12f).coerceAtMost(100f)

        phase = PlayPhase.HOLDING_TO_PLACE
        spawnBurst(removed.x, removed.layer.toFloat(), 6, "spark")
    }

    fun updatePlacementAim(slotChoice: Int, fineOffsetNorm: Float): PhysicsUiState {
        if (phase != PlayPhase.HOLDING_TO_PLACE) return buildUiState()
        val topFull = blocks.count { it.layer == topLayerIndex && !it.isShattered } >= 3
        val targetLayer = if (topFull) topLayerIndex + 1 else topLayerIndex
        val usedSlots = blocks.filter { it.layer == targetLayer && !it.isShattered }.map { it.slot }.toSet()

        aimSlot = if (slotChoice in 0..2 && slotChoice !in usedSlots) {
            slotChoice
        } else {
            (0..2).firstOrNull { it !in usedSlots } ?: aimSlot
        }
        aimOffsetNorm = fineOffsetNorm.coerceIn(-0.09f, 0.09f)
        return buildUiState()
    }

    fun placeHeldBlock(slotChoice: Int = aimSlot, precisionDropOffset: Float = aimOffsetNorm): PhysicsUiState {
        val blockType = holdingBlockType ?: return buildUiState()
        if (phase != PlayPhase.HOLDING_TO_PLACE) return buildUiState()

        val targetLayerCount = blocks.count { it.layer == topLayerIndex && !it.isShattered }
        val placeLayer = if (targetLayerCount >= 3) topLayerIndex + 1 else topLayerIndex
        topLayerIndex = maxOf(topLayerIndex, placeLayer)
        val usedSlots = blocks.filter { it.layer == placeLayer && !it.isShattered }.map { it.slot }.toSet()
        val chosenSlot = if (slotChoice !in usedSlots && slotChoice in 0..2) {
            slotChoice
        } else {
            (0..2).firstOrNull { it !in usedSlots } ?: 1
        }
        nextPlaceSlot = (chosenSlot + 1) % 3

        val isEven = placeLayer % 2 == 0
        val clampedOffset = precisionDropOffset.coerceIn(-0.09f, 0.09f)
        val slotCoord = (chosenSlot - 1) * SLOT_SPACING + clampedOffset
        val bx = if (isEven) slotCoord else clampedOffset * 0.4f
        val bz = if (isEven) 0f else slotCoord

        val isPerfectDrop = abs(clampedOffset) < 0.03f
        val swayBeforeDrop = swayEnergy()
        val newBlock = BlockState(
            id = nextBlockId++,
            layer = placeLayer,
            slot = chosenSlot,
            type = blockType,
            x = bx,
            z = bz,
            y = 0.25f, // slight drop settle height
            bouncePhase = if (blockType == BlockType.RUBBER) 1.0f else 0.18f,
            isGlued = glueArmed
        )
        glueArmed = false
        blocks.add(newBlock)
        holdingBlockType = null
        aimOffsetNorm = 0f
        movesCompleted++

        // Block-type placement physics reactions
        when (blockType) {
            BlockType.RUBBER -> {
                applyLayerBounce(placeLayer, 1.0f)
                angularVel += (rng.nextFloat() - 0.5f) * 3.4f
                onSoundEffect(SoundEffect.RUBBER_BOING)
            }
            BlockType.MAGNET -> {
                stability = (stability + 0.09f).coerceAtMost(0.98f)
                towerTiltDeg *= 0.78f
                towerTiltZDeg *= 0.78f
                angularVel *= 0.5f
                onSoundEffect(SoundEffect.MAGNET_HUM)
            }
            BlockType.GOLD -> {
                // Heavy gold shifts angular momentum according to slot offset!
                if (isEven) angularVel += bx * 4.5f else angularVelZ += bz * 4.5f
                coinsEarned += 25
                onSoundEffect(SoundEffect.COIN_DING)
            }
            else -> {
                if (isEven) angularVel += clampedOffset * 6f else angularVelZ += clampedOffset * 6f
                onSoundEffect(SoundEffect.WOOD_KNOCK)
            }
        }

        if (isPerfectDrop) {
            comboBonus += 40L
            score += 40L
        }

        // Calm Drop: timing the release to the tower's sway rewards patience, a rough drop feeds the wobble.
        riskDirty = true
        if (swayBeforeDrop <= CALM_DROP_MAX_SWAY) {
            calmDropCount++
            angularVel *= 0.45f
            angularVelZ *= 0.45f
            stability = (stability + 0.04f).coerceAtMost(0.99f)
            comboBonus += 80L
            score += 80L
            focus = (focus + 15f).coerceAtMost(100f)
            setBanner("CALM DROP  +80")
        } else if (swayBeforeDrop >= ROUGH_DROP_MIN_SWAY) {
            angularVel += (rng.nextFloat() - 0.5f) * 2.4f
            angularVelZ += (rng.nextFloat() - 0.5f) * 1.6f
            setBanner("Rough drop - wait for the sway to settle")
        }

        heightBonus += (topLayerIndex + 1) * 15L
        score += 150L + (topLayerIndex * 10L)
        spawnBurst(bx, placeLayer.toFloat(), 10, if (blockType == BlockType.GOLD) "confetti" else "dust")
        onHaptic(22L, 100)

        // Advance TNT fuses & check radial blasts
        tickBombFuses()

        // Pass & Play turn rotation
        if (config.mode == GameMode.PASS_AND_PLAY) {
            currentPlayerIndex = (currentPlayerIndex + 1) % config.playerNames.size.coerceAtLeast(1)
            setBanner("${config.playerNames[currentPlayerIndex]}'s Turn!")
        }

        // Check campaign win condition
        if (config.mode == GameMode.CAMPAIGN && movesCompleted >= config.targetMoves && phase != PlayPhase.COLLAPSING_SLOW_MO) {
            phase = PlayPhase.LEVEL_COMPLETE
            onSoundEffect(SoundEffect.LEVEL_WIN)
            spawnBurst(0f, topLayerIndex.toFloat(), 28, "confetti")
        } else if (phase == PlayPhase.HOLDING_TO_PLACE) {
            phase = PlayPhase.PLAYING
        }
        return buildUiState()
    }

    private fun applyLayerBounce(targetLayer: Int, strength: Float) {
        for (i in blocks.indices) {
            val b = blocks[i]
            if (!b.isShattered && abs(b.layer - targetLayer) <= 1) {
                val factor = if (b.layer == targetLayer) strength else strength * 0.55f
                blocks[i] = b.copy(bouncePhase = max(b.bouncePhase, factor))
            }
        }
    }

    private fun tickBombFuses() {
        val detonatedIds = mutableListOf<Int>()
        for (i in blocks.indices) {
            val b = blocks[i]
            if (b.type == BlockType.BOMB && !b.isShattered) {
                val nextFuse = b.bombFuseMoves - 1
                if (nextFuse <= 0) {
                    detonatedIds.add(b.id)
                } else {
                    blocks[i] = b.copy(bombFuseMoves = nextFuse)
                }
            }
        }
        if (detonatedIds.isNotEmpty()) {
            detonatedIds.forEach { id ->
                val idx = blocks.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    val b = blocks[idx]
                    blocks[idx] = b.copy(type = BlockType.WOOD, bombFuseMoves = 0)
                    // Radial distance-weighted blast solver on neighbors
                    for (j in blocks.indices) {
                        val other = blocks[j]
                        if (other.id == id || other.isShattered || other.isGlued) continue
                        val dLayer = abs(other.layer - b.layer).toFloat()
                        val dX = other.x - b.x
                        val dist = hypot(dX, dLayer * 0.55f)
                        if (dist <= 1.85f) {
                            val falloff = 1f / (1f + dist * dist * 1.8f)
                            if (other.type == BlockType.GLASS && dist <= 1.15f) {
                                blocks[j] = other.copy(isShattered = true)
                                spawnBurst(other.x, other.layer.toFloat(), 10, "spark")
                            } else {
                                val pushDir = if (abs(dX) > 0.05f) (if (dX > 0) 1f else -1f) else (if (rng.nextBoolean()) 1f else -1f)
                                blocks[j] = other.copy(
                                    x = (other.x + pushDir * 0.055f * falloff).coerceIn(-0.6f, 0.6f),
                                    angleDeg = other.angleDeg + pushDir * 4.5f * falloff,
                                    bouncePhase = max(other.bouncePhase, 0.7f * falloff)
                                )
                            }
                        }
                    }
                    screenShake = 1.05f
                    angularVel += (rng.nextFloat() - 0.5f) * 8.2f
                    stability -= 0.14f
                    spawnBurst(b.x, b.layer.toFloat(), 18, "ember")
                    onSoundEffect(SoundEffect.TNT_BOOM)
                    onHaptic(85L, 210)
                    setBanner("TNT Micro-Blast! Neighbors shaken!")
                }
            }
        }
    }

    fun activatePowerUp(type: PowerUpType): Pair<Boolean, PhysicsUiState> {
        if (phase != PlayPhase.PLAYING && phase != PlayPhase.HOLDING_TO_PLACE) {
            return false to buildUiState()
        }
        when (type) {
            PowerUpType.FREEZE -> {
                freezeTimer = 12f
                activeEvent = LiveEventType.NONE
                onSoundEffect(SoundEffect.FREEZE_CHILL)
                setBanner("Freeze Active! Events & melting paused (12s)")
            }
            PowerUpType.GLUE -> {
                glueArmed = true
                onSoundEffect(SoundEffect.UI_CLICK)
                setBanner("Glue Armed! Next placed block locks permanently")
            }
            PowerUpType.UNDO -> {
                val snap = historyStack.removeLastOrNull() ?: return false to buildUiState()
                blocks = snap.blocks.toMutableList()
                score = snap.score
                currentCombo = snap.combo
                movesCompleted = snap.movesCompleted
                topLayerIndex = snap.topLayerIndex
                nextPlaceSlot = snap.nextPlaceSlot
                towerTiltDeg = snap.towerTiltDeg * 0.6f
                towerTiltZDeg = snap.towerTiltZDeg * 0.6f
                angularVel = 0f
                angularVelZ = 0f
                stability = maxOf(snap.stability, 0.78f)
                activePullBlockId = null
                activePullAssessment = null
                pendingWager = null
                crushTimers.clear()
                riskDirty = true
                holdingBlockType = null
                phase = PlayPhase.PLAYING
                onSoundEffect(SoundEffect.UI_CLICK)
                setBanner("Undo Restored Last Move!")
            }
            PowerUpType.SHIELD -> {
                shieldArmed = true
                onSoundEffect(SoundEffect.COMBO_CHIME)
                setBanner("Shield Armed! Protects against 1 collapse")
            }
            PowerUpType.CRANE -> {
                // Smart Crane: picks safest inner block from a full 3-block layer to preserve balance
                val safestId = StructuralSolver.safestPulls(blocks, swayEnergy()).firstOrNull()?.blockId
                val candidate = blocks.firstOrNull { it.id == safestId }
                    ?: blocks.firstOrNull { !it.isShattered && !it.isGlued && it.layer < topLayerIndex }
                if (candidate == null) return false to buildUiState()
                pushSnapshot()
                blocks.removeAll { it.id == candidate.id }
                holdingBlockType = candidate.type
                phase = PlayPhase.HOLDING_TO_PLACE
                steadyHandPrecision = 1.0f
                placeHeldBlock(nextPlaceSlot, 0f)
                setBanner("Mini Crane ran the load solver and lifted the safest block!")
            }
        }
        onHaptic(20L, 120)
        return true to buildUiState()
    }

    fun reviveWithAd(): PhysicsUiState {
        if (phase != PlayPhase.GAME_OVER && phase != PlayPhase.COLLAPSING_SLOW_MO) return buildUiState()
        val snap = historyStack.lastOrNull()
        if (snap != null) {
            blocks = snap.blocks.toMutableList()
            topLayerIndex = snap.topLayerIndex
        } else {
            blocks.forEachIndexed { i, b ->
                blocks[i] = b.copy(y = 0f, vx = 0f, vy = 0f, vz = 0f, angleDeg = 0f, angularVel = 0f)
            }
        }
        towerTiltDeg = 0f
        towerTiltZDeg = 0f
        angularVel = 0f
        angularVelZ = 0f
        stability = 0.85f
        activeEvent = LiveEventType.NONE
        crushTimers.clear()
        riskDirty = true
        phase = PlayPhase.PLAYING
        setBanner("Tower Revived!")
        onSoundEffect(SoundEffect.COMBO_CHIME)
        return buildUiState().copy(revivedByAd = true)
    }

    fun startCollapseReplay(): PhysicsUiState {
        if (replayFrames.isEmpty()) return buildUiState()
        replayCursor = 0
        phase = PlayPhase.REPLAYING_COLLAPSE
        return buildUiState()
    }

    fun doubleCoinsWithAd(): PhysicsUiState {
        coinsEarned *= 2
        onSoundEffect(SoundEffect.COIN_DING)
        return buildUiState().copy(coinsDoubledByAd = true)
    }

    /** Toggle Stress Vision. It runs on Focus, which refills by pulling steadily, daredevil pulls and calm drops. */
    fun setXray(enabled: Boolean): PhysicsUiState {
        if (phase != PlayPhase.PLAYING && phase != PlayPhase.HOLDING_TO_PLACE) return buildUiState()
        if (enabled && focus < 8f) {
            setBanner("Focus empty - land steady pulls to recharge Stress Vision")
            return buildUiState()
        }
        xrayActive = enabled
        riskDirty = true
        onSoundEffect(SoundEffect.UI_CLICK)
        if (enabled) setBanner("Stress Vision ON - green holds, red is about to break")
        return buildUiState()
    }

    /** 0 = tower perfectly still, 1 = swaying hard. Drives Calm Drop timing and pull risk. */
    private fun swayEnergy(): Float {
        val motion = (abs(angularVel) + abs(angularVelZ)) / 6f
        val lean = hypot(towerTiltDeg, towerTiltZDeg * 0.7f) / CRITICAL_TILT_DEG * 0.5f
        return (motion + lean).coerceIn(0f, 1f)
    }

    private fun payoutDaredevil(wager: PullAssessment) {
        daredevilStreak++
        daredevilCount++
        val bonus = (wager.risk * 400f).toLong() * (1 + daredevilStreak.coerceAtMost(4))
        score += bonus
        comboBonus += bonus
        coinsEarned += 5 + (wager.risk * 20f).toInt()
        focus = (focus + 25f).coerceAtMost(100f)
        onSoundEffect(SoundEffect.COMBO_CHIME)
        onHaptic(30L, 140)
        spawnBurst(0f, topLayerIndex.toFloat(), 14, "confetti")
        setBanner("DAREDEVIL ${wager.riskPercent}%  +$bonus  (streak x$daredevilStreak)")
    }

    private fun stepStress(dt: Float) {
        stressReport = StructuralSolver.analyze(blocks)

        pendingWager?.let { wager ->
            pendingWagerTimer += dt
            if (pendingWagerTimer >= DAREDEVIL_SURVIVAL_SECONDS) {
                pendingWager = null
                payoutDaredevil(wager)
            }
        }

        riskRefreshTimer -= dt
        if (xrayActive && (riskDirty || riskRefreshTimer <= 0f)) {
            riskAssessments = StructuralSolver.riskMap(blocks, swayEnergy())
            riskRefreshTimer = 0.25f
            riskDirty = false
        } else if (!xrayActive && riskAssessments.isNotEmpty()) {
            riskAssessments = emptyMap()
        }

        if (xrayActive) {
            focus -= dt * XRAY_DRAIN_PER_SECOND
            if (focus <= 0f) {
                focus = 0f
                xrayActive = false
                setBanner("Focus spent - Stress Vision off")
            }
        } else {
            focus = (focus + dt * FOCUS_REGEN_PER_SECOND).coerceAtMost(100f)
        }

        // The tower talks: creaks + haptic ticks get faster as the worst block nears its limit.
        if (creakCooldown > 0f) creakCooldown -= dt
        val worst = stressReport.maxRatio
        if (worst > 0.85f && creakCooldown <= 0f) {
            onSoundEffect(SoundEffect.WOOD_KNOCK)
            onHaptic(12L, 60)
            creakCooldown = if (worst > 1f) 0.5f else 1.6f - (worst - 0.85f) * 6f
        }
    }

    fun step(dtRaw: Float, particlesEnabled: Boolean): PhysicsUiState {
        val dt = dtRaw.coerceIn(0.004f, 0.05f)
        if (statusBannerTimer > 0f) {
            statusBannerTimer -= dt
            if (statusBannerTimer <= 0f) statusBanner = null
        }
        if (screenShake > 0f) {
            screenShake = (screenShake - dt * 2.4f).coerceAtLeast(0f)
        }
        if (heartbeatCooldown > 0f) heartbeatCooldown -= dt

        if (particlesEnabled) {
            updateParticles(dt)
        } else {
            particles.clear()
        }

        when (phase) {
            PlayPhase.REPLAYING_COLLAPSE -> {
                if (replayFrames.isNotEmpty()) {
                    blocks = replayFrames[replayCursor].toMutableList()
                    replayCursor++
                    if (replayCursor >= replayFrames.size) {
                        phase = PlayPhase.GAME_OVER
                    }
                } else {
                    phase = PlayPhase.GAME_OVER
                }
                return buildUiState()
            }
            PlayPhase.COLLAPSING_SLOW_MO -> {
                stepSlowMoCollapse(dt * 0.28f)
                collapseSlowMoTimer -= dt
                if (collapseSlowMoTimer <= 0f) {
                    phase = PlayPhase.GAME_OVER
                }
                return buildUiState()
            }
            PlayPhase.PAUSED, PlayPhase.BOSS_INTRO, PlayPhase.LEVEL_COMPLETE, PlayPhase.GAME_OVER -> {
                return buildUiState()
            }
            PlayPhase.PLAYING, PlayPhase.HOLDING_TO_PLACE -> {
                if (config.mode == GameMode.TIME_ATTACK && freezeTimer <= 0f) {
                    timeAttackTimer -= dt
                    if (timeAttackTimer <= 0f) {
                        timeAttackTimer = 0f
                        phase = PlayPhase.LEVEL_COMPLETE
                        onSoundEffect(SoundEffect.LEVEL_WIN)
                        return buildUiState()
                    }
                }

                stepStress(dt)
                if (freezeTimer > 0f) {
                    freezeTimer = (freezeTimer - dt).coerceAtLeast(0f)
                } else {
                    stepLiveEvents(dt)
                    stepBlockLifecycles(dt)
                }

                stepMagnetAndBounceSettling(dt)
                stepTowerDynamics(dt)
            }
        }
        return buildUiState()
    }

    private fun stepLiveEvents(dt: Float) {
        if (activeEvent != LiveEventType.NONE) {
            eventActiveSec -= dt
            val heightFactor = (0.75f + topLayerIndex * 0.04f).coerceAtMost(1.8f)
            when (activeEvent) {
                LiveEventType.WIND -> {
                    val gust = (0.85f + config.baseWindStrength) * windDirection * heightFactor
                    angularVel += gust * dt * 2.4f
                    if (rng.nextFloat() < 0.25f) spawnBurst(-windDirection * 1.4f, (topLayerIndex * 0.6f), 1, "snow")
                }
                LiveEventType.EARTHQUAKE -> {
                    quakePhase += dt * 24f
                    screenShake = 0.45f
                    val tremorX = (sin(quakePhase) * 5.2f + sin(quakePhase * 1.73f) * 2.4f)
                    val tremorZ = cos(quakePhase * 1.31f) * 3.8f
                    angularVel += tremorX * dt
                    angularVelZ += tremorZ * dt
                }
                LiveEventType.RAIN -> {
                    if (rng.nextFloat() < 0.45f) spawnBurst((rng.nextFloat() - 0.5f) * 2.4f, topLayerIndex + 3f, 1, "rain")
                }
                LiveEventType.LOW_GRAVITY -> {
                    if (rng.nextFloat() < 0.15f) spawnBurst((rng.nextFloat() - 0.5f) * 2f, 1f, 1, "spark")
                }
                LiveEventType.NONE -> Unit
            }
            if (eventActiveSec <= 0f) {
                activeEvent = LiveEventType.NONE
                incomingEvent = pickNextEvent()
                eventTimerSec = if (config.mode == GameMode.SURVIVAL) {
                    (5f - (movesCompleted * 0.15f)).coerceAtLeast(2.5f)
                } else {
                    10f
                }
            }
        } else {
            eventTimerSec -= dt
            if (eventTimerSec <= 0f) {
                activeEvent = incomingEvent ?: pickNextEvent()
                incomingEvent = null
                eventActiveSec = if (config.mode == GameMode.SURVIVAL) 9f else 7f
                windDirection = if (rng.nextBoolean()) 1f else -1f
                setBanner("${activeEvent.title}! ${activeEvent.subtitle}")
            }
        }
    }

    private fun stepMagnetAndBounceSettling(dt: Float) {
        // Magnetic alignment solver + block drop settle / rubber harmonic damping
        val magnetLayers = blocks.filter { it.type == BlockType.MAGNET && !it.isShattered }
            .map { it.layer }
            .toSet()

        for (i in blocks.indices) {
            var b = blocks[i]
            if (b.isShattered) continue
            var updated = false

            // Settle newly placed block smoothly onto top surface
            if (b.y > 0.001f) {
                val ny = (b.y - dt * 3.2f).coerceAtLeast(0f)
                b = b.copy(y = ny)
                updated = true
            }

            // Damped harmonic bounce (Rubber vibration)
            if (abs(b.bouncePhase) > 0.005f) {
                val damped = b.bouncePhase * (1f - dt * 2.8f).coerceAtLeast(0f)
                b = b.copy(
                    bouncePhase = if (abs(damped) < 0.01f) 0f else damped,
                    angleDeg = if (!b.isBeingPulled) b.angleDeg * 0.92f + sin(damped * 24f) * damped * 2.2f else b.angleDeg
                )
                updated = true
            } else if (abs(b.angleDeg) > 0.05f && !b.isBeingPulled) {
                b = b.copy(angleDeg = b.angleDeg * (1f - dt * 1.4f).coerceAtLeast(0f))
                updated = true
            }

            // Magnet attraction nudges unglued blocks in same or adjacent layers toward ideal slot coordinates
            if (!b.isGlued && !b.isBeingPulled && (b.layer in magnetLayers || (b.layer - 1) in magnetLayers || (b.layer + 1) in magnetLayers)) {
                val isEven = b.layer % 2 == 0
                val idealCoord = (b.slot - 1) * SLOT_SPACING
                val targetX = if (isEven) idealCoord else 0f
                val targetZ = if (isEven) 0f else idealCoord
                val nx = b.x + (targetX - b.x) * (dt * 0.85f).coerceAtMost(0.5f)
                val nz = b.z + (targetZ - b.z) * (dt * 0.85f).coerceAtMost(0.5f)
                if (abs(nx - b.x) > 0.0002f || abs(nz - b.z) > 0.0002f) {
                    b = b.copy(x = nx, z = nz)
                    updated = true
                }
            }

            if (updated) blocks[i] = b
        }
    }

    private fun stepBlockLifecycles(dt: Float) {
        val worldMeltBonus = when (config.worldId) {
            2, 5 -> 1.45f // Desert/Lava melts Ice faster
            3 -> 0.75f    // Ice Land melts slower
            else -> 1.0f
        }

        val toShatter = mutableListOf<Int>()
        for (i in blocks.indices) {
            var b = blocks[i]
            if (b.isShattered) continue
            if (b.type == BlockType.ICE && !b.isGlued) {
                val nextMelt = (b.meltProgress + dt * 0.018f * worldMeltBonus).coerceAtMost(1f)
                b = b.copy(meltProgress = nextMelt)
                if (nextMelt >= 0.99f) {
                    val slipDir = if (b.slot == 0) -1f else if (b.slot == 2) 1f else if (rng.nextBoolean()) 1f else -1f
                    val isEven = b.layer % 2 == 0
                    val nx = if (isEven) (b.x + slipDir * 0.045f).coerceIn(-0.55f, 0.55f) else b.x
                    val nz = if (!isEven) (b.z + slipDir * 0.045f).coerceIn(-0.55f, 0.55f) else b.z
                    angularVel += slipDir * 3.8f
                    b = b.copy(meltProgress = 0.25f, x = nx, z = nz)
                    setBanner("Ice block melted & slipped!")
                    onSoundEffect(SoundEffect.ICE_CRACK)
                }
            }
            // Every block now reports the real load ratio from the structural solver.
            val ratio = (stressReport.blockRatios[b.id] ?: 0f).coerceIn(0f, 1.3f)
            if (abs(ratio - b.pressureLoad) > 0.003f) b = b.copy(pressureLoad = ratio)
            if (b.layer < topLayerIndex - 1) {
                if (b.type == BlockType.GLASS && !b.isGlued) {
                    if (ratio > 1.02f) toShatter.add(b.id)
                } else if (ratio > CRUSH_RATIO) {
                    val t = (crushTimers[b.id] ?: 0f) + dt
                    crushTimers[b.id] = t
                    if (t >= CRUSH_GRACE_SECONDS) toShatter.add(b.id)
                } else if (crushTimers.containsKey(b.id)) {
                    crushTimers.remove(b.id)
                }
            }
            blocks[i] = b
        }

        if (toShatter.isNotEmpty()) {
            val id = toShatter.first()
            val idx = blocks.indexOfFirst { it.id == id }
            if (idx >= 0) {
                val broken = blocks[idx]
                blocks[idx] = broken.copy(isShattered = true)
                crushTimers.remove(broken.id)
                riskDirty = true
                val isGlass = broken.type == BlockType.GLASS
                onSoundEffect(
                    when (broken.type) {
                        BlockType.GLASS -> SoundEffect.GLASS_SHATTER
                        BlockType.ICE -> SoundEffect.ICE_CRACK
                        else -> SoundEffect.WOOD_KNOCK
                    }
                )
                onHaptic(if (isGlass) 40L else 70L, if (isGlass) 150 else 200)
                spawnBurst(broken.x, broken.layer.toFloat(), 14, if (isGlass) "spark" else "dust")
                angularVel += (broken.slot - 1) * 5.2f
                angularVelZ += (broken.slot - 1) * 2.6f
                stability -= if (isGlass) 0.15f else 0.22f
                screenShake = maxOf(screenShake, 0.5f)
                setBanner(
                    if (isGlass) "Glass block shattered under pressure!"
                    else "${broken.type.displayName} crushed under the tower's weight!"
                )
            }
        }
    }

    /**
     * Top-down multi-layer Center-of-Mass & Support Envelope solver.
     * For every supporting layer L with overhead layers above it:
     *  - Overhead COM (X, Z) is checked against the remaining blocks' convex footprint [min, max].
     *  - Overturning torque rises steeply when overhead COM moves outside [min, max].
     */
    private fun analyzeTowerStructure(): StructuralAnalysis {
        val activeBlocks = blocks.filter { !it.isShattered }
        if (activeBlocks.isEmpty()) {
            return StructuralAnalysis(0f, 0f, 0, 0f, 0f, 0f, 0)
        }

        var globalComX = 0f
        var globalComZ = 0f
        var globalMass = 0f
        activeBlocks.forEach { b ->
            val m = b.type.mass * (1f - b.pullProgress * 0.5f).coerceAtLeast(0.35f)
            globalComX += b.x * m
            globalComZ += b.z * m
            globalMass += m
        }
        globalComX /= globalMass.coerceAtLeast(0.1f)
        globalComZ /= globalMass.coerceAtLeast(0.1f)

        val blocksByLayer = activeBlocks.groupBy { it.layer }
        var overheadMass = 0f
        var overheadMomentX = 0f
        var overheadMomentZ = 0f

        var minMargin = 1.0f
        var weakestLayer = 0
        var worstTorqueX = 0f
        var worstTorqueZ = 0f
        var missingLayer: Int? = null

        for (layer in topLayerIndex downTo 0) {
            val layerBlocks = blocksByLayer[layer].orEmpty()
            if (layer < topLayerIndex && layerBlocks.isEmpty()) {
                missingLayer = layer
            }

            if (overheadMass > 0.1f && layerBlocks.isNotEmpty()) {
                val comAboveX = overheadMomentX / overheadMass
                val comAboveZ = overheadMomentZ / overheadMass
                val isEven = layer % 2 == 0

                // Compute layer support interval [minX, maxX] and [minZ, maxZ]
                var sMinX = Float.POSITIVE_INFINITY
                var sMaxX = Float.NEGATIVE_INFINITY
                var sMinZ = Float.POSITIVE_INFINITY
                var sMaxZ = Float.NEGATIVE_INFINITY

                layerBlocks.forEach { b ->
                    val contactShrink = (1f - b.pullProgress * 0.72f - b.meltProgress * 0.25f).coerceIn(0.25f, 1f)
                    val halfX = if (isEven) BLOCK_HALF_SPAN * contactShrink else BEAM_HALF_LEN * contactShrink
                    val halfZ = if (isEven) BEAM_HALF_LEN * contactShrink else BLOCK_HALF_SPAN * contactShrink
                    sMinX = min(sMinX, b.x - halfX)
                    sMaxX = max(sMaxX, b.x + halfX)
                    sMinZ = min(sMinZ, b.z - halfZ)
                    sMaxZ = max(sMaxZ, b.z + halfZ)
                }

                val overhangX = when {
                    comAboveX < sMinX -> comAboveX - sMinX // negative -> tips left
                    comAboveX > sMaxX -> comAboveX - sMaxX // positive -> tips right
                    else -> 0f
                }
                val overhangZ = when {
                    comAboveZ < sMinZ -> comAboveZ - sMinZ
                    comAboveZ > sMaxZ -> comAboveZ - sMaxZ
                    else -> 0f
                }

                val midX = (sMinX + sMaxX) * 0.5f
                val halfSpanX = ((sMaxX - sMinX) * 0.5f).coerceAtLeast(0.05f)
                val normMarginX = (1f - abs(comAboveX - midX) / halfSpanX).coerceIn(-1f, 1f)

                val midZ = (sMinZ + sMaxZ) * 0.5f
                val halfSpanZ = ((sMaxZ - sMinZ) * 0.5f).coerceAtLeast(0.05f)
                val normMarginZ = (1f - abs(comAboveZ - midZ) / halfSpanZ).coerceIn(-1f, 1f)

                val layerMargin = min(normMarginX, normMarginZ)
                if (layerMargin < minMargin) {
                    minMargin = layerMargin
                    weakestLayer = layer
                }

                // Height lever arm amplifies upper mass overturning moment
                val leverScale = (overheadMass * 0.45f).coerceIn(0.8f, 12f)
                if (abs(overhangX) > 0.001f) {
                    val tX = overhangX * leverScale * 24f
                    if (abs(tX) > abs(worstTorqueX)) worstTorqueX = tX
                } else {
                    // Slight off-center bias still induces subtle lean torque
                    worstTorqueX += (comAboveX - midX) * 1.4f
                }
                if (abs(overhangZ) > 0.001f) {
                    val tZ = overhangZ * leverScale * 24f
                    if (abs(tZ) > abs(worstTorqueZ)) worstTorqueZ = tZ
                } else {
                    worstTorqueZ += (comAboveZ - midZ) * 1.1f
                }
            }

            layerBlocks.forEach { b ->
                val m = b.type.mass
                overheadMass += m
                overheadMomentX += b.x * m
                overheadMomentZ += b.z * m
            }
        }

        return StructuralAnalysis(
            comX = globalComX,
            comZ = globalComZ,
            weakestLayer = weakestLayer,
            minMargin = minMargin,
            overhangTorqueX = worstTorqueX,
            overhangTorqueZ = worstTorqueZ,
            missingSupportLayer = missingLayer
        )
    }

    private fun stepTowerDynamics(dt: Float) {
        val activeBlocks = blocks.filter { !it.isShattered }
        if (activeBlocks.isEmpty()) {
            collapseHingeLayer = 0
            triggerCollapseOrShield("Tower collapsed!")
            return
        }

        val analysis = analyzeTowerStructure()
        comX = analysis.comX
        weakestLayerIndex = analysis.weakestLayer

        if (analysis.missingSupportLayer != null) {
            collapseHingeLayer = analysis.missingSupportLayer
            triggerCollapseOrShield("Layer ${analysis.missingSupportLayer + 1} collapsed!")
            return
        }

        val magnetCount = activeBlocks.count { it.type == BlockType.MAGNET }
        val glueCount = activeBlocks.count { it.isGlued }
        val restoringSpring = (2.6f + magnetCount * 0.35f + glueCount * 0.25f)
        val gravMult = if (activeEvent == LiveEventType.LOW_GRAVITY) 0.62f else config.gravityMultiplier

        val torqueX = (analysis.overhangTorqueX + analysis.comX * 14f + towerTiltDeg * 0.85f) * gravMult -
            towerTiltDeg * restoringSpring * 0.28f
        val torqueZ = (analysis.overhangTorqueZ + analysis.comZ * 12f + towerTiltZDeg * 0.82f) * gravMult -
            towerTiltZDeg * restoringSpring * 0.28f

        val damping = (1f - (1.65f + magnetCount * 0.15f) * dt).coerceAtLeast(0.76f)
        angularVel = (angularVel + torqueX * dt) * damping
        angularVelZ = (angularVelZ + torqueZ * dt) * damping
        towerTiltDeg += angularVel * dt
        towerTiltZDeg += angularVelZ * dt

        // Combine geometric tilt + COM overhang margin + angular velocity for accurate stability %
        val tiltPenalty = (hypot(towerTiltDeg, towerTiltZDeg * 0.7f) / CRITICAL_TILT_DEG).coerceIn(0f, 1f)
        val marginPenalty = ((0.55f - analysis.minMargin).coerceAtLeast(0f) * 0.85f).coerceIn(0f, 0.55f)
        val velPenalty = (hypot(angularVel, angularVelZ) / 18f).coerceIn(0f, 0.35f)
        val targetStability = (1f - tiltPenalty * 0.72f - marginPenalty - velPenalty).coerceIn(0f, 1f)
        stability += (targetStability - stability) * (dt * 6f).coerceAtMost(1f)

        // Heartbeat warning in Red stability (<35%)
        if (stability < 0.35f && heartbeatCooldown <= 0f) {
            onSoundEffect(SoundEffect.HEARTBEAT)
            onHaptic(35L, 140)
            heartbeatCooldown = 0.72f
        }

        if (hypot(towerTiltDeg, towerTiltZDeg * 0.75f) > CRITICAL_TILT_DEG || stability <= 0.04f) {
            collapseHingeLayer = analysis.weakestLayer
            triggerCollapseOrShield("Tower lost balance at Layer ${collapseHingeLayer + 1}!")
        }
    }

    private fun triggerCollapseOrShield(reason: String) {
        pendingWager = null
        if (shieldArmed) {
            shieldArmed = false
            towerTiltDeg *= 0.2f
            towerTiltZDeg *= 0.2f
            angularVel = 0f
            angularVelZ = 0f
            stability = 0.82f
            onSoundEffect(SoundEffect.COMBO_CHIME)
            onHaptic(45L, 180)
            spawnBurst(0f, topLayerIndex * 0.5f, 20, "spark")
            setBanner("Shield Absorbed Collapse!")
            return
        }

        onSoundEffect(SoundEffect.COLLAPSE_BOOM)
        onHaptic(180L, 255)
        screenShake = 1.25f
        replayFrames.clear()

        // Hinge-based topple velocity initialization: blocks above collapseHingeLayer topple outward with rotational arm
        val dirSign = if (abs(towerTiltDeg) > 0.5f) {
            if (towerTiltDeg >= 0f) 1f else -1f
        } else if (comX != 0f) {
            if (comX >= 0f) 1f else -1f
        } else 1f

        blocks = blocks.map { b ->
            if (b.isShattered) b else {
                val heightAboveHinge = (b.layer - collapseHingeLayer).coerceAtLeast(0)
                val leverFactor = if (b.layer >= collapseHingeLayer) 0.55f + heightAboveHinge * 0.14f else 0.2f
                b.copy(
                    vx = dirSign * leverFactor + (rng.nextFloat() - 0.5f) * 1.1f,
                    vy = (rng.nextFloat() - 0.3f) * 0.6f,
                    vz = (rng.nextFloat() - 0.5f) * 0.8f,
                    angularVel = (rng.nextFloat() - 0.5f) * 160f + dirSign * (45f + heightAboveHinge * 8f)
                )
            }
        }.toMutableList()

        collapseSlowMoTimer = 2.6f
        phase = PlayPhase.COLLAPSING_SLOW_MO
        setBanner(reason)
    }

    private fun stepSlowMoCollapse(dt: Float) {
        val gravMult = if (activeEvent == LiveEventType.LOW_GRAVITY) 0.6f else config.gravityMultiplier
        val gravity = -8.5f * gravMult
        for (i in blocks.indices) {
            val b = blocks[i]
            if (b.isShattered) continue
            val nx = b.x + b.vx * dt
            var ny = b.y + b.vy * dt
            val nz = b.z + b.vz * dt
            var nvx = b.vx
            var nvy = b.vy + gravity * dt
            val nAngle = b.angleDeg + b.angularVel * dt
            var nAngVel = b.angularVel

            // Floor level aligned with pedestal surface (baseY - blockH)
            val floorY = -(b.layer.toFloat())
            if (ny < floorY) {
                ny = floorY
                nvy = abs(nvy) * b.type.restitution.coerceAtLeast(0.24f)
                nvx *= 0.72f
                nAngVel *= 0.6f
                if (rng.nextFloat() < 0.2f) spawnBurst(nx, 0f, 3, "dust")
            }
            blocks[i] = b.copy(x = nx, y = ny, z = nz, vx = nvx, vy = nvy, angleDeg = nAngle, angularVel = nAngVel)
        }
        if (replayFrames.size < 220) {
            replayFrames.add(blocks.toList())
        }
    }

    private fun updateParticles(dt: Float) {
        val nextList = mutableListOf<ParticleState>()
        for (p in particles) {
            val nl = p.life - dt
            if (nl > 0f) {
                nextList.add(
                    p.copy(
                        x = p.x + p.vx * dt,
                        y = p.y + p.vy * dt,
                        vy = if (p.kind == "rain") p.vy else p.vy - 1.8f * dt,
                        life = nl
                    )
                )
            }
        }
        particles = nextList
    }

    private fun spawnBurst(x: Float, y: Float, count: Int, kind: String) {
        if (particles.size > 140) return
        repeat(count) {
            particles.add(
                ParticleState(
                    x = x + (rng.nextFloat() - 0.5f) * 0.4f,
                    y = y + (rng.nextFloat() - 0.5f) * 0.4f,
                    vx = if (kind == "rain") -0.4f else (rng.nextFloat() - 0.5f) * 2.2f,
                    vy = if (kind == "rain") -4.5f else rng.nextFloat() * 2.4f,
                    size = 4f + rng.nextFloat() * 6f,
                    life = 0.75f + rng.nextFloat() * 0.65f,
                    maxLife = 1.4f,
                    kind = kind
                )
            )
        }
    }

    private fun pickNextEvent(): LiveEventType {
        val pool = listOf(
            LiveEventType.WIND,
            LiveEventType.EARTHQUAKE,
            LiveEventType.RAIN,
            LiveEventType.LOW_GRAVITY
        )
        return pool[rng.nextInt(pool.size)]
    }

    private fun pushSnapshot() {
        if (historyStack.size >= 8) historyStack.removeFirst()
        historyStack.addLast(
            TowerSnapshot(
                blocks = blocks.toList(),
                score = score,
                combo = currentCombo,
                movesCompleted = movesCompleted,
                topLayerIndex = topLayerIndex,
                nextPlaceSlot = nextPlaceSlot,
                towerTiltDeg = towerTiltDeg,
                towerTiltZDeg = towerTiltZDeg,
                stability = stability
            )
        )
    }

    private fun setBanner(msg: String) {
        statusBanner = msg
        statusBannerTimer = 2.8f
    }

    private fun computeStars(): Int {
        if (phase != PlayPhase.LEVEL_COMPLETE && movesCompleted < config.targetMoves) {
            return if (movesCompleted >= config.targetMoves / 2) 1 else 0
        }
        val acc = if (totalPulls > 0) perfectPulls.toFloat() / totalPulls else 1f
        return when {
            acc >= 0.7f && stability >= 0.65f -> 3
            acc >= 0.4f || stability >= 0.45f -> 2
            else -> 1
        }
    }

    private fun buildBlockStressMap(): Map<Int, BlockStress> {
        val out = HashMap<Int, BlockStress>(blocks.size * 2)
        for (b in blocks) {
            if (b.isShattered) continue
            out[b.id] = BlockStress(
                load = stressReport.blockLoads[b.id] ?: 0f,
                ratio = stressReport.blockRatios[b.id] ?: 0f,
                pullRisk = riskAssessments[b.id]?.risk ?: 0f
            )
        }
        return out
    }

    private fun buildUiState(): PhysicsUiState {
        val winnerName = if (config.mode == GameMode.PASS_AND_PLAY && phase == PlayPhase.GAME_OVER) {
            val prevIdx = (currentPlayerIndex - 1 + config.playerNames.size) % config.playerNames.size
            config.playerNames[prevIdx]
        } else null

        return PhysicsUiState(
            config = config,
            phase = phase,
            blocks = blocks.toList(),
            particles = particles.toList(),
            stabilityPercent = (stability * 100f).toInt().coerceIn(0, 100),
            towerTiltDeg = towerTiltDeg,
            towerTiltZDeg = towerTiltZDeg,
            centerOfMassX = comX,
            weakestLayerIndex = weakestLayerIndex,
            towerHeightLayers = topLayerIndex + 1,
            movesCompleted = movesCompleted,
            score = score,
            heightBonus = heightBonus,
            comboBonus = comboBonus,
            coinsEarned = coinsEarned,
            currentCombo = currentCombo,
            steadyHandPrecision = steadyHandPrecision,
            isDraggingBlock = activePullBlockId != null,
            activePullBlockId = activePullBlockId,
            holdingBlockType = holdingBlockType,
            aimSlot = aimSlot,
            aimOffsetNorm = aimOffsetNorm,
            activeEvent = activeEvent,
            incomingEvent = if (eventTimerSec <= 5f) incomingEvent else null,
            eventWarningSeconds = eventTimerSec.toInt().coerceAtLeast(0),
            windDirection = windDirection,
            screenShakeMagnitude = screenShake,
            freezeSecondsLeft = freezeTimer.toInt(),
            shieldArmed = shieldArmed,
            glueNextMove = glueArmed,
            timeLeftSeconds = timeAttackTimer.toInt(),
            currentPlayerIndex = currentPlayerIndex,
            lastWinningPlayerName = winnerName,
            perfectPullsCount = perfectPulls,
            totalPullsCount = totalPulls,
            starsEarned = computeStars(),
            ghostBestHeight = ghostBestHeight,
            hasCollapseReplay = replayFrames.isNotEmpty(),
            statusBannerText = statusBanner,
            canUndo = historyStack.isNotEmpty(),
            xrayActive = xrayActive,
            focusPercent = focus.toInt().coerceIn(0, 100),
            blockStress = if (xrayActive) buildBlockStressMap() else emptyMap(),
            maxLoadRatio = stressReport.maxRatio,
            pullRiskPercent = if (xrayActive) activePullAssessment?.riskPercent ?: -1 else -1,
            pullVerdict = if (xrayActive) activePullAssessment?.verdict else null,
            daredevilCount = daredevilCount,
            daredevilStreak = daredevilStreak,
            swayCalm = 1f - swayEnergy(),
            calmDropCount = calmDropCount
        )
    }
}
