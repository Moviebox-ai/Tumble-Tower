package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.ChunkyGameButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyanIce
import com.example.ui.theme.EmeraldGo
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldSun
import com.example.ui.theme.SkyfallDeepIndigo
import com.example.ui.theme.SkyfallMidnight
import com.example.ui.theme.SkyfallSlateCard
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing)
        )
        delay(200)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Panel 1 Hero Fantasy Valley & Twilight Sky Artwork
        Image(
            painter = painterResource(R.drawable.img_skyfall_hero_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0A0D28).copy(alpha = 0.25f),
                            Color.Transparent,
                            Color(0xFF06091B).copy(alpha = 0.82f)
                        )
                    )
                )
        )

        // Falling isometric blocks & centerpiece tower on cliff pedestal
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val baseY = size.height * 0.72f
            val blocksCount = (progress.value * 7).toInt().coerceIn(1, 7)

            // Two tumbling mid-air wooden blocks flanking logo like Panel 1
            rotate(degrees = -28f, pivot = Offset(cx - 190f, size.height * 0.20f)) {
                drawRoundRect(
                    color = Color(0xFFD97706),
                    topLeft = Offset(cx - 220f, size.height * 0.18f),
                    size = Size(64f, 26f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
            rotate(degrees = 42f, pivot = Offset(cx + 210f, size.height * 0.52f)) {
                drawRoundRect(
                    color = Color(0xFFB45309),
                    topLeft = Offset(cx + 175f, size.height * 0.50f),
                    size = Size(72f, 26f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }

            // Central stacked 3D-style tower
            for (i in 0 until blocksCount) {
                val w = 136f - i * 4f
                val h = 30f
                val dropOffset = (1f - (progress.value * 7 - i).coerceIn(0f, 1f)) * 90f
                val y = baseY - i * (h + 5f) - dropOffset
                val c = when (i % 4) {
                    1 -> CyanIce
                    2 -> GoldSun
                    3 -> Color(0xFFA855F7)
                    else -> Color(0xFFD97706)
                }
                drawRoundRect(
                    color = c,
                    topLeft = Offset(cx - w / 2f + (if (i % 2 == 0) -6f else 6f), y),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = GoldSun,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "TUMBLE",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        shadow = Shadow(Color(0xFF1E1B4B), Offset(0f, 6f), 10f)
                    ),
                    color = GoldSun
                )
                Text(
                    text = "TOWER",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        shadow = Shadow(Color(0xFF1E1B4B), Offset(0f, 6f), 10f)
                    ),
                    color = GoldAmber
                )
                Text(
                    text = "SKYFALL",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        shadow = Shadow(Color(0xFF0A1026), Offset(0f, 4f), 8f)
                    ),
                    color = CyanIce
                )
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A1128))
                        .border(1.5.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.value.coerceIn(0.05f, 1f))
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(EmeraldGo, Color(0xFFA3E635), GoldSun)))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Loading... ${(progress.value * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = GoldAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Powered by Firebase",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TutorialScreen(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onComplete: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val languages = listOf(
        "en" to "English",
        "hi" to "हिन्दी",
        "es" to "Español",
        "pt" to "Português",
        "ar" to "العربية",
        "in" to "Indonesia",
        "fr" to "Français",
        "de" to "Deutsch"
    )

    val steps = listOf(
        Triple(
            Icons.Default.TouchApp,
            stringResource(R.string.tut_step1_title),
            stringResource(R.string.tut_step1_desc)
        ),
        Triple(
            Icons.Default.PanTool,
            stringResource(R.string.tut_step2_title),
            stringResource(R.string.tut_step2_desc)
        ),
        Triple(
            Icons.Default.Bolt,
            stringResource(R.string.tut_step3_title),
            stringResource(R.string.tut_step3_desc)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(SkyfallMidnight, SkyfallDeepIndigo))
            )
            .safeDrawingPadding()
            .padding(20.dp)
    ) {
        TextButton(
            onClick = onComplete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .testTag("tutorial_skip_button")
        ) {
            Text(stringResource(R.string.skip), color = CyanIce)
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = stringResource(R.string.choose_language),
                style = MaterialTheme.typography.titleLarge,
                color = GoldSun
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.forEach { (code, label) ->
                    val active = code == selectedLanguage
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) GoldSun else SkyfallSlateCard)
                            .border(1.dp, if (active) Color.White else Color(0xFF33447A), RoundedCornerShape(14.dp))
                            .clickable { onLanguageSelected(code) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (active) SkyfallMidnight else Color.White
                        )
                    }
                }
            }

            val current = steps[step]
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = current.first,
                        contentDescription = null,
                        tint = GoldSun,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = current.second,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = current.third,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(steps.size) { idx ->
                            Box(
                                modifier = Modifier
                                    .size(if (idx == step) 12.dp else 8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (idx == step) GoldSun else Color.White.copy(alpha = 0.25f))
                            )
                        }
                    }
                }
            }

            ChunkyGameButton(
                text = if (step < steps.lastIndex) stringResource(R.string.next) else stringResource(R.string.start_playing),
                onClick = {
                    if (step < steps.lastIndex) step++ else onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tutorial_next_button"),
                containerColor = EmeraldGo,
                contentColor = SkyfallMidnight
            )
        }
    }
}
