package com.example

import com.example.data.model.ShootBooking
import com.example.ui.components.AnalyticsPeriod
import com.example.ui.components.StudioAnalyticsEngine
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudioVisualAnalyticsTest {

    @Test
    fun trendPoints_returnsAccurateCountForSelectedPeriod() {
        val dummyBookings = listOf(
            ShootBooking(id = "b1", serviceType = "Fashion Portfolio Photography", status = "Confirmed"),
            ShootBooking(id = "b2", serviceType = "4K Cinema Video Production", status = "Pending"),
            ShootBooking(id = "b3", serviceType = "Model Grooming & Management", status = "Confirmed")
        )

        val points7D = StudioAnalyticsEngine.generateTrendPoints(dummyBookings, AnalyticsPeriod.SEVEN_DAYS)
        assertEquals("7 Days period must generate 7 data points", 7, points7D.size)

        val points30D = StudioAnalyticsEngine.generateTrendPoints(dummyBookings, AnalyticsPeriod.THIRTY_DAYS)
        assertEquals("30 Days period must generate 10 grouped data points", 10, points30D.size)

        val pointsAll = StudioAnalyticsEngine.generateTrendPoints(dummyBookings, AnalyticsPeriod.ALL_TIME)
        assertEquals("All Time period must generate 12 weekly data points", 12, pointsAll.size)
    }

    @Test
    fun trendPoints_confirmsTotalAndStatusConsistency() {
        val bookings = listOf(
            ShootBooking(id = "b1", status = "Confirmed"),
            ShootBooking(id = "b2", status = "Pending")
        )

        val points = StudioAnalyticsEngine.generateTrendPoints(bookings, AnalyticsPeriod.SEVEN_DAYS, confirmedOnly = false)

        points.forEach { point ->
            assertEquals(
                "Total bookings must equal confirmed + pending",
                point.totalBookings,
                point.confirmedBookings + point.pendingBookings
            )
            assertTrue("Total bookings must be non-negative", point.totalBookings >= 0)
            assertNotNull("Date label must be non-null", point.dateLabel)
            assertNotNull("Full date string must be non-null", point.fullDateStr)
        }
    }

    @Test
    fun trendPoints_confirmedOnlyFilter_setsTotalToConfirmed() {
        val bookings = listOf(
            ShootBooking(id = "b1", status = "Confirmed"),
            ShootBooking(id = "b2", status = "Pending")
        )

        val points = StudioAnalyticsEngine.generateTrendPoints(bookings, AnalyticsPeriod.SEVEN_DAYS, confirmedOnly = true)

        points.forEach { point ->
            assertEquals("In confirmedOnly mode, totalBookings must equal confirmedBookings", point.totalBookings, point.confirmedBookings)
            assertEquals("In confirmedOnly mode, pendingBookings must be 0", 0, point.pendingBookings)
        }
    }

    @Test
    fun popularServices_sortsByRequestCountDescending() {
        val bookings = listOf(
            ShootBooking(id = "b1", serviceType = "Fashion Portfolio Photography"),
            ShootBooking(id = "b2", serviceType = "Fashion Portfolio Photography"),
            ShootBooking(id = "b3", serviceType = "4K Cinema Video Production")
        )

        val services = StudioAnalyticsEngine.computePopularServices(bookings)

        assertTrue("Popular services list must not be empty", services.isNotEmpty())

        // Verify descending sort order
        for (i in 0 until services.size - 1) {
            assertTrue(
                "Services must be sorted in descending order of requestCount",
                services[i].requestCount >= services[i + 1].requestCount
            )
        }

        // Verify percentages sum to approximately 100%
        val totalPercentage = services.sumOf { it.percentage.toDouble() }
        assertTrue("Sum of service percentages must be approximately 100%", totalPercentage in 98.0..102.0)

        // Core studio categories exist
        val categories = services.map { it.category }
        assertTrue(categories.contains("Photography"))
        assertTrue(categories.contains("Cinema & Reels"))
        assertTrue(categories.contains("Talent Management"))
    }

    @Test
    fun kpis_computesMeaningfulSummary() {
        val bookings = listOf(
            ShootBooking(id = "b1", status = "Confirmed"),
            ShootBooking(id = "b2", status = "Confirmed")
        )

        val trendPoints = StudioAnalyticsEngine.generateTrendPoints(bookings, AnalyticsPeriod.SEVEN_DAYS)
        val services = StudioAnalyticsEngine.computePopularServices(bookings)
        val kpis = StudioAnalyticsEngine.computeKpis(bookings, trendPoints, services)

        assertTrue("Total bookings tracked must be > 0", kpis.totalBookingsTracked > 0)
        assertTrue("Confirmation rate must be within 0%..100%", kpis.confirmationRatePercent in 0..100)
        assertNotNull("Top requested service must not be null", kpis.topRequestedService)
        assertEquals("Saturday & Sunday", kpis.peakActivityDay)
    }

    @Test
    fun analyticsPeriodEnum_hasExpectedLabelsAndDays() {
        assertEquals(7, AnalyticsPeriod.SEVEN_DAYS.days)
        assertEquals(30, AnalyticsPeriod.THIRTY_DAYS.days)
        assertEquals(90, AnalyticsPeriod.ALL_TIME.days)

        assertEquals("7 Days", AnalyticsPeriod.SEVEN_DAYS.labelEn)
        assertEquals("7 दिन", AnalyticsPeriod.SEVEN_DAYS.labelHi)
    }

    @Test
    fun computeServiceBookingsLast30Days_filtersWindowGroupsServicesAndTalliesStatuses() {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val referenceDate = fmt.parse("2026-11-13")!!
        val withinWindowTs = Timestamp(fmt.parse("2026-11-05")!!)
        val oldTs = Timestamp(fmt.parse("2026-09-15")!!)

        val bookings = listOf(
            // Within 30 days via createdAt Timestamp (overrides old preferredDate)
            ShootBooking(
                id = "b_photo_1",
                serviceType = "Fashion Portfolio Photography",
                preferredDate = "2025-01-01",
                status = "Confirmed",
                createdAt = withinWindowTs
            ),
            // Within 30 days via preferredDate
            ShootBooking(
                id = "b_photo_2",
                serviceType = "Photography",
                preferredDate = "2026-10-20",
                status = "Pending"
            ),
            ShootBooking(
                id = "b_photo_3",
                serviceType = "Bridal Stills",
                preferredDate = "2026-11-10",
                status = "Completed"
            ),
            ShootBooking(
                id = "b_model_1",
                serviceType = "Model Grooming",
                preferredDate = "2026-11-01",
                status = "" // blank counts as pending
            ),
            ShootBooking(
                id = "b_video_1",
                serviceType = "4K Cinema Video Production",
                preferredDate = "2026-10-25",
                status = "Cancelled"
            ),
            ShootBooking(
                id = "b_other_1",
                serviceType = "Drone Event Coverage",
                preferredDate = "2026-11-12",
                status = "Approved"
            ),
            // Outside 30 days — must be excluded
            ShootBooking(
                id = "b_old_1",
                serviceType = "Photography",
                preferredDate = "2026-09-01",
                status = "Confirmed"
            ),
            ShootBooking(
                id = "b_old_ts",
                serviceType = "Video Production",
                preferredDate = "2026-11-12",
                status = "Confirmed",
                createdAt = oldTs
            ),
            // Invalid / blank date without Timestamp — excluded
            ShootBooking(
                id = "b_nodate",
                serviceType = "Modeling",
                preferredDate = "",
                status = "Pending"
            )
        )

        val metrics = StudioAnalyticsEngine.computeServiceBookingsLast30Days(
            bookings = bookings,
            referenceDate = referenceDate
        )

        assertEquals(listOf("modeling", "photography", "video_production", "other"), metrics.map { it.serviceKey })

        val modeling = metrics.first { it.serviceKey == "modeling" }
        assertEquals(1, modeling.totalCount)
        assertEquals(1, modeling.pendingCount)

        val photography = metrics.first { it.serviceKey == "photography" }
        assertEquals(3, photography.totalCount)
        assertEquals(1, photography.pendingCount)
        assertEquals(1, photography.confirmedCount)
        assertEquals(1, photography.completedCount)
        assertEquals(0, photography.cancelledCount)

        val video = metrics.first { it.serviceKey == "video_production" }
        assertEquals(1, video.totalCount)
        assertEquals(1, video.cancelledCount)

        val other = metrics.first { it.serviceKey == "other" }
        assertEquals(1, other.totalCount)
        assertEquals(1, other.confirmedCount)
    }

    @Test
    fun resolveBookingTimestampMs_parsesMultipleDateFormatsAndRejectsInvalid() {
        val isoBooking = ShootBooking(id = "1", preferredDate = "2026-11-10")
        assertNotNull(StudioAnalyticsEngine.resolveBookingTimestampMs(isoBooking))

        val slashBooking = ShootBooking(id = "2", preferredDate = "10/11/2026")
        assertNotNull(StudioAnalyticsEngine.resolveBookingTimestampMs(slashBooking))

        val invalidBooking = ShootBooking(id = "3", preferredDate = "not-a-date")
        assertNull(StudioAnalyticsEngine.resolveBookingTimestampMs(invalidBooking))
    }
}
