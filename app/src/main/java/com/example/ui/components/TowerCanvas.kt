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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.rememberTextMeasurer
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

            // 1. Beveled Temple-Stone Pedestal + Live Center-of-Mass Plumb Gauge
            val pedW = 296f * unitScale
            val pedH = 46f * unitScale
            drawOval(
                color = Color.Black.copy(alpha = 0.42f),
                topLeft = Offset(centerX - pedW * 0.52f, baseY + pedH - 8f * unitScale),
                size = Size(pedW * 1.04f, 20f * unitScale)
            )
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF0F172A))),
                topLeft = Offset(centerX - pedW / 2f, baseY),
                size = Size(pedW, pedH),
                cornerRadius = CornerRadius(14f, 14f)
            )
            // Top chamfered stone slab
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF94A3B8), Color(0xFF64748B))),
                topLeft = Offset(centerX - pedW / 2f + 2f, baseY),
                size = Size(pedW - 4f, pedH * 0.28f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            // Chiselled trim inlay & world-accent rim
            drawRoundRect(
                color = worldTheme.accentColor.copy(alpha = 0.58f),
                topLeft = Offset(centerX - pedW / 2f, baseY),
                size = Size(pedW, pedH),
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = 2.4f)
            )
            // Soft contact shadow directly under layer 0 blocks
            drawOval(
                color = Color.Black.copy(alpha = 0.36f),
                topLeft = Offset(centerX - pedW * 0.42f, baseY - 5f * unitScale),
                size = Size(pedW * 0.84f, 13f * unitScale)
            )
            for (i in 0..6) {
                val sx = centerX - pedW * 0.44f + pedW * 0.88f * (i / 6f)
                drawLine(
                    color = Color.Black.copy(alpha = 0.25f),
                    start = Offset(sx, baseY + pedH * 0.28f),
                    end = Offset(sx + 6f * unitScale, baseY + pedH * (0.64f + 0.08f * (i % 3))),
                    strokeWidth = 1.8f
                )
                drawRoundRect(
                    color = worldTheme.accentColor.copy(alpha = 0.48f),
                    topLeft = Offset(sx - 5f * unitScale, baseY - 2f * unitScale),
                    size = Size(10f * unitScale, 4f * unitScale),
                    cornerRadius = CornerRadius(3f, 3f)
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
            drawCircle(comColor.copy(alpha = 0.35f), radius = 9f * unitScale, center = Offset(comIndicatorX, baseY + pedH * 0.16f))
            drawCircle(comColor, radius = 4.8f * unitScale, center = Offset(comIndicatorX, baseY + pedH * 0.16f))

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
                        drawPremiumIsometricBlock(
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
                    drawPremiumIsometricBlock(
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
