package com.example

import com.example.data.model.GalleryItem
import com.example.data.model.ShootBooking
import com.example.ui.auth.AuthValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailVerificationAndPortfolioTest {

    @Test
    fun emailVerificationAccess_requiresConfirmedEmailForBooking() {
        // Confirmed email grants access to booking features
        assertTrue(AuthValidator.canAccessBooking(isEmailVerified = true, isAnonymous = false))

        // Unconfirmed email rejects booking features
        assertFalse(AuthValidator.canAccessBooking(isEmailVerified = false, isAnonymous = false))

        // Anonymous user cannot access booking features even if flag were somehow true
        assertFalse(AuthValidator.canAccessBooking(isEmailVerified = true, isAnonymous = true))
        assertFalse(AuthValidator.canAccessBooking(isEmailVerified = false, isAnonymous = true))
    }

    @Test
    fun emailVerificationNotice_formatsEmailAccurately() {
        val notice = AuthValidator.getEmailVerificationNotice("client@starartist.com")
        assertTrue(notice.contains("client@starartist.com"))
        assertTrue(notice.contains("verification link", ignoreCase = true))
        assertTrue(notice.contains("studio booking", ignoreCase = true))
    }

    @Test
    fun portfolioFirestoreItems_containValidMediaUrlsAndCategories() {
        val firestoreItems = listOf(
            GalleryItem(
                id = "fs_item_1",
                userId = "photographer_101",
                title = "Vogue Runway Fashion Stills",
                category = "Fashion",
                mediaType = "photo",
                mediaUrl = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=800&q=80",
                thumbnailUrl = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=400&q=80",
                status = "approved"
            ),
            GalleryItem(
                id = "fs_item_2",
                userId = "cinematographer_102",
                title = "4K Cinematic Music Video Production",
                category = "Video Reel",
                mediaType = "video",
                mediaUrl = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=800&q=80",
                thumbnailUrl = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=400&q=80",
                status = "approved"
            ),
            GalleryItem(
                id = "fs_item_3",
                userId = "bridal_specialist_103",
                title = "Royal Heritage Bridal Shoot",
                category = "Bridal",
                mediaType = "photo",
                mediaUrl = "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=800&q=80",
                thumbnailUrl = "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=400&q=80",
                status = "approved"
            )
        )

        assertEquals(3, firestoreItems.size)

        // All items have non-empty image URLs for Coil rendering
        firestoreItems.forEach { item ->
            assertTrue(item.mediaUrl.startsWith("https://"))
            assertTrue(item.thumbnailUrl.startsWith("https://"))
            assertNotNull(item.category)
            assertTrue(item.status == "approved")
        }

        // Filter by category
        val fashionItems = firestoreItems.filter { it.category.equals("Fashion", ignoreCase = true) }
        assertEquals(1, fashionItems.size)
        assertEquals("Vogue Runway Fashion Stills", fashionItems.first().title)

        // Filter by video type
        val videoItems = firestoreItems.filter { it.mediaType.equals("video", ignoreCase = true) }
        assertEquals(1, videoItems.size)
        assertEquals("4K Cinematic Music Video Production", videoItems.first().title)
    }

    @Test
    fun studioSessionBookingDetails_holdsServiceDateAndTimeSlot() {
        val booking = ShootBooking(
            id = "booking_doc_101",
            userId = "verified_client_id",
            clientName = "Pooja Sharma",
            contactNumber = "+919812890081",
            serviceType = "Photography (High-Fashion Editorial)",
            preferredDate = "2026-11-28",
            preferredTime = "11:00 AM (Studio Morning Light)",
            location = "Studio (Talwara Khurd - 4K Lighting Bay)",
            notes = "Require rim lighting and black satin backdrop",
            status = "Pending"
        )

        assertEquals("Photography (High-Fashion Editorial)", booking.serviceType)
        assertEquals("2026-11-28", booking.preferredDate)
        assertEquals("11:00 AM (Studio Morning Light)", booking.preferredTime)
        assertEquals("Studio (Talwara Khurd - 4K Lighting Bay)", booking.location)
        assertEquals("Pending", booking.status)
        assertTrue(booking.clientName.isNotBlank())
        assertTrue(booking.contactNumber.length >= 10)
    }

    @Test
    fun portfolioSearchAndCategoryFilter_filtersMatchingTitles() {
        val works = listOf(
            GalleryItem(id = "1", title = "Royal Heritage Bridal Stills", category = "Bridal"),
            GalleryItem(id = "2", title = "Vogue Runway High-Fashion Look", category = "Fashion"),
            GalleryItem(id = "3", title = "Sirsa Golden Hour Pre-Wedding", category = "Bridal"),
            GalleryItem(id = "4", title = "Dramatic Rim-Light Character Portrait", category = "Portrait")
        )

        fun filterWorks(query: String, category: String): List<GalleryItem> {
            return works.filter { item ->
                val matchesCat = category == "All" || item.category.equals(category, ignoreCase = true)
                val matchesSearch = query.isBlank() ||
                        item.title.contains(query, ignoreCase = true) ||
                        item.category.contains(query, ignoreCase = true)
                matchesCat && matchesSearch
            }
        }

        // Search for "Bridal" in "All"
        val bridalSearch = filterWorks("Bridal", "All")
        assertEquals(2, bridalSearch.size)

        // Filter by "Portrait" category
        val portraitFilter = filterWorks("", "Portrait")
        assertEquals(1, portraitFilter.size)
        assertEquals("Dramatic Rim-Light Character Portrait", portraitFilter.first().title)

        // Category "Fashion" with matching search query
        val fashionSearch = filterWorks("Runway", "Fashion")
        assertEquals(1, fashionSearch.size)

        // No match query
        val noMatch = filterWorks("NonExistentTerm", "All")
        assertTrue(noMatch.isEmpty())
    }
}
