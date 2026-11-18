package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.example.data.model.AppLanguage
import com.example.ui.components.VoiceBookingAssistantPanel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ShootBookingScreen
import com.example.ui.theme.StarArtistTheme
import com.example.ui.viewmodel.VoiceBookingPrefill
import com.example.util.VoiceBookingCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class VoiceBookingAssistantTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun voiceBookingAssistant_parsesModelingCommandAndSubmitsBooking() {
        var submittedName = ""
        var submittedPhone = ""
        var submittedService = ""
        var submittedLocation = ""
        var submitCount = 0

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    VoiceBookingAssistantPanel(
                        language = AppLanguage.ENGLISH,
                        defaultClientName = "Aman Gill",
                        defaultPhoneNumber = "9812890081",
                        isEmailVerified = true,
                        onParseCommand = { transcript ->
                            VoiceBookingCommandParser.parseLocally(transcript)
                        },
                        onSubmitBooking = { name, phone, service, _, _, loc, _, onSuccess ->
                            submitCount += 1
                            submittedName = name
                            submittedPhone = phone
                            submittedService = service
                            submittedLocation = loc
                            onSuccess("voice_doc_42")
                        }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("voice_booking_assistant").assertIsDisplayed()

        composeTestRule.onNodeWithTag("voice_booking_command_input")
            .performScrollTo()
            .performTextReplacement("Book modeling runway session tomorrow at 4:30 PM outdoor")
        composeTestRule.onNodeWithTag("voice_booking_parse_button")
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("voice_parsed_booking_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Modeling (Runway & Lookbook Session)")
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("voice_confirm_booking_button")
            .performScrollTo()
            .performClick()

        assertEquals(1, submitCount)
        assertEquals("Aman Gill", submittedName)
        assertEquals("9812890081", submittedPhone)
        assertEquals("Modeling (Runway & Lookbook Session)", submittedService)
        assertEquals(VoiceBookingCommandParser.LOCATION_OUTDOOR, submittedLocation)

        composeTestRule.onNodeWithTag("voice_booking_success_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Reference ID: voice_doc_42")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun voiceBookingAssistant_sampleChipPhotographyAndApplyToFullForm_transfersPrefill() {
        var capturedPrefill: VoiceBookingPrefill? = null

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = true) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    VoiceBookingAssistantPanel(
                        language = AppLanguage.ENGLISH,
                        defaultClientName = "Priya Verma",
                        defaultPhoneNumber = "9541024098",
                        isEmailVerified = true,
                        onSubmitBooking = { _, _, _, _, _, _, _, _ -> },
                        onApplyToFullForm = { prefill ->
                            capturedPrefill = prefill
                        }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("voice_sample_chip_photography")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("voice_parsed_booking_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("voice_apply_to_form_button")
            .performScrollTo()
            .performClick()

        assertNotNull(capturedPrefill)
        assertEquals("Photography (High-Fashion Editorial)", capturedPrefill?.serviceType)
        assertEquals("10:00 AM (Morning Key Light Bay)", capturedPrefill?.preferredTime)
        assertEquals("Priya Verma", capturedPrefill?.clientName)
        assertEquals("9541024098", capturedPrefill?.contactNumber)
    }

    @Test
    fun shootBookingScreen_switchesToVoiceBookingModeAndShowsMicButton() {
        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                ShootBookingScreen(
                    language = AppLanguage.ENGLISH,
                    myBookings = emptyList(),
                    isSubmitting = false,
                    isEmailVerified = true,
                    onSubmit = { _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("booking_mode_switcher")
            .performScrollToNode(hasTestTag("mode_voice_booking_chip"))
        composeTestRule.onNodeWithTag("mode_voice_booking_chip")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("booking_form")
            .performScrollToNode(hasTestTag("voice_booking_assistant"))
        composeTestRule.onNodeWithTag("voice_booking_assistant").assertIsDisplayed()
        composeTestRule.onNodeWithTag("voice_booking_mic_button").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysVoiceBookingCtaAndTriggersCallback() {
        var opened = false
        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                HomeScreen(
                    language = AppLanguage.ENGLISH,
                    onNavigate = {},
                    onOpenVoiceBooking = { opened = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("home_screen")
            .performScrollToNode(hasTestTag("home_voice_book_button"))
        composeTestRule.onNodeWithTag("home_voice_book_button")
            .assertIsDisplayed()
            .performClick()

        assertTrue(opened)
    }
}
