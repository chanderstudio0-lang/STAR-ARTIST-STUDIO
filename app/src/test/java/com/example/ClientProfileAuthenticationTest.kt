package com.example

import com.example.data.local.entity.ProjectDraftEntity
import com.example.data.model.ShootBooking
import com.example.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientProfileAuthenticationTest {

    @Test
    fun clientDisplayNameFallback_formatsEmailGracefullyWhenNameBlank() {
        fun resolveClientDisplayName(profile: UserProfile?, email: String?): String {
            return profile?.displayName?.takeIf { it.isNotBlank() }
                ?: email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                ?: "Studio Client"
        }

        // With explicit profile display name
        val profile = UserProfile(displayName = "Pooja Malhotra", email = "pooja@example.com")
        assertEquals("Pooja Malhotra", resolveClientDisplayName(profile, "pooja@example.com"))

        // With blank display name fallback to email
        val blankProfile = UserProfile(displayName = "", email = "vikram.singh@gmail.com")
        assertEquals("Vikram.singh", resolveClientDisplayName(blankProfile, "vikram.singh@gmail.com"))

        // Null profile fallback to email
        assertEquals("Rahul", resolveClientDisplayName(null, "rahul@gmail.com"))

        // Null profile & null email fallback to default
        assertEquals("Studio Client", resolveClientDisplayName(null, null))
    }

    @Test
    fun savedPhotographyProjects_tracksDraftAttributesAndFavorites() {
        val projects = listOf(
            ProjectDraftEntity(
                id = 1L,
                draftTitle = "Royal Heritage Bridal Campaign",
                projectType = "Bridal",
                conceptNotes = "Gold zari lehenga, warm amber backlight",
                isFavorite = true
            ),
            ProjectDraftEntity(
                id = 2L,
                draftTitle = "4K Cinematic Music Video",
                projectType = "Video Reel",
                conceptNotes = "Drone shots along Sirsa canal, slow motion gimbal",
                isFavorite = false
            ),
            ProjectDraftEntity(
                id = 3L,
                draftTitle = "Vogue High-Fashion Portfolio",
                projectType = "Photography",
                conceptNotes = "Dramatic rim-lighting, black backdrop",
                isFavorite = true
            )
        )

        assertEquals(3, projects.size)

        // Filter favorite projects
        val favorites = projects.filter { it.isFavorite }
        assertEquals(2, favorites.size)
        assertTrue(favorites.any { it.draftTitle == "Royal Heritage Bridal Campaign" })
        assertTrue(favorites.any { it.draftTitle == "Vogue High-Fashion Portfolio" })
        assertFalse(favorites.any { it.draftTitle == "4K Cinematic Music Video" })
    }

    @Test
    fun bookingHistoryTracking_tracksClientShootLifecycles() {
        val clientId = "user_client_789"
        val clientBookings = listOf(
            ShootBooking(
                id = "b_active",
                userId = clientId,
                serviceType = "Photography",
                preferredDate = "2026-11-25",
                preferredTime = "11:00 AM",
                status = "Confirmed"
            ),
            ShootBooking(
                id = "b_pending",
                userId = clientId,
                serviceType = "Video Reel",
                preferredDate = "2026-11-28",
                preferredTime = "03:00 PM",
                status = "Pending"
            ),
            ShootBooking(
                id = "b_past",
                userId = clientId,
                serviceType = "Bridal",
                preferredDate = "2026-10-15",
                preferredTime = "10:00 AM",
                status = "Completed"
            )
        )

        assertEquals("Client must have 3 bookings in history", 3, clientBookings.size)

        // Active / upcoming shoots (Confirmed + Pending)
        val upcoming = clientBookings.filter {
            it.status.equals("Confirmed", ignoreCase = true) || it.status.equals("Pending", ignoreCase = true)
        }
        assertEquals(2, upcoming.size)

        // Completed history
        val completed = clientBookings.filter { it.status.equals("Completed", ignoreCase = true) }
        assertEquals(1, completed.size)
        assertEquals("b_past", completed.first().id)
    }

    @Test
    fun bookingStageCardColors_matchStageExpectations() {
        fun getStageGuidanceKey(status: String): String {
            return when {
                status.equals("Cancelled", ignoreCase = true) -> "CANCELLED"
                status.equals("Completed", ignoreCase = true) -> "COMPLETED"
                status.equals("Confirmed", ignoreCase = true) -> "CONFIRMED"
                else -> "PENDING"
            }
        }

        assertEquals("PENDING", getStageGuidanceKey("Pending"))
        assertEquals("CONFIRMED", getStageGuidanceKey("Confirmed"))
        assertEquals("COMPLETED", getStageGuidanceKey("Completed"))
        assertEquals("CANCELLED", getStageGuidanceKey("Cancelled"))
    }
}
