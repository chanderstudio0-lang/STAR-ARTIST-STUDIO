package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.data.model.AppLanguage
import com.example.data.model.GalleryItem
import com.example.ui.components.FullScreenGalleryCarousel
import com.example.ui.theme.StarArtistTheme
import com.example.util.ImageDownloadHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class GalleryImageDownloadTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun buildDownloadFileName_sanitizesTitleAndInfersExtension() {
        assertEquals(
            "star_artist_royal_heritage_bridal_stills.jpg",
            ImageDownloadHelper.buildDownloadFileName("Royal Heritage Bridal Stills", "image/jpeg")
        )
        assertEquals(
            "star_artist_runway_look_2026.png",
            ImageDownloadHelper.buildDownloadFileName("  Runway Look #2026! ", "image/png")
        )
        assertEquals(
            "star_artist_portfolio_shot.webp",
            ImageDownloadHelper.buildDownloadFileName("   ", "image/webp")
        )
    }

    @Test
    fun decodeDataUrlImageBytes_decodesValidBase64AndRejectsInvalid() {
        // Minimal 1x1 transparent PNG in base64
        val tinyPng = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
        val decoded = ImageDownloadHelper.decodeDataUrlImageBytes(tinyPng)
        assertNotNull(decoded)
        assertEquals("image/png", decoded!!.second)
        assertTrue(decoded.first.isNotEmpty())

        assertNull(ImageDownloadHelper.decodeDataUrlImageBytes("https://example.com/photo.jpg"))
        assertNull(ImageDownloadHelper.decodeDataUrlImageBytes("data:image/png;base64,"))
    }

    @Test
    fun fullScreenGalleryCarousel_displaysDownloadAndShareButtons() {
        val sampleItems = listOf(
            GalleryItem(
                id = "item_1",
                userId = "studio",
                title = "Vogue Runway Couture Editorial",
                category = "Fashion",
                mediaType = "photo",
                mediaUrl = "drawable://fashion_editorial",
                thumbnailUrl = "drawable://fashion_editorial",
                isDemo = true,
                status = "approved"
            )
        )

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                FullScreenGalleryCarousel(
                    items = sampleItems,
                    initialIndex = 0,
                    language = AppLanguage.ENGLISH,
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("gallery_fullscreen_carousel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_download_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_share_button").assertIsDisplayed()
    }
}
