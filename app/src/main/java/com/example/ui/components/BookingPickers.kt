package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.notification.SessionReminderScheduler
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pure date/time helper utilities for Material 3 booking date & time pickers.
 */
object BookingPickerUtils {

    private val utcZone: TimeZone = TimeZone.getTimeZone("UTC")

    fun todayUtcMidnightMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val local = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return Calendar.getInstance(utcZone).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun parseDateMillisForPicker(dateStr: String): Long? {
        val trimmed = dateStr.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
                timeZone = utcZone
            }
            parser.parse(trimmed)?.time
        } catch (_: Exception) {
            null
        }
    }

    fun formatUtcMillisAsIsoDate(utcMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = utcZone
        }
        return formatter.format(Date(utcMillis))
    }

    fun parseHourMinuteForPicker(timeStr: String): Pair<Int, Int> {
        return SessionReminderScheduler.parseTimeHourMinute(timeStr)
    }

    fun formatPickedTime12Hour(hourOfDay: Int, minute: Int): String {
        val safeHour = hourOfDay.coerceIn(0, 23)
        val safeMin = minute.coerceIn(0, 59)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, safeMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return SimpleDateFormat("hh:mm a", Locale.US).format(cal.time)
    }
}

/**
 * Material 3 Compose DatePickerDialog styled with Star Artist Studio's Gold & Black / Studio Light theme.
 * Restricts selectable dates to today and future dates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDatePickerModal(
    initialDate: String,
    language: AppLanguage,
    minDateUtcMillis: Long = BookingPickerUtils.todayUtcMidnightMillis(),
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isHindi = language == AppLanguage.HINDI
    val initialUtcMillis = remember(initialDate, minDateUtcMillis) {
        val parsed = BookingPickerUtils.parseDateMillisForPicker(initialDate)
        if (parsed != null && parsed >= minDateUtcMillis) parsed else minDateUtcMillis
    }
    val selectableDates = remember(minDateUtcMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= minDateUtcMillis
            }

            override fun isSelectableYear(year: Int): Boolean {
                val minYear = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = minDateUtcMillis
                }.get(Calendar.YEAR)
                return year >= minYear
            }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialUtcMillis,
        selectableDates = selectableDates
    )

    val isLight = StudioTheme.isLight
    val accentLabelColor = if (isLight) GoldDark else GoldLight

    DatePickerDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("m3_date_picker_dialog"),
        shape = RoundedCornerShape(22.dp),
        colors = DatePickerDefaults.colors(containerColor = StudioCardSurface),
        confirmButton = {
            Button(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        onDateSelected(BookingPickerUtils.formatUtcMillisAsIsoDate(millis))
                    }
                    onDismiss()
                },
                enabled = datePickerState.selectedDateMillis != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("m3_date_picker_confirm")
            ) {
                Text(
                    text = if (isHindi) "तारीख चुनें" else "Confirm Date",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("m3_date_picker_cancel")
            ) {
                Text(
                    text = if (isHindi) "रद्द करें" else "Cancel",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Row(
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "शूट की तारीख चुनें" else "Select Studio Shoot Date",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentLabelColor
                    )
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = StudioCardSurface,
                titleContentColor = accentLabelColor,
                headlineContentColor = TextPrimary,
                weekdayContentColor = TextSecondary,
                subheadContentColor = GoldPrimary,
                yearContentColor = TextPrimary,
                currentYearContentColor = GoldPrimary,
                selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedYearContainerColor = GoldPrimary,
                dayContentColor = TextPrimary,
                disabledDayContentColor = TextSecondary.copy(alpha = 0.35f),
                selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedDayContainerColor = GoldPrimary,
                todayContentColor = GoldPrimary,
                todayDateBorderColor = GoldPrimary
            )
        )
    }
}

/**
 * Material 3 Compose TimePicker modal dialog styled for Star Artist Studio.
 * Returns time formatted cleanly in 12-hour format (e.g., "03:30 PM").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingTimePickerModal(
    initialTime: String,
    language: AppLanguage,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isHindi = language == AppLanguage.HINDI
    val (initHour, initMinute) = remember(initialTime) {
        BookingPickerUtils.parseHourMinuteForPicker(initialTime)
    }
    val timePickerState = rememberTimePickerState(
        initialHour = initHour,
        initialMinute = initMinute,
        is24Hour = false
    )

    val isLight = StudioTheme.isLight

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("m3_time_picker_dialog"),
        containerColor = StudioCardSurface,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isHindi) "सत्र का समय चुनें" else "Select Session Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isLight) GoldDark else GoldLight
                    )
                    Text(
                        text = if (isHindi) "स्टूडियो समय: सुबह 10 – रात 8" else "Studio hours: 10:00 AM – 8:00 PM",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val previewFormatted = BookingPickerUtils.formatPickedTime12Hour(
                    timePickerState.hour,
                    timePickerState.minute
                )
                Surface(
                    color = GoldContainer,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = previewFormatted,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isLight) GoldDark else GoldLight,
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("m3_time_picker_preview")
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.testTag("m3_time_picker"),
                    colors = TimePickerDefaults.colors(
                        clockDialColor = StudioElevatedSurface,
                        clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        clockDialUnselectedContentColor = TextPrimary,
                        selectorColor = GoldPrimary,
                        containerColor = StudioCardSurface,
                        periodSelectorBorderColor = StudioOutline,
                        periodSelectorSelectedContainerColor = GoldPrimary,
                        periodSelectorUnselectedContainerColor = StudioElevatedSurface,
                        periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        periodSelectorUnselectedContentColor = TextSecondary,
                        timeSelectorSelectedContainerColor = GoldContainer,
                        timeSelectorUnselectedContainerColor = StudioElevatedSurface,
                        timeSelectorSelectedContentColor = if (isLight) GoldDark else GoldLight,
                        timeSelectorUnselectedContentColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val picked = BookingPickerUtils.formatPickedTime12Hour(
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    onTimeSelected(picked)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("m3_time_picker_confirm")
            ) {
                Text(
                    text = if (isHindi) "समय चुनें" else "Confirm Time",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("m3_time_picker_cancel")
            ) {
                Text(
                    text = if (isHindi) "रद्द करें" else "Cancel",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
