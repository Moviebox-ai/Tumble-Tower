package com.example.game.campaign

import androidx.compose.ui.graphics.Color
import com.example.game.physics.BossType
import com.example.game.physics.GameMode
import com.example.game.physics.GameSessionConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class WorldTheme(
    val id: Int,
    val key: String,
    val name: String,
    val subtitle: String,
    val skyTop: Color,
    val skyBottom: Color,
    val accentColor: Color,
    val gravityMult: Float,
    val baseWind: Float,
    val particleKind: String
)

data class CampaignLevel(
    val levelNumber: Int, // 1..210
    val worldId: Int,     // 1..5
    val levelInWorld: Int,// 1..42
    val targetMoves: Int,
    val initialLayers: Int,
    val bossType: BossType
)

data class ShopItem(
    val id: String,
    val category: String, // "skin", "theme", "effect"
    val name: String,
    val description: String,
    val coinPrice: Int,
    val gemPrice: Int,
    val previewPrimary: Color,
    val previewSecondary: Color
)

data class DailyRewardTier(
    val day: Int,
    val label: String,
    val coins: Int,
    val gems: Int
)

data class WheelSlice(
    val label: String,
    val coins: Int,
    val gems: Int,
    val color: Color
)

object CampaignCatalog {
    const val LEVELS_PER_WORLD = 42 // 5 worlds * 42 = 210 levels!

    val worlds: List<WorldTheme> = listOf(
        WorldTheme(
            id = 1,
            key = "forest",
            name = "Mystic Forest",
            subtitle = "Gentle winds & classic wood",
            skyTop = Color(0xFF0A1D2B),
            skyBottom = Color(0xFF1A4332),
            accentColor = Color(0xFF22C55E),
            gravityMult = 1.0f,
            baseWind = 0.08f,
            particleKind = "dust"
        ),
        WorldTheme(
            id = 2,
            key = "desert",
            name = "Sunstone Desert",
            subtitle = "Heavy gold & sand gusts",
            skyTop = Color(0xFF2B132C),
            skyBottom = Color(0xFF8B3E17),
            accentColor = Color(0xFFF59E0B),
            gravityMult = 1.06f,
            baseWind = 0.20f,
            particleKind = "dust"
        ),
        WorldTheme(
            id = 3,
            key = "ice",
            name = "Frostbite Peaks",
            subtitle = "Melting ice & blizzards",
            skyTop = Color(0xFF091B3A),
            skyBottom = Color(0xFF154E7A),
            accentColor = Color(0xFF38BDF8),
            gravityMult = 1.0f,
            baseWind = 0.25f,
            particleKind = "snow"
        ),
        WorldTheme(
            id = 4,
            key = "space",
            name = "Nebula Orbit",
            subtitle = "Low gravity & magnet cores",
            skyTop = Color(0xFF060417),
            skyBottom = Color(0xFF2B1055),
            accentColor = Color(0xFFA855F7),
            gravityMult = 0.78f,
            baseWind = 0.15f,
            particleKind = "spark"
        ),
        WorldTheme(
            id = 5,
            key = "lava",
            name = "Lava Kingdom",
            subtitle = "Seismic tremors & TNT blasts",
            skyTop = Color(0xFF1D0711),
            skyBottom = Color(0xFF6B1414),
            accentColor = Color(0xFFEF4444),
            gravityMult = 1.12f,
            baseWind = 0.30f,
            particleKind = "ember"
        )
    )

    val allLevels: List<CampaignLevel> by lazy {
        buildList {
            var globalNum = 1
            worlds.forEach { world ->
                for (i in 1..LEVELS_PER_WORLD) {
                    val isBoss = globalNum % 10 == 0
                    val boss = when {
                        !isBoss -> BossType.NONE
                        (globalNum / 10) % 3 == 1 -> BossType.LEANING_TOWER
                        (globalNum / 10) % 3 == 2 -> BossType.PYRAMID_OF_DOOM
                        else -> BossType.SPIRAL_SKYSCRAPER
                    }
                    add(
                        CampaignLevel(
                            levelNumber = globalNum,
                            worldId = world.id,
                            levelInWorld = i,
                            targetMoves = if (isBoss) 12 else (5 + (i / 6)).coerceAtMost(14),
                            initialLayers = (8 + (world.id - 1) + (i / 14)).coerceAtMost(15),
                            bossType = boss
                        )
                    )
                    globalNum++
                }
            }
        }
    }

    fun getWorld(worldId: Int): WorldTheme = worlds.firstOrNull { it.id == worldId } ?: worlds.first()

    fun getLevel(levelNumber: Int): CampaignLevel =
        allLevels.getOrElse(levelNumber - 1) { allLevels.first() }

    fun toSessionConfig(level: CampaignLevel): GameSessionConfig {
        val world = getWorld(level.worldId)
        return GameSessionConfig(
            mode = GameMode.CAMPAIGN,
            worldId = level.worldId,
            levelNumber = level.levelNumber,
            targetMoves = level.targetMoves,
            targetHeight = level.initialLayers + level.targetMoves,
            bossType = level.bossType,
            seed = 7000L + level.levelNumber * 131L,
            initialLayers = level.initialLayers,
            gravityMultiplier = world.gravityMult,
            baseWindStrength = if (level.bossType == BossType.LEANING_TOWER) world.baseWind + 0.3f else world.baseWind
        )
    }

    fun todayDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date())
    }

    fun dailyChallengeConfig(dateKey: String = todayDateKey()): GameSessionConfig {
        val seed = dateKey.hashCode().toLong() * 31337L
        val worldIdx = ((dateKey.hashCode() and 0x7FFFFFFF) % worlds.size) + 1
        val world = getWorld(worldIdx)
        val boss = if ((dateKey.hashCode() and 1) == 0) BossType.SPIRAL_SKYSCRAPER else BossType.LEANING_TOWER
        return GameSessionConfig(
            mode = GameMode.DAILY_CHALLENGE,
            worldId = world.id,
            levelNumber = 0,
            targetMoves = 999,
            targetHeight = 28,
            bossType = boss,
            seed = seed,
            initialLayers = 12,
            gravityMultiplier = world.gravityMult,
            baseWindStrength = world.baseWind + 0.15f
        )
    }

    val shopItems: List<ShopItem> = listOf(
        // Skins
        ShopItem("wood", "skin", "Royal Oak", "Warm handcrafted timber", 0, 0, Color(0xFFD97706), Color(0xFF92400E)),
        ShopItem("neon", "skin", "Neon Grid", "Electric cyber edge glow", 800, 0, Color(0xFF06B6D4), Color(0xFFEC4899)),
        ShopItem("candy", "skin", "Sugar Rush", "Pastel glazed candy bricks", 1200, 0, Color(0xFFF472B6), Color(0xFFA78BFA)),
        ShopItem("marble", "skin", "Aegean Marble", "Polished temple stone", 1800, 0, Color(0xFFE2E8F0), Color(0xFF64748B)),
        ShopItem("cyber", "skin", "Mecha Alloy", "Titanium carbon blocks", 0, 35, Color(0xFF38BDF8), Color(0xFF1E293B)),
        // Tower Themes
        ShopItem("forest", "theme", "Mystic Forest", "Emerald canopy backdrop", 0, 0, Color(0xFF22C55E), Color(0xFF064E3B)),
        ShopItem("desert", "theme", "Sunstone Dunes", "Golden twilight canyon", 900, 0, Color(0xFFF59E0B), Color(0xFF7C2D12)),
        ShopItem("ice", "theme", "Aurora Glacier", "Frozen crystalline sky", 1400, 0, Color(0xFF38BDF8), Color(0xFF0C4A6E)),
        ShopItem("space", "theme", "Nebula Void", "Deep starfield cosmos", 2000, 0, Color(0xFFA855F7), Color(0xFF1E1B4B)),
        ShopItem("lava", "theme", "Volcanic Core", "Molten obsidian throne", 0, 40, Color(0xFFEF4444), Color(0xFF450A0A)),
        // Particle Effects
        ShopItem("dust", "effect", "Classic Dust", "Soft stone impact puffs", 0, 0, Color(0xFFCBD5E1), Color(0xFF64748B)),
        ShopItem("sparks", "effect", "Plasma Sparks", "Electric gold burst trails", 750, 0, Color(0xFFFACC15), Color(0xFFF97316)),
        ShopItem("confetti", "effect", "Party Confetti", "Celebration ribbons on stack", 1100, 0, Color(0xFFEC4899), Color(0xFF38BDF8)),
        ShopItem("snow", "effect", "Frost Flurry", "Shimmering ice crystals", 1300, 0, Color(0xFFBAE6FD), Color(0xFFFFFFFF)),
        ShopItem("embers", "effect", "Solar Embers", "Rising fiery sparks", 0, 25, Color(0xFFEF4444), Color(0xFFFBBF24))
    )

    val dailyRewards: List<DailyRewardTier> = listOf(
        DailyRewardTier(1, "Day 1", 250, 0),
        DailyRewardTier(2, "Day 2", 400, 5),
        DailyRewardTier(3, "Day 3", 600, 10),
        DailyRewardTier(4, "Day 4", 850, 15),
        DailyRewardTier(5, "Day 5", 1200, 20),
        DailyRewardTier(6, "Day 6", 1600, 30),
        DailyRewardTier(7, "Day 7", 3000, 60)
    )

    val spinSlices: List<WheelSlice> = listOf(
        WheelSlice("200 Coins", 200, 0, Color(0xFF3B82F6)),
        WheelSlice("15 Gems", 0, 15, Color(0xFFA855F7)),
        WheelSlice("500 Coins", 500, 0, Color(0xFF10B981)),
        WheelSlice("5 Gems", 0, 5, Color(0xFFEC4899)),
        WheelSlice("1000 Coins", 1000, 0, Color(0xFFF59E0B)),
        WheelSlice("30 Gems", 0, 30, Color(0xFF6366F1)),
        WheelSlice("350 Coins", 350, 0, Color(0xFF06B6D4)),
        WheelSlice("JACKPOT!", 2500, 40, Color(0xFFEF4444))
    )
}
