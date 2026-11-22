package com.example

import com.example.data.model.ModelRegistration
import com.example.data.model.ShootBooking
import com.example.ui.components.AdminEmailPreviewBuilder
import com.example.ui.components.EmailPreviewType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminEmailPreviewBuilderTest {

    @Test
    fun bookingPreviewRendersLiveRecordAndEscapesMaliciousHtml() {
        val booking = ShootBooking(
            id = "bk_901",
            clientName = "Priya <script>alert(1)</script>",
            contactNumber = "+91 9812890081",
            serviceType = "Bridal Couture Shoot",
            preferredDate = "2026-12-20",
            preferredTime = "04:00 PM",
            location = "Sirsa Palace",
            notes = "Golden hour portraits & <b>cinematic</b> teaser",
            status = "Pending"
        )

        val preview = AdminEmailPreviewBuilder.buildBookingPreview(booking)

        assertEquals(EmailPreviewType.BOOKING, preview.type)
        assertFalse(preview.isSample)
        assertTrue(preview.subject.contains("Bridal Couture Shoot"))
        assertTrue(preview.subject.contains("2026-12-20"))
        assertEquals("chanderstudio0@gmail.com", preview.to)
        assertFalse(preview.htmlDocument.contains("<script>alert(1)</script>"))
        assertTrue(preview.htmlDocument.contains("&lt;script&gt;alert(1)&lt;/script&gt;"))
        assertTrue(preview.plainText.contains("Sirsa Palace"))
    }

    @Test
    fun modelApplicationAndCompanyRequestPreviewsFormatDetailsCleanly() {
        val model = ModelRegistration(
            id = "mdl_42",
            fullName = "Meera Gill",
            age = 21L,
            gender = "Female",
            city = "Ellenabad",
            phoneNumber = "+91 9812890081",
            skills = "Ramp Walk, Bridal Editorial",
            portfolioUrls = "https://gen-lang-client-0548683163.web.app/#gallery",
            consentGiven = true,
            status = "pending"
        )

        val modelPreview = AdminEmailPreviewBuilder.buildModelApplicationPreview(model)
        assertEquals(EmailPreviewType.MODEL_APPLICATION, modelPreview.type)
        assertTrue(modelPreview.isPositiveBadge)
        assertEquals("18+ VERIFIED", modelPreview.statusBadge)
        assertTrue(modelPreview.subject.contains("Meera Gill"))
        assertTrue(modelPreview.htmlDocument.contains("Ellenabad"))

        val companyPreview = AdminEmailPreviewBuilder.buildCompanyRequestPreview()
        assertEquals(EmailPreviewType.COMPANY_REQUEST, companyPreview.type)
        assertTrue(companyPreview.isSample)
        assertTrue(companyPreview.subject.contains("Royal Heritage Couture Events"))
        assertTrue(companyPreview.htmlDocument.contains("6 • Both (Male &amp; Female)"))
        assertTrue(companyPreview.plainText.contains("Grand Courtyard Banquet"))
    }
}
