package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.LeaderboardUiState
import com.example.viewmodel.TumbleTowerViewModel
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityLaunchTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearFirebaseAppsToSimulateMissingGoogleServices() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        FirebaseApp.getApps(context).toList().forEach { app ->
            runCatching { app.delete() }
        }
    }

    @Test
    fun coldLaunch_withoutGoogleServices_transitionsSplashToTutorialToHome_withoutCrash() {
        // Splash screen renders cleanly
        composeRule.onNodeWithText("SKYFALL").assertIsDisplayed()

        // Advance Compose clock past SplashScreen animation (1600ms + 200ms)
        composeRule.mainClock.advanceTimeBy(2100L)
        composeRule.waitForIdle()

        // Tutorial screen appears cleanly without Auth crash when FirebaseApp is absent
        composeRule.onNodeWithTag("tutorial_skip_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tutorial_skip_button").performClick()
        composeRule.waitForIdle()

        // Home screen renders cleanly
        composeRule.onNodeWithTag("home_play_button").assertIsDisplayed()
    }

    @Test
    fun viewModel_inLocalOfflineMode_initializesCleanly() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = TumbleTowerViewModel(app, "local_player", firebaseEnabled = false)
        assertTrue(vm.userProfile.value.userId == "local_player")
        assertTrue(vm.leaderboardState.value is LeaderboardUiState.Loading || vm.leaderboardState.value is LeaderboardUiState.Error)
        vm.startCampaignLevel(1)
    }
}
