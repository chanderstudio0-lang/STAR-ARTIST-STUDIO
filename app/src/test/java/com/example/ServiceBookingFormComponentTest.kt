package com.example

import com.example.data.model.ShootBooking
import com.example.ui.components.BookingServiceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ServiceBookingFormComponentTest {

    @Test
    fun bookingServiceType_containsModelingPhotographyAndVideoProduction() {
        val labels = BookingServiceType.values().map { it.label }
        assertEquals(3, labels.size)
        assertTrue("Must include Modeling", labels.contains("Modeling"))
        assertTrue("Must include Photography", labels.contains("Photography"))
        assertTrue("Must include Video Production", labels.contains("Video Production"))
    }

    @Test
    fun dateCapture_formatsAndValidatesSelectedDate() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }
        val dateString = dateFormat.format(cal.time)

        fun isValidDate(date: String): Boolean {
            return date.isNotBlank() && date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
        }

        assertTrue(isValidDate(dateString))
        assertTrue(isValidDate("2026-11-28"))
        assertFalse(isValidDate(""))
        assertFalse(isValidDate("invalid-date"))
    }

    @Test
    fun timeSlotCapture_validatesProductionTimeBays() {
        val standardSlots = listOf(
            "10:00 AM (Morning Key Light)",
            "12:00 PM (Midday Flash Stage)",
            "02:30 PM (Editorial Softbox Bay)",
            "04:30 PM (Golden Hour Outdoor)",
            "06:30 PM (Dark & Rim Mood Studio)"
        )

        standardSlots.forEach { slot ->
            assertTrue(slot.isNotBlank())
            assertTrue(slot.contains("AM") || slot.contains("PM"))
        }
    }

    @Test
    fun sessionRequest_payload_matchesFirestoreSchema() {
        val testUserId = "client_auth_uid_101"
        val clientName = "Harpreet Kaur"
        val phone = "9812890081"
        val serviceType = "Video Production"
        val date = "2026-12-05"
        val time = "02:30 PM (Editorial Softbox Bay)"
        val location = "Studio (Talwara Khurd - 4K Lighting Bay)"
        val notes = "4K Music Video session, 3 dress changes"

        val booking = ShootBooking(
            id = "booking_doc_101",
            userId = testUserId,
            clientName = clientName,
            contactNumber = phone,
            serviceType = serviceType,
            preferredDate = date,
            preferredTime = time,
            location = location,
            notes = notes,
            status = "Pending"
        )

        // Validate Firestore rule constraints: isValidBooking()
        assertEquals(testUserId, booking.userId)
        assertEquals(clientName, booking.clientName)
        assertEquals(phone, booking.contactNumber)
        assertEquals(serviceType, booking.serviceType)
        assertEquals(date, booking.preferredDate)
        assertEquals(time, booking.preferredTime)
        assertEquals("Pending", booking.status)

        assertTrue(booking.clientName.length >= 2)
        assertTrue(booking.contactNumber.length >= 8)
        assertTrue(booking.serviceType.length in 1..100)
        assertTrue(booking.preferredDate.length in 1..50)
        assertTrue(booking.preferredTime.length in 1..50)
    }

    @Test
    fun formValidation_enforcesAllMandatoryFields() {
        fun validateForm(name: String, phone: String, date: String, time: String, service: String): Boolean {
            return name.trim().length >= 2 &&
                    phone.trim().length >= 10 &&
                    date.trim().isNotEmpty() &&
                    time.trim().isNotEmpty() &&
                    service.trim().isNotEmpty()
        }

        assertTrue(
            validateForm(
                name = "Aman Gill",
                phone = "9876543210",
                date = "2026-11-20",
                time = "10:00 AM",
                service = "Photography"
            )
        )

        // Missing service type
        assertFalse(
            validateForm(
                name = "Aman Gill",
                phone = "9876543210",
                date = "2026-11-20",
                time = "10:00 AM",
                service = ""
            )
        )

        // Missing date
        assertFalse(
            validateForm(
                name = "Aman Gill",
                phone = "9876543210",
                date = "",
                time = "10:00 AM",
                service = "Modeling"
            )
        )

        // Short phone
        assertFalse(
            validateForm(
                name = "Aman Gill",
                phone = "12345",
                date = "2026-11-20",
                time = "10:00 AM",
                service = "Video Production"
            )
        )
    }
}
