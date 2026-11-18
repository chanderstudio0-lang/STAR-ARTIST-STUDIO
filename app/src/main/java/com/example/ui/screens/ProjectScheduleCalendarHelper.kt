package com.example.ui.screens

import com.example.data.local.entity.StudioProjectStatus
import com.example.data.local.entity.StudioSessionProjectEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class ScheduleEventKind {
    SESSION_DATE,
    DEADLINE
}

data class StudioCalendarEvent(
    val project: StudioSessionProjectEntity,
    val dateIso: String,
    val kind: ScheduleEventKind,
    val isOverdue: Boolean
)

data class ScheduleCalendarDayCell(
    val isoDate: String,
    val year: Int,
    val monthZeroBased: Int,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val sessionEvents: List<StudioCalendarEvent>,
    val deadlineEvents: List<StudioCalendarEvent>
) {
    val allEvents: List<StudioCalendarEvent>
        get() = sessionEvents + deadlineEvents

    val hasOverdueDeadline: Boolean
        get() = deadlineEvents.any { it.isOverdue }
}

data class MonthlyScheduleOverview(
    val year: Int,
    val monthZeroBased: Int,
    val monthLabel: String,
    val days: List<ScheduleCalendarDayCell>,
    val eventsByDate: Map<String, List<StudioCalendarEvent>>,
    val monthEvents: List<StudioCalendarEvent>,
    val totalSessionsInMonth: Int,
    val totalDeadlinesInMonth: Int,
    val overdueDeadlinesInMonth: Int
)

object StudioScheduleCalendarHelper {

    private val isoDateRegex = Regex("""^(\d{4})[-/](\d{1,2})[-/](\d{1,2})$""")

    fun formatIsoDate(year: Int, monthZeroBased: Int, dayOfMonth: Int): String {
        val normalizedCal = Calendar.getInstance().apply {
            clear()
            set(year, monthZeroBased, dayOfMonth, 12, 0, 0)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(normalizedCal.time)
    }

    fun todayIso(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return formatIsoDate(
            year = cal.get(Calendar.YEAR),
            monthZeroBased = cal.get(Calendar.MONTH),
            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun yearMonthFromMillis(millis: Long = System.currentTimeMillis()): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
    }

    fun shiftYearMonth(year: Int, monthZeroBased: Int, deltaMonths: Int): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, monthZeroBased, 1, 12, 0, 0)
            add(Calendar.MONTH, deltaMonths)
        }
        return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
    }

    /**
     * Parses strict or loosely padded ISO date strings (e.g. "2026-11-16", "2026/11/5")
     * into canonical "YYYY-MM-DD", returning null for blank/TBD/invalid dates.
     */
    fun normalizeIsoDate(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.equals("TBD", ignoreCase = true)) return null

        val match = isoDateRegex.matchEntire(trimmed)
        if (match != null) {
            val year = match.groupValues[1].toIntOrNull() ?: return null
            val monthOneBased = match.groupValues[2].toIntOrNull() ?: return null
            val day = match.groupValues[3].toIntOrNull() ?: return null
            if (monthOneBased !in 1..12 || day !in 1..31) return null

            val cal = Calendar.getInstance().apply {
                isLenient = false
                clear()
                set(year, monthOneBased - 1, day, 12, 0, 0)
            }
            return runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            }.getOrNull()
        }

        val fallbackFormats = listOf("MMM d, yyyy", "MMMM d, yyyy", "dd-MM-yyyy")
        for (pattern in fallbackFormats) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(trimmed)
            }.getOrNull()
            if (parsed != null) {
                return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsed)
            }
        }
        return null
    }

    fun buildMonthlyScheduleOverview(
        projects: List<StudioSessionProjectEntity>,
        year: Int,
        monthZeroBased: Int,
        todayMillis: Long = System.currentTimeMillis(),
        isHindi: Boolean = false
    ): MonthlyScheduleOverview {
        val todayIsoStr = todayIso(todayMillis)

        // Collect all valid session date & deadline events synced with the project list
        val allEvents = mutableListOf<StudioCalendarEvent>()
        projects.forEach { project ->
            normalizeIsoDate(project.scheduledDate)?.let { sessionIso ->
                allEvents.add(
                    StudioCalendarEvent(
                        project = project,
                        dateIso = sessionIso,
                        kind = ScheduleEventKind.SESSION_DATE,
                        isOverdue = false
                    )
                )
            }
            normalizeIsoDate(project.deadlineDate)?.let { deadlineIso ->
                val overdue = deadlineIso < todayIsoStr && project.status != StudioProjectStatus.COMPLETED
                allEvents.add(
                    StudioCalendarEvent(
                        project = project,
                        dateIso = deadlineIso,
                        kind = ScheduleEventKind.DEADLINE,
                        isOverdue = overdue
                    )
                )
            }
        }

        val eventsByDate = allEvents
            .sortedWith(
                compareBy<StudioCalendarEvent> { it.dateIso }
                    .thenBy { if (it.kind == ScheduleEventKind.DEADLINE) 0 else 1 }
                    .thenBy { it.project.projectTitle.lowercase() }
            )
            .groupBy { it.dateIso }

        val firstOfMonth = Calendar.getInstance().apply {
            clear()
            set(year, monthZeroBased, 1, 12, 0, 0)
        }
        val monthPrefix = String.format(Locale.US, "%04d-%02d-", year, monthZeroBased + 1)

        val monthFormatter = SimpleDateFormat(
            "MMMM yyyy",
            if (isHindi) Locale.forLanguageTag("hi-IN") else Locale.ENGLISH
        )
        val monthLabel = monthFormatter.format(firstOfMonth.time)

        val firstDayOfWeek = firstOfMonth.get(Calendar.DAY_OF_WEEK) // 1 = Sun .. 7 = Sat
        val prevPaddingCount = (firstDayOfWeek - Calendar.SUNDAY).coerceAtLeast(0)
        val maxDaysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        val dayCells = mutableListOf<ScheduleCalendarDayCell>()

        // Previous month leading cells
        if (prevPaddingCount > 0) {
            val prevMonthCal = (firstOfMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            val maxDaysPrev = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val prevYear = prevMonthCal.get(Calendar.YEAR)
            val prevMonth = prevMonthCal.get(Calendar.MONTH)
            for (d in (maxDaysPrev - prevPaddingCount + 1)..maxDaysPrev) {
                val iso = formatIsoDate(prevYear, prevMonth, d)
                val dayEvents = eventsByDate[iso].orEmpty()
                dayCells.add(
                    ScheduleCalendarDayCell(
                        isoDate = iso,
                        year = prevYear,
                        monthZeroBased = prevMonth,
                        dayOfMonth = d,
                        isCurrentMonth = false,
                        isToday = iso == todayIsoStr,
                        sessionEvents = dayEvents.filter { it.kind == ScheduleEventKind.SESSION_DATE },
                        deadlineEvents = dayEvents.filter { it.kind == ScheduleEventKind.DEADLINE }
                    )
                )
            }
        }

        // Current month cells
        for (d in 1..maxDaysInMonth) {
            val iso = formatIsoDate(year, monthZeroBased, d)
            val dayEvents = eventsByDate[iso].orEmpty()
            dayCells.add(
                ScheduleCalendarDayCell(
                    isoDate = iso,
                    year = year,
                    monthZeroBased = monthZeroBased,
                    dayOfMonth = d,
                    isCurrentMonth = true,
                    isToday = iso == todayIsoStr,
                    sessionEvents = dayEvents.filter { it.kind == ScheduleEventKind.SESSION_DATE },
                    deadlineEvents = dayEvents.filter { it.kind == ScheduleEventKind.DEADLINE }
                )
            )
        }

        // Trailing next-month cells to complete full 7-column weeks
        val trailingCount = (7 - (dayCells.size % 7)) % 7
        if (trailingCount > 0) {
            val nextMonthCal = (firstOfMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            val nextYear = nextMonthCal.get(Calendar.YEAR)
            val nextMonth = nextMonthCal.get(Calendar.MONTH)
            for (d in 1..trailingCount) {
                val iso = formatIsoDate(nextYear, nextMonth, d)
                val dayEvents = eventsByDate[iso].orEmpty()
                dayCells.add(
                    ScheduleCalendarDayCell(
                        isoDate = iso,
                        year = nextYear,
                        monthZeroBased = nextMonth,
                        dayOfMonth = d,
                        isCurrentMonth = false,
                        isToday = iso == todayIsoStr,
                        sessionEvents = dayEvents.filter { it.kind == ScheduleEventKind.SESSION_DATE },
                        deadlineEvents = dayEvents.filter { it.kind == ScheduleEventKind.DEADLINE }
                    )
                )
            }
        }

        val monthEvents = allEvents
            .filter { it.dateIso.startsWith(monthPrefix) }
            .sortedWith(
                compareBy<StudioCalendarEvent> { it.dateIso }
                    .thenBy { if (it.kind == ScheduleEventKind.DEADLINE) 0 else 1 }
                    .thenBy { it.project.projectTitle.lowercase() }
            )

        return MonthlyScheduleOverview(
            year = year,
            monthZeroBased = monthZeroBased,
            monthLabel = monthLabel,
            days = dayCells,
            eventsByDate = eventsByDate,
            monthEvents = monthEvents,
            totalSessionsInMonth = monthEvents.count { it.kind == ScheduleEventKind.SESSION_DATE },
            totalDeadlinesInMonth = monthEvents.count { it.kind == ScheduleEventKind.DEADLINE },
            overdueDeadlinesInMonth = monthEvents.count { it.kind == ScheduleEventKind.DEADLINE && it.isOverdue }
        )
    }
}
