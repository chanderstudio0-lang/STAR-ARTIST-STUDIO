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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.model.AppLanguage
import com.example.data.model.CatalogBudgetFilter
import com.example.data.model.CatalogSortOrder
import com.example.data.model.ServicePackageCategory
import com.example.data.model.ServiceReview
import com.example.data.model.StudioServicesData
import com.example.ui.screens.ServiceCatalogScreen
import com.example.ui.theme.StarArtistTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ServiceCatalogScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun allPackages_haveOrderedPricingTiersAndValidBookingLabels() {
        val packages = StudioServicesData.packages
        assertTrue(packages.isNotEmpty())

        packages.forEach { pkg ->
            assertEquals(
                "Each offering must define 3 pricing tiers for ${pkg.id}",
                3,
                pkg.pricingTiers.size
            )
            val prices = pkg.pricingTiers.map { it.priceInr }
            assertEquals(
                "First tier should align with startingPriceInr for ${pkg.id}",
                pkg.startingPriceInr,
                prices.first()
            )
            assertEquals(
                "Pricing tiers must be sorted ascending for ${pkg.id}",
                prices.sorted(),
                prices
            )
            pkg.pricingTiers.forEach { tier ->
                assertTrue(tier.tierNameEn.isNotBlank())
                assertTrue(tier.tierNameHi.isNotBlank())
                assertTrue(tier.priceFormattedEn.contains("₹"))
                assertTrue(tier.priceFormattedHi.contains("₹"))
                assertTrue(tier.deliverablesEn.size >= 3)
                assertEquals(tier.deliverablesEn.size, tier.deliverablesHi.size)
                val bookingLabel = pkg.bookingLabelForTier(tier)
                assertTrue(
                    "Booking label must stay within Firestore 100-char rule: $bookingLabel",
                    bookingLabel.length in 1..100
                )
            }
        }
    }

    @Test
    fun filterAndSortPackages_handlesCategoryQueryAndPriceSorting() {
        val modelingOnly = StudioServicesData.filterAndSortPackages(
            category = ServicePackageCategory.MODELING
        )
        assertTrue(modelingOnly.isNotEmpty())
        assertTrue(modelingOnly.all { it.category == ServicePackageCategory.MODELING })

        val searchBridal = StudioServicesData.filterAndSortPackages(
            query = "Bridal"
        )
        assertTrue(searchBridal.any { it.id == "pkg_photo_bridal" })

        val lowToHigh = StudioServicesData.filterAndSortPackages(
            sortOrder = CatalogSortOrder.PRICE_LOW_HIGH
        )
        assertEquals(
            lowToHigh.map { it.startingPriceInr }.sorted(),
            lowToHigh.map { it.startingPriceInr }
        )

        val highToLow = StudioServicesData.filterAndSortPackages(
            sortOrder = CatalogSortOrder.PRICE_HIGH_LOW
        )
        assertEquals(
            highToLow.map { it.startingPriceInr }.sortedDescending(),
            highToLow.map { it.startingPriceInr }
        )
    }

    @Test
    fun serviceCatalogScreen_displaysOfferingsAndSwitchesPricingTiers() {
        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                ServiceCatalogScreen(
                    language = AppLanguage.ENGLISH,
                    onBack = {},
                    onBookService = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("service_catalog_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("catalog_card_pkg_photo_fashion").assertIsDisplayed()

        // Default recommended tier for High-Fashion Editorial is Pro (₹21,999)
        composeTestRule.onNodeWithTag("catalog_selected_price_pkg_photo_fashion")
            .assertTextContains("₹21,999")

        // Switch to Flagship tier (₹34,999)
        composeTestRule.onNodeWithTag("catalog_tier_pkg_photo_fashion_flagship")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("catalog_selected_price_pkg_photo_fashion")
            .performScrollTo()
            .assertTextContains("₹34,999")
        composeTestRule.onNodeWithText("6 designer looks + on-set stylist & makeup bay")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun serviceCatalogScreen_categorySearchAndResetFlowWorks() {
        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = true) {
                ServiceCatalogScreen(
                    language = AppLanguage.ENGLISH,
                    onBack = {},
                    onBookService = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Filter by Production
        composeTestRule.onNodeWithTag("catalog_category_chip_production").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("catalog_results_count").assertTextContains("2 Offerings")
        composeTestRule.onNodeWithTag("catalog_card_pkg_video_music").assertIsDisplayed()

        // Search nonexistent term -> shows empty state and resets cleanly
        composeTestRule.onNodeWithTag("service_catalog_search_input")
            .performTextInput("zzznonexistentservice")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("service_catalog_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("catalog_reset_filters_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("catalog_results_count").assertTextContains("8 Offerings")
    }

    @Test
    fun serviceCatalogScreen_bookButtonForwardsServiceAndTier_andBackWorks() {
        var bookedService = ""
        var backPressed = false

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                ServiceCatalogScreen(
                    language = AppLanguage.ENGLISH,
                    onBack = { backPressed = true },
                    onBookService = { bookedService = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Select Essential tier on High-Fashion Editorial and tap Book
        composeTestRule.onNodeWithTag("catalog_tier_pkg_photo_fashion_essential")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("catalog_book_btn_pkg_photo_fashion")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("Photography (High-Fashion Editorial) - Essential", bookedService)

        // Scroll LazyColumn back up to header and tap back button
        composeTestRule.onNodeWithTag("service_catalog_list")
            .performScrollToNode(hasTestTag("service_catalog_header"))
        composeTestRule.onNodeWithTag("service_catalog_back_button")
            .performClick()
        composeTestRule.waitForIdle()
        assertTrue(backPressed)
    }

    @Test
    fun serviceCatalogScreen_supportsHindiLocalization() {
        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                ServiceCatalogScreen(
                    language = AppLanguage.HINDI,
                    onBack = {},
                    onBookService = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("स्टूडियो सेवा कैटलॉग").assertIsDisplayed()
        composeTestRule.onNodeWithText("हाई-फैशन एडिटोरियल").assertIsDisplayed()
    }

    @Test
    fun filterAndSortPackages_supportsMultiWordDeliverablesBudgetAndHindi() {
        // Multi-word video production query matches 4K cinema & commercial ad film
        val fourKVideo = StudioServicesData.filterAndSortPackages(query = "4K video")
        assertTrue(fourKVideo.any { it.id == "pkg_video_music" })
        assertTrue(fourKVideo.any { it.id == "pkg_video_brand_ad" })

        // Category + specialty photography query
        val bridalPhoto = StudioServicesData.filterAndSortPackages(query = "bridal photography")
        assertEquals(listOf("pkg_photo_bridal"), bridalPhoto.map { it.id })

        // Deliverable keyword & match snippet
        val dronePackages = StudioServicesData.filterAndSortPackages(query = "drone")
        assertTrue(dronePackages.any { it.id == "pkg_photo_bridal" })
        assertTrue(dronePackages.any { it.id == "pkg_video_music" })
        val snippet = StudioServicesData.findMatchingSnippet(
            pkg = dronePackages.first { it.id == "pkg_photo_bridal" },
            query = "drone",
            isHindi = false
        )
        assertNotNull(snippet)
        assertTrue(snippet!!.contains("drone", ignoreCase = true))

        // Hindi keyword query
        val hindiMatches = StudioServicesData.filterAndSortPackages(query = "ड्रोन")
        assertTrue(hindiMatches.any { it.id == "pkg_video_music" })

        // Budget filters
        val under15k = StudioServicesData.filterAndSortPackages(
            budgetFilter = CatalogBudgetFilter.UNDER_15K
        )
        assertEquals(4, under15k.size)
        assertTrue(under15k.all { it.startingPriceInr <= 15000L })

        val above30k = StudioServicesData.filterAndSortPackages(
            budgetFilter = CatalogBudgetFilter.ABOVE_30K
        )
        assertEquals(listOf("pkg_video_music"), above30k.map { it.id })
    }

    @Test
    fun serviceCatalogScreen_quickSearchBudgetAndMatchHintFlowWorks() {
        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                ServiceCatalogScreen(
                    language = AppLanguage.ENGLISH,
                    onBack = {},
                    onBookService = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Tap Quick-Search "4K Video" pill to find video production packages
        composeTestRule.onNodeWithTag("catalog_quick_filter_4k_video")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("catalog_results_count").assertTextContains("3 Offerings")

        composeTestRule.onNodeWithTag("service_catalog_list")
            .performScrollToNode(hasTestTag("catalog_card_pkg_video_music"))
        composeTestRule.onNodeWithTag("catalog_card_pkg_video_music").assertIsDisplayed()

        // Scroll back to filter controls and toggle Quick-Search pill off
        composeTestRule.onNodeWithTag("service_catalog_list")
            .performScrollToNode(hasTestTag("catalog_quick_search_row"))
        composeTestRule.onNodeWithTag("catalog_quick_filter_4k_video")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("catalog_results_count").assertTextContains("8 Offerings")

        // Filter by Under ₹15K budget chip
        composeTestRule.onNodeWithTag("catalog_budget_chip_under_15k")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("catalog_results_count").assertTextContains("4 Offerings")

        // Clear all active filters via inline Clear pill
        composeTestRule.onNodeWithTag("catalog_clear_all_filters_chip").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("catalog_results_count").assertTextContains("8 Offerings")

        // Search a deliverable term ("drone") -> displays match snippet hint on Bridal card
        composeTestRule.onNodeWithTag("service_catalog_search_input")
            .performTextInput("drone")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("service_catalog_list")
            .performScrollToNode(hasTestTag("catalog_card_pkg_photo_bridal"))
        composeTestRule.onNodeWithTag("catalog_card_pkg_photo_bridal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("catalog_match_hint_pkg_photo_bridal").assertIsDisplayed()

        // Scroll back to search bar and clear via trailing search clear icon
        composeTestRule.onNodeWithTag("service_catalog_list")
            .performScrollToNode(hasTestTag("service_catalog_search_input"))
        composeTestRule.onNodeWithTag("service_catalog_search_clear").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("catalog_results_count").assertTextContains("8 Offerings")
    }

    @Test
    fun serviceCatalogScreen_categoryLabelsIncludeVideoProduction_andFavoritesFilterWorks() {
        val toggledPackageIds = mutableListOf<String>()

        composeTestRule.setContent {
            var favorites by remember {
                mutableStateOf(setOf("pkg_model_portfolio"))
            }
            StarArtistTheme(isStudioLight = false) {
                ServiceCatalogScreen(
                    language = AppLanguage.ENGLISH,
                    favoritePackageIds = favorites,
                    onBack = {},
                    onBookService = {},
                    onToggleFavoritePackage = { pkg ->
                        toggledPackageIds.add(pkg.id)
                        favorites = if (pkg.id in favorites) favorites - pkg.id else favorites + pkg.id
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify explicit category labels requested by user
        composeTestRule.onNodeWithTag("catalog_category_chip_photography")
            .performScrollTo()
            .assertTextContains("Photography")
        composeTestRule.onNodeWithTag("catalog_category_chip_modeling")
            .performScrollTo()
            .assertTextContains("Modeling")
        composeTestRule.onNodeWithTag("catalog_category_chip_production")
            .performScrollTo()
            .assertTextContains("Video Production")

        // Bookmark High-Fashion Editorial package and filter to Favorites
        composeTestRule.onNodeWithTag("service_catalog_list")
            .performScrollToNode(hasTestTag("catalog_card_pkg_photo_fashion"))
        composeTestRule.onNodeWithTag("catalog_favorite_btn_pkg_photo_fashion").performClick()
        composeTestRule.waitForIdle()
        assertTrue("pkg_photo_fashion" in toggledPackageIds)

        composeTestRule.onNodeWithTag("service_catalog_list")
            .performScrollToNode(hasTestTag("service_catalog_search_input"))
        composeTestRule.onNodeWithTag("catalog_favorites_filter_chip")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("catalog_results_count").assertTextContains("2 Offerings")
        composeTestRule.onNodeWithTag("service_catalog_list")
            .performScrollToNode(hasTestTag("catalog_card_pkg_model_portfolio"))
        composeTestRule.onNodeWithTag("catalog_card_pkg_model_portfolio").assertIsDisplayed()
    }

    @Test
    fun serviceCatalogScreen_starRatingAndCommentSectionWorksOnServiceCards() {
        var submittedPackageId = ""
        var submittedRating = 0
        var submittedComment = ""

        val initialReviews = listOf(
            ServiceReview(
                id = "rev_1",
                userId = "user_other",
                bookingId = "catalog_pkg_photo_fashion",
                reviewerName = "Riya Sharma",
                serviceType = "Photography (High-Fashion Editorial)",
                rating = 5L,
                comment = "Editorial lighting and styling were top-notch."
            )
        )

        composeTestRule.setContent {
            var reviewsState by remember { mutableStateOf(initialReviews) }
            StarArtistTheme(isStudioLight = false) {
                ServiceCatalogScreen(
                    language = AppLanguage.ENGLISH,
                    studioReviews = reviewsState,
                    myReviews = emptyList(),
                    onBack = {},
                    onBookService = {},
                    onSubmitServiceReview = { packageId, serviceType, rating, comment, onSuccess ->
                        submittedPackageId = packageId
                        submittedRating = rating
                        submittedComment = comment
                        reviewsState = reviewsState + ServiceReview(
                            id = "rev_new",
                            userId = "user_me",
                            bookingId = "catalog_$packageId",
                            reviewerName = "Client Studio",
                            serviceType = serviceType,
                            rating = rating.toLong(),
                            comment = comment
                        )
                        onSuccess()
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Scroll to the feedback toggle on the first service card and expand it
        composeTestRule.onNodeWithTag("catalog_feedback_toggle_pkg_photo_fashion")
            .performScrollTo()
            .assertTextContains("5.0 (1)")
            .performClick()
        composeTestRule.waitForIdle()

        // Existing review comment is visible inside the card's reviews list
        composeTestRule.onNodeWithTag("catalog_reviews_list_pkg_photo_fashion")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Editorial lighting and styling were top-notch.")
            .performScrollTo()
            .assertIsDisplayed()

        // Select 4 stars, type a comment, and submit
        composeTestRule.onNodeWithTag("catalog_star_pkg_photo_fashion_4")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("catalog_comment_input_pkg_photo_fashion")
            .performScrollTo()
            .performTextInput("Sharp retouching and fast studio turnaround.")
        composeTestRule.onNodeWithTag("catalog_submit_review_pkg_photo_fashion")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("pkg_photo_fashion", submittedPackageId)
        assertEquals(4, submittedRating)
        assertEquals("Sharp retouching and fast studio turnaround.", submittedComment)

        // Updated average & newly posted comment render immediately in card
        composeTestRule.onNodeWithTag("catalog_feedback_toggle_pkg_photo_fashion")
            .performScrollTo()
            .assertTextContains("4.5 (2)")
        composeTestRule.onNodeWithTag("catalog_review_item_pkg_photo_fashion_1")
            .performScrollTo()
            .assertTextContains("Sharp retouching and fast studio turnaround.")
    }

    @Test
    fun serviceCatalogScreen_tierInfoBadgesAndGuideExplainAllThreeTiers() {
        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                ServiceCatalogScreen(
                    language = AppLanguage.ENGLISH,
                    onBack = {},
                    onBookService = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Default Pro tier shows active descriptive info badge
        composeTestRule.onNodeWithTag("catalog_active_tier_badge_pkg_photo_fashion")
            .performScrollTo()
            .assertTextContains("MOST POPULAR", substring = true)

        // Switch to Essential updates active tier badge description
        composeTestRule.onNodeWithTag("catalog_tier_pkg_photo_fashion_essential")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("catalog_active_tier_badge_pkg_photo_fashion")
            .performScrollTo()
            .assertTextContains("CORE SESSION", substring = true)

        // Expand Tier Info comparison guide and verify Essential, Pro, and Flagship details
        composeTestRule.onNodeWithTag("catalog_tier_guide_toggle_pkg_photo_fashion")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("catalog_tier_guide_panel_pkg_photo_fashion")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("catalog_tier_guide_item_pkg_photo_fashion_essential")
            .performScrollTo()
            .assertTextContains("CORE SESSION", substring = true)
        composeTestRule.onNodeWithTag("catalog_tier_guide_item_pkg_photo_fashion_pro")
            .performScrollTo()
            .assertTextContains("MOST POPULAR", substring = true)
        composeTestRule.onNodeWithTag("catalog_tier_guide_item_pkg_photo_fashion_flagship")
            .performScrollTo()
            .assertTextContains("FULL VIP PRODUCTION", substring = true)
    }
}
