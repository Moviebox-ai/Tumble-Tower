package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.GameSettings
import com.example.data.local.GraphicsQuality
import com.example.ui.components.AdBannerBar
import com.example.ui.components.GlassCard
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyanIce
import com.example.ui.theme.GoldSun
import com.example.ui.theme.SkyfallDeepIndigo
import com.example.ui.theme.SkyfallMidnight
import com.example.ui.theme.SkyfallSlateCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: GameSettings,
    adsRemoved: Boolean,
    onBack: () -> Unit,
    onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
    onLanguageChange: (String) -> Unit,
    onRestorePurchases: () -> Unit,
    onResetProgress: () -> Unit
) {
    val context = LocalContext.current
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogBody by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }

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
            Text("Settings", style = MaterialTheme.typography.headlineMedium, color = GoldSun)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Audio & Haptics
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Audio & Haptics", style = MaterialTheme.typography.titleLarge, color = GoldSun)
                        SettingSwitch("Sound Effects (SFX)", settings.soundEnabled) { v ->
                            onUpdateSettings { it.copy(soundEnabled = v) }
                        }
                        Text("SFX Volume (${(settings.sfxVolume * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Slider(
                            value = settings.sfxVolume,
                            onValueChange = { v -> onUpdateSettings { it.copy(sfxVolume = v) } },
                            colors = SliderDefaults.colors(thumbColor = GoldSun, activeTrackColor = GoldSun)
                        )
                        SettingSwitch("Dynamic Music", settings.musicEnabled) { v ->
                            onUpdateSettings { it.copy(musicEnabled = v) }
                        }
                        Text("Music Volume (${(settings.musicVolume * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Slider(
                            value = settings.musicVolume,
                            onValueChange = { v -> onUpdateSettings { it.copy(musicVolume = v) } },
                            colors = SliderDefaults.colors(thumbColor = CyanIce, activeTrackColor = CyanIce)
                        )
                        SettingSwitch("Haptic Vibration", settings.hapticsEnabled) { v ->
                            onUpdateSettings { it.copy(hapticsEnabled = v) }
                        }
                    }
                }
            }

            // Graphics & Performance
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Graphics & Performance", style = MaterialTheme.typography.titleLarge, color = CyanIce)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GraphicsQuality.entries.forEach { q ->
                                val active = settings.graphicsQuality == q
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) CyanIce else SkyfallMidnight)
                                        .clickable { onUpdateSettings { it.copy(graphicsQuality = q) } }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(q.name, style = MaterialTheme.typography.labelLarge, color = if (active) SkyfallMidnight else Color.White)
                                }
                            }
                        }
                        SettingSwitch("Particle Effects", settings.particlesEnabled) { v ->
                            onUpdateSettings { it.copy(particlesEnabled = v) }
                        }
                        SettingSwitch("Battery-Friendly Mode (30 FPS)", settings.batteryFriendly30Fps) { v ->
                            onUpdateSettings { it.copy(batteryFriendly30Fps = v) }
                        }
                    }
                }
            }

            // Controls & Accessibility
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Controls & Accessibility", style = MaterialTheme.typography.titleLarge, color = GoldSun)
                        Text("Pull Sensitivity (${String.format("%.1fx", settings.controlSensitivity)})", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Slider(
                            value = settings.controlSensitivity,
                            valueRange = 0.5f..1.8f,
                            onValueChange = { v -> onUpdateSettings { it.copy(controlSensitivity = v) } },
                            colors = SliderDefaults.colors(thumbColor = GoldSun, activeTrackColor = GoldSun)
                        )
                        SettingSwitch("Left-Hand Mode", settings.leftHandMode) { v ->
                            onUpdateSettings { it.copy(leftHandMode = v) }
                        }
                        SettingSwitch("Colorblind Symbols Mode", settings.colorblindMode) { v ->
                            onUpdateSettings { it.copy(colorblindMode = v) }
                        }
                        SettingSwitch("Dark Theme", settings.darkTheme) { v ->
                            onUpdateSettings { it.copy(darkTheme = v) }
                        }
                        SettingSwitch("Notifications", settings.notificationsEnabled) { v ->
                            onUpdateSettings { it.copy(notificationsEnabled = v) }
                        }
                    }
                }
            }

            // Language Selector
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Language", style = MaterialTheme.typography.titleLarge, color = CyanIce)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            languages.forEach { (code, label) ->
                                val active = settings.languageCode == code
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) GoldSun else SkyfallMidnight)
                                        .clickable { onLanguageChange(code) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (active) SkyfallMidnight else Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Legal, Support & Account Data
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Support & Info", style = MaterialTheme.typography.titleLarge, color = GoldSun)
                        SettingActionRow("Privacy Policy") {
                            infoDialogTitle = "Privacy Policy"
                            infoDialogBody = "Tumble Tower: Skyfall stores gameplay stats, campaign stars, and leaderboard scores in Firebase Firestore under your authenticated Google account. No sensitive personal data is sold or shared."
                        }
                        SettingActionRow("Terms of Service") {
                            infoDialogTitle = "Terms of Service"
                            infoDialogBody = "Virtual coins, gems, and cosmetics are non-transferable in-game items. Fair play applies to Global & Daily Challenge leaderboards."
                        }
                        SettingActionRow("Rate Us") {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                            runCatching { context.startActivity(intent) }
                        }
                        SettingActionRow("Share App") {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Play Tumble Tower: Skyfall! Build, Balance, Survive!")
                            }
                            context.startActivity(Intent.createChooser(share, "Share Tumble Tower"))
                        }
                        SettingActionRow("Contact Support") {
                            val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@tumbletower.example.com"))
                            runCatching { context.startActivity(mail) }
                        }
                        SettingActionRow("Restore Purchases") { onRestorePurchases() }
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { showResetConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset Progress")
                        }
                        Text(
                            text = "App Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        AdBannerBar(adsRemoved = adsRemoved)
    }

    if (infoDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { infoDialogTitle = null },
            title = { Text(infoDialogTitle!!) },
            text = { Text(infoDialogBody) },
            confirmButton = {
                TextButton(onClick = { infoDialogTitle = null }) { Text("OK") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Progress?") },
            text = { Text("This clears all campaign stars, local mode bests, coins, and settings.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetProgress()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingActionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SkyfallMidnight.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
        Text("›", style = MaterialTheme.typography.titleLarge, color = CyanIce)
    }
}
