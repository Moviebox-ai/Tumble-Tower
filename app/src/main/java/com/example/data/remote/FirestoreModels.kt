package com.example.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "SkyBuilder",
    val avatarId: String = "avatar_crown",
    val coins: Long = 500L,
    val gems: Long = 20L,
    val xpLevel: Long = 1L,
    val xpPoints: Long = 0L,
    val equippedSkin: String = "wood",
    val equippedTheme: String = "forest",
    val equippedEffect: String = "dust",
    val adsRemoved: Boolean = false,
    val bestHeight: Long = 0L,
    val bestScore: Long = 0L,
    val totalTowersBuilt: Long = 0L,
    val perfectPulls: Long = 0L,
    val totalPulls: Long = 0L,
    val playTimeSeconds: Long = 0L,
    val dailyStreak: Long = 0L,
    val lastDailyRewardDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LevelProgressDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val levelNumber: Long = 1L,
    val worldId: Long = 1L,
    val stars: Long = 0L,
    val highScore: Long = 0L,
    val bestHeight: Long = 0L,
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LeaderboardEntryDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "SkyBuilder",
    val avatarId: String = "avatar_crown",
    val boardType: String = "daily",
    val dateKey: String = "",
    val score: Long = 0L,
    val height: Long = 0L,
    val accuracyPercent: Long = 0L,
    val isPublic: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
