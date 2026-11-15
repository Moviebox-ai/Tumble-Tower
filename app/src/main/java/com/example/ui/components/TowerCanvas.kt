package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.campaign.CampaignCatalog
import com.example.game.physics.BlockState
import com.example.game.physics.BlockStress
import com.example.game.physics.BlockType
import com.example.game.physics.LiveEventType
import com.example.game.physics.PhysicsUiState
import com.example.game.physics.PlayPhase
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun TowerCanvas(
    state: PhysicsUiState,
    equippedSkin: String,
    equippedTheme: String,
    colorblindMode: Boolean,
    onStartPull: (Int) -> Unit,
    onUpdatePull: (Float, Float) -> Unit,
    onReleasePull: () -> Unit,
    onUpdatePlacementAim: (Int, Float) -> Unit,
    onPlaceBlock: (Int, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var yawAngleDeg by remember { mutableFloatStateOf(22f) } // 2.5D isometric viewing angle
    var draggingBlockId by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val worldTheme = remember(state.config.worldId, equippedTheme) {
        CampaignCatalog.worlds.firstOrNull { it.key == equippedTheme }
            ?: CampaignCatalog.getWorld(state.config.worldId)
    }

    val backdropRes = when (worldTheme.key) {
        "ice" -> R.drawable.img_photo_night_bg
        "space" -> R.drawable.img_photo_space_bg
        else -> R.drawable.img_skyfall_hero_bg
    }

    // Track tower-local hit boxes + last pivot/tilt for inverse-tilt screen touch matching
    val blockHitBoxes = remember { mutableMapOf<Int, Pair<Offset, Size>>() }
    var currentPivot by remember { mutableStateOf(Offset.Zero) }
    var currentTiltRad by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(backdropRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        val tintColor = when (worldTheme.key) {
            "desert" -> Color(0xFF9A3412).copy(alpha = 0.25f)
            "lava" -> Color(0xFF7F1D1D).copy(alpha = 0.34f)
            else -> Color(0xFF0A122E).copy(alpha = 0.28f)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF060B22).copy(alpha = 0.58f),
                            tintColor,
                            Color(0xFF060A1D).copy(alpha = 0.65f)
                        )
                    )
                )
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.phase, state.holdingBlockType) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        val downPos = firstDown.position

                        // Interactive Drag-to-Aim & Release-to-Drop when holding an extracted block
                        if (state.phase == PlayPhase.HOLDING_TO_PLACE && state.holdingBlockType != null) {
                            fun computeAim(pos: Offset): Pair<Int, Float> {
                                val normX = ((pos.x - size.width / 2f) / (size.width * 0.28f)).coerceIn(-1.25f, 1.25f)
                                val slot = when {
                                    normX < -0.35f -> 0
                                    normX > 0.35f -> 2
                                    else -> 1
                                }
                                val slotCenter = (slot - 1) * 0.75f
                                val fineOffset = ((normX - slotCenter) * 0.14f).coerceIn(-0.09f, 0.09f)
                                return slot to fineOffset
                            }

                            var (curSlot, curOffset) = computeAim(downPos)
                            onUpdatePlacementAim(curSlot, curOffset)

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == firstDown.id }
                                if (change == null || !change.pressed) {
                                    onPlaceBlock(curSlot, curOffset)
                                    break
                                }
                                val nextAim = computeAim(change.position)
                                curSlot = nextAim.first
                                curOffset = nextAim.second
                                onUpdatePlacementAim(curSlot, curOffset)
                                change.consume()
                            }
                            return@awaitEachGesture
                        }

                        // Inverse-rotate touch point around tower pivot so tilted blocks hit accurately
                        val dxP = downPos.x - currentPivot.x
                        val dyP = downPos.y - currentPivot.y
                        val cosT = cos(-currentTiltRad)
                        val sinT = sin(-currentTiltRad)
                        val localTouch = Offset(
                            currentPivot.x + dxP * cosT - dyP * sinT,
                            currentPivot.y + dxP * sinT + dyP * cosT
                        )

                        var hitId: Int? = null
                        if (state.phase == PlayPhase.PLAYING) {
                            val hitCandidate = blockHitBoxes.entries.lastOrNull { (_, box) ->
                                val (pos, bSize) = box
                                localTouch.x in pos.x..(pos.x + bSize.width) &&
                                    localTouch.y in pos.y..(pos.y + bSize.height)
                            }
                            hitId = hitCandidate?.key
                        }

                        if (hitId != null) {
                            draggingBlockId = hitId
                            onStartPull(hitId)
                            var prevPos = downPos
                            val dirSign = state.blocks.firstOrNull { it.id == hitId }?.pullDir ?: 1f
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == firstDown.id }
                                if (change == null || !change.pressed) {
                                    onReleasePull()
                                    draggingBlockId = null
                                    break
                                }
                                val delta = change.position - prevPos
                                prevPos = change.position
                                // Directional pull: moving away along pullDir or downward extracts; pushing opposite retracts smoothly
                                val signedHoriz = delta.x * dirSign
                                val pullDelta = (if (abs(signedHoriz) > abs(delta.y) * 0.4f) {
                                    (signedHoriz + delta.y * 0.35f)
                                } else {
                                    (abs(delta.x) + delta.y * 0.5f)
                                }) / 155f
                                val jitter = if (abs(delta.y) > 1.5f) delta.y else delta.x * 0.25f
                                onUpdatePull(pullDelta, jitter)
                                change.consume()
                            }
                        } else {
                            // Pinch-to-zoom & drag-to-orbit camera
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.none { it.pressed }) break
                                val zoomChange = event.calculateZoom()
                                val pan = event.calculatePan()
                                if (abs(zoomChange - 1f) > 0.005f) {
                                    zoomScale = (zoomScale * zoomChange).coerceIn(0.65f, 1.85f)
                                }
                                if (abs(pan.x) > 0.5f) {
                                    yawAngleDeg = (yawAngleDeg + pan.x * 0.22f).coerceIn(-55f, 55f)
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
        ) {
            val shakeX = if (state.screenShakeMagnitude > 0.01f) {
                (Random.nextFloat() - 0.5f) * 22f * state.screenShakeMagnitude
            } else 0f
            val shakeY = if (state.screenShakeMagnitude > 0.01f) {
                (Random.nextFloat() - 0.5f) * 18f * state.screenShakeMagnitude
            } else 0f

            val centerX = size.width / 2f + shakeX
            val baseY = size.height * 0.80f + shakeY
            val effectiveZoom = if (state.phase == PlayPhase.COLLAPSING_SLOW_MO) {
                zoomScale * 0.85f
            } else {
                zoomScale
            }
            val maxLayer = (state.towerHeightLayers + 1).coerceAtLeast(10)
            // Auto-fit: the whole tower always stays on screen, chunky like the reference art
            val unitScale = ((size.height * 0.60f) / (maxLayer * 36f)).coerceIn(0.5f, 1.45f) * effectiveZoom

            val blockW = 72f * unitScale
            val blockH = 34f * unitScale
            val depthShiftX = sin(Math.toRadians(yawAngleDeg.toDouble())).toFloat() * 18f * unitScale
            val depthShiftY = -10f * unitScale

            // 1. Stone Cliff Pedestal + Live Center-of-Mass Plumb Dot
            val pedW = 290f * unitScale
            val pedH = 44f * unitScale
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF64748B), Color(0xFF334155), Color(0xFF1E293B))),
                topLeft = Offset(centerX - pedW / 2f, baseY),
                size = Size(pedW, pedH),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = worldTheme.accentColor.copy(alpha = 0.45f),
                topLeft = Offset(centerX - pedW / 2f, baseY),
                size = Size(pedW, pedH),
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = 2.5f)
            )
            // Stone texture, moss tufts and a soft contact shadow so the tower feels grounded
            drawOval(
                color = Color.Black.copy(alpha = 0.32f),
                topLeft = Offset(centerX - pedW * 0.46f, baseY - 6f * unitScale),
                size = Size(pedW * 0.92f, 16f * unitScale)
            )
            for (i in 0..6) {
                val sx = centerX - pedW * 0.46f + pedW * 0.92f * (i / 6f)
                drawLine(
                    color = Color.Black.copy(alpha = 0.22f),
                    start = Offset(sx, baseY + pedH * 0.12f),
                    end = Offset(sx + 9f * unitScale, baseY + pedH * (0.55f + 0.08f * (i % 3))),
                    strokeWidth = 2f
                )
                drawRoundRect(
                    color = Color(0xFF22C55E).copy(alpha = 0.55f),
                    topLeft = Offset(sx - 6f * unitScale, baseY - 3f * unitScale),
                    size = Size(12f * unitScale, 5f * unitScale),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
            // Live COM indicator tick on pedestal edge
            val comIndicatorX = (centerX + state.centerOfMassX * blockW * 3.0f).coerceIn(
                centerX - pedW * 0.44f,
                centerX + pedW * 0.44f
            )
            val comColor = when {
                state.stabilityPercent >= 65 -> Color(0xFF22C55E)
                state.stabilityPercent >= 35 -> Color(0xFFFACC15)
                else -> Color(0xFFEF4444)
            }
            drawCircle(comColor, radius = 5f * unitScale, center = Offset(comIndicatorX, baseY + pedH * 0.25f))

            // 2. Ghost Tower Outline (Personal Best Height)
            val ghostH = state.ghostBestHeight * (blockH + 2f)
            val ghostW = blockW * 3.1f
            drawRoundRect(
                color = Color(0xFF38BDF8).copy(alpha = 0.42f),
                topLeft = Offset(centerX - ghostW / 2f, baseY - ghostH),
                size = Size(ghostW, ghostH),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(
                    width = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
            )

            // 3. Tower Blocks (alternating 90° Jenga cross-stacking projection + hit boxes)
            blockHitBoxes.clear()
            val pivot = Offset(centerX, baseY)
            currentPivot = pivot
            val globalTilt = if (state.phase == PlayPhase.COLLAPSING_SLOW_MO || state.phase == PlayPhase.REPLAYING_COLLAPSE) {
                0f
            } else {
                state.towerTiltDeg + state.towerTiltZDeg * 0.25f
            }
            currentTiltRad = Math.toRadians(globalTilt.toDouble()).toFloat()

            val sortedBlocks = state.blocks.sortedWith(
                compareBy<BlockState> { it.layer }
                    .thenBy { if (it.layer % 2 == 0) it.slot else -it.slot } // draw back-to-front cleanly
            )

            rotate(degrees = globalTilt, pivot = pivot) {
                for (block in sortedBlocks) {
                    if (block.isShattered) continue
                    val isEvenLayer = block.layer % 2 == 0
                    val bounceY = sin(block.bouncePhase * 20f) * block.bouncePhase * 4.5f * unitScale

                    val topLeft: Offset
                    val bSize: Size
                    val layerDepthX: Float
                    val layerDepthY: Float

                    if (isEvenLayer) {
                        // Even layers: 3 blocks side-by-side in X, receding in Z depth
                        val pullOffsetX = if (block.isBeingPulled) block.pullProgress * block.pullDir * blockW * 1.45f else 0f
                        val pullOffsetY = if (block.isBeingPulled) block.pullProgress * 11f * unitScale else 0f
                        val bx = centerX + block.x * (blockW * 3.05f) + pullOffsetX
                        val by = baseY - (block.layer + 1) * (blockH + 1.5f) - block.y * (blockH + 1.5f) + pullOffsetY - bounceY
                        topLeft = Offset(bx - blockW / 2f, by)
                        bSize = Size(blockW, blockH)
                        layerDepthX = depthShiftX * 0.55f
                        layerDepthY = depthShiftY
                    } else {
                        // Odd layers: perpendicular Jenga cross-beams stacked front-to-back (Z rows)
                        val rowIdx = (block.slot - 1)
                        val depthOffsetX = (block.z - rowIdx * 0.08f) * depthShiftX * 0.9f
                        val depthOffsetY = rowIdx * 4.2f * unitScale
                        val pullOffsetX = if (block.isBeingPulled) block.pullProgress * block.pullDir * blockW * 1.35f else 0f
                        val pullOffsetY = if (block.isBeingPulled) block.pullProgress * 9f * unitScale else 0f
                        val beamW = blockW * 2.86f
                        val bx = centerX + block.x * (blockW * 2.4f) + depthOffsetX + pullOffsetX
                        val by = baseY - (block.layer + 1) * (blockH + 1.5f) - block.y * (blockH + 1.5f) + depthOffsetY + pullOffsetY - bounceY
                        topLeft = Offset(bx - beamW / 2f, by)
                        bSize = Size(beamW, blockH * 0.88f)
                        layerDepthX = depthShiftX * 0.25f
                        layerDepthY = depthShiftY * 0.55f
                    }

                    if (block.layer < state.towerHeightLayers - 1) {
                        blockHitBoxes[block.id] = topLeft to bSize
                    }

                    val isWeakLayer = state.stabilityPercent < 55 && block.layer == state.weakestLayerIndex
                    rotate(degrees = block.angleDeg, pivot = Offset(topLeft.x + bSize.width / 2f, topLeft.y + bSize.height / 2f)) {
                        drawIsometricBlock(
                            block = block,
                            topLeft = topLeft,
                            bSize = bSize,
                            depthX = layerDepthX,
                            depthY = layerDepthY,
                            skin = equippedSkin,
                            colorblindMode = colorblindMode,
                            textMeasurer = textMeasurer,
                            isDragged = block.id == draggingBlockId || block.isBeingPulled,
                            isWeakWarning = isWeakLayer,
                            stress = state.blockStress[block.id],
                            xray = state.xrayActive
                        )
                    }
                }

                // 4. Interactive Top Placement Target Slots & Hovering Held Block Preview
                if (state.phase == PlayPhase.HOLDING_TO_PLACE && state.holdingBlockType != null) {
                    val topLayerCount = state.blocks.count { it.layer == (state.towerHeightLayers - 1) && !it.isShattered }
                    val placeLayer = if (topLayerCount >= 3) state.towerHeightLayers else (state.towerHeightLayers - 1).coerceAtLeast(0)
                    val targetY = baseY - (placeLayer + 1) * (blockH + 1.5f)
                    val isEvenTarget = placeLayer % 2 == 0
                    val previewCenterX = if (isEvenTarget) {
                        centerX + ((state.aimSlot - 1) * 0.34f + state.aimOffsetNorm) * (blockW * 3.05f)
                    } else {
                        centerX + state.aimOffsetNorm * (blockW * 2.4f)
                    }
                    val previewW = if (isEvenTarget) blockW else blockW * 2.86f
                    val isWellCentered = abs(state.aimOffsetNorm) < 0.035f
                    val guideColor = if (isWellCentered) Color(0xFF22C55E) else Color(0xFFFACC15)

                    // Target outline on top of tower
                    drawRoundRect(
                        color = guideColor.copy(alpha = 0.28f),
                        topLeft = Offset(previewCenterX - previewW / 2f, targetY),
                        size = Size(previewW, blockH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = guideColor,
                        topLeft = Offset(previewCenterX - previewW / 2f, targetY),
                        size = Size(previewW, blockH),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f))
                    )

                    // Hovering 3D held block floating slightly above drop slot
                    val hoverBlock = BlockState(
                        id = -1,
                        layer = placeLayer,
                        slot = state.aimSlot,
                        type = state.holdingBlockType,
                        x = 0f
                    )
                    drawIsometricBlock(
                        block = hoverBlock,
                        topLeft = Offset(previewCenterX - previewW / 2f, targetY - blockH * 1.65f),
                        bSize = Size(previewW, blockH),
                        depthX = depthShiftX * 0.45f,
                        depthY = depthShiftY,
                        skin = equippedSkin,
                        colorblindMode = colorblindMode,
                        textMeasurer = textMeasurer,
                        isDragged = true,
                        isWeakWarning = false,
                        stress = null,
                        xray = false
                    )
                }
            }

            // 5. Shield Dome & Freeze Frost Overlay
            if (state.shieldArmed) {
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.14f),
                    radius = size.width * 0.44f,
                    center = Offset(centerX, baseY - size.height * 0.25f)
                )
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.55f),
                    radius = size.width * 0.44f,
                    center = Offset(centerX, baseY - size.height * 0.25f),
                    style = Stroke(width = 3f)
                )
            }
            if (state.freezeSecondsLeft > 0) {
                drawRect(
                    color = Color(0xFF7DD3FC).copy(alpha = 0.14f),
                    size = size
                )
            }

            // 6. Stylized Wind Direction Arrows
            if (state.activeEvent == LiveEventType.WIND || state.incomingEvent == LiveEventType.WIND) {
                val dir = state.windDirection
                val arrowStartX = if (dir >= 0) size.width * 0.08f else size.width * 0.24f
                for (row in 0..2) {
                    val ay = size.height * (0.21f + row * 0.025f)
                    val len = if (row == 1) 74f else 52f
                    val axStart = arrowStartX + if (row == 1) -8f else 0f
                    val axEnd = axStart + dir * len
                    drawLine(
                        color = Color(0xFF7DD3FC).copy(alpha = 0.9f),
                        start = Offset(axStart, ay),
                        end = Offset(axEnd, ay),
                        strokeWidth = 4.5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF7DD3FC).copy(alpha = 0.9f),
                        start = Offset(axEnd - dir * 12f, ay - 7f),
                        end = Offset(axEnd, ay),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF7DD3FC).copy(alpha = 0.9f),
                        start = Offset(axEnd - dir * 12f, ay + 7f),
                        end = Offset(axEnd, ay),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 7. Particle Effects
            for (p in state.particles) {
                val px = centerX + p.x * (blockW * 1.4f)
                val py = baseY - p.y * (blockH + 2f)
                val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
                val pColor = when (p.kind) {
                    "confetti" -> Color(0xFFFACC15).copy(alpha = alpha)
                    "spark" -> Color(0xFF38BDF8).copy(alpha = alpha)
                    "ember" -> Color(0xFFEF4444).copy(alpha = alpha)
                    "snow" -> Color.White.copy(alpha = alpha)
                    "rain" -> Color(0xFF60A5FA).copy(alpha = alpha * 0.75f)
                    else -> Color(0xFFE2E8F0).copy(alpha = alpha * 0.65f)
                }
                if (p.kind == "rain") {
                    drawLine(pColor, Offset(px, py), Offset(px - 6f, py + 18f), strokeWidth = 2.5f)
                } else {
                    drawCircle(pColor, radius = p.size * unitScale * 0.45f, center = Offset(px, py))
                }
            }
        }
    }
}

private fun shade(c: Color, f: Float): Color =
    Color(
        red = (c.red * f).coerceIn(0f, 1f),
        green = (c.green * f).coerceIn(0f, 1f),
        blue = (c.blue * f).coerceIn(0f, 1f),
        alpha = c.alpha
    )

/** Deterministic 0..1 pseudo random so every block keeps the same grain/cracks frame to frame. */
private fun frac(seed: Int, k: Int): Float = (((seed * 31 + k * 57) * 2654435761L) ushr 8 and 0xFFL) / 255f

/** Stress heat colour: calm teal-green -> amber -> hot red. */
private fun heatColor(v: Float): Color {
    val t = v.coerceIn(0f, 1f)
    return if (t < 0.5f) {
        val k = t / 0.5f
        Color(
            red = 0.13f + (0.98f - 0.13f) * k,
            green = 0.77f + (0.80f - 0.77f) * k,
            blue = 0.37f + (0.08f - 0.37f) * k
        )
    } else {
        val k = (t - 0.5f) / 0.5f
        Color(
            red = 0.98f + (0.94f - 0.98f) * k,
            green = 0.80f + (0.27f - 0.80f) * k,
            blue = 0.08f + (0.27f - 0.08f) * k
        )
    }
}

private fun DrawScope.drawIsometricBlock(
    block: BlockState,
    topLeft: Offset,
    bSize: Size,
    depthX: Float,
    depthY: Float,
    skin: String,
    colorblindMode: Boolean,
    textMeasurer: TextMeasurer,
    isDragged: Boolean,
    isWeakWarning: Boolean,
    stress: BlockStress?,
    xray: Boolean
) {
    val (frontColor, topColor, borderColor) = blockColors(block.type, skin)
    val seed = block.id * 7919 + block.layer * 131
    val w = bSize.width
    val h = bSize.height
    val radius = min(h * 0.22f, 11f)
    val isGlassy = block.type == BlockType.ICE || block.type == BlockType.GLASS
    val isWoodLike = block.type == BlockType.WOOD

    // Soft drop shadow under the block
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.28f),
        topLeft = Offset(topLeft.x + 2f, topLeft.y + 4f),
        size = bSize,
        cornerRadius = CornerRadius(radius, radius)
    )

    // Top face (lit from above) + side face
    val topFace = Path().apply {
        moveTo(topLeft.x, topLeft.y)
        lineTo(topLeft.x + depthX, topLeft.y + depthY)
        lineTo(topLeft.x + w + depthX, topLeft.y + depthY)
        lineTo(topLeft.x + w, topLeft.y)
        close()
    }
    drawPath(
        topFace,
        brush = Brush.verticalGradient(
            listOf(topColor, shade(topColor, 0.86f)),
            startY = topLeft.y + depthY,
            endY = topLeft.y
        )
    )
    val sideX = if (depthX >= 0) topLeft.x + w else topLeft.x
    val sideFace = Path().apply {
        moveTo(sideX, topLeft.y)
        lineTo(sideX + depthX, topLeft.y + depthY)
        lineTo(sideX + depthX, topLeft.y + h + depthY)
        lineTo(sideX, topLeft.y + h)
        close()
    }
    drawPath(sideFace, color = shade(frontColor, 0.68f).copy(alpha = if (isGlassy) 0.55f else 0.95f))

    // Front face body
    val frontBrush = Brush.verticalGradient(
        colors = listOf(
            topColor.copy(alpha = if (isGlassy) 0.78f else 1f),
            frontColor,
            shade(frontColor, 0.80f)
        ),
        startY = topLeft.y,
        endY = topLeft.y + h
    )
    drawRoundRect(
        brush = frontBrush,
        topLeft = topLeft,
        size = bSize,
        cornerRadius = CornerRadius(radius, radius)
    )

    val clip = Path().apply {
        addRoundRect(
            RoundRect(
                left = topLeft.x,
                top = topLeft.y,
                right = topLeft.x + w,
                bottom = topLeft.y + h,
                radiusX = radius,
                radiusY = radius
            )
        )
    }
    clipPath(clip) {
        when (block.type) {
            BlockType.WOOD -> {
                // Wavy wood grain lines
                val grain = shade(frontColor, 0.62f).copy(alpha = 0.55f)
                for (i in 0..3) {
                    val gy = topLeft.y + h * (0.18f + 0.22f * i) + (frac(seed, i) - 0.5f) * h * 0.08f
                    val path = Path().apply {
                        moveTo(topLeft.x - 2f, gy)
                        quadraticBezierTo(
                            topLeft.x + w * (0.25f + frac(seed, i + 9) * 0.2f), gy - h * 0.10f,
                            topLeft.x + w * 0.55f, gy + h * 0.03f
                        )
                        quadraticBezierTo(
                            topLeft.x + w * 0.80f, gy + h * 0.12f,
                            topLeft.x + w + 2f, gy - h * 0.02f
                        )
                    }
                    drawPath(path, color = grain, style = Stroke(width = max1(h * 0.045f), cap = StrokeCap.Round))
                }
                if (frac(seed, 3) > 0.45f) {
                    val kx = topLeft.x + w * (0.2f + frac(seed, 4) * 0.6f)
                    val ky = topLeft.y + h * (0.35f + frac(seed, 5) * 0.3f)
                    drawOval(
                        color = shade(frontColor, 0.55f).copy(alpha = 0.7f),
                        topLeft = Offset(kx - h * 0.16f, ky - h * 0.11f),
                        size = Size(h * 0.32f, h * 0.22f),
                        style = Stroke(width = max1(h * 0.05f))
                    )
                }
            }
            BlockType.ICE, BlockType.GLASS -> {
                // Glossy sheen, inner frost and cracks
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                        start = Offset(topLeft.x, topLeft.y),
                        end = Offset(topLeft.x + w * 0.55f, topLeft.y + h)
                    ),
                    topLeft = topLeft,
                    size = bSize
                )
                val crack = Color.White.copy(alpha = 0.75f)
                for (i in 0..1) {
                    val cx = topLeft.x + w * (0.2f + 0.45f * i + frac(seed, i + 20) * 0.15f)
                    val path = Path().apply {
                        moveTo(cx, topLeft.y)
                        lineTo(cx + h * 0.22f, topLeft.y + h * 0.35f)
                        lineTo(cx - h * 0.10f, topLeft.y + h * 0.62f)
                        lineTo(cx + h * 0.16f, topLeft.y + h)
                    }
                    drawPath(path, color = crack, style = Stroke(width = max1(h * 0.03f)))
                }
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(topLeft.x + radius, topLeft.y + h * 0.12f),
                    end = Offset(topLeft.x + w * (0.55f * (1f - block.meltProgress * 0.5f)).coerceAtLeast(0.2f), topLeft.y + h * 0.12f),
                    strokeWidth = max1(h * 0.06f),
                    cap = StrokeCap.Round
                )
            }
            BlockType.GOLD -> {
                // Metal sheen band + the embossed X from the art
                val band = Path().apply {
                    moveTo(topLeft.x + w * 0.30f, topLeft.y)
                    lineTo(topLeft.x + w * 0.52f, topLeft.y)
                    lineTo(topLeft.x + w * 0.34f, topLeft.y + h)
                    lineTo(topLeft.x + w * 0.12f, topLeft.y + h)
                    close()
                }
                drawPath(band, color = Color.White.copy(alpha = 0.32f))
                val emboss = shade(frontColor, 0.62f).copy(alpha = 0.45f)
                drawLine(emboss, Offset(topLeft.x + w * 0.62f, topLeft.y + h * 0.12f), Offset(topLeft.x + w * 0.92f, topLeft.y + h * 0.88f), strokeWidth = max1(h * 0.05f))
                drawLine(emboss, Offset(topLeft.x + w * 0.92f, topLeft.y + h * 0.12f), Offset(topLeft.x + w * 0.62f, topLeft.y + h * 0.88f), strokeWidth = max1(h * 0.05f))
            }
            BlockType.RUBBER -> {
                drawOval(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(topLeft.x + w * 0.08f, topLeft.y + h * 0.10f),
                    size = Size(w * 0.30f, h * 0.22f)
                )
            }
            BlockType.MAGNET -> {
                drawRect(Color(0xFFEF4444), Offset(topLeft.x, topLeft.y), Size(w * 0.34f, h))
                drawRect(Color(0xFF3B82F6), Offset(topLeft.x + w * 0.66f, topLeft.y), Size(w * 0.34f, h))
                drawRect(Color.White.copy(alpha = 0.9f), Offset(topLeft.x + w * 0.30f, topLeft.y), Size(w * 0.04f, h))
                drawRect(Color.White.copy(alpha = 0.9f), Offset(topLeft.x + w * 0.66f, topLeft.y), Size(w * 0.04f, h))
            }
            BlockType.BOMB -> {
                // Hazard stripes
                for (i in 0..5) {
                    val sx = topLeft.x + w * (i / 5f) - h * 0.3f
                    val stripe = Path().apply {
                        moveTo(sx, topLeft.y + h)
                        lineTo(sx + h * 0.35f, topLeft.y)
                        lineTo(sx + h * 0.65f, topLeft.y)
                        lineTo(sx + h * 0.30f, topLeft.y + h)
                        close()
                    }
                    drawPath(stripe, color = Color.Black.copy(alpha = 0.28f))
                }
            }
        }

        // Specular highlight on the top bevel and dark underside for a chunky 3D look
        drawLine(
            color = Color.White.copy(alpha = if (isGlassy) 0.7f else 0.38f),
            start = Offset(topLeft.x + radius, topLeft.y + 1.5f),
            end = Offset(topLeft.x + w - radius, topLeft.y + 1.5f),
            strokeWidth = max1(h * 0.07f),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.30f),
            start = Offset(topLeft.x + radius, topLeft.y + h - 1.5f),
            end = Offset(topLeft.x + w - radius, topLeft.y + h - 1.5f),
            strokeWidth = max1(h * 0.08f),
            cap = StrokeCap.Round
        )

        // Tension cracks: the tower "shows" overload even without Stress Vision
        val load = block.pressureLoad
        if (load > 0.70f) {
            val a = ((load - 0.70f) / 0.30f).coerceIn(0f, 1f)
            val crackColor = Color(0xFF1F1209).copy(alpha = 0.35f + 0.5f * a)
            val cx = topLeft.x + w * (0.35f + frac(seed, 40) * 0.3f)
            val path = Path().apply {
                moveTo(cx, topLeft.y)
                lineTo(cx - h * 0.12f, topLeft.y + h * 0.30f)
                lineTo(cx + h * 0.10f, topLeft.y + h * 0.55f)
                lineTo(cx - h * 0.06f, topLeft.y + h * 0.80f)
                if (a > 0.5f) lineTo(cx + h * 0.08f, topLeft.y + h)
            }
            drawPath(path, color = crackColor, style = Stroke(width = max1(h * (0.04f + 0.04f * a)), cap = StrokeCap.Round))
        }

        // Stress Vision heat-map: tint by pull risk, load gauge along the bottom edge
        if (xray && stress != null) {
            drawRect(
                color = heatColor(stress.pullRisk).copy(alpha = 0.50f),
                topLeft = topLeft,
                size = bSize
            )
            val gaugeH = max1(h * 0.12f)
            drawRect(
                color = Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(topLeft.x, topLeft.y + h - gaugeH),
                size = Size(w, gaugeH)
            )
            drawRect(
                color = heatColor(stress.ratio),
                topLeft = Offset(topLeft.x, topLeft.y + h - gaugeH),
                size = Size(w * stress.ratio.coerceIn(0f, 1f), gaugeH)
            )
        }
    }

    // Outline
    val glow = isGlassy || block.type == BlockType.GOLD
    if (glow) {
        drawRoundRect(
            color = borderColor.copy(alpha = 0.40f),
            topLeft = Offset(topLeft.x - 2f, topLeft.y - 2f),
            size = Size(w + 4f, h + 4f),
            cornerRadius = CornerRadius(radius + 2f, radius + 2f),
            style = Stroke(width = 3f)
        )
    }
    drawRoundRect(
        color = when {
            isDragged -> Color(0xFFFEF08A)
            block.isGlued -> Color(0xFF22C55E)
            isWeakWarning -> Color(0xFFEF4444)
            else -> if (isWoodLike) shade(frontColor, 0.55f) else borderColor
        },
        topLeft = topLeft,
        size = bSize,
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = if (isDragged || block.isGlued || isWeakWarning) 3.2f else 1.8f)
    )

    if (block.type == BlockType.BOMB) {
        drawText(
            textMeasurer = textMeasurer,
            text = "TNT ${block.bombFuseMoves}",
            topLeft = Offset(topLeft.x + 6f, topLeft.y + h * 0.26f),
            style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        )
    }

    // Stress Vision numbers: risk % if this block were pulled now
    if (xray && stress != null && block.layer >= 0 && stress.pullRisk > 0.01f && w > 40f) {
        drawText(
            textMeasurer = textMeasurer,
            text = "${(stress.pullRisk * 100f).toInt()}%",
            topLeft = Offset(topLeft.x + w * 0.5f - 11f, topLeft.y + h * 0.5f - 7f),
            style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        )
    }

    if (colorblindMode) {
        drawText(
            textMeasurer = textMeasurer,
            text = block.type.colorblindTag,
            topLeft = Offset(topLeft.x + w * 0.42f, topLeft.y + h * 0.15f),
            style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        )
    }
}

private fun max1(v: Float): Float = if (v < 1f) 1f else v

private fun blockColors(type: BlockType, skin: String): Triple<Color, Color, Color> {
    if (type != BlockType.WOOD) {
        return when (type) {
            BlockType.ICE -> Triple(Color(0xFF38BDF8), Color(0xFFBAE6FD), Color(0xFFE0F2FE))
            BlockType.RUBBER -> Triple(Color(0xFFEC4899), Color(0xFFF9A8D4), Color(0xFFFCE7F3))
            BlockType.MAGNET -> Triple(Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFFFFFFFF))
            BlockType.GOLD -> Triple(Color(0xFFF59E0B), Color(0xFFFEF08A), Color(0xFFFEF9C3))
            BlockType.BOMB -> Triple(Color(0xFFB91C1C), Color(0xFFF97316), Color(0xFFFCA5A5))
            BlockType.GLASS -> Triple(Color(0xFF93C5FD).copy(alpha = 0.65f), Color(0xFFE0F2FE).copy(alpha = 0.85f), Color.White)
            BlockType.WOOD -> Triple(Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFF92400E))
        }
    }
    return when (skin) {
        "neon" -> Triple(Color(0xFF0E7490), Color(0xFF22D3EE), Color(0xFFEC4899))
        "candy" -> Triple(Color(0xFFF472B6), Color(0xFFFBCFE8), Color(0xFFA78BFA))
        "marble" -> Triple(Color(0xFFCBD5E1), Color(0xFFF8FAFC), Color(0xFF64748B))
        "cyber" -> Triple(Color(0xFF334155), Color(0xFF64748B), Color(0xFF38BDF8))
        // Warm honey-orange wood from the reference art
        else -> Triple(Color(0xFFD27A33), Color(0xFFF0A860), Color(0xFF7A3B12))
    }
}
