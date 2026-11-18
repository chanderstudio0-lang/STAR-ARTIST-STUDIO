package com.example

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.ui.theme.StarArtistTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class MainActivityAuthRedirectTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun resolveAuthRedirectTarget_mapsAuthAndVerificationHoldCorrectly() {
        assertEquals(
            AuthRedirectTarget.LOGIN,
            resolveAuthRedirectTarget(isAuthenticated = false, holdAuthForVerification = false)
        )
        assertEquals(
            AuthRedirectTarget.LOGIN,
            resolveAuthRedirectTarget(isAuthenticated = false, holdAuthForVerification = true)
        )
        assertEquals(
            AuthRedirectTarget.LOGIN,
            resolveAuthRedirectTarget(isAuthenticated = true, holdAuthForVerification = true)
        )
        assertEquals(
            AuthRedirectTarget.HOME,
            resolveAuthRedirectTarget(isAuthenticated = true, holdAuthForVerification = false)
        )
    }

    @Test
    fun mainActivity_registersAuthStateListenerAndClearsHoldOnSignOut() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start()
        val activity = controller.get()

        assertNotNull(activity.authStateListener)

        // Simulate verification hold then sign-out callback via AuthStateListener handler
        activity.setHoldAuthForVerification(true)
        assertTrue(activity.holdAuthForVerification.value)

        activity.handleAuthStateChange(null)
        assertNull(activity.currentUser.value)
        assertFalse(activity.holdAuthForVerification.value)

        controller.stop().destroy()
    }

    @Test
    fun authRedirectHost_redirectsAutomaticallyBetweenLoginAndHome() {
        var isAuthenticated by mutableStateOf(false)
        var currentUid by mutableStateOf("")
        var holdForVerification by mutableStateOf(false)

        composeTestRule.setContent {
            StarArtistTheme {
                AuthRedirectHost(
                    isAuthenticated = isAuthenticated,
                    currentUserId = currentUid,
                    holdAuthForVerification = holdForVerification,
                    loginScreen = { Text("Login Screen") },
                    homeScreen = { uid -> Text("Home Screen: $uid") }
                )
            }
        }

        // Unauthenticated starts at Login
        composeTestRule.onNodeWithTag("auth_redirect_login_container").assertIsDisplayed()
        composeTestRule.onNodeWithText("Login Screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_redirect_home_container").assertDoesNotExist()

        // New sign-up held for email verification stays on Login
        isAuthenticated = true
        currentUid = "artist_uid_101"
        holdForVerification = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auth_redirect_login_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_redirect_home_container").assertDoesNotExist()

        // Authenticated and verified redirects automatically to Home
        holdForVerification = false
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auth_redirect_home_container").assertIsDisplayed()
        composeTestRule.onNodeWithText("Home Screen: artist_uid_101").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_redirect_login_container").assertDoesNotExist()

        // Account switch updates Home scope cleanly
        currentUid = "artist_uid_202"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Home Screen: artist_uid_202").assertIsDisplayed()

        // Sign-out redirects automatically back to Login
        isAuthenticated = false
        currentUid = ""
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auth_redirect_login_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_redirect_home_container").assertDoesNotExist()
    }
}
