package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tumble_tower_prefs")

enum class GraphicsQuality { LOW, MEDIUM, HIGH }

data class GameSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val sfxVolume: Float = 0.85f,
    val musicVolume: Float = 0.65f,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val particlesEnabled: Boolean = true,
    val batteryFriendly30Fps: Boolean = false,
    val languageCode: String = "en",
    val controlSensitivity: Float = 1.0f,
    val leftHandMode: Boolean = false,
    val colorblindMode: Boolean = false,
    val darkTheme: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val tutorialCompleted: Boolean = false
)

data class PowerUpInventory(
    val freeze: Int = 3,
    val glue: Int = 2,
    val undo: Int = 2,
    val shield: Int = 2,
    val crane: Int = 2
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SFX_VOLUME = floatPreferencesKey("sfx_volume")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val GRAPHICS_QUALITY = stringPreferencesKey("graphics_quality")
        val PARTICLES_ENABLED = booleanPreferencesKey("particles_enabled")
        val BATTERY_30FPS = booleanPreferencesKey("battery_30fps")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val SENSITIVITY = floatPreferencesKey("control_sensitivity")
        val LEFT_HAND = booleanPreferencesKey("left_hand_mode")
        val COLORBLIND = booleanPreferencesKey("colorblind_mode")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val TUTORIAL_DONE = booleanPreferencesKey("tutorial_completed")

        val PU_FREEZE = intPreferencesKey("pu_freeze")
        val PU_GLUE = intPreferencesKey("pu_glue")
        val PU_UNDO = intPreferencesKey("pu_undo")
        val PU_SHIELD = intPreferencesKey("pu_shield")
        val PU_CRANE = intPreferencesKey("pu_crane")

        val OWNED_SKINS = stringPreferencesKey("owned_skins")
        val OWNED_THEMES = stringPreferencesKey("owned_themes")
        val OWNED_EFFECTS = stringPreferencesKey("owned_effects")
        val GAMES_PLAYED_SINCE_AD = intPreferencesKey("games_played_since_ad")
        val LAST_SPIN_DATE = stringPreferencesKey("last_spin_date")
    }

    val settingsFlow: Flow<GameSettings> = context.dataStore.data.map { prefs ->
        val quality = runCatching {
            GraphicsQuality.valueOf(prefs[Keys.GRAPHICS_QUALITY] ?: GraphicsQuality.HIGH.name)
        }.getOrDefault(GraphicsQuality.HIGH)

        GameSettings(
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            sfxVolume = prefs[Keys.SFX_VOLUME] ?: 0.85f,
            musicVolume = prefs[Keys.MUSIC_VOLUME] ?: 0.65f,
            graphicsQuality = quality,
            particlesEnabled = prefs[Keys.PARTICLES_ENABLED] ?: true,
            batteryFriendly30Fps = prefs[Keys.BATTERY_30FPS] ?: false,
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
            controlSensitivity = prefs[Keys.SENSITIVITY] ?: 1.0f,
            leftHandMode = prefs[Keys.LEFT_HAND] ?: false,
            colorblindMode = prefs[Keys.COLORBLIND] ?: false,
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            tutorialCompleted = prefs[Keys.TUTORIAL_DONE] ?: false
        )
    }

    val powerUpsFlow: Flow<PowerUpInventory> = context.dataStore.data.map { prefs ->
        PowerUpInventory(
            freeze = prefs[Keys.PU_FREEZE] ?: 3,
            glue = prefs[Keys.PU_GLUE] ?: 2,
            undo = prefs[Keys.PU_UNDO] ?: 2,
            shield = prefs[Keys.PU_SHIELD] ?: 2,
            crane = prefs[Keys.PU_CRANE] ?: 2
        )
    }

    val ownedCosmeticsFlow: Flow<Triple<Set<String>, Set<String>, Set<String>>> =
        context.dataStore.data.map { prefs ->
            val skins = (prefs[Keys.OWNED_SKINS] ?: "wood").split(",").filter { it.isNotBlank() }.toSet()
            val themes = (prefs[Keys.OWNED_THEMES] ?: "forest").split(",").filter { it.isNotBlank() }.toSet()
            val effects = (prefs[Keys.OWNED_EFFECTS] ?: "dust").split(",").filter { it.isNotBlank() }.toSet()
            Triple(skins, themes, effects)
        }

    val lastSpinDateFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_SPIN_DATE] ?: ""
    }

    suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
        context.dataStore.edit { prefs ->
            val current = GameSettings(
                soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
                musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: true,
                hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
                sfxVolume = prefs[Keys.SFX_VOLUME] ?: 0.85f,
                musicVolume = prefs[Keys.MUSIC_VOLUME] ?: 0.65f,
                graphicsQuality = runCatching {
                    GraphicsQuality.valueOf(prefs[Keys.GRAPHICS_QUALITY] ?: GraphicsQuality.HIGH.name)
                }.getOrDefault(GraphicsQuality.HIGH),
                particlesEnabled = prefs[Keys.PARTICLES_ENABLED] ?: true,
                batteryFriendly30Fps = prefs[Keys.BATTERY_30FPS] ?: false,
                languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
                controlSensitivity = prefs[Keys.SENSITIVITY] ?: 1.0f,
                leftHandMode = prefs[Keys.LEFT_HAND] ?: false,
                colorblindMode = prefs[Keys.COLORBLIND] ?: false,
                darkTheme = prefs[Keys.DARK_THEME] ?: true,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
                tutorialCompleted = prefs[Keys.TUTORIAL_DONE] ?: false
            )
            val updated = transform(current)
            prefs[Keys.SOUND_ENABLED] = updated.soundEnabled
            prefs[Keys.MUSIC_ENABLED] = updated.musicEnabled
            prefs[Keys.HAPTICS_ENABLED] = updated.hapticsEnabled
            prefs[Keys.SFX_VOLUME] = updated.sfxVolume
            prefs[Keys.MUSIC_VOLUME] = updated.musicVolume
            prefs[Keys.GRAPHICS_QUALITY] = updated.graphicsQuality.name
            prefs[Keys.PARTICLES_ENABLED] = updated.particlesEnabled
            prefs[Keys.BATTERY_30FPS] = updated.batteryFriendly30Fps
            prefs[Keys.LANGUAGE_CODE] = updated.languageCode
            prefs[Keys.SENSITIVITY] = updated.controlSensitivity
            prefs[Keys.LEFT_HAND] = updated.leftHandMode
            prefs[Keys.COLORBLIND] = updated.colorblindMode
            prefs[Keys.DARK_THEME] = updated.darkTheme
            prefs[Keys.NOTIFICATIONS] = updated.notificationsEnabled
            prefs[Keys.TUTORIAL_DONE] = updated.tutorialCompleted
        }
    }

    suspend fun updatePowerUps(transform: (PowerUpInventory) -> PowerUpInventory) {
        context.dataStore.edit { prefs ->
            val current = PowerUpInventory(
                freeze = prefs[Keys.PU_FREEZE] ?: 3,
                glue = prefs[Keys.PU_GLUE] ?: 2,
                undo = prefs[Keys.PU_UNDO] ?: 2,
                shield = prefs[Keys.PU_SHIELD] ?: 2,
                crane = prefs[Keys.PU_CRANE] ?: 2
            )
            val updated = transform(current)
            prefs[Keys.PU_FREEZE] = updated.freeze.coerceAtLeast(0)
            prefs[Keys.PU_GLUE] = updated.glue.coerceAtLeast(0)
            prefs[Keys.PU_UNDO] = updated.undo.coerceAtLeast(0)
            prefs[Keys.PU_SHIELD] = updated.shield.coerceAtLeast(0)
            prefs[Keys.PU_CRANE] = updated.crane.coerceAtLeast(0)
        }
    }

    suspend fun unlockCosmetic(category: String, id: String) {
        context.dataStore.edit { prefs ->
            val key = when (category) {
                "skin" -> Keys.OWNED_SKINS
                "theme" -> Keys.OWNED_THEMES
                else -> Keys.OWNED_EFFECTS
            }
            val current = (prefs[key] ?: "").split(",").filter { it.isNotBlank() }.toMutableSet()
            current.add(id)
            prefs[key] = current.joinToString(",")
        }
    }

    suspend fun setLastSpinDate(dateKey: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SPIN_DATE] = dateKey
        }
    }

    suspend fun resetAllLocalPrefs() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
