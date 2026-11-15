package com.example.game.physics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadableTowerMechanicsTest {

    private fun newEngine(layers: Int, seed: Long): Pair<TowerPhysicsEngine, PhysicsUiState> {
        val e = TowerPhysicsEngine()
        val s = e.startNewSession(GameSessionConfig(GameMode.CLASSIC, initialLayers = layers, seed = seed), 12)
        return e to s
    }

    private fun settle(e: TowerPhysicsEngine, seconds: Float): PhysicsUiState {
        var s = e.step(0.016f, false)
        var t = 0f
        while (t < seconds) { s = e.step(0.016f, false); t += 0.016f }
        return s
    }

    private fun pull(e: TowerPhysicsEngine, id: Int): PhysicsUiState {
        e.startPullBlock(id)
        var s = e.updatePullDrag(0f, 0f, 1f)
        repeat(14) { s = e.updatePullDrag(0.11f, 0.2f, 1f) }
        if (s.isDraggingBlock) s = e.releasePull()
        return s
    }

    @Test
    fun stressVision_exposesLoadsAndDrainsFocus() {
        val (e, _) = newEngine(10, 7L)
        e.setXray(true)
        var s = e.step(0.016f, false)
        assertTrue(s.xrayActive)
        assertTrue(s.blockStress.isNotEmpty())
        assertTrue(s.maxLoadRatio > 0f)
        s = settle(e, 13f)
        assertFalse(s.xrayActive)
        assertTrue(s.focusPercent < 20)
    }

    @Test
    fun stressVisionOff_hidesRiskNumbers() {
        val (e, s0) = newEngine(10, 7L)
        val b = s0.blocks.first { it.layer == 3 && it.slot == 1 }
        e.startPullBlock(b.id)
        val s = e.updatePullDrag(0.01f, 0f, 1f)
        assertEquals(-1, s.pullRiskPercent)
        assertTrue(s.blockStress.isEmpty())
    }

    @Test
    fun stressVisionOn_showsPullRiskWhileDragging() {
        val (e, s0) = newEngine(10, 7L)
        val b = s0.blocks.first { it.layer == 2 && it.slot == 0 }
        e.setXray(true)
        e.step(0.016f, false)
        e.startPullBlock(b.id)
        val s = e.updatePullDrag(0.01f, 0f, 1f)
        assertTrue(s.pullRiskPercent in 0..100)
        assertTrue(s.pullVerdict != null)
    }

    @Test
    fun deadlyPull_collapsesAndPaysNoDaredevilBonus() {
        val (e, s0) = newEngine(10, 7L)
        val edge = s0.blocks.first { it.layer == 2 && it.slot == 0 }
        e.setXray(true)
        e.step(0.016f, false)
        pull(e, edge.id)
        val s = settle(e, 2f)
        assertEquals(0, s.daredevilCount)
        assertTrue(s.phase == PlayPhase.COLLAPSING_SLOW_MO || s.phase == PlayPhase.GAME_OVER)
    }

    @Test
    fun overloadedBlock_isCrushedAfterGracePeriod() {
        val (e, s0) = newEngine(15, 3L)
        val edge = s0.blocks.first { it.layer == 0 && it.slot == 0 }
        var s = pull(e, edge.id)
        if (s.phase == PlayPhase.HOLDING_TO_PLACE) s = e.placeHeldBlock()
        s = settle(e, 4f)
        assertTrue(s.blocks.any { it.isShattered } || s.phase == PlayPhase.GAME_OVER)
    }

    @Test
    fun calmDrop_rewardedWhenTowerIsStill() {
        val (e, s0) = newEngine(10, 11L)
        val c = s0.blocks.first { it.layer == 4 && it.slot == 1 }
        var s = pull(e, c.id)
        assertEquals(PlayPhase.HOLDING_TO_PLACE, s.phase)
        s = settle(e, 3f)
        assertTrue(s.swayCalm > 0.8f)
        s = e.placeHeldBlock()
        assertEquals(1, s.calmDropCount)
    }

    @Test
    fun crane_usesSolverToPickLowestRiskBlock() {
        val (e, _) = newEngine(10, 5L)
        val (used, s) = e.activatePowerUp(PowerUpType.CRANE)
        assertTrue(used)
        assertEquals(1, s.movesCompleted)
        assertTrue(s.phase != PlayPhase.COLLAPSING_SLOW_MO)
    }
}
