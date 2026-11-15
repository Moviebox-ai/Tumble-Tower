package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PowerUpInventory
import com.example.game.physics.BlockType
import com.example.game.physics.LiveEventType
import com.example.game.physics.PowerUpType
import com.example.services.AdConfig
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyanIce
import com.example.ui.theme.EmeraldGo
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldSun
import com.example.ui.theme.SkyfallGlassBorder
import com.example.ui.theme.SkyfallMidnight
import com.example.ui.theme.SkyfallSlateCard
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MockupBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(listOf(Color(0xFF67E8F9), Color(0xFF06B6D4)))
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color(0xFF0A1436),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = SkyfallGlassBorder,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .border(1.4.dp, borderColor.copy(alpha = 0.78f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0C163B).copy(alpha = 0.88f),
        tonalElevation = 4.dp
    ) {
        content()
    }
}

@Composable
fun ChunkyGameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = GoldSun,
    contentColor: Color = SkyfallMidnight,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "btn_scale"
    )
    val bottomColor = Color(
        red = (containerColor.red * 0.76f).coerceIn(0f, 1f),
        green = (containerColor.green * 0.76f).coerceIn(0f, 1f),
        blue = (containerColor.blue * 0.76f).coerceIn(0f, 1f),
        alpha = containerColor.alpha
    )
    Row(
        modifier = modifier
            .scale(scale)
            .shadow(10.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (enabled) {
                    Brush.verticalGradient(listOf(containerColor, bottomColor))
                } else {
                    Brush.verticalGradient(listOf(containerColor.copy(alpha = 0.4f), bottomColor.copy(alpha = 0.4f)))
                }
            )
            .border(1.8.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TopCurrencyBar(
    displayName: String,
    xpLevel: Int,
    coins: Long,
    gems: Long,
    onProfileClick: () -> Unit,
    onShopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar + Lv. XP pill matching Panel 2
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0B1437).copy(alpha = 0.85f))
                .border(1.5.dp, GoldSun.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                .clickable(onClick = onProfileClick)
                .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
                .testTag("profile_chip"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFFDE047), Color(0xFFF97316))))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = displayName,
                    tint = Color(0xFF1E1B4B),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Lv. $xpLevel",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CurrencyChip(
                icon = Icons.Default.MonetizationOn,
                value = formatter.format(coins),
                tint = GoldSun,
                onClick = onShopClick
            )
            CurrencyChip(
                icon = Icons.Default.Diamond,
                value = formatter.format(gems),
                tint = Color(0xFFC084FC),
                onClick = onShopClick
            )
        }
    }
}

@Composable
private fun CurrencyChip(
    icon: ImageVector,
    value: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0A1334).copy(alpha = 0.88f))
            .border(1.2.dp, tint.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(start = 7.dp, end = 5.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(EmeraldGo),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Buy", tint = SkyfallMidnight, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun StabilityMeterBar(
    stabilityPercent: Int,
    colorblindMode: Boolean,
    modifier: Modifier = Modifier
) {
    val activeSegments = (stabilityPercent / 10).coerceIn(0, 10)
    val badgeColor by animateColorAsState(
        targetValue = when {
            stabilityPercent >= 65 -> EmeraldGo
            stabilityPercent >= 35 -> GoldSun
            else -> CrimsonDanger
        },
        label = "stab_color"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0A122E).copy(alpha = 0.86f))
            .border(1.5.dp, CyanIce.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (colorblindMode) {
                when {
                    stabilityPercent >= 65 -> "OK"
                    stabilityPercent >= 35 -> "!!"
                    else -> "XX"
                }
            } else "Stability",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            for (seg in 9 downTo 0) {
                val segColor = when {
                    seg >= 6 -> EmeraldGo
                    seg >= 3 -> GoldSun
                    else -> CrimsonDanger
                }
                val isLit = seg < activeSegments
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isLit) segColor else Color.White.copy(alpha = 0.14f))
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$stabilityPercent%",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = Color.White
        )
    }
}

@Composable
fun SteadyHandPrecisionBar(
    precision: Float,
    combo: Int,
    modifier: Modifier = Modifier
) {
    val clamped = precision.coerceIn(0.03f, 0.97f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0B1433).copy(alpha = 0.88f))
            .border(1.5.dp, Color(0xFF475569), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Precision",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            if (combo > 0) {
                Text(
                    text = "COMBO x$combo",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = GoldSun
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(EmeraldGo, GoldSun, Color(0xFFF97316), CrimsonDanger)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            )
            // Vertical sliding white/gold needle marker matching Panel 5
            Canvas(modifier = Modifier.fillMaxSize()) {
                val needleX = size.width * clamped
                drawRoundRect(
                    color = GoldSun,
                    topLeft = Offset(needleX - 4f, 0f),
                    size = Size(8f, size.height),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(needleX - 2f, 1f),
                    size = Size(4f, size.height - 2f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }
}

@Composable
fun EventWarningBanner(
    activeEvent: LiveEventType,
    incomingEvent: LiveEventType?,
    warningSeconds: Int,
    freezeSecondsLeft: Int,
    modifier: Modifier = Modifier
) {
    val event = when {
        freezeSecondsLeft > 0 -> null
        activeEvent != LiveEventType.NONE -> activeEvent
        incomingEvent != null -> incomingEvent
        else -> null
    }
    if (freezeSecondsLeft > 0) {
        Row(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF0E7490), Color(0xFF1E3A8A))))
                .border(1.5.dp, CyanIce, RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AcUnit, contentDescription = null, tint = CyanIce, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Freeze Active", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Text("${freezeSecondsLeft}s", style = MaterialTheme.typography.titleMedium, color = CyanIce)
        }
        return
    }
    if (event == null) return

    val isIncoming = activeEvent == LiveEventType.NONE
    val icon = when (event) {
        LiveEventType.WIND -> Icons.Default.Air
        LiveEventType.EARTHQUAKE -> Icons.Default.Waves
        LiveEventType.RAIN -> Icons.Default.WaterDrop
        LiveEventType.LOW_GRAVITY -> Icons.Default.FlashOn
        LiveEventType.NONE -> Icons.Default.Air
    }
    Row(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF4C1D95), Color(0xFF312E81))))
            .border(1.5.dp, Color(0xFF818CF8), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = CyanIce, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isIncoming) "${event.title} Incoming!" else "${event.title} Active!",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
        Text(
            text = if (isIncoming) "${warningSeconds}s" else "LIVE",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = GoldSun
        )
    }
}

@Composable
fun PowerUpSideBar(
    inventory: PowerUpInventory,
    shieldArmed: Boolean,
    glueArmed: Boolean,
    canUndo: Boolean,
    onUsePowerUp: (PowerUpType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PowerUpButton(
            icon = Icons.Default.AcUnit,
            label = "Freeze",
            count = inventory.freeze,
            tint = CyanIce,
            active = false,
            onClick = { onUsePowerUp(PowerUpType.FREEZE) },
            testTag = "pu_freeze"
        )
        PowerUpButton(
            icon = Icons.Default.WaterDrop,
            label = "Glue",
            count = inventory.glue,
            tint = GoldSun,
            active = glueArmed,
            onClick = { onUsePowerUp(PowerUpType.GLUE) },
            testTag = "pu_glue"
        )
        PowerUpButton(
            icon = Icons.AutoMirrored.Filled.Undo,
            label = "Undo",
            count = inventory.undo,
            tint = Color(0xFFC084FC),
            active = false,
            enabled = canUndo,
            onClick = { onUsePowerUp(PowerUpType.UNDO) },
            testTag = "pu_undo"
        )
        PowerUpButton(
            icon = Icons.Default.Shield,
            label = "Shield",
            count = inventory.shield,
            tint = Color(0xFF60A5FA),
            active = shieldArmed,
            onClick = { onUsePowerUp(PowerUpType.SHIELD) },
            testTag = "pu_shield"
        )
        PowerUpButton(
            icon = Icons.Default.Construction,
            label = "Crane",
            count = inventory.crane,
            tint = GoldAmber,
            active = false,
            onClick = { onUsePowerUp(PowerUpType.CRANE) },
            testTag = "pu_crane"
        )
    }
}

@Composable
private fun PowerUpButton(
    icon: ImageVector,
    label: String,
    count: Int,
    tint: Color,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(
                if (active) tint.copy(alpha = 0.4f) else Color(0xFF172554).copy(alpha = 0.85f)
            )
            .border(1.8.dp, if (active) Color.White else tint.copy(alpha = 0.85f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(26.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A1026))
                .border(1.2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun BlockTypeBadgeCanvas(type: BlockType, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(56.dp)) {
        val w = size.width
        val h = size.height
        when (type) {
            BlockType.WOOD -> {
                drawIsometricMiniBrick(
                    topColor = Color(0xFFE69F58),
                    frontColor = Color(0xFFB8692B),
                    sideColor = Color(0xFF834216)
                )
            }
            BlockType.ICE -> {
                drawIsometricMiniBrick(
                    topColor = Color(0xFFBAE6FD),
                    frontColor = Color(0xFF38BDF8),
                    sideColor = Color(0xFF0284C7)
                )
            }
            BlockType.RUBBER -> {
                drawIsometricMiniBrick(
                    topColor = Color(0xFFF472B6),
                    frontColor = Color(0xFFEC4899),
                    sideColor = Color(0xFFBE185D)
                )
            }
            BlockType.GOLD -> {
                drawIsometricMiniBrick(
                    topColor = Color(0xFFFEF08A),
                    frontColor = Color(0xFFFACC15),
                    sideColor = Color(0xFFCA8A04)
                )
            }
            BlockType.GLASS -> {
                drawIsometricMiniBrick(
                    topColor = Color(0xFFE0F2FE).copy(alpha = 0.6f),
                    frontColor = Color(0xFF7DD3FC).copy(alpha = 0.45f),
                    sideColor = Color(0xFF38BDF8).copy(alpha = 0.55f),
                    strokeColor = Color.White
                )
            }
            BlockType.BOMB -> {
                drawIsometricMiniBrick(
                    topColor = Color(0xFFF87171),
                    frontColor = Color(0xFFDC2626),
                    sideColor = Color(0xFF991B1B),
                    strokeColor = Color(0xFFFDE047)
                )
                drawCircle(color = GoldSun, radius = w * 0.08f, center = Offset(w * 0.72f, h * 0.16f))
            }
            BlockType.MAGNET -> {
                drawArc(
                    color = Color(0xFFEF4444),
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(w * 0.16f, h * 0.16f),
                    size = Size(w * 0.68f, h * 0.68f),
                    style = Stroke(width = w * 0.18f, cap = StrokeCap.Round)
                )
                drawCircle(color = Color(0xFFE2E8F0), radius = w * 0.10f, center = Offset(w * 0.28f, h * 0.75f))
                drawCircle(color = Color(0xFFE2E8F0), radius = w * 0.10f, center = Offset(w * 0.72f, h * 0.75f))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIsometricMiniBrick(
    topColor: Color,
    frontColor: Color,
    sideColor: Color,
    strokeColor: Color? = null
) {
    val w = size.width
    val h = size.height
    val topFace = Path().apply {
        moveTo(w * 0.12f, h * 0.42f)
        lineTo(w * 0.54f, h * 0.22f)
        lineTo(w * 0.90f, h * 0.40f)
        lineTo(w * 0.48f, h * 0.60f)
        close()
    }
    val frontFace = Path().apply {
        moveTo(w * 0.12f, h * 0.42f)
        lineTo(w * 0.48f, h * 0.60f)
        lineTo(w * 0.48f, h * 0.84f)
        lineTo(w * 0.12f, h * 0.66f)
        close()
    }
    val sideFace = Path().apply {
        moveTo(w * 0.48f, h * 0.60f)
        lineTo(w * 0.90f, h * 0.40f)
        lineTo(w * 0.90f, h * 0.64f)
        lineTo(w * 0.48f, h * 0.84f)
        close()
    }
    drawPath(topFace, topColor)
    drawPath(frontFace, frontColor)
    drawPath(sideFace, sideColor)
    if (strokeColor != null) {
        drawPath(topFace, strokeColor, style = Stroke(2f))
        drawPath(frontFace, strokeColor, style = Stroke(2f))
        drawPath(sideFace, strokeColor, style = Stroke(2f))
    }
}

@Composable
fun LiveEventBadgeCanvas(event: LiveEventType, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(54.dp)) {
        val w = size.width
        val h = size.height
        when (event) {
            LiveEventType.WIND -> {
                val p1 = Path().apply {
                    moveTo(w * 0.12f, h * 0.34f)
                    lineTo(w * 0.72f, h * 0.34f)
                    cubicTo(w * 0.88f, h * 0.34f, w * 0.88f, h * 0.18f, w * 0.70f, h * 0.20f)
                }
                val p2 = Path().apply {
                    moveTo(w * 0.10f, h * 0.54f)
                    lineTo(w * 0.86f, h * 0.54f)
                }
                val p3 = Path().apply {
                    moveTo(w * 0.20f, h * 0.72f)
                    lineTo(w * 0.64f, h * 0.72f)
                    cubicTo(w * 0.78f, h * 0.72f, w * 0.78f, h * 0.86f, w * 0.62f, h * 0.84f)
                }
                drawPath(p1, CyanIce, style = Stroke(4.5f, cap = StrokeCap.Round))
                drawPath(p2, Color(0xFFBAE6FD), style = Stroke(4.5f, cap = StrokeCap.Round))
                drawPath(p3, CyanIce, style = Stroke(4.5f, cap = StrokeCap.Round))
            }
            LiveEventType.EARTHQUAKE -> {
                val pulse = Path().apply {
                    moveTo(w * 0.08f, h * 0.54f)
                    lineTo(w * 0.26f, h * 0.54f)
                    lineTo(w * 0.38f, h * 0.22f)
                    lineTo(w * 0.50f, h * 0.82f)
                    lineTo(w * 0.64f, h * 0.32f)
                    lineTo(w * 0.74f, h * 0.54f)
                    lineTo(w * 0.92f, h * 0.54f)
                }
                drawPath(pulse, Color(0xFFF97316), style = Stroke(5.5f, cap = StrokeCap.Round))
                drawPath(pulse, Color(0xFFFED7AA), style = Stroke(2.2f, cap = StrokeCap.Round))
            }
            LiveEventType.RAIN -> {
                drawCircle(Color(0xFF7DD3FC), radius = w * 0.18f, center = Offset(w * 0.36f, h * 0.42f))
                drawCircle(Color(0xFFBAE6FD), radius = w * 0.22f, center = Offset(w * 0.52f, h * 0.36f))
                drawCircle(Color(0xFF7DD3FC), radius = w * 0.16f, center = Offset(w * 0.68f, h * 0.44f))
                drawLine(Color(0xFF38BDF8), Offset(w * 0.34f, h * 0.66f), Offset(w * 0.28f, h * 0.82f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(Color(0xFF38BDF8), Offset(w * 0.52f, h * 0.66f), Offset(w * 0.46f, h * 0.84f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(Color(0xFF38BDF8), Offset(w * 0.70f, h * 0.66f), Offset(w * 0.64f, h * 0.82f), strokeWidth = 4f, cap = StrokeCap.Round)
            }
            LiveEventType.LOW_GRAVITY, LiveEventType.NONE -> {
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFC084FC), Color(0xFF4F46E5))),
                    radius = w * 0.24f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
                drawOval(
                    color = Color(0xFF38BDF8),
                    topLeft = Offset(w * 0.10f, h * 0.38f),
                    size = Size(w * 0.80f, h * 0.24f),
                    style = Stroke(4f)
                )
            }
        }
    }
}

@Composable
fun OfflineBanner(isOnline: Boolean) {
    if (!isOnline) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CrimsonDanger.copy(alpha = 0.9f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Offline Mode — Progress cached locally",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
fun AdBannerBar(adsRemoved: Boolean, modifier: Modifier = Modifier) {
    if (adsRemoved) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(Color(0xFF060B1E))
            .border(0.5.dp, SkyfallGlassBorder.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "AdMob Banner (${AdConfig.BANNER_UNIT_ID.takeLast(10)}) • Menu Only",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B)
        )
    }
}
