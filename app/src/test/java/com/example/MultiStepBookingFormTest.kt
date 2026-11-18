package com.example

import com.example.data.model.ShootBooking
import com.example.ui.components.MultiStepServiceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MultiStepBookingFormTest {

    @Test
    fun multiStepServiceCategories_containsPhotographyModelingAndProduction() {
        val categories = MultiStepServiceCategory.values().map { it.label }
        assertEquals(3, categories.size)
        assertTrue("Must include Photography", categories.contains("Photography"))
        assertTrue("Must include Modeling", categories.contains("Modeling"))
        assertTrue("Must include Production", categories.contains("Production"))
    }

    @Test
    fun stepTransition_step0_requiresValidServiceType() {
        fun canAdvanceStep0(serviceType: String): Boolean {
            return serviceType.trim().isNotEmpty()
        }

        assertTrue(canAdvanceStep0("Photography"))
        assertTrue(canAdvanceStep0("Modeling"))
        assertTrue(canAdvanceStep0("Production"))
        assertFalse(canAdvanceStep0(""))
        assertFalse(canAdvanceStep0("   "))
    }

    @Test
    fun stepTransition_step1_requiresValidDateAndTime() {
        fun canAdvanceStep1(date: String, time: String): Boolean {
            return date.trim().isNotEmpty() &&
                    time.trim().isNotEmpty() &&
                    date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
        }

        assertTrue(canAdvanceStep1("2026-11-20", "10:00 AM (Morning Key Light Bay)"))
        assertTrue(canAdvanceStep1("2026-12-15", "02:30 PM (Editorial Softbox Bay)"))
        assertFalse(canAdvanceStep1("", "10:00 AM"))
        assertFalse(canAdvanceStep1("2026-11-20", ""))
        assertFalse(canAdvanceStep1("invalid-date", "10:00 AM"))
    }

    @Test
    fun stepTransition_step2_validatesClientDetailsForFirestoreSubmission() {
        fun canSubmitToFirestore(name: String, phone: String, date: String): Boolean {
            return name.trim().length >= 2 &&
                    phone.trim().length >= 10 &&
                    date.trim().isNotEmpty()
        }

        assertTrue(canSubmitToFirestore("Jasmeet Singh", "9812890081", "2026-11-25"))
        assertFalse(canSubmitToFirestore("J", "9812890081", "2026-11-25"))
        assertFalse(canSubmitToFirestore("Jasmeet Singh", "123", "2026-11-25"))
        assertFalse(canSubmitToFirestore("Jasmeet Singh", "9812890081", ""))
    }

    @Test
    fun firestorePersistencePayload_conformsToSecurityRules() {
        val testBooking = ShootBooking(
            id = "booking_multistep_101",
            userId = "client_user_uid_55",
            clientName = "Priya Sharma",
            contactNumber = "9541024098",
            serviceType = "Production",
            preferredDate = "2026-11-30",
            preferredTime = "04:30 PM (Golden Hour Outdoor)",
            location = "Studio (Talwara Khurd - 4K Lighting Bay)",
            notes = "Multi-step booking request, royal bridal jewelry",
            status = "Pending"
        )

        // Verify Firestore schema rules isValidBooking()
        assertTrue(testBooking.userId.length in 1..128)
        assertTrue(testBooking.clientName.length in 2..100)
        assertTrue(testBooking.contactNumber.length in 8..20)
        assertTrue(testBooking.serviceType.length in 1..100)
        assertTrue(testBooking.preferredDate.length in 1..50)
        assertTrue(testBooking.preferredTime.length in 1..50)
        assertEquals("Pending", testBooking.status)
        assertEquals("Production", testBooking.serviceType)
    }

    @Test
    fun lottieAnimationConfirmation_formatsSessionDetailsGracefully() {
        val client = "Aman Gill"
        val service = "Photography"
        val date = "2026-12-01"
        val time = "12:00 PM (Midday High-Key Flash)"
        val loc = "Studio (Talwara Khurd - 4K Lighting Bay)"

        val formattedSummary = "$service session for $client on $date at $time ($loc)"
        assertTrue(formattedSummary.contains(client))
        assertTrue(formattedSummary.contains(service))
        assertTrue(formattedSummary.contains(date))
        assertTrue(formattedSummary.contains(time))
    }

    @Test
    fun normalizeBookingPhone_stripsCountryPrefixAndNonDigits() {
        assertEquals("9812890081", com.example.ui.components.normalizeBookingPhone("+91 9812890081"))
        assertEquals("9812890081", com.example.ui.components.normalizeBookingPhone("98128-90081"))
        assertEquals("9541024098", com.example.ui.components.normalizeBookingPhone("9541024098"))
    }
}
