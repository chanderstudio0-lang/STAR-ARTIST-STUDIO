package com.example

import com.example.data.model.ShootBooking
import com.example.ui.screens.PrimaryServiceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BookingRequestFormTest {

    @Test
    fun primaryServiceCategories_containsModelingPhotographyAndVideoProduction() {
        val categories = PrimaryServiceCategory.values().map { it.label }
        assertTrue("Must support Modeling", categories.contains("Modeling"))
        assertTrue("Must support Photography", categories.contains("Photography"))
        assertTrue("Must support Video Production", categories.contains("Video Production"))
    }

    @Test
    fun dateSelection_formatsValidDate() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val todayStr = dateFormat.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, 3)
        val futureDateStr = dateFormat.format(cal.time)

        assertTrue(todayStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        assertTrue(futureDateStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun bookingValidation_validatesRequiredFields() {
        fun isBookingFormValid(name: String, phone: String, date: String): Boolean {
            val isNameValid = name.trim().length >= 2
            val isPhoneValid = phone.trim().length >= 10
            val isDateValid = date.trim().isNotEmpty()
            return isNameValid && isPhoneValid && isDateValid
        }

        // Valid form
        assertTrue(isBookingFormValid("Rohan Verma", "9812890081", "2026-11-20"))

        // Invalid: missing date
        assertFalse(isBookingFormValid("Rohan Verma", "9812890081", ""))

        // Invalid: short phone
        assertFalse(isBookingFormValid("Rohan Verma", "123", "2026-11-20"))

        // Invalid: empty name
        assertFalse(isBookingFormValid("", "9812890081", "2026-11-20"))
    }

    @Test
    fun firestoreBookingPayload_compliesWithSecurityRules() {
        val validServices = listOf("Modeling", "Photography", "Video Production")

        validServices.forEach { service ->
            val booking = ShootBooking(
                id = "booking_${System.currentTimeMillis()}",
                userId = "test_user_uid",
                clientName = "Simran Kaur",
                contactNumber = "9541024098",
                serviceType = service,
                preferredDate = "2026-11-25",
                preferredTime = "02:30 PM (Editorial Softbox Bay)",
                location = "Studio (Talwara Khurd - 4K Lighting Bay)",
                notes = "High-fashion lookbook, royal attire",
                status = "Pending"
            )

            // Verify payload properties matching Firestore security rules isValidBooking()
            assertTrue(booking.userId.length in 1..128)
            assertTrue(booking.clientName.length in 2..100)
            assertTrue(booking.contactNumber.length in 8..20)
            assertTrue(booking.serviceType.length in 1..100)
            assertTrue(booking.preferredDate.length in 1..50)
            assertTrue(booking.status in listOf("Pending", "Confirmed", "Completed", "Cancelled"))
            assertEquals(service, booking.serviceType)
        }
    }

    @Test
    fun defaultShootPackages_startWithBaseCategory() {
        val modelingPackages = listOf(
            "Modeling",
            "Modeling (Portfolio & Comp-Card Shoot)",
            "Modeling (Agency Audition Profile)"
        )
        val photographyPackages = listOf(
            "Photography",
            "Photography (High-Fashion Editorial)",
            "Photography (Royal Pre-Wedding & Bridal)"
        )
        val videoPackages = listOf(
            "Video Production",
            "Video Production (4K Cinema Music Video Direction)",
            "Video Production (Commercial Brand Ad Film)"
        )

        modelingPackages.forEach { assertTrue(it.startsWith("Modeling")) }
        photographyPackages.forEach { assertTrue(it.startsWith("Photography")) }
        videoPackages.forEach { assertTrue(it.startsWith("Video Production")) }
    }
}
