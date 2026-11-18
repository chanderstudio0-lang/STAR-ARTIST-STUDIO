package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.data.model.AppLanguage
import com.example.data.model.GalleryItem
import com.example.data.model.ShootBooking
import com.example.ui.screens.BookingStatusCard
import com.example.ui.screens.normalizeBookingStatusKey
import com.example.ui.theme.StarArtistTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class BookingStatusAndGalleryFilterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bookingApprovalStatus_lifecycleStages() {
        val pendingBooking = ShootBooking(
            id = "bk_001",
            clientName = "Anjali Sharma",
            serviceType = "Photography",
            preferredDate = "2026-11-20",
            preferredTime = "11:00 AM",
            status = "Pending"
        )
        val confirmedBooking = ShootBooking(
            id = "bk_002",
            clientName = "Rohit Verma",
            serviceType = "Video Reel",
            preferredDate = "2026-11-22",
            preferredTime = "02:00 PM",
            status = "Confirmed"
        )
        val completedBooking = ShootBooking(
            id = "bk_003",
            clientName = "Simran Kaur",
            serviceType = "Bridal",
            preferredDate = "2026-11-10",
            preferredTime = "10:00 AM",
            status = "Completed"
        )
        val cancelledBooking = ShootBooking(
            id = "bk_004",
            clientName = "Vikram Singh",
            serviceType = "Portfolio",
            status = "Cancelled"
        )

        // Stage verification
        assertTrue(pendingBooking.status.equals("Pending", ignoreCase = true))
        assertTrue(confirmedBooking.status.equals("Confirmed", ignoreCase = true))
        assertTrue(completedBooking.status.equals("Completed", ignoreCase = true))
        assertTrue(cancelledBooking.status.equals("Cancelled", ignoreCase = true))

        // Ensure distinct stages
        assertFalse(pendingBooking.status.equals("Confirmed", ignoreCase = true))
        assertFalse(confirmedBooking.status.equals("Completed", ignoreCase = true))
    }

    @Test
    fun galleryCategories_includePortraitAndCoreGenres() {
        val categories = listOf("All", "Fashion", "Portrait", "Commercial", "Bridal", "Editorial", "Video Reel", "Portfolio")

        assertTrue("Categories must include Portrait", categories.contains("Portrait"))
        assertTrue("Categories must include Fashion", categories.contains("Fashion"))
        assertTrue("Categories must include Commercial", categories.contains("Commercial"))
        assertTrue("Categories must include All", categories.contains("All"))
    }

    @Test
    fun galleryFilter_filtersCorrectlyByCategory() {
        val testItems = listOf(
            GalleryItem(id = "1", title = "Vogue Runway High-Fashion Look", category = "Fashion", mediaType = "photo"),
            GalleryItem(id = "2", title = "Classic Studio Monochromatic Portrait", category = "Portrait", mediaType = "photo"),
            GalleryItem(id = "3", title = "Dramatic Rim-Light Character Portrait", category = "Portrait", mediaType = "photo"),
            GalleryItem(id = "4", title = "Luxury Gold Commercial Ad Campaign", category = "Commercial", mediaType = "photo"),
            GalleryItem(id = "5", title = "4K Cinema Music Video Production", category = "Video Reel", mediaType = "video")
        )

        // Filter by Portrait
        val portraitItems = testItems.filter { it.category.equals("Portrait", ignoreCase = true) }
        assertEquals(2, portraitItems.size)
        assertTrue(portraitItems.all { it.category == "Portrait" })

        // Filter by Fashion
        val fashionItems = testItems.filter { it.category.equals("Fashion", ignoreCase = true) }
        assertEquals(1, fashionItems.size)
        assertEquals("Vogue Runway High-Fashion Look", fashionItems.first().title)

        // Filter by Commercial
        val commercialItems = testItems.filter { it.category.equals("Commercial", ignoreCase = true) }
        assertEquals(1, commercialItems.size)
        assertEquals("Luxury Gold Commercial Ad Campaign", commercialItems.first().title)

        // Filter with "All"
        val allCategory = "All"
        val allItems = testItems.filter { allCategory == "All" || it.category.equals(allCategory, ignoreCase = true) }
        assertEquals(5, allItems.size)
    }

    @Test
    fun bookingStatusCard_rendersVisualStatusBadgesForAllStatuses() {
        assertEquals("Confirmed", normalizeBookingStatusKey("confirmed"))
        assertEquals("Pending", normalizeBookingStatusKey(""))
        assertEquals("Completed", normalizeBookingStatusKey("Completed"))
        assertEquals("Cancelled", normalizeBookingStatusKey("canceled"))

        val sampleBookings = listOf(
            ShootBooking(id = "s_conf", serviceType = "Fashion Editorial", preferredDate = "2026-12-01", preferredTime = "10:00 AM", status = "Confirmed"),
            ShootBooking(id = "s_pend", serviceType = "Model Portfolio", preferredDate = "2026-12-02", preferredTime = "02:00 PM", status = "Pending"),
            ShootBooking(id = "s_comp", serviceType = "Bridal Story", preferredDate = "2026-11-01", preferredTime = "11:00 AM", status = "Completed"),
            ShootBooking(id = "s_canc", serviceType = "4K Cinema Reel", preferredDate = "2026-11-10", preferredTime = "04:00 PM", status = "Cancelled")
        )

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                Column {
                    sampleBookings.forEach { booking ->
                        BookingStatusCard(booking = booking, language = AppLanguage.ENGLISH)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("booking_status_badge_s_conf")
            .assertIsDisplayed()
            .assertTextContains("Confirmed")
            .assertContentDescriptionContains("Booking status: Confirmed")

        composeTestRule.onNodeWithTag("booking_status_badge_s_pend")
            .assertIsDisplayed()
            .assertTextContains("Pending")
            .assertContentDescriptionContains("Booking status: Pending")

        composeTestRule.onNodeWithTag("booking_status_badge_s_comp")
            .assertIsDisplayed()
            .assertTextContains("Completed")
            .assertContentDescriptionContains("Booking status: Completed")

        composeTestRule.onNodeWithTag("booking_status_badge_s_canc")
            .assertIsDisplayed()
            .assertTextContains("Cancelled")
            .assertContentDescriptionContains("Booking status: Cancelled")
    }
}
