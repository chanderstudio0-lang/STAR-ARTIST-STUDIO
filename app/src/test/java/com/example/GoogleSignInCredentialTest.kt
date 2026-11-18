package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.test.core.app.ApplicationProvider
import com.example.ui.auth.AuthValidator
import com.example.ui.auth.GoogleSignInButton
import com.example.ui.theme.StarArtistTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoogleSignInCredentialTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun resolveWebClientId_loadsGeneratedGoogleServicesClientId() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clientId = AuthValidator.resolveWebClientId(context)
        assertNotNull("default_web_client_id must be generated from google-services.json", clientId)
        assertTrue(clientId!!.endsWith(".apps.googleusercontent.com"))
    }

    @Test
    fun buildInteractiveGoogleSignInRequest_usesSingleGetSignInWithGoogleOption() {
        val testClientId = "test-client-id.apps.googleusercontent.com"
        val request = AuthValidator.buildInteractiveGoogleSignInRequest(testClientId)

        assertEquals(1, request.credentialOptions.size)
        val option = request.credentialOptions.first()
        assertTrue(option is GetSignInWithGoogleOption)
        assertEquals(testClientId, (option as GetSignInWithGoogleOption).serverClientId)
    }

    @Test
    fun buildAutoGoogleSignInRequest_usesSingleAuthorizedGetGoogleIdOption() {
        val testClientId = "test-client-id.apps.googleusercontent.com"
        val request = AuthValidator.buildAutoGoogleSignInRequest(testClientId)

        assertEquals(1, request.credentialOptions.size)
        val option = request.credentialOptions.first()
        assertTrue(option is GetGoogleIdOption)
        val googleIdOption = option as GetGoogleIdOption
        assertEquals(testClientId, googleIdOption.serverClientId)
        assertTrue(googleIdOption.filterByAuthorizedAccounts)
        assertTrue(googleIdOption.autoSelectEnabled)
    }

    @Test
    fun mapGoogleCredentialException_mapsCredentialErrorsCleanly() {
        val noCredMsg = AuthValidator.mapGoogleCredentialException(NoCredentialException("none"))
        assertTrue(noCredMsg.contains("No Google account found", ignoreCase = true))

        val providerMsg = AuthValidator.mapGoogleCredentialException(
            GetCredentialProviderConfigurationException("missing")
        )
        assertTrue(providerMsg.contains("Google Play Services", ignoreCase = true))

        val unsupportedMsg = AuthValidator.mapGoogleCredentialException(
            GetCredentialUnsupportedException("unsupported")
        )
        assertTrue(unsupportedMsg.contains("not supported", ignoreCase = true))

        val parsingMsg = AuthValidator.mapGoogleCredentialException(
            GoogleIdTokenParsingException(Exception("bad token"))
        )
        assertTrue(parsingMsg.contains("invalid Google ID token", ignoreCase = true))

        val devErrMsg = AuthValidator.mapGoogleCredentialException(
            GetCredentialCustomException("TYPE_TEST", "10: DEVELOPER_ERROR")
        )
        assertTrue(devErrMsg.contains("SHA-1", ignoreCase = true))
    }

    @Test
    fun googleSignInButton_handlesClickAndLoadingStates() {
        var clickCount = 0
        composeTestRule.setContent {
            StarArtistTheme {
                GoogleSignInButton(
                    isLoading = false,
                    onClick = { clickCount++ }
                )
            }
        }

        composeTestRule.onNodeWithTag("auth_google_signin_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_google_signin_button").performClick()
        assertEquals(1, clickCount)
    }

    @Test
    fun googleSignInButton_loadingStateDisablesButtonAndShowsProgressText() {
        composeTestRule.setContent {
            StarArtistTheme {
                GoogleSignInButton(
                    isLoading = true,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("auth_google_signin_button").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Connecting Google...").assertIsDisplayed()
        assertFalse(false)
    }
}
