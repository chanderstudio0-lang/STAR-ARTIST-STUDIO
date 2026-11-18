package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.repository.StudioRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagingRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAppointmentNotification_andMarkRead_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)

        val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createAppointmentNotification(
                targetUserId = uid,
                bookingId = "booking_fcm_101",
                title = "Shoot Booking Confirmed!",
                message = "Your High-Fashion Photography session is confirmed for tomorrow.",
                status = "Confirmed",
                serviceType = "High-Fashion Photography",
                targetFcmToken = "fcm_device_token_alice_01"
            )
        }
        assertTrue("Expected notification creation to succeed: ${createRes.exceptionOrNull()}", createRes.isSuccess)
        val notifId = createRes.getOrNull()!!

        val notifications = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeUserNotifications(uid).first { list -> list.any { it.id == notifId } }
        }
        val created = notifications.first { it.id == notifId }
        assertEquals("booking_fcm_101", created.bookingId)
        assertEquals("Confirmed", created.status)
        assertEquals("fcm_device_token_alice_01", created.targetFcmToken)
        assertFalse(created.isRead)

        val markRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.markNotificationRead(uid, notifId)
        }
        assertTrue("Expected markNotificationRead to succeed: ${markRes.exceptionOrNull()}", markRes.isSuccess)

        val afterRead = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeUserNotifications(uid).first { list -> list.any { it.id == notifId && it.isRead } }
        }
        assertTrue(afterRead.first { it.id == notifId }.isRead)
    }

    @Test
    fun markAllNotificationsRead_batchUpdate_marksUnreadItemsRead() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)

        val id1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createAppointmentNotification(
                targetUserId = uid,
                bookingId = "booking_batch_1",
                title = "Booking Confirmed",
                message = "Lighting bay reserved for your shoot.",
                status = "Confirmed"
            )
        }.getOrThrow()

        val id2 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createAppointmentNotification(
                targetUserId = uid,
                bookingId = "booking_batch_2",
                title = "Production Completed",
                message = "Edited 4K reels are ready for download.",
                status = "Completed"
            )
        }.getOrThrow()

        val batchRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.markAllNotificationsRead(uid, listOf(id1, id2))
        }
        assertTrue("Expected batch mark read to succeed: ${batchRes.exceptionOrNull()}", batchRes.isSuccess)

        val updated = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeUserNotifications(uid).first { list ->
                val n1 = list.firstOrNull { it.id == id1 }
                val n2 = list.firstOrNull { it.id == id2 }
                n1?.isRead == true && n2?.isRead == true
            }
        }
        assertTrue(updated.first { it.id == id1 }.isRead)
        assertTrue(updated.first { it.id == id2 }.isRead)
    }

    @Test
    fun updateNotificationPreferences_authenticatedOwner_persistsOptInsAndToken() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateUserProfile(
                userId = uid,
                email = ALICE_EMAIL,
                displayName = "Alice Notification Tester"
            )
        }

        val prefRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateNotificationPreferences(
                userId = uid,
                announcementsOptIn = false,
                appointmentPushOptIn = true,
                fcmToken = "fcm_refreshed_token_999"
            )
        }
        assertTrue("Expected updateNotificationPreferences to succeed: ${prefRes.exceptionOrNull()}", prefRes.isSuccess)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it?.fcmToken == "fcm_refreshed_token_999" }
        }
        assertNotNull(profile)
        assertFalse(profile!!.announcementsOptIn)
        assertTrue(profile.appointmentPushOptIn)
        assertEquals("fcm_refreshed_token_999", profile.fcmToken)
    }

    @Test
    fun notificationsAndAnnouncements_nonAdminAndCrossUser_deniedBySecurityRules() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        signInTestUser(BOB_EMAIL)
        val repository = StudioRepository(firestore)

        // Bob cannot create an appointment notification in Alice's inbox
        val crossCreateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createAppointmentNotification(
                targetUserId = aliceUid,
                bookingId = "forged_booking",
                title = "Forged Alert",
                message = "Should be denied by Firestore rules.",
                status = "Confirmed"
            )
        }
        assertTrue(crossCreateRes.isFailure)
        val crossEx = crossCreateRes.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(crossEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, crossEx?.code)

        // Regular user Bob cannot publish a Studio Announcement
        val publishRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.publishStudioAnnouncement(
                title = "Unauthorized Studio Broadcast",
                message = "Only the verified studio admin may broadcast announcements.",
                category = "Studio Update",
                priority = "normal"
            )
        }
        assertTrue(publishRes.isFailure)
        val pubEx = publishRes.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(pubEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, pubEx?.code)

        // Unauthenticated client cannot observe notifications or announcements
        auth.signOut()
        var unauthError: Exception? = null
        try {
            withTimeout(3000L) {
                repository.observeStudioAnnouncements().first()
            }
        } catch (e: Exception) {
            unauthError = e
        }
        assertNotNull(unauthError)
        val unauthEx = unauthError?.unwrapFirestoreException()
        assertNotNull(unauthEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, unauthEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "fcm_alice@starartist.com"
        const val BOB_EMAIL = "fcm_bob@starartist.com"
        const val DEFAULT_TIMEOUT_MS = 10000L
    }
}
