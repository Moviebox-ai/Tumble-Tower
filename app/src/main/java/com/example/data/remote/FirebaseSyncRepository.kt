package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfileDoc?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfileDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(ex, OperationType.GET, docRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(displayName: String, avatarId: String = "avatar_crown"): Result<UserProfileDoc> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(
                    UserProfileDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfileDoc(id = uid, userId = uid, displayName = displayName)
                Result.success(existing)
            } else {
                val safeName = displayName.trim().take(40).ifEmpty { "SkyBuilder" }
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "avatarId" to avatarId,
                    "coins" to 500L,
                    "gems" to 25L,
                    "xpLevel" to 1L,
                    "xpPoints" to 0L,
                    "equippedSkin" to "wood",
                    "equippedTheme" to "forest",
                    "equippedEffect" to "dust",
                    "adsRemoved" to false,
                    "bestHeight" to 0L,
                    "bestScore" to 0L,
                    "totalTowersBuilt" to 0L,
                    "perfectPulls" to 0L,
                    "totalPulls" to 0L,
                    "playTimeSeconds" to 0L,
                    "dailyStreak" to 0L,
                    "lastDailyRewardDate" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                val created = docRef.get().await().toObject(
                    UserProfileDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfileDoc(id = uid, userId = uid, displayName = safeName)
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfileStats(
        displayName: String,
        avatarId: String,
        coins: Long,
        gems: Long,
        xpLevel: Long,
        xpPoints: Long,
        equippedSkin: String,
        equippedTheme: String,
        equippedEffect: String,
        adsRemoved: Boolean,
        bestHeight: Long,
        bestScore: Long,
        totalTowersBuilt: Long,
        perfectPulls: Long,
        totalPulls: Long,
        playTimeSeconds: Long,
        dailyStreak: Long,
        lastDailyRewardDate: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val safeName = displayName.trim().take(40).ifEmpty { "SkyBuilder" }
        val updates = mapOf(
            "displayName" to safeName,
            "avatarId" to avatarId,
            "coins" to coins.coerceAtLeast(0L),
            "gems" to gems.coerceAtLeast(0L),
            "xpLevel" to xpLevel.coerceIn(1L, 1000L),
            "xpPoints" to xpPoints.coerceAtLeast(0L),
            "equippedSkin" to equippedSkin,
            "equippedTheme" to equippedTheme,
            "equippedEffect" to equippedEffect,
            "adsRemoved" to adsRemoved,
            "bestHeight" to bestHeight.coerceAtLeast(0L),
            "bestScore" to bestScore.coerceAtLeast(0L),
            "totalTowersBuilt" to totalTowersBuilt.coerceAtLeast(0L),
            "perfectPulls" to perfectPulls.coerceAtLeast(0L),
            "totalPulls" to totalPulls.coerceAtLeast(0L),
            "playTimeSeconds" to playTimeSeconds.coerceAtLeast(0L),
            "dailyStreak" to dailyStreak.coerceIn(0L, 3650L),
            "lastDailyRewardDate" to lastDailyRewardDate.take(20),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeLevelProgress(userId: String): Flow<List<LevelProgressDoc>> = flow {
        val colRef = db.collection("users").document(userId).collection("level_progress")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LevelProgressDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(ex, OperationType.LIST, colRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun saveLevelProgress(
        levelNumber: Int,
        worldId: Int,
        stars: Int,
        highScore: Long,
        bestHeight: Int,
        completed: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val docId = "level_$levelNumber"
        val docRef = db.collection("users").document(uid).collection("level_progress").document(docId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val prevStars = (existing.getLong("stars") ?: 0L).toInt()
                val prevScore = existing.getLong("highScore") ?: 0L
                val prevHeight = (existing.getLong("bestHeight") ?: 0L).toInt()
                val prevCompleted = existing.getBoolean("completed") ?: false
                docRef.update(
                    mapOf(
                        "stars" to maxOf(prevStars, stars).coerceIn(0, 3).toLong(),
                        "highScore" to maxOf(prevScore, highScore).coerceAtLeast(0L),
                        "bestHeight" to maxOf(prevHeight, bestHeight).coerceAtLeast(0).toLong(),
                        "completed" to (prevCompleted || completed),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "levelNumber" to levelNumber.toLong(),
                        "worldId" to worldId.toLong(),
                        "stars" to stars.coerceIn(0, 3).toLong(),
                        "highScore" to highScore.coerceAtLeast(0L),
                        "bestHeight" to bestHeight.coerceAtLeast(0).toLong(),
                        "completed" to completed,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeLeaderboard(boardType: String): Flow<List<LeaderboardEntryDoc>> = flow {
        val colRef = db.collection("leaderboard_entries")
        val query = colRef
            .whereEqualTo("isPublic", true)
            .whereEqualTo("boardType", boardType)
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LeaderboardEntryDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.score }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null) {
                        handleFirestoreError(ex, OperationType.LIST, colRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun submitLeaderboardScore(
        displayName: String,
        avatarId: String,
        boardType: String,
        dateKey: String,
        score: Long,
        height: Int,
        accuracyPercent: Int
    ): Result<String> {
        val uid = requireUserId()
        val safeDateKey = dateKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(32)
        val docId = "${uid}_${boardType}_${safeDateKey}".take(128)
        val docRef = db.collection("leaderboard_entries").document(docId)
        val safeName = displayName.trim().take(40).ifEmpty { "SkyBuilder" }
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val prevScore = snap.getLong("score") ?: 0L
                val prevHeight = (snap.getLong("height") ?: 0L).toInt()
                if (score >= prevScore) {
                    docRef.update(
                        mapOf(
                            "displayName" to safeName,
                            "avatarId" to avatarId,
                            "score" to score.coerceAtLeast(0L),
                            "height" to height.coerceAtLeast(prevHeight).toLong(),
                            "accuracyPercent" to accuracyPercent.coerceIn(0, 100).toLong(),
                            "isPublic" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "avatarId" to avatarId,
                        "boardType" to boardType,
                        "dateKey" to safeDateKey,
                        "score" to score.coerceAtLeast(0L),
                        "height" to height.coerceAtLeast(0).toLong(),
                        "accuracyPercent" to accuracyPercent.coerceIn(0, 100).toLong(),
                        "isPublic" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }
}
