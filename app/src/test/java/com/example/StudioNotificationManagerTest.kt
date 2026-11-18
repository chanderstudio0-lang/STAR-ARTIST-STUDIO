package com.example

import com.example.data.model.ShootBooking
import com.example.notification.StudioNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioNotificationManagerTest {

    @Test
    fun notificationPayload_confirmedBooking_formatsCorrectly() {
        val (title, body) = StudioNotificationManager.formatNotificationPayload(
            clientName = "Anjali Sharma",
            serviceType = "Photography (High-Fashion Editorial)",
            preferredDate = "2026-11-20",
            newStatus = "Confirmed"
        )

        assertTrue("Title must indicate confirmed", title.contains("Confirmed"))
        assertTrue("Body must include client name", body.contains("Anjali Sharma"))
        assertTrue("Body must include service type", body.contains("Photography (High-Fashion Editorial)"))
        assertTrue("Body must include date", body.contains("2026-11-20"))
    }

    @Test
    fun notificationPayload_completedBooking_formatsCorrectly() {
        val (title, body) = StudioNotificationManager.formatNotificationPayload(
            clientName = "Rohit Verma",
            serviceType = "Video (4K Cinema Music Video Direction)",
            preferredDate = "2026-11-22",
            newStatus = "Completed"
        )

        assertTrue("Title must indicate completed", title.contains("Completed"))
        assertTrue("Body must include client name", body.contains("Rohit Verma"))
        assertTrue("Body must mention Star Artist Studio", body.contains("Star Artist Studio"))
    }

    @Test
    fun notificationPayload_fallbackClientName_handledSafely() {
        val (title, body) = StudioNotificationManager.formatNotificationPayload(
            clientName = "",
            serviceType = "Modeling Portfolio",
            preferredDate = "2026-11-25",
            newStatus = "Confirmed"
        )

        assertNotNull(title)
        assertNotNull(body)
        assertTrue(title.isNotBlank())
        assertTrue(body.isNotBlank())
    }

    @Test
    fun channelConstants_areConfiguredCorrectly() {
        assertEquals("star_artist_booking_updates", StudioNotificationManager.CHANNEL_ID_BOOKING_UPDATES)
        assertEquals("Booking Status Updates", StudioNotificationManager.CHANNEL_NAME_BOOKING_UPDATES)
        assertTrue(StudioNotificationManager.CHANNEL_DESC_BOOKING_UPDATES.isNotBlank())
    }

    @Test
    fun shootBooking_includesFcmTokenField() {
        val booking = ShootBooking(
            id = "bk_test_123",
            clientName = "Simran Kaur",
            serviceType = "Bridal",
            preferredDate = "2026-11-28",
            status = "Pending",
            fcmToken = "test_fcm_token_xyz_987"
        )

        assertEquals("test_fcm_token_xyz_987", booking.fcmToken)
    }

    @Test
    fun announcementChannelAndTopics_areConfiguredCorrectly() {
        assertEquals(
            "star_artist_studio_announcements",
            StudioNotificationManager.CHANNEL_ID_STUDIO_ANNOUNCEMENTS
        )
        assertEquals(
            "studio_announcements",
            StudioNotificationManager.TOPIC_STUDIO_ANNOUNCEMENTS
        )
        assertEquals(
            "user_appointments_artist_123",
            StudioNotificationManager.userAppointmentsTopic("artist 123")
        )
    }

    @Test
    fun parseFcmPayload_routesStudioAnnouncementByTypeOrTopic() {
        val byType = StudioNotificationManager.parseFcmPayload(
            data = mapOf(
                "type" to "studio_announcement",
                "announcementId" to "ann_42",
                "title" to "Casting Call This Sunday",
                "message" to "Open runway auditions at Star Artist Studio.",
                "category" to "Casting Call",
                "priority" to "urgent",
                "actionRoute" to "models"
            )
        )
        assertEquals(StudioNotificationManager.FcmPayloadType.STUDIO_ANNOUNCEMENT, byType.type)
        assertEquals("ann_42", byType.announcementId)
        assertEquals("Casting Call", byType.category)
        assertEquals("urgent", byType.priority)
        assertEquals("models", byType.actionRoute)
        assertTrue(byType.title.contains("🚨"))

        val byTopic = StudioNotificationManager.parseFcmPayload(
            data = mapOf("title" to "Studio Offer", "body" to "20% off pre-wedding shoots"),
            from = "/topics/studio_announcements"
        )
        assertEquals(StudioNotificationManager.FcmPayloadType.STUDIO_ANNOUNCEMENT, byTopic.type)
        assertTrue(byTopic.title.contains("📣"))
    }

    @Test
    fun parseFcmPayload_routesAppointmentStatusUpdateCorrectly() {
        val parsed = StudioNotificationManager.parseFcmPayload(
            data = mapOf(
                "type" to "appointment_status",
                "bookingId" to "bk_901",
                "status" to "Confirmed",
                "title" to "Booking Confirmed!",
                "body" to "Your 4K Cinema Video shoot is confirmed."
            )
        )
        assertEquals(StudioNotificationManager.FcmPayloadType.APPOINTMENT_STATUS, parsed.type)
        assertEquals("bk_901", parsed.bookingId)
        assertEquals("Confirmed", parsed.status)
        assertEquals("Booking Confirmed!", parsed.title)
        assertEquals("Your 4K Cinema Video shoot is confirmed.", parsed.message)
    }

    @Test
    fun resolveAnnouncementDestination_mapsKnownRoutes() {
        assertEquals(
            com.example.ui.viewmodel.StudioScreen.ShootBooking,
            com.example.ui.screens.resolveAnnouncementDestination("bookings")
        )
        assertEquals(
            com.example.ui.viewmodel.StudioScreen.ServiceCatalog,
            com.example.ui.screens.resolveAnnouncementDestination("catalog")
        )
        assertEquals(
            com.example.ui.viewmodel.StudioScreen.Portfolio,
            com.example.ui.screens.resolveAnnouncementDestination("portfolio")
        )
        assertEquals(
            com.example.ui.viewmodel.StudioScreen.ModelRegistration,
            com.example.ui.screens.resolveAnnouncementDestination("models")
        )
        assertEquals(
            com.example.ui.viewmodel.StudioScreen.Home,
            com.example.ui.screens.resolveAnnouncementDestination("unknown")
        )
    }

    @Test
    fun isExpectedUnavailableFcmError_detectsTransientAndEmulatorFailures() {
        assertTrue(
            StudioNotificationManager.isExpectedUnavailableFcmError(
                RuntimeException("FCM Registration failed!")
            )
        )
        assertTrue(
            StudioNotificationManager.isExpectedUnavailableFcmError(
                RuntimeException("java.io.IOException: SERVICE_NOT_AVAILABLE")
            )
        )
        assertTrue(
            StudioNotificationManager.isExpectedUnavailableFcmError(
                RuntimeException("FIS_AUTH_ERROR")
            )
        )
        assertFalse(
            StudioNotificationManager.isExpectedUnavailableFcmError(
                IllegalArgumentException("Invalid notification channel")
            )
        )
    }
}
