package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.model.AppLanguage
import com.example.data.model.ShootBooking
import com.example.ui.screens.BookingScheduleBucket
import com.example.ui.screens.BookingScheduleHelper
import com.example.ui.screens.BookingStatusFilter
import com.example.ui.screens.MyBookingsScreen
import com.example.ui.theme.StarArtistTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class MyBookingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val refTodayMillis =
        BookingScheduleHelper.parsePreferredDateMidnightMillis("2026-11-13")!!

    private val sampleBookings = listOf(
        ShootBooking(
            id = "bk_up_1",
            userId = "user_1",
            clientName = "Aarav Verma",
            contactNumber = "9812890081",
            serviceType = "Photography (High-Fashion Editorial)",
            preferredDate = "2026-11-20",
            preferredTime = "10:00 AM",
            location = "Studio (Talwara Khurd)",
            notes = "Bring 3 editorial outfits",
            status = "Confirmed"
        ),
        ShootBooking(
            id = "bk_up_2",
            userId = "user_1",
            clientName = "Aarav Verma",
            contactNumber = "9812890081",
            serviceType = "Modeling (Comp-Card Shoot)",
            preferredDate = "2026-11-14",
            preferredTime = "02:00 PM",
            location = "Studio (Talwara Khurd)",
            status = "Pending"
        ),
        ShootBooking(
            id = "bk_past_1",
            userId = "user_1",
            clientName = "Aarav Verma",
            contactNumber = "9812890081",
            serviceType = "Video Production (4K Music Video)",
            preferredDate = "2026-10-25",
            preferredTime = "11:00 AM",
            location = "Sirsa Outdoor Set",
            status = "Completed"
        ),
        ShootBooking(
            id = "bk_past_2",
            userId = "user_1",
            clientName = "Aarav Verma",
            contactNumber = "9812890081",
            serviceType = "Photography (Bridal)",
            preferredDate = "2026-11-18",
            preferredTime = "04:00 PM",
            location = "Studio (Talwara Khurd)",
            status = "Cancelled"
        ),
        ShootBooking(
            id = "bk_past_3",
            userId = "user_1",
            clientName = "Aarav Verma",
            contactNumber = "9812890081",
            serviceType = "Photography (Studio Portrait)",
            preferredDate = "2026-11-01",
            preferredTime = "12:00 PM",
            location = "Studio (Talwara Khurd)",
            status = "Confirmed"
        )
    )

    @Test
    fun bookingScheduleHelper_partitionsAndSortsUpcomingVsPastAccurately() {
        val partitioned = BookingScheduleHelper.partitionAndFilterBookings(
            bookings = sampleBookings,
            todayMidnightMillis = refTodayMillis
        )

        assertEquals(listOf("bk_up_2", "bk_up_1"), partitioned.upcoming.map { it.id })
        assertEquals(listOf("bk_past_2", "bk_past_3", "bk_past_1"), partitioned.past.map { it.id })

        assertEquals(
            BookingScheduleBucket.UPCOMING,
            BookingScheduleHelper.classifyBooking(sampleBookings[0], refTodayMillis)
        )
        assertEquals(
            BookingScheduleBucket.PAST,
            BookingScheduleHelper.classifyBooking(sampleBookings[2], refTodayMillis)
        )
        assertEquals(
            BookingScheduleBucket.PAST,
            BookingScheduleHelper.classifyBooking(sampleBookings[3], refTodayMillis)
        )
        assertEquals(
            BookingScheduleBucket.PAST,
            BookingScheduleHelper.classifyBooking(sampleBookings[4], refTodayMillis)
        )
    }

    @Test
    fun bookingScheduleHelper_relativeLabelsAndStatusSearchFilteringWork() {
        assertEquals(
            "Tomorrow",
            BookingScheduleHelper.relativeDayLabel(sampleBookings[1], AppLanguage.ENGLISH, refTodayMillis)
        )
        assertEquals(
            "In 7 days",
            BookingScheduleHelper.relativeDayLabel(sampleBookings[0], AppLanguage.ENGLISH, refTodayMillis)
        )
        assertEquals(
            "Session Completed",
            BookingScheduleHelper.relativeDayLabel(sampleBookings[2], AppLanguage.ENGLISH, refTodayMillis)
        )
        assertEquals(
            "Cancelled",
            BookingScheduleHelper.relativeDayLabel(sampleBookings[3], AppLanguage.ENGLISH, refTodayMillis)
        )

        val confirmedOnly = BookingScheduleHelper.partitionAndFilterBookings(
            bookings = sampleBookings,
            statusFilter = BookingStatusFilter.CONFIRMED,
            todayMidnightMillis = refTodayMillis
        )
        assertEquals(1, confirmedOnly.upcoming.size)
        assertEquals(1, confirmedOnly.past.size)

        val searchOutdoor = BookingScheduleHelper.partitionAndFilterBookings(
            bookings = sampleBookings,
            searchQuery = "Sirsa Outdoor",
            todayMidnightMillis = refTodayMillis
        )
        assertEquals(0, searchOutdoor.upcoming.size)
        assertEquals(listOf("bk_past_1"), searchOutdoor.past.map { it.id })
    }

    @Test
    fun myBookingsScreen_displaysUpcomingAndPastWithStatusIndicators() {
        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                MyBookingsScreen(
                    language = AppLanguage.ENGLISH,
                    myBookings = sampleBookings,
                    referenceTodayMillis = refTodayMillis,
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("my_bookings_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("my_bookings_upcoming_header")
            .assertIsDisplayed()
            .assertTextContains("Upcoming Appointments (2)")

        composeTestRule.onNodeWithTag("my_bookings_list")
            .performScrollToNode(hasTestTag("my_booking_card_bk_up_2"))
        composeTestRule.onNodeWithTag("my_booking_card_bk_up_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("my_booking_status_bk_up_2").assertTextContains("Pending")

        composeTestRule.onNodeWithTag("my_bookings_list")
            .performScrollToNode(hasTestTag("my_booking_card_bk_up_1"))
        composeTestRule.onNodeWithTag("my_booking_card_bk_up_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("my_booking_status_bk_up_1").assertTextContains("Confirmed")

        composeTestRule.onNodeWithTag("my_bookings_list")
            .performScrollToNode(hasTestTag("my_bookings_past_header"))
        composeTestRule.onNodeWithTag("my_bookings_past_header")
            .assertIsDisplayed()
            .assertTextContains("Past Appointments (3)")
    }

    @Test
    fun myBookingsScreen_timelineAndStatusFiltersAndResetWork() {
        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = true) {
                MyBookingsScreen(
                    language = AppLanguage.ENGLISH,
                    myBookings = sampleBookings,
                    referenceTodayMillis = refTodayMillis,
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("my_bookings_timeline_past").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("my_bookings_past_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("my_booking_card_bk_up_1").assertDoesNotExist()

        composeTestRule.onNodeWithTag("my_bookings_search_input").performTextInput("nomatchxyz")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("my_bookings_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("my_bookings_reset_filters_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("my_bookings_upcoming_header").assertIsDisplayed()
    }

    @Test
    fun myBookingsScreen_cancelConfirmationRebookAndBackCallbacksWork() {
        var cancelledId = ""
        var rebookedService = ""
        var backPressed = false

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                MyBookingsScreen(
                    language = AppLanguage.ENGLISH,
                    myBookings = sampleBookings,
                    referenceTodayMillis = refTodayMillis,
                    onBack = { backPressed = true },
                    onBookAgain = { rebookedService = it },
                    onCancelBooking = { cancelledId = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("my_bookings_list")
            .performScrollToNode(hasTestTag("my_booking_card_bk_up_2"))
        composeTestRule.onNodeWithTag("my_booking_cancel_bk_up_2")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("confirm_cancel_booking_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_cancel_booking_yes").performClick()
        composeTestRule.waitForIdle()
        assertEquals("bk_up_2", cancelledId)

        composeTestRule.onNodeWithTag("my_bookings_list")
            .performScrollToNode(hasTestTag("my_booking_card_bk_past_1"))
        composeTestRule.onNodeWithTag("my_booking_rebook_bk_past_1")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals("Video Production (4K Music Video)", rebookedService)

        composeTestRule.onNodeWithTag("my_bookings_list")
            .performScrollToNode(hasTestTag("my_bookings_header_card"))
        composeTestRule.onNodeWithTag("my_bookings_back_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertTrue(backPressed)
    }

    @Test
    fun myBookingsScreen_supportsHindiLocalization() {
        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                MyBookingsScreen(
                    language = AppLanguage.HINDI,
                    myBookings = sampleBookings,
                    referenceTodayMillis = refTodayMillis,
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("मेरी स्टूडियो बुकिंग्स").assertIsDisplayed()
        composeTestRule.onNodeWithText("आगामी अपॉइंटमेंट्स (2)").assertIsDisplayed()
    }
}
