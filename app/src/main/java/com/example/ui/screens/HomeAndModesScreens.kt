package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.remote.UserProfileDoc
import com.example.game.campaign.CampaignCatalog
import com.example.game.physics.BlockType
import com.example.game.physics.GameMode
import com.example.game.physics.LiveEventType
import com.example.game.physics.PowerUpType
import com.example.ui.components.AdBannerBar
import com.example.ui.components.BlockTypeBadgeCanvas
import com.example.ui.components.ChunkyGameButton
import com.example.ui.components.LiveEventBadgeCanvas
import com.example.ui.components.MockupBackButton
import com.example.ui.components.OfflineBanner
import com.example.ui.components.TopCurrencyBar
import com.example.ui.theme.CyanIce
import com.example.ui.theme.EmeraldGo
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldSun
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SkyfallMidnight
import com.example.ui.theme.SkyfallSlateCard

@Composable
fun HomeScreen(
    profile: UserProfileDoc,
    isOnline: Boolean,
    onPlayQuick: () -> Unit,
    onOpenCampaign: () -> Unit,
    onStartMode: (GameMode) -> Unit,
    onOpenAllModes: () -> Unit,
    onOpenRewards: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "home_bg")
    val sway by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3400), RepeatMode.Reverse),
        label = "sway"
    )
    val todayKey = remember { CampaignCatalog.todayDateKey() }
    val dailyUnclaimed = profile.lastDailyRewardDate != todayKey

    Box(modifier = Modifier.fillMaxSize()) {
        // Panel 2 Hero Fantasy Mountain Valley Artwork
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
                            Color(0xFF0A0E2A).copy(alpha = 0.55f),
                            Color.Transparent,
                            Color(0xFF070C22).copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // Animated 2.5D Isometric Centerpiece Tower swaying on cliff pedestal
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val baseY = size.height * 0.49f
            // Pedestal glow
            drawOval(
                brush = Brush.radialGradient(listOf(Color(0xFF38BDF8).copy(alpha = 0.25f), Color.Transparent)),
                topLeft = Offset(cx - 160f, baseY - 12f),
                size = Size(320f, 44f)
            )
            // Stone cliff pedestal
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF1E293B))),
                topLeft = Offset(cx - 110f, baseY),
                size = Size(220f, 34f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            // Stacked colorful glossy tower layers
            for (i in 0 until 7) {
                val w = 132f - i * 3f
                val h = 25f
                val offsetX = sway * (i * 1.9f) + (if (i % 2 == 0) -6f else 6f)
                val y = baseY - (i + 1) * (h + 3f)
                val topCol = when (i % 4) {
                    1 -> Color(0xFF67E8F9)
                    2 -> Color(0xFFC084FC)
                    else -> Color(0xFFFBBF24)
                }
                val frontCol = when (i % 4) {
                    1 -> Color(0xFF0284C7)
                    2 -> Color(0xFF7E22CE)
                    else -> Color(0xFFD97706)
                }
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(topCol, frontCol)),
                    topLeft = Offset(cx - w / 2f + offsetX, y),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.45f),
                    topLeft = Offset(cx - w / 2f + offsetX, y),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 1.6f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            OfflineBanner(isOnline)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 600.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopCurrencyBar(
                    displayName = profile.displayName,
                    xpLevel = profile.xpLevel.toInt(),
                    coins = profile.coins,
                    gems = profile.gems,
                    onProfileClick = onOpenProfile,
                    onShopClick = onOpenShop
                )

                // Title & Daily Reward Gift Box Chip matching Panel 2
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TUMBLE",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                shadow = Shadow(Color(0xFF0A1026), Offset(0f, 4f), 8f)
                            ),
                            color = GoldSun
                        )
                        Text(
                            text = "TOWER",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                shadow = Shadow(Color(0xFF0A1026), Offset(0f, 4f), 8f)
                            ),
                            color = GoldAmber
                        )
                        Text(
                            text = "SKYFALL",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                shadow = Shadow(Color(0xFF0A1026), Offset(0f, 3f), 6f)
                            ),
                            color = CyanIce
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        if (dailyUnclaimed) Color(0xFF5B21B6) else Color(0xFF1E293B),
                                        Color(0xFF1E1B4B)
                                    )
                                )
                            )
                            .border(1.5.dp, GoldSun, RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenRewards)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("daily_reward_chip"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = GoldSun,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (dailyUnclaimed) "Daily\nReward" else "Daily\nGift",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 12.sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.55f))

                // Giant Glowing Gold 3D PLAY Button matching Panel 2
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ChunkyGameButton(
                        text = "PLAY",
                        icon = Icons.Default.PlayArrow,
                        onClick = onPlayQuick,
                        containerColor = Color(0xFFFACC15),
                        contentColor = Color(0xFF2B1404),
                        modifier = Modifier
                            .fillMaxWidth(0.76f)
                            .height(62.dp)
                            .testTag("home_play_button")
                    )

                    // 4 Quick Mode Tiles matching Panel 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickModeTile(
                            title = "Campaign",
                            icon = Icons.Default.WorkspacePremium,
                            tint = GoldSun,
                            bgColors = listOf(Color(0xFF3B2471), Color(0xFF1E1B4B)),
                            modifier = Modifier.weight(1f),
                            onClick = onOpenCampaign
                        )
                        QuickModeTile(
                            title = "Classic",
                            icon = Icons.Default.ViewInAr,
                            tint = Color(0xFFF472B6),
                            bgColors = listOf(Color(0xFF3E206D), Color(0xFF1F1646)),
                            modifier = Modifier.weight(1f)
                        ) { onStartMode(GameMode.CLASSIC) }
                        QuickModeTile(
                            title = "Endless\nRise",
                            icon = Icons.Default.AllInclusive,
                            tint = CyanIce,
                            bgColors = listOf(Color(0xFF163D6B), Color(0xFF121D4A)),
                            modifier = Modifier.weight(1f)
                        ) { onStartMode(GameMode.ENDLESS) }
                        QuickModeTile(
                            title = "More",
                            icon = Icons.Default.MoreHoriz,
                            tint = Color(0xFFA78BFA),
                            bgColors = listOf(Color(0xFF27225B), Color(0xFF15163B)),
                            modifier = Modifier.weight(1f),
                            onClick = onOpenAllModes
                        )
                    }

                    // Daily Challenge Gradient Banner matching Panel 2
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(10.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF4338CA), Color(0xFF7E22CE), Color(0xFFC026D3))
                                )
                            )
                            .border(1.5.dp, Color(0xFFE879F9).copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                            .clickable { onStartMode(GameMode.DAILY_CHALLENGE) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("daily_challenge_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = GoldSun,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Daily Challenge",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                                Text(
                                    text = "New Tower Today!",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFFFDE68A)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Daily", tint = GoldSun)
                        }
                    }
                }
            }

            // Bottom Navigation Bar + Menu Ad Banner matching Panel 2
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF070C20).copy(alpha = 0.96f))
                    .border(0.8.dp, Color(0xFF334155))
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomNavItem("Shop", Icons.Default.ShoppingBag, GoldSun, onOpenShop, "nav_shop")
                    BottomNavItem("Missions", Icons.Default.CheckCircle, Color(0xFFFBBF24), onOpenRewards, "nav_missions")
                    BottomNavItem("Leaderboard", Icons.Default.Leaderboard, GoldSun, onOpenLeaderboard, "nav_leaderboard")
                    BottomNavItem("Settings", Icons.Default.Settings, Color(0xFFE2E8F0), onOpenSettings, "nav_settings")
                }
                AdBannerBar(adsRemoved = profile.adsRemoved)
            }
        }
    }
}

@Composable
private fun QuickModeTile(
    title: String,
    icon: ImageVector,
    tint: Color,
    bgColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(bgColors))
            .border(1.4.dp, tint.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 12.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = Color(0xFFE2E8F0),
            maxLines = 1
        )
    }
}

@Composable
fun GameModesScreen(
    onBack: () -> Unit,
    onOpenCampaign: () -> Unit,
    onStartMode: (GameMode, List<String>) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showPassPlayDialog by remember { mutableStateOf(false) }

    val screenTitle = when (selectedTab) {
        0 -> "Game Modes"
        1 -> "Block Types"
        2 -> "Live Events"
        else -> "Power-Ups"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Tab-specific backdrop matching Panels 3, 6, 7, 8
        if (selectedTab == 2) {
            // Panel 7 twilight volcanic silhouette artwork
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
                                Color(0xFF0A2444).copy(alpha = 0.85f),
                                Color(0xFF16325C).copy(alpha = 0.72f),
                                Color(0xFF1F1638).copy(alpha = 0.86f)
                            )
                        )
                    )
            )
        } else {
            // Deep starry dark sky matching Panels 3, 6, 8
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF050A1F), Color(0xFF0C183D), Color(0xFF09112A))))
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0 until 32) {
                    val sx = ((i * 157 + 31) % size.width.toInt().coerceAtLeast(1)).toFloat()
                    val sy = ((i * 211 + 53) % size.height.toInt().coerceAtLeast(1)).toFloat()
                    drawCircle(
                        color = Color.White.copy(alpha = if (i % 3 == 0) 0.5f else 0.2f),
                        radius = if (i % 4 == 0) 2.2f else 1.2f,
                        center = Offset(sx, sy)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Header matching Panels 3, 6, 7, 8: Cyan circular Back button + centered bold white title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                MockupBackButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = screenTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Clean capsule sub-switcher to jump seamlessly between Game Modes / Block Types / Live Events / Power-Ups
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0B1435).copy(alpha = 0.88f))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(18.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Modes", "Blocks", "Events", "Power-Ups").forEachIndexed { idx, label ->
                    val selected = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) {
                                    Brush.horizontalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF2563EB)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable { selectedTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (selected) Color.White else Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            when (selectedTab) {
                // PANEL 3: Game Modes
                0 -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(GameMode.entries) { mode ->
                        val style = modeBannerStyle(mode)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.horizontalGradient(style.gradient))
                                .border(1.6.dp, style.borderColor, RoundedCornerShape(18.dp))
                                .clickable {
                                    when (mode) {
                                        GameMode.CAMPAIGN -> onOpenCampaign()
                                        GameMode.PASS_AND_PLAY -> showPassPlayDialog = true
                                        else -> onStartMode(mode, emptyList())
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black.copy(alpha = 0.22f))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = style.icon,
                                    contentDescription = null,
                                    tint = style.iconTint,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = style.caption,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // PANEL 6: Block Types
                1 -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val descs = mapOf(
                        BlockType.WOOD to "Normal and reliable.",
                        BlockType.ICE to "Slippery and melts over time.",
                        BlockType.RUBBER to "Bouncy and gives a jolt.",
                        BlockType.MAGNET to "Slightly pulls nearby blocks.",
                        BlockType.GOLD to "Heavy but gives extra coins.",
                        BlockType.BOMB to "Explodes after a timer and shakes nearby blocks.",
                        BlockType.GLASS to "Breaks under high pressure."
                    )
                    items(BlockType.entries) { b ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF101C45).copy(alpha = 0.92f))
                                .border(1.5.dp, Color(0xFF60A5FA).copy(alpha = 0.72f), RoundedCornerShape(18.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BlockTypeBadgeCanvas(type = b, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (b == BlockType.BOMB) "Bomb (TNT)" else b.displayName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                                Text(
                                    text = descs[b].orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }

                // PANEL 7: Live Events
                2 -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(LiveEventType.entries.filter { it != LiveEventType.NONE }) { ev ->
                        val (borderCol, bgTop, bgBottom, lineText) = when (ev) {
                            LiveEventType.WIND -> listOf(
                                Color(0xFF38BDF8), Color(0xFF0E4970), Color(0xFF0A2E4B), "Pushes the tower in a direction."
                            )
                            LiveEventType.EARTHQUAKE -> listOf(
                                Color(0xFFF97316), Color(0xFF5A2518), Color(0xFF2B1211), "Shakes the tower periodically."
                            )
                            LiveEventType.RAIN -> listOf(
                                Color(0xFF60A5FA), Color(0xFF163D6E), Color(0xFF0F264A), "Makes blocks slippery."
                            )
                            else -> listOf(
                                Color(0xFFA78BFA), Color(0xFF34246E), Color(0xFF1A1642), "Blocks feel lighter!"
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(10.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf((bgTop as Color).copy(alpha = 0.90f), (bgBottom as Color).copy(alpha = 0.92f))
                                    )
                                )
                                .border(2.dp, borderCol as Color, RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LiveEventBadgeCanvas(event = ev, modifier = Modifier.size(60.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ev.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = lineText as String,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }

                // PANEL 8: Power-Ups
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val puSpecs = mapOf(
                        PowerUpType.FREEZE to Triple(Icons.Default.AcUnit, Color(0xFF38BDF8), "Stops time and live events."),
                        PowerUpType.GLUE to Triple(Icons.Default.WaterDrop, GoldSun, "Permanently stick one block."),
                        PowerUpType.UNDO to Triple(Icons.AutoMirrored.Filled.Undo, Color(0xFFC084FC), "Revert your last move."),
                        PowerUpType.SHIELD to Triple(Icons.Default.Shield, Color(0xFF60A5FA), "Forgives one mistake (prevents collapse)."),
                        PowerUpType.CRANE to Triple(Icons.Default.Construction, GoldAmber, "Safely pick and place a block on top.")
                    )
                    items(PowerUpType.entries) { pu ->
                        val spec = puSpecs[pu] ?: Triple(Icons.Default.AcUnit, CyanIce, pu.description)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0E1B44).copy(alpha = 0.94f))
                                .border(1.5.dp, Color(0xFF60A5FA).copy(alpha = 0.72f), RoundedCornerShape(18.dp))
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(spec.second.copy(alpha = 0.35f), Color(0xFF091334))
                                        )
                                    )
                                    .border(2.dp, spec.second, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = spec.first,
                                    contentDescription = pu.title,
                                    tint = spec.second,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pu.title,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                                Text(
                                    text = spec.third,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, spec.second.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${pu.gemCost} Gems",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CyanIce
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPassPlayDialog) {
        PassAndPlaySetupDialog(
            onDismiss = { showPassPlayDialog = false },
            onStart = { names ->
                showPassPlayDialog = false
                onStartMode(GameMode.PASS_AND_PLAY, names)
            }
        )
    }
}

private data class ModeBannerStyle(
    val gradient: List<Color>,
    val borderColor: Color,
    val icon: ImageVector,
    val iconTint: Color,
    val caption: String
)

private fun modeBannerStyle(mode: GameMode): ModeBannerStyle = when (mode) {
    GameMode.CAMPAIGN -> ModeBannerStyle(
        gradient = listOf(Color(0xFF0284C7), Color(0xFF1E3A8A)),
        borderColor = Color(0xFF7DD3FC),
        icon = Icons.Default.Landscape,
        iconTint = Color(0xFFFDE047),
        caption = "200+ Levels, 5 Worlds"
    )
    GameMode.CLASSIC -> ModeBannerStyle(
        gradient = listOf(Color(0xFF4338CA), Color(0xFF5B21B6)),
        borderColor = Color(0xFFA78BFA),
        icon = Icons.Default.ViewInAr,
        iconTint = GoldSun,
        caption = "The original tower challenge"
    )
    GameMode.ENDLESS -> ModeBannerStyle(
        gradient = listOf(Color(0xFF0EA5E9), Color(0xFF1D4ED8)),
        borderColor = Color(0xFF67E8F9),
        icon = Icons.Default.AllInclusive,
        iconTint = Color(0xFFA5F3FC),
        caption = "Build as high as you can!"
    )
    GameMode.TIME_ATTACK -> ModeBannerStyle(
        gradient = listOf(Color(0xFF9333EA), Color(0xFF4C1D95)),
        borderColor = Color(0xFFF0ABFC),
        icon = Icons.Default.Timer,
        iconTint = Color.White,
        caption = "Build max blocks in 90 sec!"
    )
    GameMode.SURVIVAL -> ModeBannerStyle(
        gradient = listOf(Color(0xFF1D4ED8), Color(0xFF312E81)),
        borderColor = Color(0xFF93C5FD),
        icon = Icons.Default.Thunderstorm,
        iconTint = Color(0xFFBAE6FD),
        caption = "Face non-stop events!"
    )
    GameMode.PASS_AND_PLAY -> ModeBannerStyle(
        gradient = listOf(Color(0xFF6D28D9), Color(0xFF4338CA)),
        borderColor = Color(0xFFC4B5FD),
        icon = Icons.Default.Groups,
        iconTint = GoldSun,
        caption = "2-4 Players, Same Device"
    )
    GameMode.DAILY_CHALLENGE -> ModeBannerStyle(
        gradient = listOf(Color(0xFF7E22CE), Color(0xFFBE185D)),
        borderColor = Color(0xFFF9A8D4),
        icon = Icons.Default.CalendarMonth,
        iconTint = Color.White,
        caption = "A new tower every day!"
    )
}

@Composable
private fun PassAndPlaySetupDialog(
    onDismiss: () -> Unit,
    onStart: (List<String>) -> Unit
) {
    var playerCount by remember { mutableIntStateOf(2) }
    val names = remember { mutableStateListOf("Player 1", "Player 2", "Player 3", "Player 4") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pass & Play Setup", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4).forEach { count ->
                        val selected = playerCount == count
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) GoldSun else SkyfallSlateCard)
                                .clickable { playerCount = count }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("$count Players", color = if (selected) SkyfallMidnight else Color.White)
                        }
                    }
                }
                for (i in 0 until playerCount) {
                    OutlinedTextField(
                        value = names[i],
                        onValueChange = { names[i] = it.take(20) },
                        label = { Text("Player ${i + 1}") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onStart(names.take(playerCount).map { it.ifBlank { "Player" } }) }) {
                Text("Start Match")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
