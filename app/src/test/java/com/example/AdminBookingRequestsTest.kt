package com.example

import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.model.AppLanguage
import com.example.data.model.ShootBooking
import com.example.ui.components.AdminBookingRequestsComponent
import com.example.ui.components.AdminDashboardMetrics
import com.example.ui.components.AdminItemStatus
import com.example.ui.components.AdminSummaryDashboard
import com.example.ui.components.countPendingBookingsByService
import com.example.ui.components.filterAdminBookingRequests
import com.example.ui.components.isPendingBookingStatus
import com.example.ui.theme.StarArtistTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class AdminBookingRequestsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleBookings(): List<ShootBooking> = listOf(
        ShootBooking(
            id = "bk_pending_1",
            userId = "u1",
            clientName = "Aarav Mehta",
            contactNumber = "9876543210",
            serviceType = "Photography",
            preferredDate = "2026-12-01",
            preferredTime = "10:00 AM",
            location = "Studio A (Talwara Khurd)",
            notes = "Portfolio lookbook",
            status = "Pending"
        ),
        ShootBooking(
            id = "bk_pending_2",
            userId = "u2",
            clientName = "Neha Kapoor",
            contactNumber = "9123456780",
            serviceType = "Video Production",
            preferredDate = "2026-12-03",
            preferredTime = "03:00 PM",
            location = "Studio B",
            status = "" // blank defaults to pending
        ),
        ShootBooking(
            id = "bk_confirmed_1",
            userId = "u3",
            clientName = "Kabir Sharma",
            contactNumber = "9988776655",
            serviceType = "Modeling",
            preferredDate = "2026-12-05",
            preferredTime = "11:00 AM",
            status = "Confirmed"
        ),
        ShootBooking(
            id = "bk_completed_1",
            userId = "u4",
            clientName = "Priya Verma",
            contactNumber = "9001122334",
            serviceType = "Photography",
            preferredDate = "2026-11-12",
            preferredTime = "02:00 PM",
            status = "Completed"
        ),
        ShootBooking(
            id = "bk_cancelled_1",
            userId = "u5",
            clientName = "Rohan Gill",
            contactNumber = "9554433221",
            serviceType = "Modeling",
            preferredDate = "2026-11-15",
            preferredTime = "04:00 PM",
            status = "Cancelled"
        )
    )

    @Test
    fun isPendingBookingStatus_identifiesPendingAndBlankValues() {
        assertTrue(isPendingBookingStatus("Pending"))
        assertTrue(isPendingBookingStatus("pending"))
        assertTrue(isPendingBookingStatus(""))
        assertTrue(isPendingBookingStatus("   "))
        assertFalse(isPendingBookingStatus("Confirmed"))
        assertFalse(isPendingBookingStatus("Completed"))
        assertFalse(isPendingBookingStatus("Cancelled"))
    }

    @Test
    fun filterAdminBookingRequests_filtersStatusServiceAndSearchAccurately() {
        val all = sampleBookings()

        val pendingOnly = filterAdminBookingRequests(all, statusFilter = "PENDING")
        assertEquals(listOf("bk_pending_1", "bk_pending_2"), pendingOnly.map { it.id })

        val confirmedOnly = filterAdminBookingRequests(all, statusFilter = "CONFIRMED")
        assertEquals(listOf("bk_confirmed_1"), confirmedOnly.map { it.id })

        val completedOnly = filterAdminBookingRequests(all, statusFilter = "COMPLETED")
        assertEquals(listOf("bk_completed_1"), completedOnly.map { it.id })

        val cancelledOnly = filterAdminBookingRequests(all, statusFilter = "CANCELLED")
        assertEquals(listOf("bk_cancelled_1"), cancelledOnly.map { it.id })

        val videoPending = filterAdminBookingRequests(
            all,
            statusFilter = "PENDING",
            serviceFilter = "VIDEO_PRODUCTION"
        )
        assertEquals(listOf("bk_pending_2"), videoPending.map { it.id })

        val searchByPhone = filterAdminBookingRequests(
            all,
            statusFilter = "ALL",
            searchQuery = "9988776655"
        )
        assertEquals(listOf("bk_confirmed_1"), searchByPhone.map { it.id })

        val searchByClient = filterAdminBookingRequests(
            all,
            statusFilter = "PENDING",
            searchQuery = "aarav"
        )
        assertEquals(listOf("bk_pending_1"), searchByClient.map { it.id })
    }

    @Test
    fun countPendingBookingsByService_aggregatesOnlyPendingRequests() {
        val counts = countPendingBookingsByService(sampleBookings())
        assertEquals(1, counts["Photography"])
        assertEquals(1, counts["Video Production"])
        assertEquals(0, counts["Modeling"])
        assertEquals(0, counts["Other"])
    }

    @Test
    fun adminBookingRequestsComponent_defaultsToPendingAndHandlesActions() {
        val statusUpdates = mutableListOf<Pair<String, String>>()
        val deletedIds = mutableListOf<String>()

        composeTestRule.setContent {
            StarArtistTheme {
                AdminBookingRequestsComponent(
                    bookings = sampleBookings(),
                    isLoading = false,
                    language = AppLanguage.ENGLISH,
                    onUpdateStatus = { id, status -> statusUpdates.add(id to status) },
                    onDeleteBooking = { id -> deletedIds.add(id) }
                )
            }
        }

        // Pending banner and first pending booking card appear immediately by default
        composeTestRule.onNodeWithTag("admin_pending_bookings_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_booking_card_bk_pending_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_booking_card_bk_confirmed_1").assertDoesNotExist()

        // Confirm Slot one-tap action on first pending card
        composeTestRule.onNodeWithTag("admin_bookings_lazy_list")
            .performScrollToNode(hasTestTag("booking_confirm_action_bk_pending_1"))
        composeTestRule.onNodeWithTag("booking_confirm_action_bk_pending_1").performClick()
        assertEquals(listOf("bk_pending_1" to "Confirmed"), statusUpdates)

        // Delete with confirmation dialog on first pending card
        composeTestRule.onNodeWithTag("admin_bookings_lazy_list")
            .performScrollToNode(hasTestTag("booking_delete_button_bk_pending_1"))
        composeTestRule.onNodeWithTag("booking_delete_button_bk_pending_1").performClick()
        composeTestRule.onNodeWithTag("booking_delete_dialog_bk_pending_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("booking_delete_confirm_bk_pending_1").performClick()
        assertEquals(listOf("bk_pending_1"), deletedIds)

        // Scroll down to second pending booking and decline with confirmation dialog
        composeTestRule.onNodeWithTag("admin_bookings_lazy_list")
            .performScrollToNode(hasTestTag("admin_booking_card_bk_pending_2"))
        composeTestRule.onNodeWithTag("admin_booking_card_bk_pending_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_bookings_lazy_list")
            .performScrollToNode(hasTestTag("booking_decline_action_bk_pending_2"))
        composeTestRule.onNodeWithTag("booking_decline_action_bk_pending_2").performClick()
        composeTestRule.onNodeWithTag("booking_decline_dialog_bk_pending_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("booking_decline_confirm_bk_pending_2").performClick()
        assertEquals(
            listOf("bk_pending_1" to "Confirmed", "bk_pending_2" to "Cancelled"),
            statusUpdates
        )

        // Scroll back to filter chips and switch to All + search to verify confirmed booking appears
        composeTestRule.onNodeWithTag("admin_bookings_lazy_list")
            .performScrollToNode(hasTestTag("admin_booking_filter_all"))
        composeTestRule.onNodeWithTag("admin_booking_filter_all").performClick()
        composeTestRule.onNodeWithTag("admin_booking_search_input").performTextInput("Kabir")
        composeTestRule.onNodeWithTag("admin_bookings_lazy_list")
            .performScrollToNode(hasTestTag("admin_booking_card_bk_confirmed_1"))
        composeTestRule.onNodeWithTag("admin_booking_card_bk_confirmed_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_booking_card_bk_pending_1").assertDoesNotExist()
    }

    @Test
    fun bookingStatusChips_renderDistinctColorCodedIndicatorsForBookingRequests() {
        assertNotEquals(AdminItemStatus.PENDING.primaryColor, AdminItemStatus.CONFIRMED.primaryColor)
        assertNotEquals(AdminItemStatus.CONFIRMED.primaryColor, AdminItemStatus.CANCELLED.primaryColor)
        assertNotEquals(AdminItemStatus.PENDING.backgroundColor, AdminItemStatus.CANCELLED.backgroundColor)
        assertEquals("Pending", AdminItemStatus.from("pending").shortLabel)
        assertEquals("Confirmed", AdminItemStatus.from("Confirmed").shortLabel)
        assertEquals("Cancelled", AdminItemStatus.from("Cancelled").shortLabel)

        val bookings = sampleBookings()
        val metrics = AdminDashboardMetrics.compute(emptyList(), bookings, emptyList())
        var clickedTab = -1

        composeTestRule.setContent {
            StarArtistTheme {
                androidx.compose.foundation.layout.Column {
                    AdminSummaryDashboard(
                        metrics = metrics,
                        language = AppLanguage.ENGLISH,
                        onSelectTab = { clickedTab = it }
                    )
                    AdminBookingRequestsComponent(
                        bookings = bookings,
                        isLoading = false,
                        language = AppLanguage.ENGLISH,
                        onUpdateStatus = { _, _ -> }
                    )
                }
            }
        }

        // Overview color-coded status chips
        composeTestRule.onNodeWithTag("admin_overview_booking_status_chips").assertIsDisplayed()
        composeTestRule.onNodeWithTag("overview_chip_booking_pending")
            .assertIsDisplayed()
            .assertContentDescriptionContains("Pending (2)", substring = true)
        composeTestRule.onNodeWithTag("overview_chip_booking_confirmed")
            .assertIsDisplayed()
            .assertContentDescriptionContains("Confirmed (1)", substring = true)
        composeTestRule.onNodeWithTag("overview_chip_booking_cancelled")
            .assertIsDisplayed()
            .assertContentDescriptionContains("Cancelled (1)", substring = true)
            .performClick()
        assertEquals(1, clickedTab)

        // Booking queue header summary chips & card status chip
        composeTestRule.onNodeWithTag("admin_booking_status_summary_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("booking_status_chip_bk_pending_1")
            .assertIsDisplayed()
            .assertContentDescriptionContains("Pending", substring = true)

        // Tap Cancelled color-coded summary chip to inspect cancelled card chip
        composeTestRule.onNodeWithTag("admin_summary_chip_cancelled").performClick()
        composeTestRule.onNodeWithTag("admin_bookings_lazy_list")
            .performScrollToNode(hasTestTag("admin_booking_card_bk_cancelled_1"))
        composeTestRule.onNodeWithTag("booking_status_chip_bk_cancelled_1")
            .assertIsDisplayed()
            .assertContentDescriptionContains("Cancelled", substring = true)
    }

    @Test
    fun searchBar_filtersPendingBookingsByCustomerNameAndServiceType() {
        composeTestRule.setContent {
            StarArtistTheme {
                AdminBookingRequestsComponent(
                    bookings = sampleBookings(),
                    isLoading = false,
                    language = AppLanguage.ENGLISH,
                    onUpdateStatus = { _, _ -> }
                )
            }
        }

        // Pinned search bar is immediately displayed on Pending queue
        composeTestRule.onNodeWithTag("admin_booking_search_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_booking_card_bk_pending_1").assertIsDisplayed()

        // Filter pending booking requests by customer name ("Aarav")
        composeTestRule.onNodeWithTag("admin_booking_search_input").performTextInput("Aarav")
        composeTestRule.onNodeWithTag("admin_booking_search_summary").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_booking_card_bk_pending_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_booking_card_bk_pending_2").assertDoesNotExist()
        composeTestRule.onNodeWithTag("admin_booking_card_bk_confirmed_1").assertDoesNotExist()

        // Clear search restores pending bookings
        composeTestRule.onNodeWithTag("admin_booking_search_clear").performClick()
        composeTestRule.onNodeWithTag("admin_booking_search_summary").assertDoesNotExist()

        // Filter pending booking requests by service type ("Video Production")
        composeTestRule.onNodeWithTag("admin_booking_search_input").performTextInput("Video Production")
        composeTestRule.onNodeWithTag("admin_booking_search_summary").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_booking_card_bk_pending_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_booking_card_bk_pending_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("admin_booking_card_bk_confirmed_1").assertDoesNotExist()
    }
}
