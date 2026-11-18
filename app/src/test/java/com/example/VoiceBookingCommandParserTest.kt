package com.example

import com.example.util.VoiceBookingCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class VoiceBookingCommandParserTest {

    private fun fixedMondayCalendar(): Calendar {
        return Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 16, 9, 0, 0) // Monday 2026-11-16
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun parseLocally_photographyEditorialTomorrowMorning_extractsAllFields() {
        val parsed = VoiceBookingCommandParser.parseLocally(
            transcript = "Book high fashion editorial photography shoot tomorrow at 10 AM in studio",
            referenceCalendar = fixedMondayCalendar()
        )

        assertTrue(parsed.isBookingIntent)
        assertFalse(parsed.isConfirmCommand)
        assertEquals("Photography", parsed.intentCategory)
        assertEquals("Photography (High-Fashion Editorial)", parsed.serviceType)
        assertEquals("2026-11-17", parsed.preferredDate)
        assertEquals("10:00 AM (Morning Key Light Bay)", parsed.preferredTime)
        assertEquals(VoiceBookingCommandParser.LOCATION_STUDIO, parsed.location)
    }

    @Test
    fun parseLocally_modelingCompCardSaturdayGoldenHourOutdoor_extractsAllFields() {
        val parsed = VoiceBookingCommandParser.parseLocally(
            transcript = "Schedule a modeling comp-card portfolio shoot this Saturday at 4:30 PM outdoor",
            referenceCalendar = fixedMondayCalendar()
        )

        assertEquals("Modeling", parsed.intentCategory)
        assertEquals("Modeling (Portfolio & Comp-Card Shoot)", parsed.serviceType)
        assertEquals("2026-11-21", parsed.preferredDate) // Saturday
        assertEquals("04:30 PM (Golden Hour Outdoor)", parsed.preferredTime)
        assertEquals(VoiceBookingCommandParser.LOCATION_OUTDOOR, parsed.location)
    }

    @Test
    fun parseLocally_bridalPhotographyExplicitDateAndPhoneAndName_extractsDetails() {
        val parsed = VoiceBookingCommandParser.parseLocally(
            transcript = "My name is Simran Kaur, book royal pre-wedding bridal photography on 25 December at 12 noon, phone 9812890081",
            referenceCalendar = fixedMondayCalendar()
        )

        assertEquals("Photography", parsed.intentCategory)
        assertEquals("Photography (Royal Pre-Wedding & Bridal)", parsed.serviceType)
        assertEquals("2026-12-25", parsed.preferredDate)
        assertEquals("12:00 PM (Midday High-Key Flash)", parsed.preferredTime)
        assertEquals("Simran Kaur", parsed.extractedClientName)
        assertEquals("9812890081", parsed.extractedPhoneNumber)
    }

    @Test
    fun parseLocally_hindiHinglishCommand_parsesModelingAndRelativeDate() {
        val parsed = VoiceBookingCommandParser.parseLocally(
            transcript = "कल शाम मॉडलिंग ऑडिशन पोर्टफोलियो बुक करें",
            referenceCalendar = fixedMondayCalendar()
        )

        assertEquals("Modeling", parsed.intentCategory)
        assertEquals("Modeling (Agency Audition Profile)", parsed.serviceType)
        assertEquals("2026-11-17", parsed.preferredDate)
        assertEquals("04:30 PM (Golden Hour Outdoor)", parsed.preferredTime)
    }

    @Test
    fun parseLocally_numericDateAnd24HourTime_parsesCorrectly() {
        val parsed = VoiceBookingCommandParser.parseLocally(
            transcript = "Book portrait photography on 05/12/2026 at 14:30 at client venue",
            referenceCalendar = fixedMondayCalendar()
        )

        assertEquals("Photography", parsed.intentCategory)
        assertEquals("Photography (Portrait & Glamour Stills)", parsed.serviceType)
        assertEquals("2026-12-05", parsed.preferredDate)
        assertEquals("02:30 PM (Editorial Softbox Bay)", parsed.preferredTime)
        assertEquals(VoiceBookingCommandParser.LOCATION_CLIENT, parsed.location)
    }

    @Test
    fun parseLocally_confirmUtterances_detectedAsConfirmCommands() {
        assertTrue(VoiceBookingCommandParser.parseLocally("Confirm booking", fixedMondayCalendar()).isConfirmCommand)
        assertTrue(VoiceBookingCommandParser.parseLocally("yes confirm", fixedMondayCalendar()).isConfirmCommand)
        assertTrue(VoiceBookingCommandParser.parseLocally("हाँ बुक करें", fixedMondayCalendar()).isConfirmCommand)
    }
}
