package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.theme.DarkStudioBlack
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Shoot production time slot representation with status indicator
 */
data class StudioTimeSlot(
    val id: String,
    val timeLabel: String,
    val period: String, // e.g. "Morning", "Afternoon", "Golden Hour", "Evening"
    val setupDesc: String, // e.g. "Key Light Bay", "Rim Mood Stage"
    val isAvailable: Boolean = true
)

/**
 * Canonical Studio Time Slots
 */
object StudioTimeSlotData {
    val defaultSlots = listOf(
        StudioTimeSlot(
            id = "slot_10am",
            timeLabel = "10:00 AM",
            period = "Morning",
            setupDesc = "Key Light Bay",
            isAvailable = true
        ),
        StudioTimeSlot(
            id = "slot_12pm",
            timeLabel = "12:00 PM",
            period = "Midday",
            setupDesc = "High-Key Flash",
            isAvailable = true
        ),
        StudioTimeSlot(
            id = "slot_0230pm",
            timeLabel = "02:30 PM",
            period = "Afternoon",
            setupDesc = "Softbox Editorial",
            isAvailable = true
        ),
        StudioTimeSlot(
            id = "slot_0430pm",
            timeLabel = "04:30 PM",
            period = "Golden Hour",
            setupDesc = "Outdoor & Terrace",
            isAvailable = true
        ),
        StudioTimeSlot(
            id = "slot_0630pm",
            timeLabel = "06:30 PM",
            period = "Evening",
            setupDesc = "Dark & Rim Stage",
            isAvailable = true
        )
    )
}

/**
 * Calendar day item model
 */
data class CalendarDay(
    val dateString: String, // YYYY-MM-DD
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isPast: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean
)

/**
 * Interactive calendar view with an available time-slot grid.
 * Clients can browse months, select preferred dates, and pick a production time slot.
 */
@Composable
fun InteractiveBookingCalendar(
    modifier: Modifier = Modifier,
    language: AppLanguage,
    selectedDate: String,
    selectedTimeSlot: String,
    onDateSelected: (String) -> Unit,
    onTimeSlotSelected: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val monthYearFormatEn = remember { SimpleDateFormat("MMMM yyyy", Locale.ENGLISH) }
    val monthYearFormatHi = remember { SimpleDateFormat("MMMM yyyy", Locale("hi", "IN")) }

    // Month offset from today (0 = current month, 1 = next month, etc.)
    var monthOffset by remember { mutableIntStateOf(0) }

    // Today reference
    val todayCal = remember { Calendar.getInstance() }
    val todayDateStr = remember { dateFormat.format(todayCal.time) }

    // Active displayed calendar view based on monthOffset
    val currentMonthCal = remember(monthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val monthHeader = remember(monthOffset, language) {
        val date = currentMonthCal.time
        if (language == AppLanguage.HINDI) monthYearFormatHi.format(date) else monthYearFormatEn.format(date)
    }

    // Days in current grid (including padding from previous and next month)
    val daysInMonthGrid = remember(monthOffset, selectedDate) {
        val days = mutableListOf<CalendarDay>()
        val cal = currentMonthCal.clone() as Calendar

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday
        val maxDaysInCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Previous month padding
        val prevMonthCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val maxDaysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prevPaddingCount = firstDayOfWeek - 1

        for (i in (maxDaysInPrevMonth - prevPaddingCount + 1)..maxDaysInPrevMonth) {
            prevMonthCal.set(Calendar.DAY_OF_MONTH, i)
            val dStr = dateFormat.format(prevMonthCal.time)
            days.add(
                CalendarDay(
                    dateString = dStr,
                    dayOfMonth = i,
                    isCurrentMonth = false,
                    isPast = true,
                    isToday = false,
                    isSelected = selectedDate == dStr
                )
            )
        }

        // Current month days
        for (i in 1..maxDaysInCurrentMonth) {
            cal.set(Calendar.DAY_OF_MONTH, i)
            val dStr = dateFormat.format(cal.time)

            // Normalize for comparison
            val isPast = cal.before(Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            })
            val isToday = dStr == todayDateStr

            days.add(
                CalendarDay(
                    dateString = dStr,
                    dayOfMonth = i,
                    isCurrentMonth = true,
                    isPast = isPast,
                    isToday = isToday,
                    isSelected = selectedDate == dStr
                )
            )
        }

        // Next month padding to complete row of 7
        val remaining = (7 - (days.size % 7)) % 7
        val nextMonthCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        for (i in 1..remaining) {
            nextMonthCal.set(Calendar.DAY_OF_MONTH, i)
            val dStr = dateFormat.format(nextMonthCal.time)
            days.add(
                CalendarDay(
                    dateString = dStr,
                    dayOfMonth = i,
                    isCurrentMonth = false,
                    isPast = false,
                    isToday = false,
                    isSelected = selectedDate == dStr
                )
            )
        }

        days
    }

    val dayHeaders = if (language == AppLanguage.HINDI) {
        listOf("रवि", "सोम", "मंगल", "बुध", "गुरु", "शुक्र", "शनि")
    } else {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(listOf(GoldPrimary.copy(alpha = 0.6f), StudioOutline))
                ),
                RoundedCornerShape(16.dp)
            )
            .testTag("interactive_booking_calendar"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Month Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(GoldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = monthHeader,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (monthOffset > 0) monthOffset-- },
                        enabled = monthOffset > 0,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("calendar_prev_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = if (monthOffset > 0) GoldLight else Color.DarkGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { if (monthOffset < 6) monthOffset++ },
                        enabled = monthOffset < 6,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("calendar_next_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = if (monthOffset < 6) GoldLight else Color.DarkGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day of Week Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dayHeaders.forEach { header ->
                    Text(
                        text = header,
                        color = GoldPrimary.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Calendar Days Grid (7 columns)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                daysInMonthGrid.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        week.forEach { day ->
                            val isClickable = day.isCurrentMonth && !day.isPast
                            val isSelected = day.isSelected

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> GoldPrimary
                                            day.isToday -> GoldContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = when {
                                            isSelected -> 1.5.dp
                                            day.isToday -> 1.dp
                                            else -> 0.dp
                                        },
                                        color = when {
                                            isSelected -> GoldLight
                                            day.isToday -> GoldPrimary.copy(alpha = 0.5f)
                                            else -> Color.Transparent
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable(enabled = isClickable) {
                                        onDateSelected(day.dateString)
                                    }
                                    .testTag("calendar_day_${day.dateString}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    color = when {
                                        isSelected -> DarkStudioBlack
                                        day.isPast || !day.isCurrentMonth -> Color(0xFF555555)
                                        day.isToday -> GoldLight
                                        else -> Color.White
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selected Date Indicator Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioElevatedSurface)
                    .border(0.5.dp, StudioOutline, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (selectedDate.isNotEmpty()) SuccessGreen else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "चयनित शूट तारीख:" else "Selected Shoot Date:",
                        color = Color(0xFFA5A5A5),
                        fontSize = 11.5.sp
                    )
                }

                Text(
                    text = if (selectedDate.isNotEmpty()) selectedDate else (if (language == AppLanguage.HINDI) "कोई नहीं" else "None"),
                    color = if (selectedDate.isNotEmpty()) GoldPrimary else Color.Gray,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    modifier = Modifier.testTag("selected_date_display")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =================================================================
            // TIME-SLOT GRID SECTION
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "उपलब्ध समय स्लॉट ग्रिड" else "Available Time-Slot Grid",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF231F14))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "10 AM - 8 PM" else "STUDIO LIGHTING BAYS",
                        color = GoldPrimary,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time Slots Grid (2 columns or flex rows)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("time_slot_grid"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudioTimeSlotData.defaultSlots.chunked(2).forEach { rowSlots ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSlots.forEach { slot ->
                            // Full display string used across bookings
                            val fullSlotValue = "${slot.timeLabel} (${slot.setupDesc})"
                            val isSelected = selectedTimeSlot.startsWith(slot.timeLabel)

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isSelected) GoldPrimary else StudioOutline
                                        ),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable(enabled = slot.isAvailable) {
                                        onTimeSlotSelected(fullSlotValue)
                                    }
                                    .testTag("time_slot_card_${slot.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF2C2412) else StudioElevatedSurface
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = slot.timeLabel,
                                            color = if (isSelected) GoldLight else Color.White,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = slot.setupDesc,
                                            color = if (isSelected) GoldPrimary else Color(0xFFA0A0A0),
                                            fontSize = 9.5.sp,
                                            maxLines = 1
                                        )
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(GoldPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = DarkStudioBlack,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Fill space if row only has 1 item
                        if (rowSlots.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
