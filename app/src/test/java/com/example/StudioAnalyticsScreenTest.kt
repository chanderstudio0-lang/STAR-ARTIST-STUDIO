package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.AppLanguage
import com.example.data.model.ShootBooking
import com.example.ui.components.AnalyticsPeriod
import com.example.ui.components.StudioAnalyticsEngine
import com.example.ui.screens.StudioAnalyticsScreen
import com.example.ui.theme.StarArtistTheme
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class StudioAnalyticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleFirestoreBookings(): List<ShootBooking> {
        val nowMs = System.currentTimeMillis()
        val oneDayMs = 24L * 60L * 60L * 1000L
        return listOf(
            ShootBooking(
                id = "b1",
                clientName = "Aarav Sharma",
                serviceType = "Fashion Portfolio Photography",
                status = "Confirmed",
                createdAt = Timestamp(Date(nowMs - oneDayMs))
            ),
            ShootBooking(
                id = "b2",
                clientName = "Meera Kaur",
                serviceType = "Fashion Portfolio Photography",
                status = "Completed",
                createdAt = Timestamp(Date(nowMs - 2 * oneDayMs))
            ),
            ShootBooking(
                id = "b3",
                clientName = "Kabir Verma",
                serviceType = "4K Cinema Video Production",
                status = "Pending",
                createdAt = Timestamp(Date(nowMs - 3 * oneDayMs))
            ),
            ShootBooking(
                id = "b4",
                clientName = "Riya Singh",
                serviceType = "Model Grooming & Management",
                status = "Cancelled",
                createdAt = Timestamp(Date(nowMs - 4 * oneDayMs))
            )
        )
    }

    @Test
    fun analyticsEngine_liveFirestoreMode_reflectsExactCountsWithoutSyntheticNoise() {
        val bookings = sampleFirestoreBookings()
        val trend7D = StudioAnalyticsEngine.generateTrendPoints(
            bookings = bookings,
            period = AnalyticsPeriod.SEVEN_DAYS,
            confirmedOnly = false,
            includeStudioBaseline = false
        )
        // Cancelled bookings are excluded from active trend points, leaving 3 active (2 confirmed/completed, 1 pending)
        assertEquals(3, trend7D.sumOf { it.totalBookings })
        assertEquals(2, trend7D.sumOf { it.confirmedBookings })
        assertEquals(1, trend7D.sumOf { it.pendingBookings })

        val confirmedOnly7D = StudioAnalyticsEngine.generateTrendPoints(
            bookings = bookings,
            period = AnalyticsPeriod.SEVEN_DAYS,
            confirmedOnly = true,
            includeStudioBaseline = false
        )
        assertEquals(2, confirmedOnly7D.sumOf { it.totalBookings })
        assertEquals(0, confirmedOnly7D.sumOf { it.pendingBookings })

        val popular = StudioAnalyticsEngine.computePopularServices(
            bookings = bookings,
            includeStudioBaseline = false
        )
        assertEquals("Fashion Portfolio Photography", popular.first().serviceName)
        assertEquals(2, popular.first().requestCount)
        assertEquals(50, popular.first().percentage.toInt())

        val kpis = StudioAnalyticsEngine.computeKpis(
            bookings = bookings,
            trendPoints = trend7D,
            popularServices = popular,
            includeStudioBaseline = false
        )
        assertEquals(4, kpis.totalBookingsTracked)
        assertEquals(50, kpis.confirmationRatePercent)
    }

    @Test
    fun studioAnalyticsScreen_rendersChartsStatusBreakdownAndInteractiveFilters() {
        var backClicked = false
        var refreshClicked = false
        var bookedService: String? = null
        val bookings = sampleFirestoreBookings()

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                StudioAnalyticsScreen(
                    language = AppLanguage.ENGLISH,
                    bookings = bookings,
                    isAdmin = true,
                    isLoading = false,
                    onBack = { backClicked = true },
                    onRefresh = { refreshClicked = true },
                    onBookShoot = { service -> bookedService = service }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("studio_analytics_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Booking Trends & Popularity").assertIsDisplayed()
        composeTestRule.onNodeWithText("Studio-Wide Firestore • 4 Records").assertIsDisplayed()

        // Status breakdown card
        composeTestRule.onNodeWithTag("analytics_status_breakdown_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("analytics_status_confirmed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("analytics_status_pending").assertIsDisplayed()

        // Scroll to Recharts visual dashboard cards
        composeTestRule.onNodeWithTag("recharts_booking_trends_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("recharts_area_trend_canvas").performScrollTo().assertIsDisplayed()

        // Period & Confirmed filters
        composeTestRule.onNodeWithTag("period_chip_thirty_days").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("filter_confirmed_only_chip").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Scroll to Popular Services horizontal bar chart and click top bar
        composeTestRule.onNodeWithTag("recharts_popular_services_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_bar_photography").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("selected_service_detail_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("analytics_book_selected_service_button").performScrollTo().performClick()
        assertEquals("Fashion Portfolio Photography", bookedService)

        // Scroll to Donut card
        composeTestRule.onNodeWithTag("recharts_service_distribution_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("recharts_donut_canvas").performScrollTo().assertIsDisplayed()

        // Refresh and Back actions
        composeTestRule.onNodeWithTag("analytics_refresh_button").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("analytics_back_button").performScrollTo().performClick()
        assertTrue(refreshClicked)
        assertTrue(backClicked)
    }

    @Test
    fun studioAnalyticsScreen_emptyFirestoreStateAndLightThemeSupport() {
        var bookedService: String? = null

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = true) {
                StudioAnalyticsScreen(
                    language = AppLanguage.ENGLISH,
                    bookings = emptyList(),
                    isAdmin = false,
                    isLoading = false,
                    onBack = {},
                    onRefresh = {},
                    onBookShoot = { service -> bookedService = service }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("My Firestore Bookings • 0 Records").assertIsDisplayed()
        composeTestRule.onNodeWithTag("analytics_firestore_empty_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Live Firestore Bookings Yet").assertIsDisplayed()

        composeTestRule.onNodeWithTag("analytics_empty_book_cta").performClick()
        assertEquals("Fashion Portfolio Photography", bookedService)

        // Switching to Benchmark mode dismisses empty callout and populates benchmark totals
        composeTestRule.onNodeWithTag("analytics_switch_benchmark_cta").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("analytics_firestore_empty_card").assertDoesNotExist()
    }

    @Test
    fun studioAnalyticsScreen_supportsHindiLocalization() {
        composeTestRule.setContent {
            StarArtistTheme {
                StudioAnalyticsScreen(
                    language = AppLanguage.HINDI,
                    bookings = sampleFirestoreBookings(),
                    isAdmin = false,
                    isLoading = false,
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("बुकिंग रुझान व लोकप्रियता").assertIsDisplayed()
        composeTestRule.onNodeWithText("मेरा Firestore डेटा • 4 रिकॉर्ड").assertIsDisplayed()
    }
}
