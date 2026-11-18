package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.data.model.AppLanguage
import com.example.ui.components.ServicesOfferedGridSection
import com.example.ui.theme.StarArtistTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ServicesOfferedGridSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun servicesOfferedGridSection_filtersByCategoriesAndFavoritesAndSorts() {
        val toggledIds = mutableListOf<String>()

        composeTestRule.setContent {
            var favorites by remember { mutableStateOf(setOf("pkg_photo_fashion")) }
            StarArtistTheme(isStudioLight = false) {
                ServicesOfferedGridSection(
                    language = AppLanguage.ENGLISH,
                    favoritePackageIds = favorites,
                    onBookPackage = {},
                    onToggleFavorite = { pkg ->
                        toggledIds.add(pkg.id)
                        favorites = if (pkg.id in favorites) favorites - pkg.id else favorites + pkg.id
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify Photography, Modeling, Video Production chips are labeled properly
        composeTestRule.onNodeWithTag("service_filter_photography")
            .performScrollTo()
            .assertTextContains("Photography")
        composeTestRule.onNodeWithTag("service_filter_modeling")
            .performScrollTo()
            .assertTextContains("Modeling")
        composeTestRule.onNodeWithTag("service_filter_production")
            .performScrollTo()
            .assertTextContains("Video Production")

        // Filter by Video Production
        composeTestRule.onNodeWithTag("service_filter_production").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("services_grid_results_count").assertTextContains("2 Packages")
        composeTestRule.onNodeWithTag("services_lazy_vertical_grid")
            .performScrollToNode(hasTestTag("package_card_pkg_video_music"))
        composeTestRule.onNodeWithTag("package_card_pkg_video_music").assertIsDisplayed()

        // Toggle Favorites filter while Video Production is active (none saved yet -> empty state -> reset)
        composeTestRule.onNodeWithTag("service_filter_favorites")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("services_grid_empty_state").assertIsDisplayed()

        composeTestRule.onNodeWithTag("services_grid_reset_filter_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("services_grid_results_count").assertTextContains("8 Packages")

        // Bookmark Modeling package and filter to Favorites across All categories
        composeTestRule.onNodeWithTag("services_lazy_vertical_grid")
            .performScrollToNode(hasTestTag("package_card_pkg_model_portfolio"))
        composeTestRule.onNodeWithTag("service_favorite_btn_pkg_model_portfolio").performClick()
        composeTestRule.waitForIdle()
        assertTrue("pkg_model_portfolio" in toggledIds)

        composeTestRule.onNodeWithTag("service_filter_favorites")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("services_grid_results_count").assertTextContains("2 Packages")
    }
}
