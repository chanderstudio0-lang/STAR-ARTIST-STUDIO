package com.example

import com.example.ui.components.CalendarDay
import com.example.ui.components.StudioTimeSlotData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InteractiveBookingCalendarTest {

    @Test
    fun studioTimeSlots_containsAllDailyProductionBays() {
        val slots = StudioTimeSlotData.defaultSlots
        assertTrue("Time slots must not be empty", slots.isNotEmpty())
        assertTrue("Must have at least 5 standard production slots", slots.size >= 5)

        val times = slots.map { it.timeLabel }
        assertTrue("Must include 10:00 AM slot", times.contains("10:00 AM"))
        assertTrue("Must include 12:00 PM slot", times.contains("12:00 PM"))
        assertTrue("Must include 02:30 PM slot", times.contains("02:30 PM"))
        assertTrue("Must include 04:30 PM slot", times.contains("04:30 PM"))
        assertTrue("Must include 06:30 PM slot", times.contains("06:30 PM"))

        slots.forEach { slot ->
            assertTrue("Slot ID must not be blank", slot.id.isNotBlank())
            assertTrue("Period must not be blank", slot.period.isNotBlank())
            assertTrue("Setup description must not be blank", slot.setupDesc.isNotBlank())
            assertTrue("Slot should be available by default", slot.isAvailable)
        }
    }

    @Test
    fun calendarDay_dateFormattingAndSelectionLogic() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance()
        val todayStr = dateFormat.format(today.time)

        val selectedDay = CalendarDay(
            dateString = todayStr,
            dayOfMonth = today.get(Calendar.DAY_OF_MONTH),
            isCurrentMonth = true,
            isPast = false,
            isToday = true,
            isSelected = true
        )

        assertEquals(todayStr, selectedDay.dateString)
        assertTrue(selectedDay.isCurrentMonth)
        assertFalse(selectedDay.isPast)
        assertTrue(selectedDay.isToday)
        assertTrue(selectedDay.isSelected)
    }

    @Test
    fun calendarDay_pastDateDetection() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -5)
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val pastStr = dateFormat.format(cal.time)

        val pastDay = CalendarDay(
            dateString = pastStr,
            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
            isCurrentMonth = true,
            isPast = true,
            isToday = false,
            isSelected = false
        )

        assertTrue("Past day must be marked isPast=true", pastDay.isPast)
        assertFalse("Past day should not be marked as today", pastDay.isToday)
    }

    @Test
    fun timeSlotValue_formattingMatchesBookingModel() {
        val slot = StudioTimeSlotData.defaultSlots[0]
        val formattedSlotValue = "${slot.timeLabel} (${slot.setupDesc})"

        assertTrue("Must contain time label", formattedSlotValue.contains("10:00 AM"))
        assertTrue("Must contain setup description", formattedSlotValue.contains("Key Light Bay"))
    }
}
