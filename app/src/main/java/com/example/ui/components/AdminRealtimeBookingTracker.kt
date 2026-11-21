package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.ShootBooking
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class BookingTimelinePhase {
    PASSED,
    TODAY,
    UPCOMING,
    UNKNOWN
}

data class BookingAutoTransitionRecord(
    val bookingId: String,
    val clientName: String,
    val serviceType: String,
    val preferredDate: String,
    val fromStatus: String = "Confirmed",
    val toStatus: String = "Completed",
    val transitionedAtMillis: Long
)

data class RealtimeBookingTrackerSummary(
    val confirmedUpcoming: Int,
    val confirmedToday: Int,
    val confirmedPastDue: Int,
    val completed: Int,
    val pending: Int,
    val cancelled: Int,
    val dueBookings: List<ShootBooking>
)

data class RealtimeBookingTrackerUiState(
    val isEnabled: Boolean = true,
    val isSyncing: Boolean = false,
    val isLiveConnected: Boolean = false,
    val lastCheckedAtMillis: Long? = null,
    val lastError: String? = null,
    val totalAutoCompleted: Int = 0,
    val recentTransitions: List<BookingAutoTransitionRecord> = emptyList()
)

/**
 * Pure date-evaluation and status-transition rules for the Admin Dashboard
 * Real-Time Booking Status Tracker.
 *
 * A Confirmed event's date is considered passed strictly starting the following local calendar
 * day so events happening today stay 'Confirmed' until the day ends.
 */
object AdminRealtimeBookingTrackerLogic {

    private val datePatterns = listOf(
        "yyyy-MM-dd",
        "dd MMM yyyy",
        "dd-MM-yyyy",
        "dd/MM/yyyy",
        "MMM dd, yyyy",
        "yyyy/MM/dd",
        "dd MMMM yyyy",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )

    fun todayMidnightMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun parseEventDateMidnightMillis(rawDate: String): Long? {
        val trimmed = rawDate.trim()
        if (trimmed.isEmpty()) return null
        for (pattern in datePatterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }
                val parsed = parser.parse(trimmed) ?: continue
                val cal = Calendar.getInstance().apply {
                    time = parsed
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            } catch (_: Exception) {
                // Try next supported format
            }
        }
        return null
    }

    fun hasEventDatePassed(
        preferredDate: String,
        todayMidnightMillis: Long = todayMidnightMillis()
    ): Boolean {
        val eventMidnight = parseEventDateMidnightMillis(preferredDate) ?: return false
        return eventMidnight < todayMidnightMillis
    }

    fun timelinePhase(
        preferredDate: String,
        todayMidnightMillis: Long = todayMidnightMillis()
    ): Pair<BookingTimelinePhase, Int?> {
        val eventMidnight = parseEventDateMidnightMillis(preferredDate)
            ?: return BookingTimelinePhase.UNKNOWN to null
        val diffDays = ((eventMidnight - todayMidnightMillis) / 86_400_000L).toInt()
        val phase = when {
            eventMidnight < todayMidnightMillis -> BookingTimelinePhase.PASSED
            eventMidnight == todayMidnightMillis -> BookingTimelinePhase.TODAY
            else -> BookingTimelinePhase.UPCOMING
        }
        return phase to diffDays
    }

    fun findDueConfirmedBookings(
        bookings: List<ShootBooking>,
        todayMidnightMillis: Long = todayMidnightMillis()
    ): List<ShootBooking> {
        return bookings.filter { booking ->
            booking.id.isNotBlank() &&
                booking.status.trim().equals("Confirmed", ignoreCase = true) &&
                hasEventDatePassed(booking.preferredDate, todayMidnightMillis)
        }
    }

    fun summarize(
        bookings: List<ShootBooking>,
        todayMidnightMillis: Long = todayMidnightMillis()
    ): RealtimeBookingTrackerSummary {
        var upcoming = 0
        var today = 0
        var pastDue = 0
        var completed = 0
        var pending = 0
        var cancelled = 0
        val due = mutableListOf<ShootBooking>()

        for (booking in bookings) {
            when (booking.status.trim().lowercase(Locale.ROOT)) {
                "confirmed" -> {
                    val (phase, _) = timelinePhase(booking.preferredDate, todayMidnightMillis)
                    when (phase) {
                        BookingTimelinePhase.PASSED -> {
                            pastDue += 1
                            if (booking.id.isNotBlank()) due.add(booking)
                        }
                        BookingTimelinePhase.TODAY -> today += 1
                        else -> upcoming += 1
                    }
                }
                "completed" -> completed += 1
                "cancelled", "rejected" -> cancelled += 1
                else -> pending += 1
            }
        }

        return RealtimeBookingTrackerSummary(
            confirmedUpcoming = upcoming,
            confirmedToday = today,
            confirmedPastDue = pastDue,
            completed = completed,
            pending = pending,
            cancelled = cancelled,
            dueBookings = due
        )
    }
}

@Composable
fun AdminRealtimeBookingTrackerCard(
    bookings: List<ShootBooking>,
    trackerState: RealtimeBookingTrackerUiState,
    language: AppLanguage,
    onToggleAutoComplete: () -> Unit,
    onCheckNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHindi = language == AppLanguage.HINDI
    val summary = remember(bookings, trackerState.lastCheckedAtMillis) {
        AdminRealtimeBookingTrackerLogic.summarize(bookings)
    }

    val pulseTransition = rememberInfiniteTransition(label = "booking_tracker_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "booking_tracker_pulse_alpha"
    )

    val statusDotColor = when {
        !trackerState.isEnabled -> Color(0xFF9E9E9E)
        trackerState.isSyncing -> GoldPrimary
        else -> SuccessGreen
    }

    val statusLabel = when {
        !trackerState.isEnabled -> if (isHindi) "रोका गया" else "Paused"
        trackerState.isSyncing -> if (isHindi) "अपडेट हो रहा है..." else "Syncing..."
        trackerState.isLiveConnected -> if (isHindi) "लाइव ट्रैकिंग" else "Live Tracking"
        else -> if (isHindi) "सक्रिय" else "Active"
    }

    val lastCheckedText = remember(trackerState.lastCheckedAtMillis, language) {
        val ts = trackerState.lastCheckedAtMillis
        if (ts == null) {
            "—"
        } else {
            SimpleDateFormat("hh:mm:ss a", if (isHindi) Locale.forLanguageTag("hi-IN") else Locale.US).format(Date(ts))
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.55f), StudioOutline))
                ),
                RoundedCornerShape(14.dp)
            )
            .testTag("admin_realtime_booking_tracker_card"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Title + Live Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isHindi) "रीयल-टाइम बुकिंग स्थिति ट्रैकर" else "Real-Time Booking Status Tracker",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isHindi)
                                "इवेंट तिथि बीतने पर Confirmed → Completed स्वतः अपडेट"
                            else
                                "Auto-transitions past-date Confirmed events to Completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB5B5B5),
                            fontSize = 10.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusDotColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, statusDotColor.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .alpha(if (trackerState.isEnabled) pulseAlpha else 1f)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = statusLabel,
                            color = statusDotColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 KPI Counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrackerKpiChip(
                    label = if (isHindi) "आगामी" else "Upcoming",
                    value = summary.confirmedUpcoming.toString(),
                    accentColor = GoldLight,
                    modifier = Modifier.weight(1f)
                )
                TrackerKpiChip(
                    label = if (isHindi) "आज" else "Today",
                    value = summary.confirmedToday.toString(),
                    accentColor = GoldPrimary,
                    modifier = Modifier.weight(1f)
                )
                TrackerKpiChip(
                    label = if (isHindi) "तिथि बीती" else "Past Due",
                    value = summary.confirmedPastDue.toString(),
                    accentColor = if (summary.confirmedPastDue > 0) Color(0xFFFFB74D) else Color(0xFF9E9E9E),
                    modifier = Modifier.weight(1f)
                )
                TrackerKpiChip(
                    label = if (isHindi) "पूर्ण" else "Completed",
                    value = summary.completed.toString(),
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Controls Row: Toggle Auto-Complete + Check Now
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onToggleAutoComplete,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("admin_tracker_toggle_btn"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    border = BorderStroke(
                        1.dp,
                        if (trackerState.isEnabled) GoldPrimary else StudioOutline
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (trackerState.isEnabled) GoldContainer.copy(alpha = 0.35f) else Color.Transparent,
                        contentColor = if (trackerState.isEnabled) GoldLight else Color(0xFFB5B5B5)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (trackerState.isEnabled) {
                            if (isHindi) "ऑटो-पूर्ण: चालू" else "Auto-Complete: ON"
                        } else {
                            if (isHindi) "ऑटो-पूर्ण: बंद" else "Auto-Complete: OFF"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onCheckNow,
                    enabled = !trackerState.isSyncing,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("admin_tracker_check_now_btn"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GoldPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isHindi) "अभी जाँचें" else "Check Now",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Recent Auto-Completed Transitions Feed
            if (trackerState.recentTransitions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioElevatedSurface)
                        .border(1.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isHindi)
                            "हाल ही में स्वतः पूर्ण (${trackerState.totalAutoCompleted})"
                        else
                            "Auto-Completed This Session (${trackerState.totalAutoCompleted})",
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                    trackerState.recentTransitions.take(3).forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${entry.clientName.ifBlank { "Client" }} • ${entry.preferredDate}",
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${entry.fromStatus} → ${entry.toStatus}",
                                color = SuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (!trackerState.lastError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = trackerState.lastError,
                    color = Color(0xFFFF8A80),
                    fontSize = 10.5.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isHindi) "अंतिम जाँच: $lastCheckedText" else "Last checked: $lastCheckedText",
                    color = Color(0xFF9E9E9E),
                    fontSize = 10.sp
                )
                Text(
                    text = if (isHindi) "प्रत्येक 60 सेकंड में लाइव सिंक" else "Live Firestore sync • 60s check",
                    color = GoldPrimary.copy(alpha = 0.85f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun TrackerKpiChip(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(StudioElevatedSurface)
            .border(1.dp, StudioOutline, RoundedCornerShape(10.dp))
            .padding(vertical = 7.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = accentColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = Color(0xFFB5B5B5),
            fontSize = 9.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
