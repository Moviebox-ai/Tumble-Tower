package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.UserProfileDoc
import com.example.ui.components.AdBannerBar
import com.example.ui.components.ChunkyGameButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyanIce
import com.example.ui.theme.EmeraldGo
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldSun
import com.example.ui.theme.SkyfallDeepIndigo
import com.example.ui.theme.SkyfallMidnight
import com.example.ui.theme.SkyfallSlateCard
import com.example.viewmodel.LeaderboardUiState

@Composable
fun LeaderboardScreen(
    selectedBoard: String,
    leaderboardState: LeaderboardUiState,
    currentUserId: String,
    adsRemoved: Boolean,
    onSelectBoard: (String) -> Unit,
    onBack: () -> Unit
) {
    val boards = listOf(
        "daily" to "Daily",
        "weekly" to "Weekly",
        "all_time" to "All-Time",
        "daily_challenge" to "Challenge"
    )
    val selectedIdx = boards.indexOfFirst { it.first == selectedBoard }.coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyfallMidnight, SkyfallDeepIndigo)))
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Global Leaderboard", style = MaterialTheme.typography.headlineMedium, color = GoldSun)
        }

        TabRow(selectedTabIndex = selectedIdx, containerColor = SkyfallSlateCard, contentColor = GoldSun) {
            boards.forEach { (key, label) ->
                Tab(
                    selected = key == selectedBoard,
                    onClick = { onSelectBoard(key) },
                    text = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (leaderboardState) {
                is LeaderboardUiState.Loading -> {
                    CircularProgressIndicator(
                        color = GoldSun,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is LeaderboardUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Leaderboard, contentDescription = null, tint = CyanIce, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(leaderboardState.message, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    }
                }
                is LeaderboardUiState.Success -> {
                    if (leaderboardState.entries.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldSun, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No scores posted yet on this board. Build a tower to claim #1!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(leaderboardState.entries) { index, entry ->
                                val isMe = entry.userId == currentUserId
                                val rankColor = when (index) {
                                    0 -> GoldSun
                                    1 -> Color(0xFFE2E8F0)
                                    2 -> GoldAmber
                                    else -> CyanIce
                                }
                                GlassCard(
                                    borderColor = if (isMe) EmeraldGo else rankColor.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(rankColor.copy(alpha = 0.2f))
                                                .border(1.5.dp, rankColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("#${index + 1}", style = MaterialTheme.typography.labelLarge, color = rankColor)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isMe) "${entry.displayName} (You)" else entry.displayName,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${entry.height} Floors • ${entry.accuracyPercent}% Accuracy",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFFCBD5E1)
                                            )
                                        }
                                        Text("${entry.score}", style = MaterialTheme.typography.titleLarge, color = GoldSun)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AdBannerBar(adsRemoved = adsRemoved)
    }
}

@Composable
fun ProfileStatsScreen(
    profile: UserProfileDoc,
    onBack: () -> Unit,
    onSaveProfile: (String, String) -> Unit,
    onSignOut: () -> Unit
) {
    var nameInput by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var selectedAvatar by remember(profile.avatarId) { mutableStateOf(profile.avatarId) }
    val avatars = listOf("avatar_crown" to "Crown", "avatar_ice" to "Frost", "avatar_fire" to "Blaze", "avatar_star" to "Orbit")

    val accuracy = if (profile.totalPulls > 0) {
        ((profile.perfectPulls * 100) / profile.totalPulls).toInt()
    } else 100

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyfallMidnight, SkyfallDeepIndigo)))
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Player Profile & Stats", style = MaterialTheme.typography.headlineMedium, color = GoldSun)
        }

        GlassCard(borderColor = GoldSun, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Customize Identity", style = MaterialTheme.typography.titleLarge, color = CyanIce)
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(30) },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    avatars.forEach { (id, label) ->
                        val active = id == selectedAvatar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) GoldSun else SkyfallMidnight)
                                .clickable { selectedAvatar = id }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) SkyfallMidnight else Color.White
                            )
                        }
                    }
                }
                ChunkyGameButton(
                    text = "Save Profile",
                    icon = Icons.Default.Person,
                    onClick = { onSaveProfile(nameInput, selectedAvatar) },
                    containerColor = EmeraldGo,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Career Statistics", style = MaterialTheme.typography.titleLarge, color = GoldSun)
                StatLine("Level", "Lv. ${profile.xpLevel} (${profile.xpPoints} XP)")
                StatLine("Best Tower Height", "${profile.bestHeight} Floors")
                StatLine("Best Score", "${profile.bestScore}")
                StatLine("Total Towers Built", "${profile.totalTowersBuilt}")
                StatLine("Steady-Hand Accuracy", "$accuracy%")
                StatLine("Total Play Time", "${profile.playTimeSeconds / 60}m ${profile.playTimeSeconds % 60}s")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        ChunkyGameButton(
            text = "Sign Out",
            icon = Icons.AutoMirrored.Filled.Logout,
            onClick = onSignOut,
            containerColor = CrimsonDanger,
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFCBD5E1))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}
