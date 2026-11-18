package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.data.model.AppLanguage
import com.example.ui.components.BookingPickerUtils
import com.example.ui.components.MultiStepBookingForm
import com.example.ui.components.ServiceBookingFormComponent
import com.example.ui.theme.StarArtistTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class BookingPickersAndFormTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bookingPickerUtils_roundTripsIsoDatesInUtcWithoutTimezoneShift() {
        val isoDate = "2026-12-25"
        val utcMillis = BookingPickerUtils.parseDateMillisForPicker(isoDate)
        assertNotNull(utcMillis)
        assertEquals(isoDate, BookingPickerUtils.formatUtcMillisAsIsoDate(utcMillis!!))
        assertNull(BookingPickerUtils.parseDateMillisForPicker(""))
        assertNull(BookingPickerUtils.parseDateMillisForPicker("25/12/2026"))
    }

    @Test
    fun bookingPickerUtils_parsesAndFormats12HourAnd24HourTimes() {
        assertEquals(14 to 30, BookingPickerUtils.parseHourMinuteForPicker("02:30 PM (Editorial Softbox Bay)"))
        assertEquals(10 to 0, BookingPickerUtils.parseHourMinuteForPicker("10:00 AM"))
        assertEquals(18 to 45, BookingPickerUtils.parseHourMinuteForPicker("18:45"))
        assertEquals(11 to 0, BookingPickerUtils.parseHourMinuteForPicker("Flexible"))

        assertEquals("09:05 AM", BookingPickerUtils.formatPickedTime12Hour(9, 5))
        assertEquals("03:45 PM", BookingPickerUtils.formatPickedTime12Hour(15, 45))
        assertEquals("12:00 PM", BookingPickerUtils.formatPickedTime12Hour(12, 0))
        assertEquals("12:00 AM", BookingPickerUtils.formatPickedTime12Hour(0, 0))
    }

    @Test
    fun serviceBookingForm_pickersAndServiceTypeSelectionSubmitExpectedPayload() {
        var capturedServiceType = ""
        var capturedDate = ""
        var capturedTime = ""
        var capturedName = ""
        var capturedPhone = ""
        var capturedNotes = ""
        var submitCount = 0

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ServiceBookingFormComponent(
                        language = AppLanguage.ENGLISH,
                        initialServiceType = "Photography",
                        initialDate = "2099-11-20",
                        initialTime = "02:30 PM (Editorial Softbox Bay)",
                        onSaveToFirestore = { name, phone, service, date, time, _, notes, onSuccess ->
                            submitCount += 1
                            capturedName = name
                            capturedPhone = phone
                            capturedServiceType = service
                            capturedDate = date
                            capturedTime = time
                            capturedNotes = notes
                            onSuccess("firestore_booking_101")
                        }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("service_booking_form").assertIsDisplayed()

        // 1. Select Video Production service type
        composeTestRule.onNodeWithTag("service_type_video_production")
            .performScrollTo()
            .performClick()

        // 2. Open Material 3 DatePicker dialog and confirm selected date
        composeTestRule.onNodeWithTag("service_booking_date_picker_button")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("m3_date_picker_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("m3_date_picker_confirm").performClick()
        composeTestRule.onNodeWithTag("service_booking_date_input")
            .performScrollTo()
            .assertTextContains("2099-11-20")

        // 3. Open Material 3 TimePicker dialog, verify clock & preview, then confirm
        composeTestRule.onNodeWithTag("service_booking_time_picker_button")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("m3_time_picker_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("m3_time_picker").assertIsDisplayed()
        composeTestRule.onNodeWithTag("m3_time_picker_preview").assertTextContains("02:30 PM")
        composeTestRule.onNodeWithTag("m3_time_picker_confirm").performClick()

        composeTestRule.onNodeWithTag("service_booking_time_input")
            .performScrollTo()
            .assertTextContains("02:30 PM")

        // 4. Fill client details & notes and submit to Firestore callback
        composeTestRule.onNodeWithTag("service_booking_client_name")
            .performScrollTo()
            .performTextReplacement("Simran Kaur")
        composeTestRule.onNodeWithTag("service_booking_phone_input")
            .performScrollTo()
            .performTextReplacement("9812890081")
        composeTestRule.onNodeWithTag("service_booking_notes_input")
            .performScrollTo()
            .performTextReplacement("4K gimbal reel sequence")

        composeTestRule.onNodeWithTag("service_booking_submit_button")
            .performScrollTo()
            .performClick()

        assertEquals(1, submitCount)
        assertEquals("Video Production", capturedServiceType)
        assertEquals("2099-11-20", capturedDate)
        assertEquals("02:30 PM", capturedTime)
        assertEquals("Simran Kaur", capturedName)
        assertEquals("9812890081", capturedPhone)
        assertEquals("4K gimbal reel sequence", capturedNotes)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("booking_success_confirmation")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Reference ID: firestore_booking_101")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("booking_success_track_btn")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun multiStepBookingForm_lightTheme_navigatesPickersAndSavesPayload() {
        var savedServiceType = ""
        var savedDate = ""
        var savedTime = ""
        var savedName = ""
        var savedPhone = ""
        var saveInvoked = false

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = true) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    MultiStepBookingForm(
                        language = AppLanguage.ENGLISH,
                        initialServiceType = "Photography",
                        initialDate = "2099-12-10",
                        initialTime = "04:30 PM (Golden Hour Outdoor)",
                        onSaveToFirestore = { name, phone, service, date, time, _, _, onSuccess ->
                            saveInvoked = true
                            savedName = name
                            savedPhone = phone
                            savedServiceType = service
                            savedDate = date
                            savedTime = time
                            onSuccess("ms_doc_77")
                        }
                    )
                }
            }
        }

        // Step 0: Select Modeling category and advance
        composeTestRule.onNodeWithTag("multi_step_booking_form").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_type_modeling")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("multi_step_next_button")
            .performScrollTo()
            .performClick()

        // Step 1: Use M3 DatePicker and M3 TimePicker modals, then advance
        composeTestRule.onNodeWithTag("step_date_time").assertIsDisplayed()
        composeTestRule.onNodeWithTag("booking_date_picker_button")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("m3_date_picker_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("m3_date_picker_confirm").performClick()

        composeTestRule.onNodeWithTag("booking_time_picker_button")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("m3_time_picker_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("m3_time_picker_preview").assertTextContains("04:30 PM")
        composeTestRule.onNodeWithTag("m3_time_picker_confirm").performClick()

        composeTestRule.onNodeWithTag("booking_time_input")
            .performScrollTo()
            .assertTextContains("04:30 PM")

        composeTestRule.onNodeWithTag("multi_step_next_button")
            .performScrollTo()
            .performClick()

        // Step 2: Client details & Firestore submit
        composeTestRule.onNodeWithTag("step_user_details").assertIsDisplayed()
        composeTestRule.onNodeWithTag("booking_clientname_input")
            .performScrollTo()
            .performTextReplacement("Harpreet Gill")
        composeTestRule.onNodeWithTag("booking_phone_input")
            .performScrollTo()
            .performTextReplacement("9876543210")

        composeTestRule.onNodeWithTag("multi_step_submit_button")
            .performScrollTo()
            .performClick()

        assertTrue(saveInvoked)
        assertEquals("Modeling", savedServiceType)
        assertEquals("2099-12-10", savedDate)
        assertEquals("04:30 PM", savedTime)
        assertEquals("Harpreet Gill", savedName)
        assertEquals("9876543210", savedPhone)

        composeTestRule.onNodeWithTag("booking_success_animation")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
