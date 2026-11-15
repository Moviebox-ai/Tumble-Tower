package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

private inline fun <reified T : Throwable> assertFailsWith(block: () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        if (T::class.java.isInstance(e)) return e as T
        throw AssertionError("Expected ${T::class.java.simpleName}, got ${e::class.java.simpleName}", e)
    }
    fail("Expected ${T::class.java.simpleName} to be thrown, but nothing was thrown")
    throw AssertionError("Unreachable")
}

class FirebaseSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_andSaveLevelProgress_succeedsForOwner() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = FirebaseSyncRepository(firestore, auth)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile("AliceSky")
        }
        assertTrue(profileResult.isSuccess)
        assertEquals(aliceUid, profileResult.getOrThrow().userId)

        val levelResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveLevelProgress(
                levelNumber = 1,
                worldId = 1,
                stars = 3,
                highScore = 4500L,
                bestHeight = 16,
                completed = true
            )
        }
        assertTrue(levelResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeLevelProgress(aliceUid).first { it.isNotEmpty() }
        }
        assertEquals(3L, observed.first().stars)
    }

    @Test
    fun observeUserProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirebaseSyncRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.ensureUserProfile("AliceSky").getOrThrow() }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FirebaseSyncRepository(firestore, auth)
        val exception = assertFailsWith<Exception> {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeUserProfile(aliceUid).first()
            }
        }
        val firestoreException = checkNotNull(exception.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $exception"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun leaderboard_submitAndObserve_succeedsAuthenticated_andFailsUnauthenticated() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = FirebaseSyncRepository(firestore, auth)
        val submitResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitLeaderboardScore(
                displayName = "AliceSky",
                avatarId = "avatar_crown",
                boardType = "daily",
                dateKey = "2026-11-13",
                score = 8420L,
                height = 24,
                accuracyPercent = 94
            )
        }
        assertTrue(
            "submitResult failed: ${submitResult.exceptionOrNull()?.stackTraceToString()}",
            submitResult.isSuccess
        )

        val entries = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeLeaderboard("daily").first { it.isNotEmpty() }
        }
        assertTrue(entries.any { it.displayName == "AliceSky" && it.score == 8420L })

        auth.signOut()
        val unauthEx = assertFailsWith<Exception> {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeLeaderboard("daily").first()
            }
        }
        val firestoreException = checkNotNull(unauthEx.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $unauthEx"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_tower@test.com"
        const val BOB_EMAIL = "bob_tower@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
