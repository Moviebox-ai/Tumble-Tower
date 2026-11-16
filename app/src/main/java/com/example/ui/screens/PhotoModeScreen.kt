package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.physics.BlockState
import com.example.game.physics.BlockType
import com.example.game.physics.PhysicsUiState
import com.example.services.PhotoBackdrop
import com.example.ui.components.ChunkyGameButton
import com.example.ui.components.MockupBackButton
import com.example.ui.components.drawPremiumIsometricBlock
import com.example.ui.theme.GoldSun

@Composable
fun PhotoModeScreen(
    physicsState: PhysicsUiState,
    onBack: () -> Unit,
    onCaptureAndShare: (PhotoBackdrop, Boolean) -> Unit,
    equippedSkin: String = "wood"
) {
    var selectedBackdrop by remember { mutableStateOf(PhotoBackdrop.SUNSET) }
    var showWatermark by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050A1F), Color(0xFF0B1536))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header matching Panel 11: Cyan circular Back button + centered white "Photo Mode"
            Box(modifier = Modifier.fillMaxWidth()) {
                MockupBackButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = "Photo Mode",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Framed Tower Preview with L-shaped camera viewfinder corner brackets matching Panel 11
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .aspectRatio(0.95f)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(backdropDrawableFor(selectedBackdrop)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (selectedBackdrop == PhotoBackdrop.SUNSET) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF7C3AED).copy(alpha = 0.25f),
                                        Color(0xFFF97316).copy(alpha = 0.30f)
                                    )
                                )
                            )
                    )
                }

                // Tower preview & white L-shaped camera viewfinder brackets
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val baseY = size.height * 0.84f
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(cx - 115f, baseY),
                        size = Size(230f, 30f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    val previewBlocks = physicsState.blocks
                    if (previewBlocks.isNotEmpty()) {
                        val scale = (size.height * 0.56f / (physicsState.towerHeightLayers.coerceAtLeast(8) * 26f)).coerceIn(0.6f, 1.25f)
                        previewBlocks.sortedBy { it.layer }.forEach { b ->
                            if (b.isShattered) return@forEach
                            val bx = cx + b.x * 75f * scale
                            val by = baseY - (b.layer + 1) * 24f * scale
                            val isEven = b.layer % 2 == 0
                            val bw = if (isEven) 56f * scale else 156f * scale
                            val bh = if (isEven) 21f * scale else 18.5f * scale
                            drawPremiumIsometricBlock(
                                block = b,
                                topLeft = Offset(bx - bw / 2f, by),
                                bSize = Size(bw, bh),
                                depthX = 6f * scale,
                                depthY = -5f * scale,
                                skin = equippedSkin,
                                colorblindMode = false,
                                textMeasurer = null
                            )
                        }
                    } else {
                        for (i in 0 until 8) {
                            val y = baseY - (i + 1) * 26f
                            val demoType = if (i % 3 == 1) BlockType.ICE else BlockType.WOOD
                            drawPremiumIsometricBlock(
                                block = BlockState(id = i + 1, layer = i, slot = 1, type = demoType, x = 0f),
                                topLeft = Offset(cx - 65f, y),
                                bSize = Size(130f, 22f),
                                depthX = 8f,
                                depthY = -6f,
                                skin = equippedSkin,
                                colorblindMode = false,
                                textMeasurer = null
                            )
                        }
                    }

                    // Four crisp white L-shaped camera brackets
                    val m = 24f
                    val arm = 48f
                    val stroke = 7f
                    // Top-Left
                    drawLine(Color.White, Offset(m, m), Offset(m + arm, m), strokeWidth = stroke, cap = StrokeCap.Round)
                    drawLine(Color.White, Offset(m, m), Offset(m, m + arm), strokeWidth = stroke, cap = StrokeCap.Round)
                    // Top-Right
                    drawLine(Color.White, Offset(size.width - m, m), Offset(size.width - m - arm, m), strokeWidth = stroke, cap = StrokeCap.Round)
                    drawLine(Color.White, Offset(size.width - m, m), Offset(size.width - m, m + arm), strokeWidth = stroke, cap = StrokeCap.Round)
                    // Bottom-Left
                    drawLine(Color.White, Offset(m, size.height - m), Offset(m + arm, size.height - m), strokeWidth = stroke, cap = StrokeCap.Round)
                    drawLine(Color.White, Offset(m, size.height - m), Offset(m, size.height - m - arm), strokeWidth = stroke, cap = StrokeCap.Round)
                    // Bottom-Right
                    drawLine(Color.White, Offset(size.width - m, size.height - m), Offset(size.width - m - arm, size.height - m), strokeWidth = stroke, cap = StrokeCap.Round)
                    drawLine(Color.White, Offset(size.width - m, size.height - m), Offset(size.width - m, size.height - m - arm), strokeWidth = stroke, cap = StrokeCap.Round)
                }

                if (showWatermark) {
                    Text(
                        text = "Tumble Tower: Skyfall",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 18.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.widthIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 4 Real Backdrop Thumbnail Tiles matching Panel 11
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PhotoBackdrop.entries.forEach { bd ->
                        val selected = bd == selectedBackdrop
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) GoldSun else Color.White.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedBackdrop = bd }
                            ) {
                                Image(
                                    painter = painterResource(backdropDrawableFor(bd)),
                                    contentDescription = bd.label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (bd == PhotoBackdrop.SUNSET) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFF97316).copy(alpha = 0.28f))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = bd.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                                ),
                                color = if (selected) GoldSun else Color.White
                            )
                        }
                    }
                }

                // Show Watermark Row with green checkbox matching Panel 11
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showWatermark = !showWatermark }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Show Watermark",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (showWatermark) Color(0xFF1E3A34) else Color(0xFF0F172A))
                            .border(
                                1.8.dp,
                                if (showWatermark) Color(0xFF22C55E) else Color(0xFF475569),
                                RoundedCornerShape(7.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showWatermark) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Enabled",
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                ChunkyGameButton(
                    text = "Capture & Share",
                    icon = Icons.Default.CameraAlt,
                    onClick = { onCaptureAndShare(selectedBackdrop, showWatermark) },
                    containerColor = Color(0xFFFACC15),
                    contentColor = Color(0xFF2B1404),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("capture_share_button")
                )
            }
        }
    }
}

private fun backdropDrawableFor(backdrop: PhotoBackdrop): Int = when (backdrop) {
    PhotoBackdrop.DEFAULT, PhotoBackdrop.SUNSET -> R.drawable.img_skyfall_hero_bg
    PhotoBackdrop.NIGHT -> R.drawable.img_photo_night_bg
    PhotoBackdrop.SPACE -> R.drawable.img_photo_space_bg
}
