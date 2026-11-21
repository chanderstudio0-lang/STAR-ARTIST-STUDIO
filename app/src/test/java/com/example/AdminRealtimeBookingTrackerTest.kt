package com.example

import com.example.data.model.ShootBooking
import com.example.ui.components.AdminRealtimeBookingTrackerLogic
import com.example.ui.components.BookingTimelinePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AdminRealtimeBookingTrackerTest {

    private fun midnightOf(year: Int, monthOneBased: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOneBased - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun hasEventDatePassed_respectsSameDayAndMultipleDateFormats() {
        val today = midnightOf(2026, 11, 21)

        assertTrue(AdminRealtimeBookingTrackerLogic.hasEventDatePassed("2026-11-20", today))
        assertTrue(AdminRealtimeBookingTrackerLogic.hasEventDatePassed("19 Nov 2026", today))
        assertTrue(AdminRealtimeBookingTrackerLogic.hasEventDatePassed("18/11/2026", today))

        // Same day stays Confirmed until the next calendar day
        assertFalse(AdminRealtimeBookingTrackerLogic.hasEventDatePassed("2026-11-21", today))
        assertFalse(AdminRealtimeBookingTrackerLogic.hasEventDatePassed("21 Nov 2026", today))

        // Future dates & invalid strings are never passed
        assertFalse(AdminRealtimeBookingTrackerLogic.hasEventDatePassed("2026-11-25", today))
        assertFalse(AdminRealtimeBookingTrackerLogic.hasEventDatePassed("not-a-date", today))
        assertNull(AdminRealtimeBookingTrackerLogic.parseEventDateMidnightMillis("2026-02-31"))
    }

    @Test
    fun findDueConfirmedBookings_selectsOnlyPastDateConfirmedBookings() {
        val today = midnightOf(2026, 11, 21)
        val bookings = listOf(
            ShootBooking(id = "b_past_1", clientName = "Aarav", preferredDate = "2026-11-18", status = "Confirmed"),
            ShootBooking(id = "b_past_2", clientName = "Meera", preferredDate = "20 Nov 2026", status = "Confirmed"),
            ShootBooking(id = "b_today", clientName = "Riya", preferredDate = "2026-11-21", status = "Confirmed"),
            ShootBooking(id = "b_future", clientName = "Kabir", preferredDate = "2026-11-26", status = "Confirmed"),
            ShootBooking(id = "b_pending", clientName = "Unconfirmed", preferredDate = "2026-11-10", status = "Pending"),
            ShootBooking(id = "b_cancelled", clientName = "Cancelled", preferredDate = "2026-11-10", status = "Cancelled"),
            ShootBooking(id = "b_done", clientName = "Finished", preferredDate = "2026-11-05", status = "Completed")
        )

        val due = AdminRealtimeBookingTrackerLogic.findDueConfirmedBookings(bookings, today)
        assertEquals(listOf("b_past_1", "b_past_2"), due.map { it.id })

        val summary = AdminRealtimeBookingTrackerLogic.summarize(bookings, today)
        assertEquals(1, summary.confirmedUpcoming)
        assertEquals(1, summary.confirmedToday)
        assertEquals(2, summary.confirmedPastDue)
        assertEquals(1, summary.completed)
        assertEquals(1, summary.pending)
        assertEquals(1, summary.cancelled)

        val (todayPhase, todayDelta) = AdminRealtimeBookingTrackerLogic.timelinePhase("2026-11-21", today)
        assertEquals(BookingTimelinePhase.TODAY, todayPhase)
        assertEquals(0, todayDelta)

        val (futurePhase, futureDelta) = AdminRealtimeBookingTrackerLogic.timelinePhase("2026-11-26", today)
        assertEquals(BookingTimelinePhase.UPCOMING, futurePhase)
        assertEquals(5, futureDelta)
    }
}
