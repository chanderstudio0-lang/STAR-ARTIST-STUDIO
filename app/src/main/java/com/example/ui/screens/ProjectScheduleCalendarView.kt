package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.StudioSessionProjectEntity
import com.example.data.model.AppLanguage
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val SessionAccentBlue = Color(0xFF38BDF8)
private val DeadlineAccentAmber = Color(0xFFF59E0B)

/**
 * Interactive monthly studio schedule calendar component synced live with Room
 * [StudioSessionProjectEntity] records to visualize both session shoot dates and delivery deadlines.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectScheduleCalendarView(
    projects: List<StudioSessionProjectEntity>,
    language: AppLanguage,
    currentTimeMillis: Long = System.currentTimeMillis(),
    onInspectProject: (StudioSessionProjectEntity) -> Unit,
    onScheduleOnDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHindi = language == AppLanguage.HINDI
    val todayPair = remember(currentTimeMillis) {
        StudioScheduleCalendarHelper.yearMonthFromMillis(currentTimeMillis)
    }
    val todayIso = remember(currentTimeMillis) {
        StudioScheduleCalendarHelper.todayIso(currentTimeMillis)
    }

    var displayYear by remember { mutableIntStateOf(todayPair.first) }
    var displayMonthZeroBased by remember { mutableIntStateOf(todayPair.second) }
    var selectedDateIso by remember { mutableStateOf(todayIso) }
    var showFullMonthAgenda by remember { mutableStateOf(false) }

    val overview = remember(projects, displayYear, displayMonthZeroBased, currentTimeMillis, isHindi) {
        StudioScheduleCalendarHelper.buildMonthlyScheduleOverview(
            projects = projects,
            year = displayYear,
            monthZeroBased = displayMonthZeroBased,
            todayMillis = currentTimeMillis,
            isHindi = isHindi
        )
    }

    val selectedDayEvents = remember(overview, selectedDateIso) {
        overview.eventsByDate[selectedDateIso].orEmpty()
    }

    val displayedAgendaEvents = if (showFullMonthAgenda) overview.monthEvents else selectedDayEvents

    val dayHeaders = if (isHindi) {
        listOf("रवि", "सोम", "मंगल", "बुध", "गुरु", "शुक्र", "शनि")
    } else {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("project_schedule_calendar_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Monthly Calendar Grid Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.verticalGradient(listOf(GoldPrimary.copy(alpha = 0.65f), StudioOutline))
                    ),
                    RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Month navigation header + Today jump
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GoldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = overview.monthLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldLight,
                                modifier = Modifier.testTag("project_calendar_month_label")
                            )
                            Text(
                                text = if (isHindi)
                                    "Room से सिंक (${projects.size} प्रोजेक्ट)"
                                else
                                    "Synced with Room (${projects.size} projects)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FilterChip(
                            selected = displayYear == todayPair.first &&
                                displayMonthZeroBased == todayPair.second &&
                                selectedDateIso == todayIso,
                            onClick = {
                                displayYear = todayPair.first
                                displayMonthZeroBased = todayPair.second
                                selectedDateIso = todayIso
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (isHindi) "आज" else "Today",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldContainer,
                                selectedLabelColor = GoldPrimary,
                                containerColor = StudioElevatedSurface,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("project_calendar_today_btn")
                        )

                        IconButton(
                            onClick = {
                                val (ny, nm) = StudioScheduleCalendarHelper.shiftYearMonth(
                                    displayYear,
                                    displayMonthZeroBased,
                                    -1
                                )
                                displayYear = ny
                                displayMonthZeroBased = nm
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("project_calendar_prev_month_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isHindi) "पिछला महीना" else "Previous month",
                                tint = GoldLight
                            )
                        }

                        IconButton(
                            onClick = {
                                val (ny, nm) = StudioScheduleCalendarHelper.shiftYearMonth(
                                    displayYear,
                                    displayMonthZeroBased,
                                    +1
                                )
                                displayYear = ny
                                displayMonthZeroBased = nm
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("project_calendar_next_month_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = if (isHindi) "अगला महीना" else "Next month",
                                tint = GoldLight
                            )
                        }
                    }
                }

                // Monthly schedule summary metrics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalendarMetricPill(
                        label = if (isHindi) "शूट सेशन" else "Sessions",
                        count = overview.totalSessionsInMonth,
                        accent = SessionAccentBlue,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("project_calendar_month_sessions_count")
                    )
                    CalendarMetricPill(
                        label = if (isHindi) "डेडलाइन्स" else "Deadlines",
                        count = overview.totalDeadlinesInMonth,
                        accent = DeadlineAccentAmber,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("project_calendar_month_deadlines_count")
                    )
                    CalendarMetricPill(
                        label = if (isHindi) "विलंबित" else "Overdue",
                        count = overview.overdueDeadlinesInMonth,
                        accent = if (overview.overdueDeadlinesInMonth > 0) ErrorRed else SuccessGreen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("project_calendar_month_overdue_count")
                    )
                }

                // Visual legend strip
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioElevatedSurface)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    CalendarLegendDot(
                        color = SessionAccentBlue,
                        label = if (isHindi) "शूट तारीख" else "Session Date"
                    )
                    CalendarLegendDot(
                        color = DeadlineAccentAmber,
                        label = if (isHindi) "डिलीवरी डेडलाइन" else "Deadline"
                    )
                    CalendarLegendDot(
                        color = ErrorRed,
                        label = if (isHindi) "ओवरड्यू" else "Overdue"
                    )
                }

                // Weekday headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayHeaders.forEach { dayName ->
                        Text(
                            text = dayName,
                            color = GoldPrimary.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 7-column monthly grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_calendar_grid"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    overview.days.chunked(7).forEach { weekCells ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            weekCells.forEach { cell ->
                                CalendarDayTile(
                                    cell = cell,
                                    isSelected = cell.isoDate == selectedDateIso,
                                    onClick = {
                                        selectedDateIso = cell.isoDate
                                        showFullMonthAgenda = false
                                        if (!cell.isCurrentMonth) {
                                            displayYear = cell.year
                                            displayMonthZeroBased = cell.monthZeroBased
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Synchronized Schedule Agenda Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
                .testTag("project_calendar_agenda_card"),
            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (showFullMonthAgenda) {
                                if (isHindi) "${overview.monthLabel} — पूरा शेड्यूल" else "${overview.monthLabel} Schedule"
                            } else {
                                if (isHindi) "चयनित तिथि: $selectedDateIso" else "Schedule • $selectedDateIso"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldLight,
                            modifier = Modifier.testTag("project_calendar_agenda_header")
                        )
                        Text(
                            text = if (showFullMonthAgenda) {
                                if (isHindi) "महीने के सभी शूट व डेडलाइन्स" else "All shoot sessions & deadlines in this month"
                            } else {
                                if (isHindi) "किसी भी तारीख पर टैप करें या विवरण खोलें" else "Tap a day above or tap an item to inspect project details"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                }

                // Switcher: Selected Day vs Full Month Timeline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !showFullMonthAgenda,
                        onClick = { showFullMonthAgenda = false },
                        label = {
                            Text(
                                text = if (isHindi)
                                    "चयनित दिन (${selectedDayEvents.size})"
                                else
                                    "Selected Day (${selectedDayEvents.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color(0xFF141000),
                            containerColor = StudioElevatedSurface,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .testTag("project_calendar_agenda_day_tab")
                    )

                    FilterChip(
                        selected = showFullMonthAgenda,
                        onClick = { showFullMonthAgenda = true },
                        label = {
                            Text(
                                text = if (isHindi)
                                    "पूरा महीना (${overview.monthEvents.size})"
                                else
                                    "Full Month (${overview.monthEvents.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color(0xFF141000),
                            containerColor = StudioElevatedSurface,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .testTag("project_calendar_agenda_month_tab")
                    )
                }

                if (displayedAgendaEvents.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioElevatedSurface)
                            .padding(16.dp)
                            .testTag("project_calendar_empty_agenda"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (showFullMonthAgenda) {
                                if (isHindi) "इस महीने कोई सेशन या डेडलाइन नहीं" else "No sessions or deadlines in ${overview.monthLabel}"
                            } else {
                                if (isHindi) "$selectedDateIso के लिए कोई शूट या डेडलाइन नहीं" else "No shoot or deadline on $selectedDateIso"
                            },
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = { onScheduleOnDate(selectedDateIso) },
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("project_calendar_add_for_date_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "$selectedDateIso पर सेशन जोड़ें" else "Schedule on $selectedDateIso",
                                color = GoldLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        displayedAgendaEvents.forEach { event ->
                            CalendarAgendaEventCard(
                                event = event,
                                showDateBadge = showFullMonthAgenda,
                                isHindi = isHindi,
                                onClick = { onInspectProject(event.project) }
                            )
                        }

                        if (!showFullMonthAgenda && overview.monthEvents.size > selectedDayEvents.size) {
                            Button(
                                onClick = { showFullMonthAgenda = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioElevatedSurface,
                                    contentColor = GoldLight
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .border(1.dp, StudioOutline, RoundedCornerShape(10.dp))
                                    .testTag("project_calendar_view_all_month_btn")
                            ) {
                                Text(
                                    text = if (isHindi)
                                        "${overview.monthLabel} के सभी ${overview.monthEvents.size} इवेंट देखें"
                                    else
                                        "View all ${overview.monthEvents.size} events in ${overview.monthLabel}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarMetricPill(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(StudioElevatedSurface)
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            color = accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CalendarLegendDot(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CalendarDayTile(
    cell: ScheduleCalendarDayCell,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSessions = cell.sessionEvents.isNotEmpty()
    val hasDeadlines = cell.deadlineEvents.isNotEmpty()
    val hasOverdue = cell.hasOverdueDeadline

    val containerBg = when {
        isSelected -> GoldPrimary
        cell.isToday -> GoldContainer
        hasOverdue -> ErrorRed.copy(alpha = 0.14f)
        hasSessions && hasDeadlines -> Color(0xFF1E293B)
        hasSessions -> SessionAccentBlue.copy(alpha = 0.12f)
        hasDeadlines -> DeadlineAccentAmber.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val borderStrokeColor = when {
        isSelected -> GoldLight
        cell.isToday -> GoldPrimary
        hasOverdue -> ErrorRed.copy(alpha = 0.7f)
        hasSessions || hasDeadlines -> StudioOutline
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(containerBg)
            .border(
                width = if (isSelected || cell.isToday) 1.5.dp else 1.dp,
                color = borderStrokeColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp, horizontal = 2.dp)
            .testTag("project_calendar_day_${cell.isoDate}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = cell.dayOfMonth.toString(),
            color = when {
                isSelected -> Color(0xFF141000)
                !cell.isCurrentMonth -> TextSecondary.copy(alpha = 0.45f)
                cell.isToday -> GoldLight
                else -> TextPrimary
            },
            fontSize = 12.sp,
            fontWeight = if (isSelected || cell.isToday || hasSessions || hasDeadlines) {
                FontWeight.ExtraBold
            } else {
                FontWeight.Normal
            }
        )

        // Event marker dots (blue for session date, amber/red for deadline)
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(10.dp)
        ) {
            if (hasSessions) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF0A2540) else SessionAccentBlue)
                        .testTag("project_calendar_dot_session_${cell.isoDate}")
                )
            }
            if (hasDeadlines) {
                val dotColor = when {
                    isSelected && hasOverdue -> ErrorRed
                    isSelected -> Color(0xFF5C2D00)
                    hasOverdue -> ErrorRed
                    else -> DeadlineAccentAmber
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(dotColor)
                        .testTag("project_calendar_dot_deadline_${cell.isoDate}")
                )
            }
        }
    }
}

@Composable
private fun CalendarAgendaEventCard(
    event: StudioCalendarEvent,
    showDateBadge: Boolean,
    isHindi: Boolean,
    onClick: () -> Unit
) {
    val project = event.project
    val isDeadline = event.kind == ScheduleEventKind.DEADLINE
    val statusAccent = statusAccentColor(project.status)
    val typeAccent = when {
        event.isOverdue -> ErrorRed
        isDeadline -> DeadlineAccentAmber
        else -> SessionAccentBlue
    }
    val typeLabel = when {
        event.isOverdue -> if (isHindi) "ओवरड्यू डेडलाइन" else "OVERDUE DEADLINE"
        isDeadline -> if (isHindi) "डेडलाइन" else "DEADLINE DUE"
        else -> if (isHindi) "शूट सेशन" else "SESSION SHOOT"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, typeAccent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("project_calendar_event_${event.kind.name.lowercase()}_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = when {
                            event.isOverdue -> Icons.Default.WarningAmber
                            isDeadline -> Icons.Default.Flag
                            else -> Icons.Default.CameraAlt
                        },
                        contentDescription = null,
                        tint = typeAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeAccent.copy(alpha = 0.18f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            color = typeAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    if (showDateBadge) {
                        Text(
                            text = event.dateIso,
                            color = GoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(statusAccent.copy(alpha = 0.16f))
                        .border(1.dp, statusAccent.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isHindi) project.status.labelHi else project.status.labelEn,
                        color = statusAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = project.projectTitle,
                color = GoldLight,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${project.sessionCode.ifBlank { "SAS-${project.id}" }} • ${project.clientName}",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (project.status == com.example.data.local.entity.StudioProjectStatus.COMPLETED) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = "${project.progressPercent}%",
                        color = statusAccent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            val scheduleLine = buildString {
                if (project.scheduledDate.isNotBlank()) {
                    append(if (isHindi) "शूट: ${project.scheduledDate}" else "Shoot: ${project.scheduledDate}")
                }
                if (project.deadlineDate.isNotBlank()) {
                    if (isNotEmpty()) append("  •  ")
                    append(if (isHindi) "डेडलाइन: ${project.deadlineDate}" else "Deadline: ${project.deadlineDate}")
                }
            }
            if (scheduleLine.isNotEmpty()) {
                Text(
                    text = scheduleLine,
                    color = TextSecondary,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}
