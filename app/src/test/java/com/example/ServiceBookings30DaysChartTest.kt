package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.AppLanguage
import com.example.data.model.ShootBooking
import com.example.ui.components.ServiceBookings30DaysChartCard
import com.example.ui.components.StudioAnalyticsEngine
import com.example.ui.theme.StarArtistTheme
import com.google.firebase.Timestamp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ServiceBookings30DaysChartTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun serviceBookings30DaysChart_rendersBarsAndTogglesDetailCard() {
        val now = Date()
        val recentTs = Timestamp(Date(now.time - 3L * 24L * 60L * 60L * 1000L))
        val oldTs = Timestamp(Date(now.time - 45L * 24L * 60L * 60L * 1000L))

        val bookings = listOf(
            ShootBooking(id = "p1", serviceType = "Photography", status = "Pending", createdAt = recentTs),
            ShootBooking(id = "p2", serviceType = "Fashion Portfolio Photography", status = "Confirmed", createdAt = recentTs),
            ShootBooking(id = "m1", serviceType = "Modeling", status = "Completed", createdAt = recentTs),
            ShootBooking(id = "v1", serviceType = "Video Production", status = "Cancelled", createdAt = recentTs),
            ShootBooking(id = "old1", serviceType = "Video Production", status = "Confirmed", createdAt = oldTs)
        )
        val metrics = StudioAnalyticsEngine.computeServiceBookingsLast30Days(bookings, referenceDate = now)

        composeTestRule.setContent {
            StarArtistTheme {
                var selectedKey by remember { mutableStateOf<String?>(null) }
                ServiceBookings30DaysChartCard(
                    metrics = metrics,
                    language = AppLanguage.ENGLISH,
                    selectedServiceKey = selectedKey,
                    onSelectService = { key ->
                        selectedKey = if (selectedKey == key) null else key
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("service_30d_chart_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bookings per Service (Last 30 Days)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_30d_total_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("4 BOOKINGS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_30d_bar_chart_canvas").assertIsDisplayed()

        composeTestRule.onNodeWithTag("service_30d_bar_chip_modeling").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_30d_bar_chip_photography").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_30d_bar_chip_video_production").assertIsDisplayed()

        composeTestRule.onNodeWithTag("service_30d_detail_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("service_30d_bar_chip_photography").performClick()

        composeTestRule.onNodeWithTag("service_30d_detail_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Photography — Last 30 Days (2 bookings)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pending: 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirmed: 1").assertIsDisplayed()

        // Clicking again toggles detail closed
        composeTestRule.onNodeWithTag("service_30d_bar_chip_photography").performClick()
        composeTestRule.onNodeWithTag("service_30d_detail_card").assertDoesNotExist()
    }

    @Test
    fun serviceBookings30DaysChart_showsEmptyStateWhenNoRecentBookings() {
        val now = Date()
        val oldTs = Timestamp(Date(now.time - 60L * 24L * 60L * 60L * 1000L))
        val bookings = listOf(
            ShootBooking(id = "old1", serviceType = "Photography", status = "Confirmed", createdAt = oldTs)
        )
        val metrics = StudioAnalyticsEngine.computeServiceBookingsLast30Days(bookings, referenceDate = now)

        composeTestRule.setContent {
            StarArtistTheme {
                ServiceBookings30DaysChartCard(
                    metrics = metrics,
                    language = AppLanguage.ENGLISH,
                    selectedServiceKey = null,
                    onSelectService = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("service_30d_chart_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 BOOKINGS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_30d_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("No bookings recorded over the last 30 days.").assertIsDisplayed()
    }
}
