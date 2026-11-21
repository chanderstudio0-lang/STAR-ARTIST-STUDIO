package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.model.AppLanguage
import com.example.data.model.GalleryItem
import com.example.ui.components.FullScreenGalleryCarousel
import com.example.ui.components.nextCarouselIndex
import com.example.ui.theme.StarArtistTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class GalleryLightboxSlideshowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleItems = listOf(
        GalleryItem(
            id = "slide_1",
            title = "Royal Gold Rim Editorial",
            category = "Editorial",
            mediaUrl = "drawable://fashion_editorial",
            mediaType = "photo"
        ),
        GalleryItem(
            id = "slide_2",
            title = "Heritage Bridal Majesty",
            category = "Bridal",
            mediaUrl = "drawable://bridal_heritage",
            mediaType = "photo"
        ),
        GalleryItem(
            id = "slide_3",
            title = "Noir Dramatic Portrait",
            category = "Portrait",
            mediaUrl = "drawable://dramatic_portrait",
            mediaType = "photo"
        )
    )

    @Test
    fun nextCarouselIndex_wrapsForwardAndBackwardCleanly() {
        assertEquals(1, nextCarouselIndex(0, 3, 1))
        assertEquals(0, nextCarouselIndex(2, 3, 1))
        assertEquals(2, nextCarouselIndex(0, 3, -1))
        assertEquals(1, nextCarouselIndex(2, 3, -1))
        assertEquals(0, nextCarouselIndex(0, 1, 1))
    }

    @Test
    fun lightboxCarousel_supportsManualWrapAroundArrowsAndAutoSlideshowToggle() {
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

        // Starts at slide 1 / 3 with both wrap-around arrows & slideshow toggle visible
        composeTestRule.onNodeWithTag("carousel_counter").assertTextContains("1 / 3")
        composeTestRule.onNodeWithTag("carousel_prev_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_next_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_slideshow_toggle_button").assertIsDisplayed()

        // Manual Next cycles forward to 2 / 3
        composeTestRule.onNodeWithTag("carousel_next_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("carousel_counter").assertTextContains("2 / 3")

        // Manual Prev twice wraps from 1 / 3 back to 3 / 3
        composeTestRule.onNodeWithTag("carousel_prev_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("carousel_prev_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("carousel_counter").assertTextContains("3 / 3")

        // Toggling automatic slideshow displays live AUTO badge and pauses cleanly
        composeTestRule.onNodeWithTag("carousel_slideshow_toggle_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("carousel_slideshow_badge").assertIsDisplayed()

        composeTestRule.onNodeWithTag("carousel_slideshow_toggle_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("carousel_slideshow_badge").assertDoesNotExist()
    }
}
