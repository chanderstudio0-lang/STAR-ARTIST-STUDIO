package com.example

import com.example.data.model.AppLanguage
import com.example.util.StudioConstants
import com.example.util.StudioContactDetails
import com.example.util.StudioStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageToggleTest {

    @Test
    fun studioStrings_englishLocalization_returnsCorrectLabels() {
        val stringsEn = StudioStrings(AppLanguage.ENGLISH)
        assertEquals("Home", stringsEn.navHome)
        assertEquals("Model Reg", stringsEn.navModelReg)
        assertEquals("Shoot Booking", stringsEn.navBooking)
        assertEquals("Gallery", stringsEn.navGallery)
        assertEquals("AI Studio", stringsEn.navAiStudio)
        assertEquals("Admin", stringsEn.navAdmin)
        assertEquals("Contact", stringsEn.navContact)
        assertEquals("Model Registration", stringsEn.modelRegTitle)
        assertEquals("Book a Production Shoot", stringsEn.bookingTitle)
    }

    @Test
    fun studioStrings_hindiLocalization_returnsCorrectLabels() {
        val stringsHi = StudioStrings(AppLanguage.HINDI)
        assertEquals("होम", stringsHi.navHome)
        assertEquals("मॉडल पंजीकरण", stringsHi.navModelReg)
        assertEquals("शूट बुकिंग", stringsHi.navBooking)
        assertEquals("गैलरी", stringsHi.navGallery)
        assertEquals("एआई स्टूडियो", stringsHi.navAiStudio)
        assertEquals("एडमिन पैनल", stringsHi.navAdmin)
        assertEquals("संपर्क करें", stringsHi.navContact)
        assertEquals("मॉडल पंजीकरण फॉर्म", stringsHi.modelRegTitle)
        assertEquals("शूट अपॉइंटमेंट बुक करें", stringsHi.bookingTitle)
    }

    @Test
    fun studioStrings_dynamicToggle_altersAllScreenStrings() {
        val en = StudioStrings(AppLanguage.ENGLISH)
        val hi = StudioStrings(AppLanguage.HINDI)

        assertNotEquals(en.navHome, hi.navHome)
        assertNotEquals(en.modelRegTitle, hi.modelRegTitle)
        assertNotEquals(en.bookingTitle, hi.bookingTitle)
        assertNotEquals(en.galleryTitle, hi.galleryTitle)
        assertNotEquals(en.aiStudioTitle, hi.aiStudioTitle)
    }

    @Test
    fun studioContactConstants_preservedAcrossLanguages() {
        // Invariant Studio details
        assertEquals("STAR ARTIST STUDIO", StudioConstants.STUDIO_NAME)
        assertEquals("9812890081", StudioContactDetails.MOBILE_NUMBER)
        assertEquals("9541024098", StudioContactDetails.WHATSAPP_NUMBER)
        assertEquals("chanderstudio0@gmail.com", StudioContactDetails.ADMIN_EMAIL)
        assertTrue(StudioContactDetails.ADDRESS.contains("Talwara Khurd"))
        assertTrue(StudioContactDetails.ADDRESS.contains("Sirsa"))
        assertTrue(StudioContactDetails.ADDRESS.contains("Haryana"))
    }
}
