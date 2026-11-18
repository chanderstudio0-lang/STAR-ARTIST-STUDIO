package com.example

import android.app.Application
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.SuccessResult
import com.example.data.model.AppLanguage
import com.example.data.model.GalleryItem
import com.example.ui.components.resolvePortfolioMediaModel
import com.example.ui.screens.PortfolioScreen
import com.example.ui.theme.StarArtistTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class PortfolioGalleryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUpCoil() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fakeLoader = ImageLoader.Builder(context)
            .components {
                add(coil.intercept.Interceptor { chain ->
                    SuccessResult(
                        drawable = ColorDrawable(Color.DKGRAY),
                        request = chain.request,
                        dataSource = DataSource.MEMORY_CACHE
                    )
                })
            }
            .build()
        Coil.setImageLoader(fakeLoader)
    }

    @After
    fun tearDownCoil() {
        Coil.reset()
    }

    @Test
    fun resolvePortfolioMediaModel_mapsBundledPhotographyDrawables() {
        assertEquals(
            R.drawable.img_sample_fashion_editorial_1794620320125,
            resolvePortfolioMediaModel("drawable://fashion_editorial")
        )
        assertEquals(
            R.drawable.img_sample_bridal_heritage_1794620318592,
            resolvePortfolioMediaModel("drawable://bridal_heritage")
        )
        assertEquals(
            R.drawable.img_sample_commercial_luxury_1794620321554,
            resolvePortfolioMediaModel("drawable://commercial_luxury")
        )
        assertEquals(
            R.drawable.img_sample_dramatic_portrait_1794620320124,
            resolvePortfolioMediaModel("drawable://dramatic_portrait")
        )
        assertEquals(
            "https://images.unsplash.com/photo-1.jpg",
            resolvePortfolioMediaModel("https://images.unsplash.com/photo-1.jpg")
        )
    }

    @Test
    fun portfolioScreen_displaysPhotographySamplesAndMergesFirestoreWorks() {
        val customFirestorePhoto = GalleryItem(
            id = "firestore_custom_1",
            userId = "user_99",
            title = "Sirsa Dusk Fashion Editorial",
            category = "Fashion",
            mediaType = "photo",
            mediaUrl = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=800",
            thumbnailUrl = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=400",
            isDemo = false,
            status = "approved"
        )

        composeTestRule.setContent {
            StarArtistTheme {
                PortfolioScreen(
                    language = AppLanguage.ENGLISH,
                    portfolioItems = listOf(customFirestorePhoto)
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("portfolio_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("portfolio_grid").assertIsDisplayed()
        composeTestRule.onNodeWithTag("portfolio_works_count").assertTextContains("9 Samples")
        composeTestRule.onNodeWithTag("portfolio_card_firestore_custom_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("portfolio_card_portfolio_fashion_editorial").assertIsDisplayed()
        composeTestRule.onNodeWithTag("portfolio_card_portfolio_bridal_royal").assertIsDisplayed()
    }

    @Test
    fun portfolioScreen_filtersByPhotosCategorySearchAndResetsEmptyState() {
        composeTestRule.setContent {
            StarArtistTheme {
                PortfolioScreen(
                    language = AppLanguage.ENGLISH,
                    portfolioItems = emptyList()
                )
            }
        }
        composeTestRule.waitForIdle()

        // Filter to Photos only (6 curated photography samples)
        composeTestRule.onNodeWithTag("portfolio_filter_photos").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("portfolio_works_count").assertTextContains("6 Samples")

        // Filter by Portrait category
        composeTestRule.onNodeWithTag("portfolio_chip_portrait").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("portfolio_works_count").assertTextContains("1 Samples")
        composeTestRule.onNodeWithTag("portfolio_card_portfolio_portrait_rimlight").assertIsDisplayed()

        // Search mismatch triggers empty state with Reset Filters action
        composeTestRule.onNodeWithTag("portfolio_search_input").performTextInput("zzzz_nomatch")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("portfolio_empty_state").assertIsDisplayed()

        composeTestRule.onNodeWithTag("portfolio_reset_filters_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("portfolio_works_count").assertTextContains("8 Samples")
    }

    @Test
    fun portfolioScreen_tapSampleCard_opensFullScreenViewerWithZoomNavigationAndClose() {
        var bookedFromFullscreen = false

        composeTestRule.setContent {
            StarArtistTheme {
                PortfolioScreen(
                    language = AppLanguage.ENGLISH,
                    portfolioItems = emptyList(),
                    onNavigateToBooking = { bookedFromFullscreen = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Tap the second photography sample (Bridal Heritage)
        composeTestRule.onNodeWithTag("portfolio_grid")
            .performScrollToNode(hasTestTag("portfolio_card_portfolio_bridal_royal"))
        composeTestRule.onNodeWithTag("portfolio_card_portfolio_bridal_royal").performClick()
        composeTestRule.waitForIdle()

        // Verify full-screen viewer opens at item 2 / 8
        composeTestRule.onNodeWithTag("gallery_fullscreen_carousel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_fullscreen_image").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_counter").assertTextContains("2 / 8")
        composeTestRule.onNodeWithTag("carousel_item_title").assertTextContains("Royal Heritage Bridal Stills")

        // Toggle zoom in and back out cleanly
        composeTestRule.onNodeWithTag("carousel_zoom_toggle_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("carousel_zoom_status").assertTextContains("2.2x", substring = true)
        composeTestRule.onNodeWithTag("carousel_zoom_toggle_button").performClick()
        composeTestRule.waitForIdle()

        // Navigate to next image via chevron
        composeTestRule.onNodeWithTag("carousel_next_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("carousel_counter").assertTextContains("3 / 8")
        composeTestRule.onNodeWithTag("carousel_item_title").assertTextContains("Dramatic Rim-Light Character Portrait")

        // Close full-screen viewer back to grid
        composeTestRule.onNodeWithTag("carousel_close_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gallery_fullscreen_carousel").assertDoesNotExist()
        composeTestRule.onNodeWithTag("portfolio_grid").assertIsDisplayed()

        // Re-open first card and trigger Book This Style CTA
        composeTestRule.onNodeWithTag("portfolio_grid")
            .performScrollToNode(hasTestTag("portfolio_card_portfolio_fashion_editorial"))
        composeTestRule.onNodeWithTag("portfolio_card_portfolio_fashion_editorial").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("carousel_book_style_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(bookedFromFullscreen)
        composeTestRule.onNodeWithTag("gallery_fullscreen_carousel").assertDoesNotExist()
    }

    @Test
    fun portfolioScreen_rendersHindiAndStudioLightThemeCleanly() {
        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = true) {
                PortfolioScreen(
                    language = AppLanguage.HINDI,
                    portfolioItems = emptyList()
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("portfolio_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("स्टूडियो पोर्टफोलियो व गैलरी").assertIsDisplayed()
        composeTestRule.onNodeWithTag("portfolio_works_count").assertTextContains("8 सैंपल")

        composeTestRule.onNodeWithTag("portfolio_card_portfolio_fashion_editorial").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gallery_fullscreen_carousel").assertIsDisplayed()
        composeTestRule.onNodeWithText("यह स्टाइल बुक करें").assertIsDisplayed()
    }
}
