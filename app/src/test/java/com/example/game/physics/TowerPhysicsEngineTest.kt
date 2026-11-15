package com.example.game.physics

import com.example.game.campaign.CampaignCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TowerPhysicsEngineTest {

    @Test
    fun startSession_buildsInitialTower_andBossIntroForLevel10() {
        val engine = TowerPhysicsEngine()
        val bossConfig = CampaignCatalog.toSessionConfig(CampaignCatalog.getLevel(10))
        val state = engine.startNewSession(bossConfig, ghostHeight = 15)

        assertEquals(PlayPhase.BOSS_INTRO, state.phase)
        assertEquals(BossType.LEANING_TOWER, state.config.bossType)
        assertTrue(state.blocks.isNotEmpty())

        val afterDismiss = engine.dismissBossIntro()
        assertEquals(PlayPhase.PLAYING, afterDismiss.phase)
    }

    @Test
    fun cranePowerUp_safelyExtractsAndPlacesBlock() {
        val engine = TowerPhysicsEngine()
        val cfg = GameSessionConfig(mode = GameMode.CLASSIC, initialLayers = 8)
        engine.startNewSession(cfg, ghostHeight = 12)

        val (used, updated) = engine.activatePowerUp(PowerUpType.CRANE)
        assertTrue(used)
        assertEquals(1, updated.movesCompleted)
        assertTrue(updated.score > 0L)
    }

    @Test
    fun shieldPowerUp_armsShieldAndFreezeStopsEvents() {
        val engine = TowerPhysicsEngine()
        engine.startNewSession(GameSessionConfig(mode = GameMode.SURVIVAL), ghostHeight = 14)

        val (_, shielded) = engine.activatePowerUp(PowerUpType.SHIELD)
        assertTrue(shielded.shieldArmed)

        val (_, frozen) = engine.activatePowerUp(PowerUpType.FREEZE)
        assertTrue(frozen.freezeSecondsLeft > 0)
    }

    @Test
    fun pullAndAimPlacement_completesMoveWithSteadyHandCombo() {
        val engine = TowerPhysicsEngine()
        val initial = engine.startNewSession(GameSessionConfig(mode = GameMode.CLASSIC, initialLayers = 8, seed = 42L), 12)
        val centerLowBlock = initial.blocks.first { it.layer == 2 && it.slot == 1 }

        engine.startPullBlock(centerLowBlock.id)
        var pulled = initial
        repeat(10) {
            pulled = engine.updatePullDrag(deltaPull = 0.14f, lateralJitter = 0.2f, sensitivity = 1.0f)
        }
        if (pulled.phase == PlayPhase.PLAYING) {
            pulled = engine.releasePull()
        }
        assertEquals(PlayPhase.HOLDING_TO_PLACE, pulled.phase)
        assertEquals(1, pulled.currentCombo)

        val aimed = engine.updatePlacementAim(slotChoice = 1, fineOffsetNorm = 0.01f)
        assertEquals(1, aimed.aimSlot)

        val placed = engine.placeHeldBlock(slotChoice = 1, precisionDropOffset = 0.01f)
        assertEquals(PlayPhase.PLAYING, placed.phase)
        assertEquals(1, placed.movesCompleted)
        assertTrue(placed.comboBonus > 0L)
    }

    @Test
    fun removingAllBlocksInLowerLayer_triggersCollapseAndSlowMoPhysics() {
        val engine = TowerPhysicsEngine()
        val initial = engine.startNewSession(GameSessionConfig(mode = GameMode.CLASSIC, initialLayers = 6, seed = 99L), 10)
        val layer1Blocks = initial.blocks.filter { it.layer == 1 }

        var lastState = initial
        layer1Blocks.forEach { block ->
            if (lastState.phase == PlayPhase.PLAYING) {
                engine.startPullBlock(block.id)
                repeat(12) {
                    lastState = engine.updatePullDrag(deltaPull = 0.15f, lateralJitter = 0f, sensitivity = 1.0f)
                }
                if (lastState.phase == PlayPhase.HOLDING_TO_PLACE) {
                    lastState = engine.placeHeldBlock(block.slot, 0f)
                }
            }
        }

        assertEquals(PlayPhase.COLLAPSING_SLOW_MO, lastState.phase)
        val afterStep = engine.step(0.03f, particlesEnabled = true)
        assertTrue(afterStep.hasCollapseReplay)
    }

    @Test
    fun unbalancedTowerOverhang_increasesTiltAndReducesStabilityOverTime() {
        val engine = TowerPhysicsEngine()
        val initial = engine.startNewSession(GameSessionConfig(mode = GameMode.CLASSIC, initialLayers = 8, seed = 7L), 12)

        // Remove left & center blocks from layer 2, leaving only right outer block supporting upper 5 layers
        val toExtract = initial.blocks.filter { it.layer == 2 && it.slot in listOf(0, 1) }
        var state = initial
        toExtract.forEach { block ->
            engine.startPullBlock(block.id)
            repeat(12) {
                state = engine.updatePullDrag(0.15f, 0f, 1.0f)
            }
            if (state.phase == PlayPhase.HOLDING_TO_PLACE) {
                state = engine.placeHeldBlock(slotChoice = 2, precisionDropOffset = 0.06f)
            }
        }

        // Step physics forward and observe real overturning torque tilting tower
        repeat(25) {
            state = engine.step(0.03f, particlesEnabled = false)
        }
        assertTrue(state.stabilityPercent < initial.stabilityPercent)
    }
}
