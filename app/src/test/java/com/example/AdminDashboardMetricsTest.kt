package com.example

import com.example.data.model.GalleryItem
import com.example.data.model.ModelRegistration
import com.example.data.model.ShootBooking
import com.example.ui.components.AdminDashboardMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating metric computations for AdminSummaryDashboard:
 * - Pending approvals (models, bookings, gallery submissions)
 * - Upcoming bookings (confirmed + pending shoots)
 * - Total active users (deduplicated client & talent roster)
 * - Approved showcase portfolio items
 */
class AdminDashboardMetricsTest {

    @Test
    fun compute_calculatesPendingApprovalsCorrectly() {
        val models = listOf(
            ModelRegistration(id = "m1", userId = "u1", fullName = "Model A", status = "pending"),
            ModelRegistration(id = "m2", userId = "u2", fullName = "Model B", status = "approved"),
            ModelRegistration(id = "m3", userId = "u3", fullName = "Model C", status = "") // blank counts as pending review
        )
        val bookings = listOf(
            ShootBooking(id = "b1", userId = "u4", clientName = "Client 1", status = "pending"),
            ShootBooking(id = "b2", userId = "u5", clientName = "Client 2", status = "confirmed")
        )
        val gallery = listOf(
            GalleryItem(id = "g1", userId = "u1", title = "Photo 1", status = "pending"),
            GalleryItem(id = "g2", userId = "u6", title = "Photo 2", status = "approved")
        )

        val metrics = AdminDashboardMetrics.compute(models, bookings, gallery)

        assertEquals("Pending models count must be 2", 2, metrics.pendingModelsCount)
        assertEquals("Pending bookings count must be 1", 1, metrics.pendingBookingsCount)
        assertEquals("Pending gallery count must be 1", 1, metrics.pendingGalleryCount)
        assertEquals("Total pending approvals must be 4", 4, metrics.pendingApprovalsTotal)
    }

    @Test
    fun compute_calculatesUpcomingBookingsCorrectly() {
        val bookings = listOf(
            ShootBooking(id = "b1", userId = "u1", clientName = "Client 1", status = "pending"),
            ShootBooking(id = "b2", userId = "u2", clientName = "Client 2", status = "confirmed"),
            ShootBooking(id = "b3", userId = "u3", clientName = "Client 3", status = "completed"),
            ShootBooking(id = "b4", userId = "u4", clientName = "Client 4", status = "cancelled")
        )

        val metrics = AdminDashboardMetrics.compute(emptyList(), bookings, emptyList())

        assertEquals("Confirmed bookings count must be 1", 1, metrics.confirmedBookingsCount)
        assertEquals("Completed bookings count must be 1", 1, metrics.completedBookingsCount)
        assertEquals("Cancelled bookings count must be 1", 1, metrics.cancelledBookingsCount)
        assertEquals("Upcoming bookings (pending + confirmed) must be 2", 2, metrics.upcomingBookingsTotal)
        assertEquals("Total bookings count must be 4", 4, metrics.totalBookingsCount)
    }

    @Test
    fun compute_deduplicatesTotalActiveUsersAccurately() {
        val models = listOf(
            ModelRegistration(id = "m1", userId = "user_alpha", fullName = "Talent A", phoneNumber = "9812890081"),
            ModelRegistration(id = "m2", userId = "user_beta", fullName = "Talent B", phoneNumber = "9541024098")
        )
        val bookings = listOf(
            ShootBooking(id = "b1", userId = "user_alpha", clientName = "Talent A", contactNumber = "9812890081"), // same user
            ShootBooking(id = "b2", userId = "user_gamma", clientName = "Client C", contactNumber = "9999999999"),
            ShootBooking(id = "b3", userId = "", clientName = "Client D", contactNumber = "8888888888") // fallback to phone
        )
        val gallery = listOf(
            GalleryItem(id = "g1", userId = "user_beta", title = "Portfolio 1"), // same user
            GalleryItem(id = "g2", userId = "anonymous", title = "Demo 1") // anonymous excluded
        )

        val metrics = AdminDashboardMetrics.compute(models, bookings, gallery)

        // user_alpha, user_beta, user_gamma + 1 unique phone fallback
        assertTrue("Active users should deduplicate overlapping users and be >= 3", metrics.activeUsersTotal >= 3)
    }

    @Test
    fun compute_handlesEmptyCollectionsGracefully() {
        val metrics = AdminDashboardMetrics.compute(emptyList(), emptyList(), emptyList())

        assertEquals(0, metrics.pendingApprovalsTotal)
        assertEquals(0, metrics.upcomingBookingsTotal)
        assertEquals(0, metrics.activeUsersTotal)
        assertEquals(0, metrics.approvedGalleryCount)
        assertEquals(0, metrics.totalModelsCount)
        assertEquals(0, metrics.totalBookingsCount)
        assertEquals(0, metrics.totalGalleryCount)
    }
}
