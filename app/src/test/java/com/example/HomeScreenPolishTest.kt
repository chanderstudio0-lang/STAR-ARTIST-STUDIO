package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.AppLanguage
import com.example.ui.screens.HomeConnectSection
import com.example.ui.theme.StarArtistTheme
import com.example.util.StudioConstants
import com.example.util.StudioContactDetails
import com.example.util.StudioStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class HomeScreenPolishTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun studioOfficialAddress_exactMatch() {
        val expectedAddress = "Star Artist Studio, Talwara Khurd, Tehsil Ellenabad, District Sirsa, Haryana, India - 125102."
        assertEquals(expectedAddress, StudioContactDetails.ADDRESS)
        assertEquals(expectedAddress, StudioConstants.ADDRESS)
    }

    @Test
    fun studioPhoneAndWhatsApp_exactMatch() {
        assertEquals("9812890081", StudioContactDetails.MOBILE_NUMBER)
        assertEquals("9541024098", StudioContactDetails.WHATSAPP_NUMBER)
        assertEquals("tel:+919812890081", StudioContactDetails.CALL_URI)
        assertEquals("https://wa.me/919541020948", StudioContactDetails.WHATSAPP_URL)
        assertEquals("https://www.instagram.com/starartiststudio", StudioContactDetails.INSTAGRAM_URL)
        assertEquals("https://www.facebook.com/starartiststudio", StudioContactDetails.FACEBOOK_URL)
        assertEquals("https://www.linkedin.com/company/star-artist-studio", StudioContactDetails.LINKEDIN_URL)
    }

    @Test
    fun studioStrings_noUnverifiedPromotionsOrClaims() {
        val stringsEn = StudioStrings(AppLanguage.ENGLISH)
        val stringsHi = StudioStrings(AppLanguage.HINDI)

        // Ensure no unverified percentage discounts
        assertFalse(stringsEn.heroSubtext.contains("%"))
        assertFalse(stringsHi.heroSubtext.contains("%"))

        // Ensure no unsupported superlative claims
        assertFalse(stringsEn.heroSubtext.contains("most modern"))
        assertFalse(stringsHi.heroSubtext.contains("सबसे आधुनिक"))
        assertFalse(stringsEn.studioIntroBody.contains("premier destination"))

        // Ensure factual address details are present
        assertTrue(stringsEn.studioIntroBody.contains("125102"))
        assertTrue(stringsHi.studioIntroBody.contains("125102"))
    }

    @Test
    fun homeConnectSection_displaysAndOpensInstagramFacebookAndLinkedinProfiles() {
        val openedUrls = mutableListOf<String>()

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                HomeConnectSection(
                    language = AppLanguage.ENGLISH,
                    onOpenUrl = { openedUrls.add(it) }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_connect_section").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connect").assertIsDisplayed()

        composeTestRule.onNodeWithTag("connect_instagram_btn").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("connect_facebook_btn").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("connect_linkedin_btn").assertIsDisplayed().performClick()

        assertEquals(
            listOf(
                "https://www.instagram.com/starartiststudio",
                "https://www.facebook.com/starartiststudio",
                "https://www.linkedin.com/company/star-artist-studio"
            ),
            openedUrls
        )
    }
}
