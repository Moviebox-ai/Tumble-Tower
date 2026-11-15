package com.example.game.physics

enum class BlockType(
    val displayName: String,
    val mass: Float,
    val baseFriction: Float,
    val restitution: Float,
    val coinValue: Int,
    val colorblindTag: String
) {
    WOOD("Wood", 1.0f, 0.78f, 0.08f, 5, "W"),
    ICE("Ice", 0.9f, 0.28f, 0.05f, 10, "I"),
    RUBBER("Rubber", 0.85f, 0.85f, 0.65f, 12, "R"),
    MAGNET("Magnet", 1.15f, 0.88f, 0.05f, 15, "M"),
    GOLD("Gold", 1.85f, 0.72f, 0.04f, 40, "G"),
    BOMB("Bomb (TNT)", 1.05f, 0.70f, 0.12f, 25, "T"),
    GLASS("Glass", 0.75f, 0.55f, 0.02f, 18, "X")
}

enum class LiveEventType(val title: String, val subtitle: String) {
    NONE("Calm Skies", "Steady tower physics"),
    WIND("Wind Gust", "Lateral wind pushes upper layers!"),
    EARTHQUAKE("Earthquake", "Seismic tremors shake every layer!"),
    RAIN("Storm Rain", "Wet blocks become extra slippery!"),
    LOW_GRAVITY("Low Gravity", "Blocks feel light and floaty!")
}

enum class PowerUpType(val title: String, val description: String, val gemCost: Int) {
    FREEZE("Freeze", "Stops timers, melt & live events for 12s", 5),
    GLUE("Glue", "Permanently locks a pulled or placed block", 6),
    UNDO("Undo", "Reverts your last move safely", 8),
    SHIELD("Shield", "Forgives one fatal tower collapse", 10),
    CRANE("Mini Crane", "Safely extracts and stacks a block on top", 12)
}

enum class GameMode(val routeKey: String, val title: String, val subtitle: String) {
    CAMPAIGN("campaign", "Campaign", "210 Levels across 5 Worlds"),
    CLASSIC("classic", "Classic", "The original tower challenge"),
    ENDLESS("endless", "Endless Rise", "Build as high as you can!"),
    TIME_ATTACK("time_attack", "Time Attack", "Build max blocks in 90 sec!"),
    SURVIVAL("survival", "Survival", "Face non-stop escalating events!"),
    PASS_AND_PLAY("pass_play", "Pass & Play", "2–4 Players, same device"),
    DAILY_CHALLENGE("daily_challenge", "Daily Challenge", "A new seeded tower every day!")
}

enum class BossType(val title: String, val rules: List<String>) {
    NONE("Standard Tower", emptyList()),
    LEANING_TOWER(
        "Leaning Tower",
        listOf(
            "Tower starts pre-tilted by 4.5°.",
            "Higher lateral wind intensity.",
            "Pull & stack blocks to survive target moves!"
        )
    ),
    PYRAMID_OF_DOOM(
        "Pyramid of Doom",
        listOf(
            "Heavy Gold & volatile TNT blocks inside.",
            "Glass blocks crack under heavy upper layers.",
            "Keep stability above danger zone!"
        )
    ),
    SPIRAL_SKYSCRAPER(
        "Spiral Skyscraper",
        listOf(
            "Alternating spiral layer offsets.",
            "Frequent Earthquakes + Ice friction shifts.",
            "Use Steady Hand combos to score 3 stars!"
        )
    )
}

data class BlockState(
    val id: Int,
    val layer: Int,
    val slot: Int, // 0, 1, 2
    val type: BlockType,
    val x: Float, // normalized horizontal coordinate (-1.5..1.5)
    val z: Float = 0f, // normalized perpendicular coordinate for alternating layers
    val y: Float = 0f, // vertical displacement during collapse or rubber bounce
    val vx: Float = 0f,
    val vy: Float = 0f,
    val vz: Float = 0f,
    val angleDeg: Float = 0f,
    val angularVel: Float = 0f,
    val meltProgress: Float = 0f, // 0..1 for ICE
    val bombFuseMoves: Int = 3, // moves left before TNT micro-blast
    val pressureLoad: Float = 0f, // 0..1+ for GLASS
    val bouncePhase: Float = 0f, // harmonic vibration amplitude for RUBBER
    val isGlued: Boolean = false,
    val isBeingPulled: Boolean = false,
    val pullProgress: Float = 0f, // 0..1 along extraction vector
    val pullDir: Float = 1f, // +1 or -1 pull direction
    val isShattered: Boolean = false
)

data class ParticleState(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val life: Float,
    val maxLife: Float,
    val kind: String // "dust", "spark", "confetti", "snow", "ember", "rain"
)

data class TowerSnapshot(
    val blocks: List<BlockState>,
    val score: Long,
    val combo: Int,
    val movesCompleted: Int,
    val topLayerIndex: Int,
    val nextPlaceSlot: Int,
    val towerTiltDeg: Float,
    val towerTiltZDeg: Float,
    val stability: Float
)

enum class PlayPhase {
    BOSS_INTRO,
    PLAYING,
    HOLDING_TO_PLACE,
    PAUSED,
    COLLAPSING_SLOW_MO,
    REPLAYING_COLLAPSE,
    LEVEL_COMPLETE,
    GAME_OVER
}

data class GameSessionConfig(
    val mode: GameMode,
    val worldId: Int = 1,
    val levelNumber: Int = 1,
    val targetMoves: Int = 8,
    val targetHeight: Int = 14,
    val bossType: BossType = BossType.NONE,
    val seed: Long = System.currentTimeMillis(),
    val playerNames: List<String> = listOf("Player 1", "Player 2"),
    val initialLayers: Int = 10,
    val gravityMultiplier: Float = 1.0f,
    val baseWindStrength: Float = 0.0f
)

data class PhysicsUiState(
    val config: GameSessionConfig = GameSessionConfig(GameMode.CLASSIC),
    val phase: PlayPhase = PlayPhase.PLAYING,
    val blocks: List<BlockState> = emptyList(),
    val particles: List<ParticleState> = emptyList(),
    val stabilityPercent: Int = 92,
    val towerTiltDeg: Float = 0f,
    val towerTiltZDeg: Float = 0f,
    val centerOfMassX: Float = 0f,
    val weakestLayerIndex: Int = 0,
    val towerHeightLayers: Int = 10,
    val movesCompleted: Int = 0,
    val score: Long = 0L,
    val heightBonus: Long = 0L,
    val comboBonus: Long = 0L,
    val coinsEarned: Int = 0,
    val currentCombo: Int = 0,
    val steadyHandPrecision: Float = 1.0f, // 0..1
    val isDraggingBlock: Boolean = false,
    val activePullBlockId: Int? = null,
    val holdingBlockType: BlockType? = null,
    val aimSlot: Int = 1,
    val aimOffsetNorm: Float = 0f, // -0.10..0.10 fine placement offset
    val activeEvent: LiveEventType = LiveEventType.NONE,
    val incomingEvent: LiveEventType? = null,
    val eventWarningSeconds: Int = 0,
    val windDirection: Float = 1f, // -1 left, +1 right
    val screenShakeMagnitude: Float = 0f,
    val freezeSecondsLeft: Int = 0,
    val shieldArmed: Boolean = false,
    val glueNextMove: Boolean = false,
    val timeLeftSeconds: Int = 90,
    val currentPlayerIndex: Int = 0,
    val lastWinningPlayerName: String? = null,
    val perfectPullsCount: Int = 0,
    val totalPullsCount: Int = 0,
    val starsEarned: Int = 0,
    val ghostBestHeight: Int = 14,
    val hasCollapseReplay: Boolean = false,
    val statusBannerText: String? = null,
    val canUndo: Boolean = false,
    val coinsDoubledByAd: Boolean = false,
    val revivedByAd: Boolean = false,
    // Readable-tower mechanic (see StructuralSolver)
    val xrayActive: Boolean = false,
    val focusPercent: Int = 100,
    val blockStress: Map<Int, BlockStress> = emptyMap(),
    val maxLoadRatio: Float = 0f,
    val pullRiskPercent: Int = -1, // -1 = hidden (Stress Vision off or nothing being pulled)
    val pullVerdict: PullVerdict? = null,
    val daredevilCount: Int = 0,
    val daredevilStreak: Int = 0,
    val swayCalm: Float = 1f, // 1 = still, 0 = swaying hard; release a block when this is high for a Calm Drop
    val calmDropCount: Int = 0
)
