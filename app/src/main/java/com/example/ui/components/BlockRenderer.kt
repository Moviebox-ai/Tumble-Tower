package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.game.physics.BlockState
import com.example.game.physics.BlockStress
import com.example.game.physics.BlockType
import kotlin.math.min

data class BlockPalette(
    val frontTop: Color,
    val frontMid: Color,
    val frontBottom: Color,
    val topLight: Color,
    val topDark: Color,
    val sideTop: Color,
    val sideBottom: Color,
    val border: Color,
    val bevelHighlight: Color,
    val accent: Color
)

fun shadeColor(c: Color, factor: Float): Color = Color(
    red = (c.red * factor).coerceIn(0f, 1f),
    green = (c.green * factor).coerceIn(0f, 1f),
    blue = (c.blue * factor).coerceIn(0f, 1f),
    alpha = c.alpha
)

fun tintColor(c: Color, amount: Float): Color = Color(
    red = (c.red + (1f - c.red) * amount).coerceIn(0f, 1f),
    green = (c.green + (1f - c.green) * amount).coerceIn(0f, 1f),
    blue = (c.blue + (1f - c.blue) * amount).coerceIn(0f, 1f),
    alpha = c.alpha
)

private fun max1(v: Float): Float = if (v < 1f) 1f else v

/** Deterministic 0..1 pseudo random so every block keeps the same grain/veins/cracks frame to frame. */
private fun blockFrac(seed: Int, k: Int): Float =
    (((seed * 31 + k * 57) * 2654435761L) ushr 8 and 0xFFL) / 255f

/** Stress heat colour: calm emerald-teal -> amber -> hot crimson. */
fun stressHeatColor(v: Float): Color {
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

fun resolveBlockPalette(type: BlockType, skin: String): BlockPalette {
    if (type != BlockType.WOOD) {
        return when (type) {
            BlockType.ICE -> BlockPalette(
                frontTop = Color(0xFFBAE6FD).copy(alpha = 0.90f),
                frontMid = Color(0xFF38BDF8).copy(alpha = 0.86f),
                frontBottom = Color(0xFF0284C7).copy(alpha = 0.92f),
                topLight = Color(0xFFF0F9FF).copy(alpha = 0.95f),
                topDark = Color(0xFF7DD3FC).copy(alpha = 0.88f),
                sideTop = Color(0xFF0EA5E9).copy(alpha = 0.78f),
                sideBottom = Color(0xFF0369A1).copy(alpha = 0.88f),
                border = Color(0xFFE0F2FE),
                bevelHighlight = Color.White,
                accent = Color(0xFFE0F2FE)
            )
            BlockType.RUBBER -> BlockPalette(
                frontTop = Color(0xFFF472B6),
                frontMid = Color(0xFFEC4899),
                frontBottom = Color(0xFF9D174D),
                topLight = Color(0xFFFCE7F3),
                topDark = Color(0xFFF472B6),
                sideTop = Color(0xFFDB2777),
                sideBottom = Color(0xFF831843),
                border = Color(0xFFFBCFE8),
                bevelHighlight = Color(0xFFFDF2F8),
                accent = Color(0xFFFDF2F8)
            )
            BlockType.MAGNET -> BlockPalette(
                frontTop = Color(0xFFCBD5E1),
                frontMid = Color(0xFF64748B),
                frontBottom = Color(0xFF334155),
                topLight = Color(0xFFF8FAFC),
                topDark = Color(0xFF94A3B8),
                sideTop = Color(0xFF475569),
                sideBottom = Color(0xFF1E293B),
                border = Color(0xFFE2E8F0),
                bevelHighlight = Color.White,
                accent = Color(0xFF38BDF8)
            )
            BlockType.GOLD -> BlockPalette(
                frontTop = Color(0xFFFEF08A),
                frontMid = Color(0xFFF59E0B),
                frontBottom = Color(0xFFB45309),
                topLight = Color(0xFFFEFCE8),
                topDark = Color(0xFFFACC15),
                sideTop = Color(0xFFD97706),
                sideBottom = Color(0xFF78350F),
                border = Color(0xFFFEF9C3),
                bevelHighlight = Color(0xFFFFFFFF),
                accent = Color(0xFFFFFBEB)
            )
            BlockType.BOMB -> BlockPalette(
                frontTop = Color(0xFFEF4444),
                frontMid = Color(0xFFB91C1C),
                frontBottom = Color(0xFF450A0A),
                topLight = Color(0xFFFCA5A5),
                topDark = Color(0xFFDC2626),
                sideTop = Color(0xFF991B1B),
                sideBottom = Color(0xFF3B0712),
                border = Color(0xFFFCA5A5),
                bevelHighlight = Color(0xFFFEE2E2),
                accent = Color(0xFFFACC15)
            )
            BlockType.GLASS -> BlockPalette(
                frontTop = Color(0xFFE0F2FE).copy(alpha = 0.54f),
                frontMid = Color(0xFF93C5FD).copy(alpha = 0.36f),
                frontBottom = Color(0xFF3B82F6).copy(alpha = 0.44f),
                topLight = Color.White.copy(alpha = 0.72f),
                topDark = Color(0xFFBAE6FD).copy(alpha = 0.46f),
                sideTop = Color(0xFF93C5FD).copy(alpha = 0.38f),
                sideBottom = Color(0xFF2563EB).copy(alpha = 0.48f),
                border = Color.White.copy(alpha = 0.90f),
                bevelHighlight = Color.White,
                accent = Color(0xFFE0F2FE)
            )
            BlockType.WOOD -> error("Handled below")
        }
    }

    return when (skin) {
        "neon" -> BlockPalette(
            frontTop = Color(0xFF164E63),
            frontMid = Color(0xFF0E1B38),
            frontBottom = Color(0xFF060B1E),
            topLight = Color(0xFF67E8F9),
            topDark = Color(0xFF0891B2),
            sideTop = Color(0xFF155E75),
            sideBottom = Color(0xFF082F49),
            border = Color(0xFF22D3EE),
            bevelHighlight = Color(0xFFA5F3FC),
            accent = Color(0xFFEC4899)
        )
        "candy" -> BlockPalette(
            frontTop = Color(0xFFFBCFE8),
            frontMid = Color(0xFFF472B6),
            frontBottom = Color(0xFFBE185D),
            topLight = Color(0xFFFFF1F2),
            topDark = Color(0xFFF9A8D4),
            sideTop = Color(0xFFEC4899),
            sideBottom = Color(0xFF9D174D),
            border = Color(0xFFFCE7F3),
            bevelHighlight = Color.White,
            accent = Color(0xFFA78BFA)
        )
        "marble" -> BlockPalette(
            frontTop = Color(0xFFF8FAFC),
            frontMid = Color(0xFFE2E8F0),
            frontBottom = Color(0xFF94A3B8),
            topLight = Color(0xFFFFFFFF),
            topDark = Color(0xFFCBD5E1),
            sideTop = Color(0xFFCBD5E1),
            sideBottom = Color(0xFF64748B),
            border = Color(0xFFF1F5F9),
            bevelHighlight = Color.White,
            accent = Color(0xFFD97706)
        )
        "cyber" -> BlockPalette(
            frontTop = Color(0xFF475569),
            frontMid = Color(0xFF1E293B),
            frontBottom = Color(0xFF0F172A),
            topLight = Color(0xFF94A3B8),
            topDark = Color(0xFF334155),
            sideTop = Color(0xFF334155),
            sideBottom = Color(0xFF020617),
            border = Color(0xFF38BDF8),
            bevelHighlight = Color(0xFFBAE6FD),
            accent = Color(0xFF38BDF8)
        )
        // Royal Oak: handcrafted warm honey-teak timber
        else -> BlockPalette(
            frontTop = Color(0xFFF6B773),
            frontMid = Color(0xFFD27A33),
            frontBottom = Color(0xFF8C4415),
            topLight = Color(0xFFFCD9AC),
            topDark = Color(0xFFE4944A),
            sideTop = Color(0xFFB35D1F),
            sideBottom = Color(0xFF6B310B),
            border = Color(0xFF5D2909),
            bevelHighlight = Color(0xFFFFE7C7),
            accent = Color(0xFF7A3B12)
        )
    }
}

fun DrawScope.drawPremiumIsometricBlock(
    block: BlockState,
    topLeft: Offset,
    bSize: Size,
    depthX: Float,
    depthY: Float,
    skin: String,
    colorblindMode: Boolean,
    textMeasurer: TextMeasurer?,
    isDragged: Boolean = false,
    isWeakWarning: Boolean = false,
    stress: BlockStress? = null,
    xray: Boolean = false
) {
    val palette = resolveBlockPalette(block.type, skin)
    val seed = block.id * 7919 + block.layer * 131
    val w = bSize.width
    val h = bSize.height
    val radius = min(h * 0.24f, 11f)
    val isGlassy = block.type == BlockType.ICE || block.type == BlockType.GLASS
    val liftOffset = if (isDragged) 3.5f else 0f
    val blockTopLeft = Offset(topLeft.x, topLeft.y - liftOffset)

    // 1. Multi-layered Ambient Occlusion & Contact Shadow
    drawRoundRect(
        color = Color.Black.copy(alpha = if (isDragged) 0.34f else 0.24f),
        topLeft = Offset(blockTopLeft.x + 2f, blockTopLeft.y + 4.5f + liftOffset),
        size = Size(w + 1f, h + 1f),
        cornerRadius = CornerRadius(radius, radius)
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.16f),
        topLeft = Offset(blockTopLeft.x + 0.5f, blockTopLeft.y + 2f),
        size = Size(w, h + 1.5f),
        cornerRadius = CornerRadius(radius, radius)
    )

    // 2. Luminous Back Halo for Dragged, Gold, Neon, or Glassy blocks
    if (isDragged || block.type == BlockType.GOLD || (block.type == BlockType.WOOD && skin == "neon")) {
        val haloColor = when {
            isDragged -> Color(0xFFFEF08A).copy(alpha = 0.42f)
            block.type == BlockType.GOLD -> Color(0xFFFACC15).copy(alpha = 0.26f)
            else -> Color(0xFF22D3EE).copy(alpha = 0.28f)
        }
        drawRoundRect(
            color = haloColor,
            topLeft = Offset(blockTopLeft.x - 3f, blockTopLeft.y - 3f),
            size = Size(w + 6f, h + 6f),
            cornerRadius = CornerRadius(radius + 3f, radius + 3f),
            style = Stroke(width = max1(h * 0.12f))
        )
    }

    // 3. Top Face (3D perspective bevel lit from above)
    val topInset = min(w * 0.04f, 3f)
    val topFace = Path().apply {
        moveTo(blockTopLeft.x + topInset, blockTopLeft.y + 1f)
        lineTo(blockTopLeft.x + depthX + topInset, blockTopLeft.y + depthY)
        lineTo(blockTopLeft.x + w + depthX - topInset, blockTopLeft.y + depthY)
        lineTo(blockTopLeft.x + w - topInset, blockTopLeft.y + 1f)
        close()
    }
    drawPath(
        path = topFace,
        brush = Brush.verticalGradient(
            colors = listOf(palette.topLight, palette.topDark),
            startY = blockTopLeft.y + depthY,
            endY = blockTopLeft.y + 1f
        )
    )
    // Specular highlight strip along back-top edge of top face
    drawLine(
        color = palette.bevelHighlight.copy(alpha = if (isGlassy) 0.55f else 0.34f),
        start = Offset(blockTopLeft.x + depthX + topInset + 2f, blockTopLeft.y + depthY + 1f),
        end = Offset(blockTopLeft.x + w + depthX - topInset - 2f, blockTopLeft.y + depthY + 1f),
        strokeWidth = max1(h * 0.045f),
        cap = StrokeCap.Round
    )

    // 4. Side Extrusion Face (3D depth with realistic shadow falloff)
    val isRightSide = depthX >= 0f
    val sideEdgeX = if (isRightSide) blockTopLeft.x + w - 1f else blockTopLeft.x + 1f
    val sideFace = Path().apply {
        moveTo(sideEdgeX, blockTopLeft.y + 2f)
        lineTo(sideEdgeX + depthX, blockTopLeft.y + depthY + 1f)
        lineTo(sideEdgeX + depthX, blockTopLeft.y + h + depthY - 2f)
        lineTo(sideEdgeX, blockTopLeft.y + h - 2f)
        close()
    }
    drawPath(
        path = sideFace,
        brush = Brush.verticalGradient(
            colors = listOf(palette.sideTop, palette.sideBottom),
            startY = blockTopLeft.y + depthY,
            endY = blockTopLeft.y + h
        )
    )

    // Side end-grain / material detail so the 3D thickness feels authentic
    if (absMin(depthX) > 4f) {
        if (block.type == BlockType.WOOD && (skin == "wood" || skin.isBlank())) {
            val midSideX = sideEdgeX + depthX * 0.5f
            val midSideY = blockTopLeft.y + h * 0.5f + depthY * 0.5f
            drawCircle(
                color = shadeColor(palette.sideBottom, 0.72f).copy(alpha = 0.45f),
                radius = min(absMin(depthX) * 0.28f, h * 0.22f),
                center = Offset(midSideX, midSideY),
                style = Stroke(width = 1.2f)
            )
        } else if (block.type == BlockType.GOLD) {
            drawLine(
                color = palette.topLight.copy(alpha = 0.48f),
                start = Offset(sideEdgeX + depthX * 0.25f, blockTopLeft.y + h * 0.28f + depthY * 0.25f),
                end = Offset(sideEdgeX + depthX * 0.75f, blockTopLeft.y + h * 0.28f + depthY * 0.75f),
                strokeWidth = 1.5f
            )
        }
    }

    // 5. Front Face Rich 3-Stop Studio Gradient
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.frontTop, palette.frontMid, palette.frontBottom),
            startY = blockTopLeft.y,
            endY = blockTopLeft.y + h
        ),
        topLeft = blockTopLeft,
        size = bSize,
        cornerRadius = CornerRadius(radius, radius)
    )

    // 6. Clipped Material Craftsmanship (grains, bevels, facets, metallic framing, rivets)
    val clip = Path().apply {
        addRoundRect(
            RoundRect(
                left = blockTopLeft.x,
                top = blockTopLeft.y,
                right = blockTopLeft.x + w,
                bottom = blockTopLeft.y + h,
                radiusX = radius,
                radiusY = radius
            )
        )
    }

    clipPath(clip) {
        when (block.type) {
            BlockType.WOOD -> drawWoodSkinDetail(blockTopLeft, w, h, radius, seed, skin, palette)
            BlockType.ICE -> drawIceCrystalDetail(blockTopLeft, w, h, radius, seed, block.meltProgress, palette)
            BlockType.GLASS -> drawGlassPrismDetail(blockTopLeft, w, h, radius, seed)
            BlockType.GOLD -> drawGoldBullionDetail(blockTopLeft, w, h, radius, palette)
            BlockType.RUBBER -> drawRubberGripDetail(blockTopLeft, w, h, radius, palette)
            BlockType.MAGNET -> drawMagnetHardwareDetail(blockTopLeft, w, h, palette)
            BlockType.BOMB -> drawBombCrateDetail(blockTopLeft, w, h, palette)
        }

        // Universal Machined Chamfer / Inner Bevel Rim for Premium Tactility
        val inset = max1(h * 0.08f)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.bevelHighlight.copy(alpha = if (isGlassy) 0.55f else 0.34f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.22f)
                ),
                startY = blockTopLeft.y,
                endY = blockTopLeft.y + h
            ),
            topLeft = Offset(blockTopLeft.x + inset * 0.5f, blockTopLeft.y + inset * 0.5f),
            size = Size((w - inset).coerceAtLeast(2f), (h - inset).coerceAtLeast(2f)),
            cornerRadius = CornerRadius((radius - inset * 0.4f).coerceAtLeast(2f)),
            style = Stroke(width = max1(h * 0.055f))
        )

        // Subtle curved top-left studio catchlight sheen
        drawLine(
            color = Color.White.copy(alpha = if (isGlassy) 0.78f else 0.46f),
            start = Offset(blockTopLeft.x + radius + 1f, blockTopLeft.y + max1(h * 0.06f)),
            end = Offset(blockTopLeft.x + w - radius - 1f, blockTopLeft.y + max1(h * 0.06f)),
            strokeWidth = max1(h * 0.065f),
            cap = StrokeCap.Round
        )

        // Tension Cracks (Visible overload feedback even without Stress Vision)
        val load = block.pressureLoad
        if (load > 0.70f) {
            val a = ((load - 0.70f) / 0.30f).coerceIn(0f, 1f)
            val crackColor = Color(0xFF1F1209).copy(alpha = 0.40f + 0.50f * a)
            val cx = blockTopLeft.x + w * (0.35f + blockFrac(seed, 40) * 0.30f)
            val path = Path().apply {
                moveTo(cx, blockTopLeft.y)
                lineTo(cx - h * 0.14f, blockTopLeft.y + h * 0.30f)
                lineTo(cx + h * 0.11f, blockTopLeft.y + h * 0.56f)
                lineTo(cx - h * 0.07f, blockTopLeft.y + h * 0.80f)
                if (a > 0.45f) lineTo(cx + h * 0.09f, blockTopLeft.y + h)
            }
            drawPath(
                path = path,
                color = crackColor,
                style = Stroke(width = max1(h * (0.045f + 0.045f * a)), cap = StrokeCap.Round)
            )
            if (a > 0.6f) {
                drawPath(
                    path = path,
                    color = Color(0xFFFCA5A5).copy(alpha = 0.35f * a),
                    style = Stroke(width = max1(h * 0.025f), cap = StrokeCap.Round)
                )
            }
        }

        // Glued Emerald Resin Seal Dots on corners
        if (block.isGlued) {
            val beadR = max1(h * 0.11f)
            drawCircle(Color(0xFF4ADE80), radius = beadR, center = Offset(blockTopLeft.x + w * 0.10f, blockTopLeft.y + h * 0.22f))
            drawCircle(Color(0xFF4ADE80), radius = beadR, center = Offset(blockTopLeft.x + w * 0.90f, blockTopLeft.y + h * 0.22f))
        }

        // Stress Vision X-Ray Heat Overlay + Load Gauge
        if (xray && stress != null) {
            drawRect(
                color = stressHeatColor(stress.pullRisk).copy(alpha = 0.50f),
                topLeft = blockTopLeft,
                size = bSize
            )
            val gaugeH = max1(h * 0.13f)
            drawRect(
                color = Color.Black.copy(alpha = 0.50f),
                topLeft = Offset(blockTopLeft.x, blockTopLeft.y + h - gaugeH),
                size = Size(w, gaugeH)
            )
            drawRect(
                color = stressHeatColor(stress.ratio),
                topLeft = Offset(blockTopLeft.x, blockTopLeft.y + h - gaugeH),
                size = Size(w * stress.ratio.coerceIn(0f, 1f), gaugeH)
            )
        }
    }

    // 7. Crisp Outer Frame Border
    val borderColor = when {
        isDragged -> Color(0xFFFEF08A)
        block.isGlued -> Color(0xFF22C55E)
        isWeakWarning -> Color(0xFFEF4444)
        else -> palette.border
    }
    val strokeW = if (isDragged || block.isGlued || isWeakWarning) 3.0f else 1.7f
    drawRoundRect(
        color = borderColor,
        topLeft = blockTopLeft,
        size = bSize,
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = strokeW)
    )

    // 8. TNT Badge + Lit Spark Wick
    if (block.type == BlockType.BOMB && textMeasurer != null) {
        drawRoundRect(
            color = Color(0xFF18181B).copy(alpha = 0.82f),
            topLeft = Offset(blockTopLeft.x + w * 0.12f, blockTopLeft.y + h * 0.22f),
            size = Size((w * 0.54f).coerceAtLeast(36f), h * 0.56f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawText(
            textMeasurer = textMeasurer,
            text = "TNT ${block.bombFuseMoves}",
            topLeft = Offset(blockTopLeft.x + w * 0.16f, blockTopLeft.y + h * 0.26f),
            style = TextStyle(color = Color(0xFFFDE047), fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
        )
        // Sparking fuse ember on top-right edge
        drawCircle(
            color = Color(0xFFFACC15),
            radius = max1(h * 0.13f),
            center = Offset(blockTopLeft.x + w * 0.86f, blockTopLeft.y + h * 0.24f)
        )
        drawCircle(
            color = Color.White,
            radius = max1(h * 0.06f),
            center = Offset(blockTopLeft.x + w * 0.86f, blockTopLeft.y + h * 0.24f)
        )
    }

    // 9. Stress Vision Pull-Risk % Label
    if (xray && stress != null && block.layer >= 0 && stress.pullRisk > 0.01f && w > 40f && textMeasurer != null) {
        drawText(
            textMeasurer = textMeasurer,
            text = "${(stress.pullRisk * 100f).toInt()}%",
            topLeft = Offset(blockTopLeft.x + w * 0.5f - 11f, blockTopLeft.y + h * 0.5f - 7f),
            style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        )
    }

    // 10. Colorblind Tag
    if (colorblindMode && textMeasurer != null) {
        drawText(
            textMeasurer = textMeasurer,
            text = block.type.colorblindTag,
            topLeft = Offset(blockTopLeft.x + w * 0.42f, blockTopLeft.y + h * 0.16f),
            style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        )
    }
}

private fun absMin(v: Float): Float = if (v < 0f) -v else v

private fun DrawScope.drawWoodSkinDetail(
    topLeft: Offset,
    w: Float,
    h: Float,
    radius: Float,
    seed: Int,
    skin: String,
    palette: BlockPalette
) {
    when (skin) {
        "neon" -> {
            // Neon circuit traces & electric magenta-cyan edge pulse
            val traceColor = palette.border.copy(alpha = 0.65f)
            drawLine(
                color = traceColor,
                start = Offset(topLeft.x + w * 0.08f, topLeft.y + h * 0.30f),
                end = Offset(topLeft.x + w * 0.42f, topLeft.y + h * 0.30f),
                strokeWidth = max1(h * 0.045f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = traceColor,
                start = Offset(topLeft.x + w * 0.42f, topLeft.y + h * 0.30f),
                end = Offset(topLeft.x + w * 0.52f, topLeft.y + h * 0.66f),
                strokeWidth = max1(h * 0.045f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = palette.accent.copy(alpha = 0.85f),
                start = Offset(topLeft.x + w * 0.52f, topLeft.y + h * 0.66f),
                end = Offset(topLeft.x + w * 0.90f, topLeft.y + h * 0.66f),
                strokeWidth = max1(h * 0.045f),
                cap = StrokeCap.Round
            )
            drawCircle(palette.accent, radius = max1(h * 0.09f), center = Offset(topLeft.x + w * 0.14f, topLeft.y + h * 0.68f))
            drawCircle(palette.border, radius = max1(h * 0.09f), center = Offset(topLeft.x + w * 0.84f, topLeft.y + h * 0.30f))
        }
        "candy" -> {
            // Glossy diagonal sugar-glaze stripes + soft cream highlight
            for (i in -1..3) {
                val sx = topLeft.x + w * (0.12f + i * 0.26f)
                val stripe = Path().apply {
                    moveTo(sx, topLeft.y + h)
                    lineTo(sx + h * 0.55f, topLeft.y)
                    lineTo(sx + h * 0.82f, topLeft.y)
                    lineTo(sx + h * 0.27f, topLeft.y + h)
                    close()
                }
                drawPath(stripe, color = Color.White.copy(alpha = 0.20f))
            }
            drawOval(
                color = Color.White.copy(alpha = 0.42f),
                topLeft = Offset(topLeft.x + w * 0.08f, topLeft.y + h * 0.14f),
                size = Size(w * 0.26f, h * 0.20f)
            )
        }
        "marble" -> {
            // Organic Aegean gold & slate mineral veining
            val veinDark = Color(0xFF475569).copy(alpha = 0.28f)
            val veinGold = palette.accent.copy(alpha = 0.34f)
            val p1 = Path().apply {
                moveTo(topLeft.x, topLeft.y + h * (0.25f + blockFrac(seed, 1) * 0.25f))
                quadraticTo(
                    topLeft.x + w * 0.35f, topLeft.y + h * 0.12f,
                    topLeft.x + w * 0.62f, topLeft.y + h * 0.58f
                )
                quadraticTo(
                    topLeft.x + w * 0.82f, topLeft.y + h * 0.88f,
                    topLeft.x + w, topLeft.y + h * 0.42f
                )
            }
            drawPath(p1, color = veinDark, style = Stroke(width = max1(h * 0.055f), cap = StrokeCap.Round))
            val p2 = Path().apply {
                moveTo(topLeft.x + w * 0.18f, topLeft.y + h)
                quadraticTo(
                    topLeft.x + w * 0.48f, topLeft.y + h * 0.52f,
                    topLeft.x + w * 0.88f, topLeft.y
                )
            }
            drawPath(p2, color = veinGold, style = Stroke(width = max1(h * 0.035f), cap = StrokeCap.Round))
        }
        "cyber" -> {
            // Brushed alloy plates, recessed tech groove & corner micro-rivets
            drawRect(
                color = Color.Black.copy(alpha = 0.25f),
                topLeft = Offset(topLeft.x + w * 0.18f, topLeft.y + h * 0.24f),
                size = Size(w * 0.64f, h * 0.52f)
            )
            drawLine(
                color = palette.accent.copy(alpha = 0.85f),
                start = Offset(topLeft.x + w * 0.24f, topLeft.y + h * 0.50f),
                end = Offset(topLeft.x + w * 0.76f, topLeft.y + h * 0.50f),
                strokeWidth = max1(h * 0.07f),
                cap = StrokeCap.Round
            )
            val rivetR = max1(h * 0.065f)
            val rivetCol = Color(0xFF94A3B8)
            drawCircle(rivetCol, rivetR, Offset(topLeft.x + w * 0.08f, topLeft.y + h * 0.24f))
            drawCircle(rivetCol, rivetR, Offset(topLeft.x + w * 0.92f, topLeft.y + h * 0.24f))
            drawCircle(rivetCol, rivetR, Offset(topLeft.x + w * 0.08f, topLeft.y + h * 0.76f))
            drawCircle(rivetCol, rivetR, Offset(topLeft.x + w * 0.92f, topLeft.y + h * 0.76f))
        }
        else -> {
            // Classic Royal Oak: dual-tone handcrafted wood grain + satin lacquer reflection
            val darkGrain = shadeColor(palette.frontMid, 0.58f).copy(alpha = 0.48f)
            val lightGrain = tintColor(palette.frontTop, 0.22f).copy(alpha = 0.26f)
            for (i in 0..3) {
                val gy = topLeft.y + h * (0.18f + 0.21f * i) + (blockFrac(seed, i) - 0.5f) * h * 0.07f
                val path = Path().apply {
                    moveTo(topLeft.x - 2f, gy)
                    quadraticTo(
                        topLeft.x + w * (0.26f + blockFrac(seed, i + 9) * 0.20f), gy - h * 0.10f,
                        topLeft.x + w * 0.55f, gy + h * 0.03f
                    )
                    quadraticTo(
                        topLeft.x + w * 0.82f, gy + h * 0.12f,
                        topLeft.x + w + 2f, gy - h * 0.02f
                    )
                }
                drawPath(path, color = if (i % 2 == 0) darkGrain else lightGrain, style = Stroke(width = max1(h * 0.045f), cap = StrokeCap.Round))
            }
            if (blockFrac(seed, 3) > 0.35f) {
                val kx = topLeft.x + w * (0.22f + blockFrac(seed, 4) * 0.56f)
                val ky = topLeft.y + h * (0.36f + blockFrac(seed, 5) * 0.28f)
                drawOval(
                    color = shadeColor(palette.frontBottom, 0.65f).copy(alpha = 0.55f),
                    topLeft = Offset(kx - h * 0.17f, ky - h * 0.11f),
                    size = Size(h * 0.34f, h * 0.22f),
                    style = Stroke(width = max1(h * 0.045f))
                )
            }
            // Diagonal subtle wood polish sheen
            val sheen = Path().apply {
                moveTo(topLeft.x + w * 0.10f, topLeft.y)
                lineTo(topLeft.x + w * 0.28f, topLeft.y)
                lineTo(topLeft.x + w * 0.14f, topLeft.y + h)
                lineTo(topLeft.x + w * 0.02f, topLeft.y + h)
                close()
            }
            drawPath(sheen, color = Color.White.copy(alpha = 0.12f))
        }
    }
}

private fun DrawScope.drawIceCrystalDetail(
    topLeft: Offset,
    w: Float,
    h: Float,
    radius: Float,
    seed: Int,
    meltProgress: Float,
    palette: BlockPalette
) {
    // Internal diamond crystal facet shards
    val facet1 = Path().apply {
        moveTo(topLeft.x + w * 0.14f, topLeft.y)
        lineTo(topLeft.x + w * 0.42f, topLeft.y)
        lineTo(topLeft.x + w * 0.26f, topLeft.y + h)
        lineTo(topLeft.x + w * 0.05f, topLeft.y + h)
        close()
    }
    drawPath(facet1, color = Color.White.copy(alpha = 0.28f))

    val facet2 = Path().apply {
        moveTo(topLeft.x + w * 0.60f, topLeft.y)
        lineTo(topLeft.x + w * 0.78f, topLeft.y + h * 0.58f)
        lineTo(topLeft.x + w * 0.52f, topLeft.y + h)
        close()
    }
    drawPath(facet2, color = palette.topLight.copy(alpha = 0.22f))

    // Crisp glacial fractures
    val crackColor = Color.White.copy(alpha = 0.78f)
    for (i in 0..1) {
        val cx = topLeft.x + w * (0.24f + 0.44f * i + blockFrac(seed, i + 20) * 0.12f)
        val path = Path().apply {
            moveTo(cx, topLeft.y)
            lineTo(cx + h * 0.20f, topLeft.y + h * 0.34f)
            lineTo(cx - h * 0.09f, topLeft.y + h * 0.64f)
            lineTo(cx + h * 0.15f, topLeft.y + h)
        }
        drawPath(path, color = crackColor, style = Stroke(width = max1(h * 0.032f), cap = StrokeCap.Round))
    }

    // Dynamic frost rim (shrinks slightly as block melts)
    drawLine(
        color = Color.White.copy(alpha = 0.92f),
        start = Offset(topLeft.x + radius, topLeft.y + h * 0.13f),
        end = Offset(
            topLeft.x + w * (0.62f * (1f - meltProgress * 0.45f)).coerceAtLeast(0.22f),
            topLeft.y + h * 0.13f
        ),
        strokeWidth = max1(h * 0.065f),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawGlassPrismDetail(
    topLeft: Offset,
    w: Float,
    h: Float,
    radius: Float,
    seed: Int
) {
    // Architectural dual glass reflection streaks
    val streak1 = Path().apply {
        moveTo(topLeft.x + w * 0.18f, topLeft.y)
        lineTo(topLeft.x + w * 0.34f, topLeft.y)
        lineTo(topLeft.x + w * 0.16f, topLeft.y + h)
        lineTo(topLeft.x + w * 0.04f, topLeft.y + h)
        close()
    }
    val streak2 = Path().apply {
        moveTo(topLeft.x + w * 0.42f, topLeft.y)
        lineTo(topLeft.x + w * 0.49f, topLeft.y)
        lineTo(topLeft.x + w * 0.31f, topLeft.y + h)
        lineTo(topLeft.x + w * 0.24f, topLeft.y + h)
        close()
    }
    drawPath(streak1, color = Color.White.copy(alpha = 0.42f))
    drawPath(streak2, color = Color.White.copy(alpha = 0.26f))

    // Subtle prism tint along bottom refraction edge
    drawLine(
        color = Color(0xFF38BDF8).copy(alpha = 0.55f),
        start = Offset(topLeft.x + radius, topLeft.y + h - 2f),
        end = Offset(topLeft.x + w - radius, topLeft.y + h - 2f),
        strokeWidth = max1(h * 0.06f),
        cap = StrokeCap.Round
    )
    if (blockFrac(seed, 7) > 0.5f) {
        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(topLeft.x + w * 0.72f, topLeft.y + h * 0.20f),
            end = Offset(topLeft.x + w * 0.84f, topLeft.y + h * 0.46f),
            strokeWidth = max1(h * 0.03f),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawGoldBullionDetail(
    topLeft: Offset,
    w: Float,
    h: Float,
    radius: Float,
    palette: BlockPalette
) {
    // Minted inner recessed ingot bezel
    val padX = max1(w * 0.10f)
    val padY = max1(h * 0.18f)
    drawRoundRect(
        color = shadeColor(palette.frontBottom, 0.72f).copy(alpha = 0.48f),
        topLeft = Offset(topLeft.x + padX, topLeft.y + padY),
        size = Size((w - padX * 2f).coerceAtLeast(4f), (h - padY * 2f).coerceAtLeast(4f)),
        cornerRadius = CornerRadius((radius * 0.5f).coerceAtLeast(2f)),
        style = Stroke(width = max1(h * 0.055f))
    )

    // Diagonal brushed 24K metallic gleam
    val gleam = Path().apply {
        moveTo(topLeft.x + w * 0.22f, topLeft.y)
        lineTo(topLeft.x + w * 0.46f, topLeft.y)
        lineTo(topLeft.x + w * 0.28f, topLeft.y + h)
        lineTo(topLeft.x + w * 0.04f, topLeft.y + h)
        close()
    }
    drawPath(gleam, color = Color.White.copy(alpha = 0.38f))

    // Center diamond hallmark crest
    val cx = topLeft.x + w * 0.5f
    val cy = topLeft.y + h * 0.5f
    val dr = min(w * 0.10f, h * 0.24f)
    val crest = Path().apply {
        moveTo(cx, cy - dr)
        lineTo(cx + dr * 1.15f, cy)
        lineTo(cx, cy + dr)
        lineTo(cx - dr * 1.15f, cy)
        close()
    }
    drawPath(crest, color = Color(0xFFFFFBEB).copy(alpha = 0.60f))
    drawPath(crest, color = shadeColor(palette.frontBottom, 0.65f).copy(alpha = 0.65f), style = Stroke(width = 1.2f))
}

private fun DrawScope.drawRubberGripDetail(
    topLeft: Offset,
    w: Float,
    h: Float,
    radius: Float,
    palette: BlockPalette
) {
    // Tactile grip ribs + bouncy satin sheen
    val dotR = max1(h * 0.06f)
    val dotColor = shadeColor(palette.frontBottom, 0.70f).copy(alpha = 0.45f)
    val cols = if (w > 95f) 7 else 4
    for (c in 0 until cols) {
        val x = topLeft.x + w * (0.18f + 0.64f * (c / (cols - 1).coerceAtLeast(1).toFloat()))
        drawCircle(dotColor, dotR, Offset(x, topLeft.y + h * 0.38f))
        drawCircle(dotColor, dotR, Offset(x, topLeft.y + h * 0.66f))
    }
    drawOval(
        color = Color.White.copy(alpha = 0.36f),
        topLeft = Offset(topLeft.x + w * 0.08f, topLeft.y + h * 0.12f),
        size = Size(w * 0.32f, h * 0.18f)
    )
}

private fun DrawScope.drawMagnetHardwareDetail(
    topLeft: Offset,
    w: Float,
    h: Float,
    palette: BlockPalette
) {
    // Dual anodized red & cobalt poles flanking a brushed steel bridge
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFF87171), Color(0xFFDC2626), Color(0xFF7F1D1D))),
        topLeft = topLeft,
        size = Size(w * 0.34f, h)
    )
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB), Color(0xFF1E3A8A))),
        topLeft = Offset(topLeft.x + w * 0.66f, topLeft.y),
        size = Size(w * 0.34f, h)
    )
    // Chrome divider collars
    drawRect(Color.White.copy(alpha = 0.90f), Offset(topLeft.x + w * 0.32f, topLeft.y), Size(max1(w * 0.04f), h))
    drawRect(Color.White.copy(alpha = 0.90f), Offset(topLeft.x + w * 0.64f, topLeft.y), Size(max1(w * 0.04f), h))
    // Glowing core flux line
    drawLine(
        color = palette.accent.copy(alpha = 0.85f),
        start = Offset(topLeft.x + w * 0.38f, topLeft.y + h * 0.5f),
        end = Offset(topLeft.x + w * 0.62f, topLeft.y + h * 0.5f),
        strokeWidth = max1(h * 0.07f),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawBombCrateDetail(
    topLeft: Offset,
    w: Float,
    h: Float,
    palette: BlockPalette
) {
    // Caution hazard chevrons + dark metallic frame plates
    val stripeColor = palette.accent.copy(alpha = 0.42f)
    val bandH = max1(h * 0.18f)
    for (i in 0..6) {
        val sx = topLeft.x + w * (i / 6f) - h * 0.25f
        val topStripe = Path().apply {
            moveTo(sx, topLeft.y + bandH)
            lineTo(sx + bandH * 0.7f, topLeft.y)
            lineTo(sx + bandH * 1.35f, topLeft.y)
            lineTo(sx + bandH * 0.65f, topLeft.y + bandH)
            close()
        }
        drawPath(topStripe, color = stripeColor)
    }
    drawRect(
        color = Color.Black.copy(alpha = 0.35f),
        topLeft = Offset(topLeft.x, topLeft.y + h - bandH),
        size = Size(w, bandH)
    )
    // Corner rivets
    val rivetR = max1(h * 0.055f)
    drawCircle(Color(0xFFFCA5A5), rivetR, Offset(topLeft.x + w * 0.07f, topLeft.y + h * 0.75f))
    drawCircle(Color(0xFFFCA5A5), rivetR, Offset(topLeft.x + w * 0.93f, topLeft.y + h * 0.75f))
}
