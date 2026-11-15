package com.example.game.physics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class StructuralSolverTest {

    /** Same geometry as TowerPhysicsEngine.buildInitialTower (3 blocks per layer, alternating axis). */
    private fun tower(layers: Int, type: BlockType = BlockType.WOOD): List<BlockState> {
        val out = mutableListOf<BlockState>()
        var id = 1
        for (layer in 0 until layers) {
            val even = layer % 2 == 0
            for (slot in 0..2) {
                val coord = (slot - 1) * 0.34f
                out += BlockState(
                    id = id++, layer = layer, slot = slot, type = type,
                    x = if (even) coord else 0f,
                    z = if (even) 0f else coord
                )
            }
        }
        return out
    }

    private fun BlockState.idIn(list: List<BlockState>) = list.first { it.layer == layer && it.slot == slot }.id

    @Test
    fun reactionsAlwaysSumToAppliedLoad() {
        val pos = floatArrayOf(-0.34f, 0f, 0.34f)
        val k = floatArrayOf(1f, 1f, 1f)
        for (com in listOf(-0.5f, -0.2f, 0f, 0.1f, 0.3f, 0.6f)) {
            val r = StructuralSolver.distribute(20f, com, pos, k)
            assertEquals("com=$com", 20f, r.sum(), 0.01f)
            assertTrue("no tension allowed (com=$com)", r.all { it >= -1e-3f })
        }
    }

    @Test
    fun loadFollowsCenterOfMass() {
        val pos = floatArrayOf(-0.34f, 0f, 0.34f)
        val k = floatArrayOf(1f, 1f, 1f)
        val centered = StructuralSolver.distribute(30f, 0f, pos, k)
        assertEquals(centered[0], centered[2], 0.01f)

        val shiftedRight = StructuralSolver.distribute(30f, 0.15f, pos, k)
        assertTrue(shiftedRight[2] > shiftedRight[1])
        assertTrue(shiftedRight[1] > shiftedRight[0])
    }

    @Test
    fun extremeOffsetLiftsFarBlockOffTheSupport() {
        val pos = floatArrayOf(-0.34f, 0f, 0.34f)
        val k = floatArrayOf(1f, 1f, 1f)
        val r = StructuralSolver.distribute(30f, 0.45f, pos, k)
        assertEquals(0f, r[0], 1e-3f)
        assertEquals(30f, r.sum(), 0.01f)
    }

    @Test
    fun centerPullIsSaferThanEdgePullInLowLayer() {
        val blocks = tower(10)
        val center = blocks.first { it.layer == 2 && it.slot == 1 }
        val edge = blocks.first { it.layer == 2 && it.slot == 0 }
        val rc = StructuralSolver.assessPull(blocks, center.id)
        val re = StructuralSolver.assessPull(blocks, edge.id)
        assertTrue("center ${rc.risk} should be < edge ${re.risk}", rc.risk < re.risk)
    }

    @Test
    fun pullingNearTheTopIsSaferThanNearTheBottom() {
        val blocks = tower(12)
        val high = blocks.first { it.layer == 9 && it.slot == 1 }
        val low = blocks.first { it.layer == 1 && it.slot == 1 }
        assertTrue(
            StructuralSolver.assessPull(blocks, high.id).risk <=
                StructuralSolver.assessPull(blocks, low.id).risk
        )
    }

    @Test
    fun removingLastBlockOfALayerIsFatal() {
        val full = tower(8)
        val lone = full.filterNot { it.layer == 3 && it.slot != 1 }
        val onlyOne = lone.first { it.layer == 3 }
        val a = StructuralSolver.assessPull(lone, onlyOne.id)
        assertEquals(PullVerdict.FATAL, a.verdict)
        assertTrue(a.leavesLayerEmpty)
    }

    @Test
    fun glassCrushesBeforeWoodUnderTheSameLoad() {
        val glass = tower(14, BlockType.GLASS)
        val wood = tower(14, BlockType.WOOD)
        val gRatio = StructuralSolver.analyze(glass).maxRatio
        val wRatio = StructuralSolver.analyze(wood).maxRatio
        assertTrue(gRatio > wRatio * 1.5f)
    }

    @Test
    fun meltingIceAndGlueChangeCapacity() {
        val fresh = BlockState(1, 0, 1, BlockType.ICE, 0f)
        val melted = fresh.copy(meltProgress = 1f)
        val glued = BlockState(2, 0, 1, BlockType.WOOD, 0f, isGlued = true)
        val plain = glued.copy(isGlued = false)
        assertTrue(StructuralSolver.capacityOf(melted) < StructuralSolver.capacityOf(fresh))
        assertTrue(StructuralSolver.capacityOf(glued) > StructuralSolver.capacityOf(plain))
    }

    @Test
    fun riskMapCoversOnlyLegalPulls() {
        val blocks = tower(8).toMutableList()
        val glueTarget = blocks.indexOfFirst { it.layer == 2 && it.slot == 0 }
        blocks[glueTarget] = blocks[glueTarget].copy(isGlued = true)
        val map = StructuralSolver.riskMap(blocks)
        assertTrue(blocks[glueTarget].id !in map.keys)
        assertTrue(blocks.filter { it.layer == 7 }.none { it.id in map.keys })
        assertTrue(map.values.all { it.risk in 0f..1f })
    }

    @Test
    fun safestPullsAreSortedByRisk() {
        val ranked = StructuralSolver.safestPulls(tower(10))
        assertTrue(ranked.isNotEmpty())
        for (i in 1 until ranked.size) assertTrue(ranked[i - 1].risk <= ranked[i].risk + 1e-6f)
    }

    @Test
    fun swayMakesPullsRiskier() {
        val blocks = tower(10)
        val id = blocks.first { it.layer == 3 && it.slot == 1 }.id
        val calm = StructuralSolver.assessPull(blocks, id, swayEnergy = 0f).risk
        val shaky = StructuralSolver.assessPull(blocks, id, swayEnergy = 1f).risk
        assertTrue(shaky > calm)
        assertTrue(abs(shaky - calm) <= 0.26f)
    }
}
