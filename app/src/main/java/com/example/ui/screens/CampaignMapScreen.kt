package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.CachedLevelEntity
import com.example.game.campaign.CampaignCatalog
import com.example.ui.components.MockupBackButton
import com.example.ui.theme.GoldSun
import kotlin.math.sin

@Composable
fun CampaignMapScreen(
    cachedLevels: List<CachedLevelEntity>,
    highestUnlocked: Int,
    onBack: () -> Unit,
    onSelectLevel: (Int) -> Unit
) {
    var selectedWorldIndex by remember { mutableIntStateOf(0) }
    val world = CampaignCatalog.worlds[selectedWorldIndex]
    val progressMap = remember(cachedLevels) { cachedLevels.associateBy { it.levelNumber } }
    val worldLevels = remember(world.id) {
        CampaignCatalog.allLevels.filter { it.worldId == world.id }
    }
    val earnedWorldStars = worldLevels.sumOf { progressMap[it.levelNumber]?.stars ?: 0 }
    val totalWorldStars = worldLevels.size * 3

    Box(modifier = Modifier.fillMaxSize()) {
        // Panel 4 Enchanted Forest Waterfall & Cobblestone Map Artwork
        val mapDrawable = when (world.key) {
            "ice" -> R.drawable.img_photo_night_bg
            "space" -> R.drawable.img_photo_space_bg
            else -> R.drawable.img_forest_map_bg
        }
        Image(
            painter = painterResource(mapDrawable),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Subtle world theme tint for Desert / Lava / Night readability
        val worldOverlay = when (world.key) {
            "desert" -> Color(0xFF78350F).copy(alpha = 0.34f)
            "lava" -> Color(0xFF7F1D1D).copy(alpha = 0.42f)
            "ice" -> Color(0xFF0C4A6E).copy(alpha = 0.36f)
            "space" -> Color(0xFF1E1B4B).copy(alpha = 0.32f)
            else -> Color(0xFF041C24).copy(alpha = 0.24f)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(worldOverlay)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Top bar matching Panel 4: Cyan circular back button + Golden shadowed "Campaign"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                MockupBackButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = "Campaign",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        shadow = Shadow(Color(0xFF071324), Offset(0f, 4f), 8f)
                    ),
                    color = GoldSun,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // World Header Card flanked by circular < and > navigation buttons matching Panel 4
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WorldNavArrowButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous World",
                    onClick = {
                        selectedWorldIndex = (selectedWorldIndex - 1 + CampaignCatalog.worlds.size) % CampaignCatalog.worlds.size
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0D2F3F).copy(alpha = 0.90f), Color(0xFF071D29).copy(alpha = 0.92f))
                            )
                        )
                        .border(1.6.dp, Color(0xFF38BDF8).copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = world.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "World ${world.id}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFE2E8F0)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldSun,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$earnedWorldStars/$totalWorldStars",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                    }
                }

                WorldNavArrowButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next World",
                    onClick = {
                        selectedWorldIndex = (selectedWorldIndex + 1) % CampaignCatalog.worlds.size
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Winding Golden Cobblestone Level Path matching Panel 4
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(worldLevels) { idx, level ->
                    val isUnlocked = level.levelNumber <= highestUnlocked
                    val stars = progressMap[level.levelNumber]?.stars ?: 0
                    val isBoss = level.levelNumber % 10 == 0
                    val waveBias = sin(idx * 0.95f)
                    val nextBias = sin((idx + 1) * 0.95f)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Draw connecting golden path curve to next node
                        if (idx < worldLevels.lastIndex) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(78.dp)
                            ) {
                                val startX = when {
                                    waveBias < -0.32f -> size.width * 0.24f
                                    waveBias > 0.32f -> size.width * 0.76f
                                    else -> size.width * 0.50f
                                }
                                val endX = when {
                                    nextBias < -0.32f -> size.width * 0.24f
                                    nextBias > 0.32f -> size.width * 0.76f
                                    else -> size.width * 0.50f
                                }
                                val path = Path().apply {
                                    moveTo(startX, size.height * 0.38f)
                                    cubicTo(
                                        startX, size.height * 0.85f,
                                        endX, size.height * 0.55f,
                                        endX, size.height * 1.15f
                                    )
                                }
                                drawPath(
                                    path = path,
                                    color = Color(0xFF92400E).copy(alpha = 0.7f),
                                    style = Stroke(width = 18f, cap = StrokeCap.Round)
                                )
                                drawPath(
                                    path = path,
                                    color = Color(0xFFFDE68A).copy(alpha = 0.85f),
                                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = when {
                                waveBias < -0.32f -> Alignment.CenterStart
                                waveBias > 0.32f -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(horizontal = 18.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(enabled = isUnlocked) { onSelectLevel(level.levelNumber) }
                                    .padding(4.dp)
                                    .testTag("campaign_level_${level.levelNumber}")
                            ) {
                                if (isBoss) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = "Boss",
                                        tint = GoldSun,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(if (isBoss) 74.dp else 60.dp)
                                        .shadow(8.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                !isUnlocked -> Brush.verticalGradient(
                                                    listOf(Color(0xFF334155), Color(0xFF1E293B))
                                                )
                                                isBoss -> Brush.verticalGradient(
                                                    listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                                                )
                                                else -> Brush.verticalGradient(
                                                    listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))
                                                )
                                            }
                                        )
                                        .border(
                                            width = if (isBoss) 3.dp else 2.5.dp,
                                            color = if (isUnlocked) Color(0xFFFEF9C3) else Color(0xFF64748B),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!isUnlocked) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Color(0xFFCBD5E1),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${level.levelNumber}",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontSize = if (isBoss) 26.sp else 23.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                shadow = Shadow(Color(0xFF451A03), Offset(0f, 2f), 4f)
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }

                                if (isBoss) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF7F1D1D))
                                            .border(1.dp, GoldSun, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "BOSS",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp
                                            ),
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        for (s in 1..3) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (s <= stars) GoldSun else Color.Black.copy(alpha = 0.45f),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // World Pagination Dots at bottom matching Panel 4
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CampaignCatalog.worlds.forEachIndexed { idx, _ ->
                    val active = idx == selectedWorldIndex
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (active) 11.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (active) GoldSun else Color.White.copy(alpha = 0.5f))
                            .clickable { selectedWorldIndex = idx }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorldNavArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFF0D2F3F).copy(alpha = 0.90f))
            .border(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}
