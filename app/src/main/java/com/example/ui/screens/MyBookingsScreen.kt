package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.ServiceReview
import com.example.data.model.ShootBooking
import com.example.notification.SessionReminderScheduler
import com.example.notification.SessionReminderUiState
import com.example.ui.components.BookingReviewStatusBlock
import com.example.ui.components.BookingsSkeletonList
import com.example.ui.components.ScrollToTopFab
import com.example.ui.components.ServiceReviewDialog
import com.example.ui.components.openWhatsApp
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen
import com.example.util.StudioContactDetails
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class BookingTimelineFilter(val id: String, val labelEn: String, val labelHi: String) {
    ALL("all", "All", "सभी"),
    UPCOMING("upcoming", "Upcoming", "आगामी"),
    PAST("past", "Past", "पिछले")
}

enum class BookingStatusFilter(val id: String, val labelEn: String, val labelHi: String) {
    ALL("all", "All Status", "सभी स्थिति"),
    PENDING("pending", "Pending", "लंबित"),
    CONFIRMED("confirmed", "Confirmed", "पुष्ट"),
    COMPLETED("completed", "Completed", "पूर्ण"),
    CANCELLED("cancelled", "Cancelled", "रद्द")
}

enum class BookingScheduleBucket {
    UPCOMING,
    PAST
}

data class PartitionedBookings(
    val upcoming: List<ShootBooking>,
    val past: List<ShootBooking>
) {
    val totalCount: Int get() = upcoming.size + past.size
}

object BookingScheduleHelper {

    private val datePatterns = listOf("yyyy-MM-dd", "yyyy/MM/dd", "dd-MM-yyyy", "dd/MM/yyyy")

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

    fun parsePreferredDateMidnightMillis(preferredDate: String): Long? {
        val raw = preferredDate.trim()
        if (raw.isEmpty()) return null
        for (pattern in datePatterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }
                val parsed = parser.parse(raw) ?: continue
                val cal = Calendar.getInstance().apply {
                    time = parsed
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            } catch (_: Exception) {
                // Try next date format
            }
        }
        return null
    }

    fun classifyBooking(
        booking: ShootBooking,
        todayMidnightMillis: Long = todayMidnightMillis()
    ): BookingScheduleBucket {
        val normalizedStatus = booking.status.trim().lowercase(Locale.ROOT)
        if (normalizedStatus == "completed" || normalizedStatus == "cancelled") {
            return BookingScheduleBucket.PAST
        }
        val parsedMillis = parsePreferredDateMidnightMillis(booking.preferredDate)
        return if (parsedMillis != null && parsedMillis < todayMidnightMillis) {
            BookingScheduleBucket.PAST
        } else {
            BookingScheduleBucket.UPCOMING
        }
    }

    fun relativeDayLabel(
        booking: ShootBooking,
        language: AppLanguage,
        todayMidnightMillis: Long = todayMidnightMillis()
    ): String {
        val isHindi = language == AppLanguage.HINDI
        val normalizedStatus = booking.status.trim().lowercase(Locale.ROOT)
        if (normalizedStatus == "cancelled") {
            return if (isHindi) "रद्द किया गया" else "Cancelled"
        }
        if (normalizedStatus == "completed") {
            return if (isHindi) "शूट पूर्ण" else "Session Completed"
        }
        val parsedMillis = parsePreferredDateMidnightMillis(booking.preferredDate)
            ?: return if (isHindi) "निर्धारित" else "Scheduled"
        val diffDays = TimeUnit.MILLISECONDS.toDays(parsedMillis - todayMidnightMillis)
        return when {
            diffDays == 0L -> if (isHindi) "आज का शूट" else "Today"
            diffDays == 1L -> if (isHindi) "कल" else "Tomorrow"
            diffDays > 1L -> if (isHindi) "$diffDays दिन शेष" else "In $diffDays days"
            diffDays == -1L -> if (isHindi) "बीता हुआ कल" else "Yesterday"
            else -> if (isHindi) "${-diffDays} दिन पहले" else "${-diffDays} days ago"
        }
    }

    fun partitionAndFilterBookings(
        bookings: List<ShootBooking>,
        statusFilter: BookingStatusFilter = BookingStatusFilter.ALL,
        searchQuery: String = "",
        todayMidnightMillis: Long = todayMidnightMillis()
    ): PartitionedBookings {
        val cleanQuery = searchQuery.trim().lowercase(Locale.ROOT)
        val filtered = bookings.filter { booking ->
            val matchesStatus = when (statusFilter) {
                BookingStatusFilter.ALL -> true
                BookingStatusFilter.PENDING -> booking.status.equals("Pending", ignoreCase = true)
                BookingStatusFilter.CONFIRMED -> booking.status.equals("Confirmed", ignoreCase = true)
                BookingStatusFilter.COMPLETED -> booking.status.equals("Completed", ignoreCase = true)
                BookingStatusFilter.CANCELLED -> booking.status.equals("Cancelled", ignoreCase = true)
            }
            val matchesQuery = cleanQuery.isEmpty() ||
                booking.serviceType.lowercase(Locale.ROOT).contains(cleanQuery) ||
                booking.clientName.lowercase(Locale.ROOT).contains(cleanQuery) ||
                booking.location.lowercase(Locale.ROOT).contains(cleanQuery) ||
                booking.preferredDate.lowercase(Locale.ROOT).contains(cleanQuery) ||
                booking.status.lowercase(Locale.ROOT).contains(cleanQuery) ||
                booking.notes.lowercase(Locale.ROOT).contains(cleanQuery)
            matchesStatus && matchesQuery
        }

        val upcoming = filtered
            .filter { classifyBooking(it, todayMidnightMillis) == BookingScheduleBucket.UPCOMING }
            .sortedWith(
                compareBy<ShootBooking> {
                    parsePreferredDateMidnightMillis(it.preferredDate) ?: Long.MAX_VALUE
                }.thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
            )

        val past = filtered
            .filter { classifyBooking(it, todayMidnightMillis) == BookingScheduleBucket.PAST }
            .sortedWith(
                compareByDescending<ShootBooking> {
                    parsePreferredDateMidnightMillis(it.preferredDate)
                        ?: it.createdAt?.toDate()?.time
                        ?: 0L
                }.thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
            )

        return PartitionedBookings(upcoming = upcoming, past = past)
    }
}

@Composable
fun MyBookingsScreen(
    language: AppLanguage,
    myBookings: List<ShootBooking>,
    myReviews: List<ServiceReview> = emptyList(),
    isLoading: Boolean = false,
    isSubmitting: Boolean = false,
    referenceTodayMillis: Long = BookingScheduleHelper.todayMidnightMillis(),
    referenceNowMillis: Long = System.currentTimeMillis(),
    onBack: () -> Unit,
    onRefresh: () -> Unit = {},
    onBookNewShoot: () -> Unit = {},
    onBookAgain: (String) -> Unit = {},
    onCancelBooking: (String) -> Unit = {},
    onSubmitReview: (
        bookingId: String,
        serviceType: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit
    ) -> Unit = { _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val isHindi = language == AppLanguage.HINDI

    var timelineFilter by remember { mutableStateOf(BookingTimelineFilter.ALL) }
    var statusFilter by remember { mutableStateOf(BookingStatusFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var bookingToCancel by remember { mutableStateOf<ShootBooking?>(null) }
    var reviewTargetBooking by remember { mutableStateOf<ShootBooking?>(null) }

    val allPartitioned = remember(myBookings, referenceTodayMillis) {
        BookingScheduleHelper.partitionAndFilterBookings(
            bookings = myBookings,
            statusFilter = BookingStatusFilter.ALL,
            searchQuery = "",
            todayMidnightMillis = referenceTodayMillis
        )
    }

    val filteredPartitioned = remember(myBookings, statusFilter, searchQuery, referenceTodayMillis) {
        BookingScheduleHelper.partitionAndFilterBookings(
            bookings = myBookings,
            statusFilter = statusFilter,
            searchQuery = searchQuery,
            todayMidnightMillis = referenceTodayMillis
        )
    }

    val confirmedCount = remember(myBookings) {
        myBookings.count { it.status.equals("Confirmed", ignoreCase = true) }
    }
    val pendingCount = remember(myBookings) {
        myBookings.count { it.status.equals("Pending", ignoreCase = true) }
    }

    val showUpcoming = timelineFilter != BookingTimelineFilter.PAST && filteredPartitioned.upcoming.isNotEmpty()
    val showPast = timelineFilter != BookingTimelineFilter.UPCOMING && filteredPartitioned.past.isNotEmpty()

    val hasFutureConfirmedReminder = remember(allPartitioned.upcoming, referenceNowMillis) {
        allPartitioned.upcoming.any { booking ->
            SessionReminderScheduler.computeReminderTriggerAtMillis(
                booking = booking,
                nowMillis = referenceNowMillis
            ) != null
        }
    }
    val canScheduleExact = remember(context, allPartitioned.upcoming) {
        SessionReminderScheduler.canScheduleExactAlarms(context)
    }

    bookingToCancel?.let { target ->
        AlertDialog(
            onDismissRequest = { bookingToCancel = null },
            modifier = Modifier.testTag("confirm_cancel_booking_dialog"),
            containerColor = StudioCardSurface,
            title = {
                Text(
                    text = if (isHindi) "बुकिंग रद्द करें?" else "Cancel Appointment?",
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
            },
            text = {
                Text(
                    text = if (isHindi)
                        "क्या आप वाकई '${target.serviceType}' (${target.preferredDate}) की बुकिंग रद्द करना चाहते हैं?"
                    else
                        "Are you sure you want to cancel your '${target.serviceType}' session scheduled on ${target.preferredDate}?",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelBooking(target.id)
                        bookingToCancel = null
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("confirm_cancel_booking_yes")
                ) {
                    Text(
                        text = if (isHindi) "हाँ, रद्द करें" else "Cancel Booking",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { bookingToCancel = null },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(
                        text = if (isHindi) "वापस जाएं" else "Keep Appointment",
                        color = GoldPrimary
                    )
                }
            }
        )
    }

    reviewTargetBooking?.let { targetBooking ->
        val existingReview = myReviews.find { it.bookingId == targetBooking.id }
        ServiceReviewDialog(
            booking = targetBooking,
            existingReview = existingReview,
            language = language,
            isSubmitting = isSubmitting,
            onDismiss = { reviewTargetBooking = null },
            onSubmitReview = { rating, comment, onSuccess ->
                onSubmitReview(
                    targetBooking.id,
                    targetBooking.serviceType,
                    rating,
                    comment
                ) {
                    onSuccess()
                    reviewTargetBooking = null
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("my_bookings_screen")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("my_bookings_list"),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. HEADER CARD WITH BACK, REFRESH, LIVE KPI STRIP & NEW SHOOT CTA
            item(key = "my_bookings_header") {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.horizontalGradient(listOf(GoldPrimary, GoldDark))
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .testTag("my_bookings_header_card"),
                    colors = CardDefaults.elevatedCardColors(containerColor = StudioCardSurface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .clip(CircleShape)
                                        .background(StudioElevatedSurface)
                                        .border(1.dp, StudioOutline, CircleShape)
                                        .testTag("my_bookings_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = if (isHindi) "वापस जाएं" else "Back",
                                        tint = GoldPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = if (isHindi) "मेरी स्टूडियो बुकिंग्स" else "My Bookings",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldPrimary
                                    )
                                    Text(
                                        text = if (isHindi)
                                            "आगामी व पिछली अपॉइंटमेंट्स • लाइव स्थिति"
                                        else
                                            "Upcoming & past studio sessions • Live sync",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(CircleShape)
                                    .background(StudioElevatedSurface)
                                    .border(1.dp, StudioOutline, CircleShape)
                                    .testTag("my_bookings_refresh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = if (isHindi) "रीफ्रेश करें" else "Refresh Bookings",
                                    tint = GoldLight
                                )
                            }
                        }

                        // KPI Summary Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BookingSummaryKpi(
                                value = "${allPartitioned.upcoming.size}",
                                label = if (isHindi) "आगामी" else "Upcoming",
                                accent = GoldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            BookingSummaryKpi(
                                value = "$confirmedCount",
                                label = if (isHindi) "पुष्ट" else "Confirmed",
                                accent = SuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                            BookingSummaryKpi(
                                value = "$pendingCount",
                                label = if (isHindi) "लंबित" else "Pending",
                                accent = GoldLight,
                                modifier = Modifier.weight(1f)
                            )
                            BookingSummaryKpi(
                                value = "${allPartitioned.past.size}",
                                label = if (isHindi) "पिछली" else "Past",
                                accent = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = onBookNewShoot,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("my_bookings_new_shoot_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "नया स्टूडियो शूट बुक करें" else "Book New Studio Session",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Optional Exact Alarm prompt banner when confirmed >24h reminders exist on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                hasFutureConfirmedReminder &&
                !canScheduleExact
            ) {
                item(key = "my_bookings_exact_alarm_banner") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 760.dp)
                            .testTag("my_bookings_exact_alarm_banner"),
                        color = GoldContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHindi)
                                        "सटीक 24-घंटे रिमाइंडर चालू है"
                                    else
                                        "24-Hour Reminder Active",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isHindi)
                                        "ठीक 24 घंटे पहले अलर्ट पाने के लिए सेटिंग्स में सटीक अलार्म की अनुमति दें।"
                                    else
                                        "Allow exact alarms in Settings for minute-accurate 24h shoot alerts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            TextButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            SessionReminderScheduler.buildExactAlarmSettingsIntent(context)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("my_bookings_allow_exact_alarm_btn")
                            ) {
                                Text(
                                    text = if (isHindi) "सेटिंग्स" else "Settings",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 2. TIMELINE SEGMENTED FILTER + STATUS CHIPS + SEARCH
            item(key = "my_bookings_filters") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Timeline Segmented Buttons: All / Upcoming / Past
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BookingTimelineFilter.values().forEach { filter ->
                            val isSelected = timelineFilter == filter
                            val count = when (filter) {
                                BookingTimelineFilter.ALL -> filteredPartitioned.totalCount
                                BookingTimelineFilter.UPCOMING -> filteredPartitioned.upcoming.size
                                BookingTimelineFilter.PAST -> filteredPartitioned.past.size
                            }
                            val label = if (isHindi) filter.labelHi else filter.labelEn
                            Surface(
                                onClick = { timelineFilter = filter },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("my_bookings_timeline_${filter.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) GoldPrimary else StudioCardSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) GoldPrimary else StudioOutline
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "$label ($count)",
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("my_bookings_search_input"),
                        placeholder = {
                            Text(
                                text = if (isHindi)
                                    "सेवा, तारीख या स्थान से खोजें..."
                                else
                                    "Search by service, date, or studio location...",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = if (isHindi) "खोजें" else "Search",
                                tint = GoldPrimary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .testTag("my_bookings_search_clear")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isHindi) "खोज साफ करें" else "Clear search",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = StudioOutline,
                            focusedContainerColor = StudioCardSurface,
                            unfocusedContainerColor = StudioCardSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Status Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BookingStatusFilter.values().forEach { sFilter ->
                            val isSelected = statusFilter == sFilter
                            val label = if (isHindi) sFilter.labelHi else sFilter.labelEn
                            FilterChip(
                                selected = isSelected,
                                onClick = { statusFilter = sFilter },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldContainer,
                                    selectedLabelColor = OnGoldContainer,
                                    selectedLeadingIconColor = GoldPrimary,
                                    containerColor = StudioCardSurface,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) GoldPrimary else StudioOutline
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("my_bookings_status_chip_${sFilter.id}")
                            )
                        }
                    }
                }
            }

            // 3. CONTENT BODY: LOADING / EMPTY / UPCOMING + PAST SECTIONS
            if (isLoading) {
                item(key = "my_bookings_skeleton") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 760.dp)
                            .testTag("my_bookings_loading_skeleton")
                    ) {
                        BookingsSkeletonList()
                    }
                }
            } else if (!showUpcoming && !showPast) {
                item(key = "my_bookings_empty") {
                    val hasAnyBookings = myBookings.isNotEmpty()
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 760.dp)
                            .border(1.dp, StudioOutline, RoundedCornerShape(20.dp))
                            .testTag("my_bookings_empty_state"),
                        colors = CardDefaults.elevatedCardColors(containerColor = StudioCardSurface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(StudioElevatedSurface)
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Text(
                                text = if (hasAnyBookings) {
                                    if (isHindi) "इस फ़िल्टर में कोई बुकिंग नहीं मिली" else "No Appointments in This Filter"
                                } else {
                                    if (isHindi) "अभी तक कोई स्टूडियो बुकिंग नहीं है" else "No Studio Appointments Yet"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Text(
                                text = if (hasAnyBookings) {
                                    if (isHindi)
                                        "अपनी सभी आगामी और पिछली बुकिंग्स देखने के लिए फ़िल्टर रीसेट करें।"
                                    else
                                        "Reset your timeline, status, or search filter to view all your studio appointments."
                                } else {
                                    if (isHindi)
                                        "तलवाड़ा खुर्द में अपना पहला फोटोग्राफी, मॉडलिंग या 4K वीडियो सत्र बुक करें।"
                                    else
                                        "Schedule your first photography, modeling, or 4K cinema shoot at Star Artist Studio."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            if (hasAnyBookings) {
                                Button(
                                    onClick = {
                                        timelineFilter = BookingTimelineFilter.ALL
                                        statusFilter = BookingStatusFilter.ALL
                                        searchQuery = ""
                                    },
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("my_bookings_reset_filters_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldPrimary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "सभी फ़िल्टर रीसेट करें" else "Show All Bookings",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onBookNewShoot,
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("my_bookings_empty_book_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldPrimary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "अभी शूट बुक करें" else "Book Your First Shoot",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // UPCOMING APPOINTMENTS SECTION
                if (showUpcoming) {
                    item(key = "upcoming_section_header") {
                        BookingSectionHeader(
                            icon = Icons.Default.Upcoming,
                            title = if (isHindi)
                                "आगामी अपॉइंटमेंट्स (${filteredPartitioned.upcoming.size})"
                            else
                                "Upcoming Appointments (${filteredPartitioned.upcoming.size})",
                            accentColor = GoldPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 760.dp)
                                .testTag("my_bookings_upcoming_header")
                        )
                    }

                    items(
                        items = filteredPartitioned.upcoming,
                        key = { "upcoming_${it.id.ifEmpty { "${it.preferredDate}_${it.serviceType}" }}" }
                    ) { booking ->
                        val existingReview = myReviews.find { it.bookingId == booking.id }
                        val reminderUiState = remember(booking, referenceNowMillis) {
                            SessionReminderScheduler.evaluateUiState(
                                context = context,
                                booking = booking,
                                nowMillis = referenceNowMillis
                            )
                        }
                        MyAppointmentCard(
                            booking = booking,
                            bucket = BookingScheduleBucket.UPCOMING,
                            relativeLabel = BookingScheduleHelper.relativeDayLabel(
                                booking = booking,
                                language = language,
                                todayMidnightMillis = referenceTodayMillis
                            ),
                            language = language,
                            existingReview = existingReview,
                            reminderUiState = reminderUiState,
                            onCancelClick = { bookingToCancel = booking },
                            onWhatsAppClick = {
                                val msg = if (isHindi) {
                                    "नमस्ते स्टार आर्टिस्ट स्टूडियो! मेरी आगामी बुकिंग (${booking.serviceType}, तारीख: ${booking.preferredDate}) की स्थिति: ${booking.status} के बारे में जानकारी चाहिए।"
                                } else {
                                    "Hello Star Artist Studio! Checking in on my upcoming ${booking.serviceType} session on ${booking.preferredDate} (Status: ${booking.status})."
                                }
                                openWhatsApp(context, StudioContactDetails.buildWhatsAppUrl(msg))
                            },
                            onBookAgainClick = { onBookAgain(booking.serviceType) },
                            onOpenReviewDialog = { reviewTargetBooking = booking }
                        )
                    }
                }

                // PAST APPOINTMENTS SECTION
                if (showPast) {
                    item(key = "past_section_header") {
                        BookingSectionHeader(
                            icon = Icons.Default.History,
                            title = if (isHindi)
                                "पिछली अपॉइंटमेंट्स (${filteredPartitioned.past.size})"
                            else
                                "Past Appointments (${filteredPartitioned.past.size})",
                            accentColor = TextSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 760.dp)
                                .padding(top = if (showUpcoming) 8.dp else 0.dp)
                                .testTag("my_bookings_past_header")
                        )
                    }

                    items(
                        items = filteredPartitioned.past,
                        key = { "past_${it.id.ifEmpty { "${it.preferredDate}_${it.serviceType}" }}" }
                    ) { booking ->
                        val existingReview = myReviews.find { it.bookingId == booking.id }
                        MyAppointmentCard(
                            booking = booking,
                            bucket = BookingScheduleBucket.PAST,
                            relativeLabel = BookingScheduleHelper.relativeDayLabel(
                                booking = booking,
                                language = language,
                                todayMidnightMillis = referenceTodayMillis
                            ),
                            language = language,
                            existingReview = existingReview,
                            onCancelClick = { bookingToCancel = booking },
                            onWhatsAppClick = {
                                val msg = if (isHindi) {
                                    "नमस्ते स्टार आर्टिस्ट स्टूडियो! मेरी बुकिंग (${booking.serviceType}, तारीख: ${booking.preferredDate}) के बारे में सहायता चाहिए।"
                                } else {
                                    "Hello Star Artist Studio! Reaching out regarding my ${booking.serviceType} appointment from ${booking.preferredDate}."
                                }
                                openWhatsApp(context, StudioContactDetails.buildWhatsAppUrl(msg))
                            },
                            onBookAgainClick = { onBookAgain(booking.serviceType) },
                            onOpenReviewDialog = { reviewTargetBooking = booking }
                        )
                    }
                }
            }

            item(key = "my_bookings_bottom_spacer") {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        ScrollToTopFab(
            listState = listState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .testTag("my_bookings_scroll_top_fab")
        )
    }
}

@Composable
private fun BookingSummaryKpi(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = StudioElevatedSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioOutline)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BookingSectionHeader(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor
        )
    }
}

@Composable
fun MyAppointmentCard(
    booking: ShootBooking,
    bucket: BookingScheduleBucket,
    relativeLabel: String,
    language: AppLanguage,
    existingReview: ServiceReview? = null,
    reminderUiState: SessionReminderUiState = SessionReminderUiState.NotApplicable,
    onCancelClick: () -> Unit = {},
    onWhatsAppClick: () -> Unit = {},
    onBookAgainClick: () -> Unit = {},
    onOpenReviewDialog: () -> Unit = {}
) {
    val isHindi = language == AppLanguage.HINDI
    val normalizedStatus = booking.status.trim().lowercase(Locale.ROOT)

    val (statusColor, statusIcon, statusLabel) = when (normalizedStatus) {
        "confirmed" -> Triple(
            SuccessGreen,
            Icons.Default.CheckCircle,
            if (isHindi) "पुष्ट (Confirmed)" else "Confirmed"
        )
        "completed" -> Triple(
            GoldPrimary,
            Icons.Default.TaskAlt,
            if (isHindi) "पूर्ण (Completed)" else "Completed"
        )
        "cancelled" -> Triple(
            ErrorRed,
            Icons.Default.Cancel,
            if (isHindi) "रद्द (Cancelled)" else "Cancelled"
        )
        else -> Triple(
            if (StudioTheme.isLight) GoldDark else GoldLight,
            Icons.Default.Schedule,
            if (isHindi) "लंबित (Pending)" else "Pending"
        )
    }

    val isUpcomingActionable = bucket == BookingScheduleBucket.UPCOMING &&
        (normalizedStatus == "pending" || normalizedStatus == "confirmed")

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .border(
                BorderStroke(
                    width = if (bucket == BookingScheduleBucket.UPCOMING) 1.5.dp else 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(statusColor.copy(alpha = 0.6f), StudioOutline)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .animateContentSize()
            .testTag("my_booking_card_${booking.id}"),
        colors = CardDefaults.elevatedCardColors(containerColor = StudioCardSurface),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (bucket == BookingScheduleBucket.UPCOMING) 4.dp else 1.dp
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Header: Upcoming/Past + Relative Day Pill & Color-Coded Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (bucket == BookingScheduleBucket.UPCOMING) GoldContainer else StudioElevatedSurface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(
                            0.5.dp,
                            if (bucket == BookingScheduleBucket.UPCOMING) GoldPrimary else StudioOutline
                        )
                    ) {
                        Text(
                            text = if (bucket == BookingScheduleBucket.UPCOMING) {
                                if (isHindi) "आगामी" else "UPCOMING"
                            } else {
                                if (isHindi) "पिछला" else "PAST"
                            },
                            color = if (bucket == BookingScheduleBucket.UPCOMING) OnGoldContainer else TextSecondary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = "• $relativeLabel",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (bucket == BookingScheduleBucket.UPCOMING) GoldLight else TextSecondary
                    )
                }

                // Color-Coded Status Indicator Badge
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, statusColor),
                    modifier = Modifier
                        .semantics(mergeDescendants = true) {}
                        .testTag("my_booking_status_${booking.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Service Type Title
            Text(
                text = booking.serviceType,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = GoldLight
            )

            // Date, Time & Location Detail Surface
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = StudioElevatedSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, StudioOutline)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = booking.preferredDate.ifBlank { "TBD" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = GoldLight,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = booking.preferredTime.ifBlank { "Flexible" },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldLight
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = booking.location.ifBlank { "Studio (Talwara Khurd)" },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    HorizontalDivider(color = StudioOutline.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = booking.clientName,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = booking.contactNumber,
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Optional Notes
            if (booking.notes.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = GoldContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, StudioOutline)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.EventNote,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = booking.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Lifecycle Stepper for Upcoming Pending/Confirmed bookings
            if (isUpcomingActionable) {
                AppointmentLifecycleTracker(
                    isConfirmed = normalizedStatus == "confirmed",
                    isHindi = isHindi
                )
            }

            // 24-Hour AlarmManager Reminder Status Badge for Confirmed Upcoming Bookings
            if (bucket == BookingScheduleBucket.UPCOMING && reminderUiState !is SessionReminderUiState.NotApplicable) {
                val reminderText = when (reminderUiState) {
                    is SessionReminderUiState.Scheduled -> {
                        val formattedTrigger = remember(reminderUiState.triggerAtMillis) {
                            SimpleDateFormat("MMM dd, hh:mm a", Locale.US)
                                .format(java.util.Date(reminderUiState.triggerAtMillis))
                        }
                        if (isHindi) {
                            "24 घंटे पहले रिमाइंडर सेट • $formattedTrigger"
                        } else {
                            "24h reminder scheduled • $formattedTrigger"
                        }
                    }
                    is SessionReminderUiState.Within24Hours -> {
                        if (isHindi) {
                            "आपका पुष्ट शूट 24 घंटे के भीतर है"
                        } else {
                            "Session starts within 24 hours"
                        }
                    }
                    SessionReminderUiState.NotApplicable -> ""
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {}
                        .testTag("my_booking_reminder_${booking.id}"),
                    color = SuccessGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.8.dp, SuccessGreen.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = reminderText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SuccessGreen
                        )
                    }
                }
            }

            // Review block for Completed bookings
            BookingReviewStatusBlock(
                booking = booking,
                existingReview = existingReview,
                language = language,
                onOpenReviewDialog = onOpenReviewDialog
            )

            // Action Buttons (all >= 48.dp touch targets)
            if (isUpcomingActionable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onWhatsAppClick,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("my_booking_whatsapp_${booking.id}"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
                        border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isHindi) "व्हाट्सऐप" else "WhatsApp",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("my_booking_cancel_${booking.id}"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isHindi) "रद्द करें" else "Cancel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Button(
                    onClick = onBookAgainClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("my_booking_rebook_${booking.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "यह सेवा फिर बुक करें" else "Book This Service Again",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentLifecycleTracker(
    isConfirmed: Boolean,
    isHindi: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LifecycleStepDot(
            label = if (isHindi) "अनुरोध" else "Requested",
            completed = true,
            activeColor = GoldPrimary
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 2.dp,
            color = GoldPrimary
        )
        LifecycleStepDot(
            label = if (isHindi) "समीक्षा / पुष्टि" else "Confirmed",
            completed = isConfirmed,
            activeColor = SuccessGreen
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 2.dp,
            color = if (isConfirmed) SuccessGreen.copy(alpha = 0.5f) else StudioOutline
        )
        LifecycleStepDot(
            label = if (isHindi) "शूट दिवस" else "Shoot Day",
            completed = false,
            activeColor = GoldLight
        )
    }
}

@Composable
private fun LifecycleStepDot(
    label: String,
    completed: Boolean,
    activeColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (completed) activeColor else StudioElevatedSurface)
                .border(1.dp, if (completed) activeColor else StudioOutline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal,
            color = if (completed) activeColor else TextSecondary
        )
    }
}
