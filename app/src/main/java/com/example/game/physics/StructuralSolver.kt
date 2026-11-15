package com.example.game.physics

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Tumble Tower's signature mechanic: the tower is "readable".
 *
 * StructuralSolver runs a real load-path analysis (top layer -> bottom layer):
 *  1. All mass above a layer, together with its centre of mass (COM), is treated as one
 *     vertical load applied to that layer along the axis its blocks are spread on
 *     (X for even layers, Z for odd layers - the Jenga cross-stacking).
 *  2. The layer is a rigid plate resting on springs (its blocks). The reaction in every block is
 *        R_i = N * ( k_i/K + e * k_i * (p_i - pc) / I )
 *     where k_i is block stiffness (contact fraction), pc the stiffness centroid, e the COM eccentricity
 *     and I the second moment of the supports.
 *  3. Blocks cannot pull (no tension). Any negative reaction means that block is lifted off, so it is
 *     dropped and the plate is re-solved (active-set iteration) until every reaction is >= 0.
 *  4. ratio = reaction / capacity(type, melt, glue). ratio >= 1 means that block is crushed.
 *
 * Pull risk simulates "what if this block were removed?" and reports how much worse the tower gets
 * (overload of blocks that now carry more, shrinking stability margin, or a layer left empty).
 * The same numbers drive: the X-Ray heat-map, the Daredevil risk wager, creaking audio/haptics,
 * crush failures and the Mini Crane's choice of block, so what the player sees is what can kill them.
 */
enum class PullVerdict(val label: String) {
    SAFE("Safe"),
    RISKY("Risky"),
    DANGEROUS("Dangerous"),
    FATAL("Fatal")
}

data class LayerStress(
    val layer: Int,
    val loadAbove: Float,
    /** 1 = load sits dead-centre on supports, 0 = on the edge, < 0 = outside (tips over). */
    val margin: Float,
    val overturning: Boolean
)

data class StressReport(
    val blockLoads: Map<Int, Float>,
    val blockRatios: Map<Int, Float>,
    val layers: List<LayerStress>,
    val maxRatio: Float,
    val maxRatioBlockId: Int?,
    val minMargin: Float,
    val weakestLayer: Int,
    val missingSupportLayer: Int?
) {
    companion object {
        val EMPTY = StressReport(emptyMap(), emptyMap(), emptyList(), 0f, null, 1f, 0, null)
    }
}

data class PullAssessment(
    val blockId: Int,
    val risk: Float,
    val verdict: PullVerdict,
    val predictedMaxRatio: Float,
    val predictedMinMargin: Float,
    val leavesLayerEmpty: Boolean
) {
    val riskPercent: Int get() = (risk * 100f).toInt().coerceIn(0, 100)
}

/** Per-block numbers the UI needs for the X-Ray heat-map. */
data class BlockStress(
    val load: Float = 0f,
    /** load / capacity. 0..1+ (>= 1 means crushing). */
    val ratio: Float = 0f,
    /** 0..1 chance-of-disaster if this block is pulled right now. */
    val pullRisk: Float = 0f
)

object StructuralSolver {

    /** Half width of a block along the axis its layer is spread on. */
    const val BLOCK_HALF_SPAN = 0.165f

    const val SAFE_BELOW = 0.25f
    const val RISKY_BELOW = 0.60f
    const val DANGEROUS_BELOW = 0.85f

    fun capacityOf(b: BlockState): Float {
        val base = when (b.type) {
            BlockType.WOOD -> 24f
            BlockType.ICE -> 14f
            BlockType.RUBBER -> 20f
            BlockType.MAGNET -> 24f
            BlockType.GOLD -> 34f
            BlockType.BOMB -> 18f
            BlockType.GLASS -> 10.5f
        }
        val meltFactor = 1f - b.meltProgress.coerceIn(0f, 1f) * 0.4f
        val glueFactor = if (b.isGlued) 1.25f else 1f
        return base * meltFactor * glueFactor
    }

    /** How much of a block still carries load (a block that is being pulled out bears less). */
    fun contactFactor(b: BlockState): Float =
        (1f - b.pullProgress * 0.72f - b.meltProgress * 0.25f).coerceIn(0.25f, 1f)

    fun analyze(blocks: List<BlockState>, excludeId: Int? = null): StressReport {
        val active = blocks.filter { !it.isShattered && it.id != excludeId }
        if (active.isEmpty()) return StressReport.EMPTY

        val byLayer = active.groupBy { it.layer }
        val top = byLayer.keys.maxOrNull() ?: return StressReport.EMPTY

        val loads = HashMap<Int, Float>(active.size * 2)
        val ratios = HashMap<Int, Float>(active.size * 2)
        val layers = ArrayList<LayerStress>(top + 1)

        var massAbove = 0f
        var momentX = 0f
        var momentZ = 0f
        var missing: Int? = null
        var minMargin = 1f
        var weakest = 0
        var maxRatio = 0f
        var maxRatioId: Int? = null

        for (layer in top downTo 0) {
            val layerBlocks = byLayer[layer].orEmpty()
            if (layerBlocks.isEmpty()) {
                if (layer < top) missing = layer
                continue
            }

            if (massAbove > 0.1f) {
                val spreadOnX = layer % 2 == 0
                val com = if (spreadOnX) momentX / massAbove else momentZ / massAbove
                val pos = FloatArray(layerBlocks.size) { if (spreadOnX) layerBlocks[it].x else layerBlocks[it].z }
                val stiff = FloatArray(layerBlocks.size) { contactFactor(layerBlocks[it]) }
                val reactions = distribute(massAbove, com, pos, stiff)

                // Support extent (contact patch of every block in the layer, incl. lifted ones)
                var lo = Float.POSITIVE_INFINITY
                var hi = Float.NEGATIVE_INFINITY
                for (i in layerBlocks.indices) {
                    val half = BLOCK_HALF_SPAN * stiff[i]
                    lo = min(lo, pos[i] - half)
                    hi = max(hi, pos[i] + half)
                }
                val mid = (lo + hi) * 0.5f
                val halfSpan = ((hi - lo) * 0.5f).coerceAtLeast(0.05f)
                val margin = (1f - abs(com - mid) / halfSpan).coerceIn(-1f, 1f)
                val overturning = com < lo || com > hi

                layers.add(LayerStress(layer, massAbove, margin, overturning))
                if (margin < minMargin) {
                    minMargin = margin
                    weakest = layer
                }

                for (i in layerBlocks.indices) {
                    val b = layerBlocks[i]
                    val load = reactions[i]
                    val ratio = load / capacityOf(b)
                    loads[b.id] = load
                    ratios[b.id] = ratio
                    if (ratio > maxRatio) {
                        maxRatio = ratio
                        maxRatioId = b.id
                    }
                }
            } else {
                layerBlocks.forEach {
                    loads[it.id] = 0f
                    ratios[it.id] = 0f
                }
            }

            for (b in layerBlocks) {
                val m = b.type.mass
                massAbove += m
                momentX += b.x * m
                momentZ += b.z * m
            }
        }

        return StressReport(loads, ratios, layers, maxRatio, maxRatioId, minMargin, weakest, missing)
    }

    /**
     * Rigid plate on non-tension springs. Returns the vertical reaction for every support.
     * [com] is where the total load [n] acts along the spread axis.
     */
    internal fun distribute(n: Float, com: Float, pos: FloatArray, stiffness: FloatArray): FloatArray {
        val count = pos.size
        val k = stiffness.copyOf()
        val result = FloatArray(count)
        if (count == 0) return result

        var guard = 0
        while (guard++ <= count) {
            var sumK = 0f
            var sumKp = 0f
            var live = 0
            for (i in 0 until count) {
                if (k[i] > 0f) {
                    sumK += k[i]
                    sumKp += k[i] * pos[i]
                    live++
                }
            }
            if (live == 0 || sumK <= 1e-6f) {
                // Nothing left to carry the load: dump it on the stiffest block so the caller sees a huge ratio.
                val best = stiffness.indices.maxByOrNull { stiffness[it] } ?: 0
                result.fill(0f)
                result[best] = n
                return result
            }
            val centroid = sumKp / sumK
            var inertia = 0f
            for (i in 0 until count) {
                if (k[i] > 0f) inertia += k[i] * (pos[i] - centroid) * (pos[i] - centroid)
            }

            var worstIdx = -1
            var worstVal = 0f
            for (i in 0 until count) {
                if (k[i] <= 0f) {
                    result[i] = 0f
                    continue
                }
                val r = if (live == 1 || inertia < 1e-6f) {
                    n * k[i] / sumK
                } else {
                    n * (k[i] / sumK + (com - centroid) * k[i] * (pos[i] - centroid) / inertia)
                }
                result[i] = r
                if (r < worstVal - 1e-4f) {
                    worstVal = r
                    worstIdx = i
                }
            }
            if (worstIdx < 0) return result
            k[worstIdx] = 0f // lifted off - re-solve without it
        }
        return result
    }

    /**
     * What happens to the tower if [blockId] is removed? Only blocks / layers that get WORSE are held
     * against the pull (a tower that is already stressed elsewhere does not make every pull look deadly).
     * [swayEnergy] (0..1) is the current wobble: pulling a shaking tower is riskier.
     */
    fun assessPull(
        blocks: List<BlockState>,
        blockId: Int,
        swayEnergy: Float = 0f,
        before: StressReport = analyze(blocks)
    ): PullAssessment {
        val after = analyze(blocks, excludeId = blockId)
        return assessAgainst(before, after, blockId, swayEnergy)
    }

    private fun assessAgainst(
        before: StressReport,
        after: StressReport,
        blockId: Int,
        swayEnergy: Float
    ): PullAssessment {
        if (after.missingSupportLayer != null && before.missingSupportLayer == null) {
            return PullAssessment(blockId, 1f, PullVerdict.FATAL, after.maxRatio, after.minMargin, true)
        }

        var worstRatio = 0f
        for ((id, ratio) in after.blockRatios) {
            val was = before.blockRatios[id] ?: 0f
            if (ratio - was > 0.02f) worstRatio = max(worstRatio, ratio)
        }
        val beforeMargins = before.layers.associate { it.layer to it.margin }
        var worstMargin = 1f
        for (layer in after.layers) {
            val was = beforeMargins[layer.layer] ?: 1f
            if (layer.margin < was - 0.02f) worstMargin = min(worstMargin, layer.margin)
        }

        val overload = ((worstRatio - 0.6f) / 0.5f).coerceIn(0f, 1f)
        val tipping = ((0.5f - worstMargin) / 0.6f).coerceIn(0f, 1f)
        val sway = (swayEnergy * 0.25f).coerceIn(0f, 0.25f)
        val risk = (1f - (1f - overload) * (1f - tipping) * (1f - sway)).coerceIn(0f, 1f)

        return PullAssessment(
            blockId = blockId,
            risk = risk,
            verdict = verdictFor(risk),
            predictedMaxRatio = after.maxRatio,
            predictedMinMargin = after.minMargin,
            leavesLayerEmpty = false
        )
    }

    fun verdictFor(risk: Float): PullVerdict = when {
        risk < SAFE_BELOW -> PullVerdict.SAFE
        risk < RISKY_BELOW -> PullVerdict.RISKY
        risk < DANGEROUS_BELOW -> PullVerdict.DANGEROUS
        else -> PullVerdict.FATAL
    }

    /** Which blocks can legally be pulled right now (not top layer, not glued, not shattered). */
    fun pullableBlocks(blocks: List<BlockState>): List<BlockState> {
        val top = blocks.filter { !it.isShattered }.maxOfOrNull { it.layer } ?: return emptyList()
        return blocks.filter { !it.isShattered && !it.isGlued && it.layer < top }
    }

    /** Risk for every pullable block. The "before" report is shared so this is O(n) analyses. */
    fun riskMap(blocks: List<BlockState>, swayEnergy: Float = 0f): Map<Int, PullAssessment> {
        val before = analyze(blocks)
        val result = HashMap<Int, PullAssessment>()
        for (b in pullableBlocks(blocks)) {
            result[b.id] = assessPull(blocks, b.id, swayEnergy, before)
        }
        return result
    }

    /** Lowest-risk pullable blocks first (ties broken by lower layer count of overload). */
    fun safestPulls(blocks: List<BlockState>, swayEnergy: Float = 0f): List<PullAssessment> =
        riskMap(blocks, swayEnergy).values.sortedWith(
            compareBy<PullAssessment> { it.risk }.thenBy { it.predictedMaxRatio }
        )
}
