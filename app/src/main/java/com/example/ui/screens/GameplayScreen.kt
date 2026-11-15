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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.GameSettings
import com.example.data.local.PowerUpInventory
import com.example.game.physics.GameMode
import com.example.game.physics.PhysicsUiState
import com.example.game.physics.PlayPhase
import com.example.game.physics.PowerUpType
import com.example.ui.components.ChunkyGameButton
import com.example.ui.components.EventWarningBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.PowerUpSideBar
import com.example.ui.components.StabilityMeterBar
import com.example.ui.components.StressVisionHud
import com.example.ui.components.SteadyHandPrecisionBar
import com.example.ui.components.TowerCanvas
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyanIce
import com.example.ui.theme.EmeraldGo
import com.example.ui.theme.GoldSun
import com.example.ui.theme.SkyfallMidnight
import com.example.ui.theme.SkyfallSlateCard
import java.text.NumberFormat
import java.util.Locale

@Composable
fun GameplayScreen(
    state: PhysicsUiState,
    settings: GameSettings,
    powerUps: PowerUpInventory,
    equippedSkin: String,
    equippedTheme: String,
    totalCoins: Long,
    onStartPull: (Int) -> Unit,
    onUpdatePull: (Float, Float) -> Unit,
    onReleasePull: () -> Unit,
    onUpdatePlacementAim: (Int, Float) -> Unit,
    onPlaceBlock: (Int, Float) -> Unit,
    onUsePowerUp: (PowerUpType) -> Unit,
    onToggleXray: (Boolean) -> Unit,
    onDismissBossIntro: () -> Unit,
    onTogglePause: (Boolean) -> Unit,
    onWatchAdDoubleCoins: () -> Unit,
    onWatchAdRevive: () -> Unit,
    onReplayCollapse: () -> Unit,
    onOpenPhotoMode: () -> Unit,
    onNextLevel: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onExitHome: () -> Unit
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full-Bleed 2.5D Physics Tower Canvas
        TowerCanvas(
            state = state,
            equippedSkin = equippedSkin,
            equippedTheme = equippedTheme,
            colorblindMode = settings.colorblindMode,
            onStartPull = onStartPull,
            onUpdatePull = onUpdatePull,
            onReleasePull = onReleasePull,
            onUpdatePlacementAim = onUpdatePlacementAim,
            onPlaceBlock = onPlaceBlock
        )

        // 2. Gameplay HUD matching Panel 5
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular Pause Button matching Panel 5
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF0A1436).copy(alpha = 0.88f))
                            .border(1.5.dp, CyanIce.copy(alpha = 0.7f), CircleShape)
                            .clickable { onTogglePause(true) }
                            .testTag("pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    // Center Level / Score display matching Panel 5
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val headerTitle = when (state.config.mode) {
                            GameMode.CAMPAIGN -> "Level ${state.config.worldId}-${((state.config.levelNumber - 1) % 42) + 1} • ${state.movesCompleted}/${state.config.targetMoves}"
                            GameMode.TIME_ATTACK -> "Time Attack • ${state.timeLeftSeconds}s"
                            GameMode.PASS_AND_PLAY -> "${state.config.playerNames.getOrElse(state.currentPlayerIndex) { "Player" }}'s Turn"
                            else -> "${state.config.mode.title} • ${state.towerHeightLayers}F"
                        }
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = formatter.format(state.score),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                shadow = Shadow(Color.Black, Offset(0f, 3f), 6f)
                            ),
                            color = Color.White
                        )
                    }

                    // Coins Pill with green + badge matching Panel 5
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0A1436).copy(alpha = 0.88f))
                            .border(1.3.dp, GoldSun.copy(alpha = 0.68f), RoundedCornerShape(20.dp))
                            .padding(start = 8.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldSun, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = formatter.format(totalCoins + state.coinsEarned),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(EmeraldGo),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = SkyfallMidnight, modifier = Modifier.size(13.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                EventWarningBanner(
                    activeEvent = state.activeEvent,
                    incomingEvent = state.incomingEvent,
                    warningSeconds = state.eventWarningSeconds,
                    freezeSecondsLeft = state.freezeSecondsLeft
                )

                if (state.statusBannerText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.statusBannerText,
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldSun,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SkyfallMidnight.copy(alpha = 0.78f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Middle Side Panels (Left-Hand Mode mirrors Stability & Power-ups)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (settings.leftHandMode) {
                    PowerUpSideBar(
                        inventory = powerUps,
                        shieldArmed = state.shieldArmed,
                        glueArmed = state.glueNextMove,
                        canUndo = state.canUndo,
                        onUsePowerUp = onUsePowerUp
                    )
                    StabilityMeterBar(
                        stabilityPercent = state.stabilityPercent,
                        colorblindMode = settings.colorblindMode
                    )
                } else {
                    StabilityMeterBar(
                        stabilityPercent = state.stabilityPercent,
                        colorblindMode = settings.colorblindMode
                    )
                    PowerUpSideBar(
                        inventory = powerUps,
                        shieldArmed = state.shieldArmed,
                        glueArmed = state.glueNextMove,
                        canUndo = state.canUndo,
                        onUsePowerUp = onUsePowerUp
                    )
                }
            }

            // Bottom Precision Bar + Camera Photo Button matching Panel 5
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StressVisionHud(state = state, onToggleXray = onToggleXray)
                if (state.phase == PlayPhase.COLLAPSING_SLOW_MO || state.phase == PlayPhase.REPLAYING_COLLAPSE) {
                    Text(
                        text = if (state.phase == PlayPhase.REPLAYING_COLLAPSE) "SLOW-MO REPLAY" else "SLOW-MO COLLAPSE!",
                        style = MaterialTheme.typography.titleLarge,
                        color = CrimsonDanger,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(SkyfallMidnight.copy(alpha = 0.85f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SteadyHandPrecisionBar(
                            precision = state.steadyHandPrecision,
                            combo = state.currentCombo,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onOpenPhotoMode,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0B1433).copy(alpha = 0.9f))
                                .border(1.4.dp, CyanIce.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Photo Mode", tint = CyanIce)
                        }
                    }
                }
            }
        }

        // 3. PANEL 9: Boss Intro Overlay with dramatic Leaning Tower Artwork
        if (state.phase == PlayPhase.BOSS_INTRO) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(R.drawable.img_boss_leaning_tower),
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
                                    Color(0xFF1A092B).copy(alpha = 0.65f),
                                    Color.Transparent,
                                    Color(0xFF0A081E).copy(alpha = 0.90f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Crimson-Gold BOSS TOWER ribbon + Crown
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = GoldSun,
                            modifier = Modifier.size(38.dp)
                        )
                        Box(
                            modifier = Modifier
                                .shadow(10.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF991B1B), Color(0xFFDC2626), Color(0xFF991B1B))
                                    )
                                )
                                .border(2.5.dp, GoldSun, RoundedCornerShape(16.dp))
                                .padding(horizontal = 28.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "BOSS TOWER",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    shadow = Shadow(Color(0xFF450A0A), Offset(0f, 3f), 6f)
                                ),
                                color = Color(0xFFFEF08A)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = state.config.bossType.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                shadow = Shadow(Color.Black, Offset(0f, 3f), 6f)
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Bottom Special Rules translucent card + Emerald PLAY BOSS CTA
                    Column(
                        modifier = Modifier.widthIn(max = 440.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1F1638).copy(alpha = 0.88f))
                                .border(1.5.dp, Color(0xFFA855F7).copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Special Rules:",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                            state.config.bossType.rules.forEach { rule ->
                                Text(
                                    text = "• $rule",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }

                        ChunkyGameButton(
                            text = "PLAY BOSS",
                            onClick = onDismissBossIntro,
                            containerColor = Color(0xFF22C55E),
                            contentColor = Color(0xFF052E16),
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .height(62.dp)
                        )
                    }
                }
            }
        }

        // 4. Pause Menu Overlay
        if (state.phase == PlayPhase.PAUSED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(modifier = Modifier.widthIn(max = 380.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Game Paused", style = MaterialTheme.typography.headlineLarge, color = GoldSun)
                        ChunkyGameButton("Resume", onClick = { onTogglePause(false) }, containerColor = EmeraldGo, icon = Icons.Default.PlayArrow, modifier = Modifier.fillMaxWidth())
                        ChunkyGameButton("Restart", onClick = onRetry, containerColor = CyanIce, icon = Icons.Default.Replay, modifier = Modifier.fillMaxWidth())
                        ChunkyGameButton("Settings", onClick = onOpenSettings, containerColor = SkyfallSlateCard, contentColor = Color.White, icon = Icons.Default.Settings, modifier = Modifier.fillMaxWidth())
                        ChunkyGameButton("Home", onClick = onExitHome, containerColor = CrimsonDanger, contentColor = Color.White, icon = Icons.Default.Home, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // 5. PANEL 10: Level Complete / Game Over Overlay with Festive Confetti Gradient
        if (state.phase == PlayPhase.LEVEL_COMPLETE || state.phase == PlayPhase.GAME_OVER) {
            val isWin = state.phase == PlayPhase.LEVEL_COMPLETE
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1A0D35).copy(alpha = 0.94f),
                                Color(0xFF581C87).copy(alpha = 0.92f),
                                Color(0xFFC2410C).copy(alpha = 0.92f)
                            )
                        )
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Festive confetti sparkles
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val confettiColors = listOf(GoldSun, CyanIce, Color(0xFFF472B6), EmeraldGo)
                    for (i in 0 until 24) {
                        val cx = ((i * 179 + 41) % size.width.toInt().coerceAtLeast(1)).toFloat()
                        val cy = ((i * 251 + 67) % size.height.toInt().coerceAtLeast(1)).toFloat()
                        drawCircle(
                            color = confettiColors[i % confettiColors.size].copy(alpha = 0.45f),
                            radius = if (i % 3 == 0) 6f else 3.5f,
                            center = Offset(cx, cy)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 3 Large Golden Stars (center star raised) matching Panel 10
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (state.starsEarned >= 1) GoldSun else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(56.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (state.starsEarned >= 2) GoldSun else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier
                                .size(72.dp)
                                .offset(y = (-10).dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (state.starsEarned >= 3) GoldSun else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Text(
                        text = when {
                            state.lastWinningPlayerName != null -> "${state.lastWinningPlayerName} Wins!"
                            isWin -> "Level Complete!"
                            else -> "Tower Collapsed!"
                        },
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            shadow = Shadow(Color(0xFF1E0B36), Offset(0f, 4f), 8f)
                        ),
                        color = if (isWin) Color(0xFFFEF08A) else Color.White,
                        textAlign = TextAlign.Center
                    )

                    // Dark Translucent Stats Box matching Panel 10
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF180E30).copy(alpha = 0.78f))
                            .border(1.4.dp, Color(0xFFFDE047).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BreakdownRow("Score", formatter.format(state.score), Color.White)
                        BreakdownRow("Height Bonus", "+${formatter.format(state.heightBonus)}", Color(0xFFFDE68A))
                        BreakdownRow("Combo Bonus", "+${formatter.format(state.comboBonus)}", EmeraldGo)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Coins Earned", style = MaterialTheme.typography.titleMedium, color = Color(0xFFE2E8F0))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldSun, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formatter.format(state.coinsEarned),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = GoldSun
                                )
                            }
                        }
                    }

                    if (!state.coinsDoubledByAd && state.coinsEarned > 0) {
                        ChunkyGameButton(
                            text = "Watch Ad for 2x Coins",
                            icon = Icons.Default.Videocam,
                            onClick = onWatchAdDoubleCoins,
                            containerColor = GoldSun,
                            contentColor = Color(0xFF2B1404),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (!isWin && !state.revivedByAd) {
                        ChunkyGameButton(
                            text = "Watch Ad to Revive Tower",
                            icon = Icons.Default.Videocam,
                            onClick = onWatchAdRevive,
                            containerColor = CyanIce,
                            contentColor = SkyfallMidnight,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Bottom 3 Square Chunky Action Buttons matching Panel 10 (Replay, Next/Retry, Share)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ResultSquareAction(
                            label = if (state.hasCollapseReplay && !isWin) "Slow-Mo" else "Replay",
                            icon = Icons.Default.Replay,
                            gradient = listOf(Color(0xFF4F46E5), Color(0xFF312E81)),
                            onClick = if (state.hasCollapseReplay && !isWin) onReplayCollapse else onRetry
                        )
                        if (isWin && state.config.mode == GameMode.CAMPAIGN && state.config.levelNumber < 210) {
                            ResultSquareAction(
                                label = "Next",
                                icon = Icons.Default.PlayArrow,
                                gradient = listOf(Color(0xFF4ADE80), Color(0xFF16A34A)),
                                onClick = onNextLevel
                            )
                        } else if (isWin) {
                            ResultSquareAction(
                                label = "Home",
                                icon = Icons.Default.Home,
                                gradient = listOf(Color(0xFF4ADE80), Color(0xFF16A34A)),
                                onClick = onExitHome
                            )
                        } else {
                            ResultSquareAction(
                                label = "Retry",
                                icon = Icons.Default.PlayArrow,
                                gradient = listOf(Color(0xFF4ADE80), Color(0xFF16A34A)),
                                onClick = onRetry
                            )
                        }
                        ResultSquareAction(
                            label = "Share",
                            icon = Icons.Default.Share,
                            gradient = listOf(Color(0xFF7E22CE), Color(0xFF4C1D95)),
                            onClick = onOpenPhotoMode
                        )
                    }

                    if (!isWin) {
                        Text(
                            text = "Return Home",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onExitHome)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSquareAction(
    label: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(gradient))
                .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = Color.White
        )
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFCBD5E1))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = valueColor)
    }
}
