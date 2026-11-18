package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.ShootBooking
import com.example.ui.theme.DarkGoldPrimary
import com.example.ui.theme.DarkStudioBlack
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.StudioStrings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Filter time range for Visual Analytics.
 */
enum class AnalyticsPeriod(val days: Int, val labelEn: String, val labelHi: String) {
    SEVEN_DAYS(7, "7 Days", "7 दिन"),
    THIRTY_DAYS(30, "30 Days", "30 दिन"),
    ALL_TIME(90, "All Time", "सभी समय")
}

/**
 * Data point for time-series booking trends area chart.
 */
data class BookingTrendDataPoint(
    val dayIndex: Int,
    val dateLabel: String,
    val fullDateStr: String,
    val totalBookings: Int,
    val confirmedBookings: Int,
    val pendingBookings: Int
)

/**
 * Categorical metric for popular service requests.
 */
data class ServiceAnalyticsItem(
    val serviceName: String,
    val serviceNameHi: String,
    val category: String,
    val requestCount: Int,
    val percentage: Float,
    val accentColor: Color,
    val growthTag: String,
    val primaryBay: String,
    val avgSessionHours: String
)

/**
 * Executive KPI summary for analytics console.
 */
data class AnalyticsKpis(
    val totalBookingsTracked: Int,
    val confirmationRatePercent: Int,
    val peakActivityDay: String,
    val topRequestedService: String
)

/**
 * Aggregated booking volume for a single studio service type over the last 30 days.
 */
data class ServiceLast30DaysMetric(
    val serviceKey: String,
    val labelEn: String,
    val labelHi: String,
    val totalCount: Int,
    val pendingCount: Int,
    val confirmedCount: Int,
    val completedCount: Int,
    val cancelledCount: Int,
    val accentColor: Color
)

/**
 * Computes business analytics from real client bookings with realistic studio operational baselines.
 */
object StudioAnalyticsEngine {

    fun generateTrendPoints(
        bookings: List<ShootBooking>,
        period: AnalyticsPeriod,
        confirmedOnly: Boolean = false,
        includeStudioBaseline: Boolean = true,
        referenceDate: Date = Date()
    ): List<BookingTrendDataPoint> {
        val count = when (period) {
            AnalyticsPeriod.SEVEN_DAYS -> 7
            AnalyticsPeriod.THIRTY_DAYS -> 10 // Grouped into 3-day buckets for clean Recharts display
            AnalyticsPeriod.ALL_TIME -> 12    // Grouped into weekly buckets
        }
        val stepDays = when (period) {
            AnalyticsPeriod.SEVEN_DAYS -> 1
            AnalyticsPeriod.THIRTY_DAYS -> 3
            AnalyticsPeriod.ALL_TIME -> 7
        }

        // Realistic seed baseline distribution for Star Artist Studio in Ellenabad/Sirsa
        val basePattern = when (period) {
            AnalyticsPeriod.SEVEN_DAYS -> listOf(3, 4, 2, 5, 7, 9, 6)
            AnalyticsPeriod.THIRTY_DAYS -> listOf(4, 6, 8, 5, 9, 12, 10, 14, 11, 15)
            AnalyticsPeriod.ALL_TIME -> listOf(8, 12, 10, 15, 14, 18, 22, 19, 25, 23, 28, 31)
        }

        val endCal = Calendar.getInstance().apply {
            time = referenceDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val windowEndMs = endCal.timeInMillis
        val bucketSpanMs = stepDays.toLong() * 24L * 60L * 60L * 1000L
        val windowStartMs = windowEndMs - (count.toLong() * bucketSpanMs) + 1L

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM d", Locale.ENGLISH)
        val fullDateFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.ENGLISH)

        // Tally real bookings from Firestore into their true chronological bucket when timestamps exist
        val realBookingsByBucket = mutableMapOf<Int, MutableList<ShootBooking>>()
        bookings.forEachIndexed { index, booking ->
            val ts = resolveBookingTimestampMs(booking)
            val bucket = if (ts != null) {
                if (ts < windowStartMs || ts > windowEndMs) {
                    if (!includeStudioBaseline) -1 else index % count
                } else {
                    (((ts - windowStartMs) / bucketSpanMs).toInt()).coerceIn(0, count - 1)
                }
            } else {
                if (!includeStudioBaseline) count - 1 else index % count
            }
            if (bucket in 0 until count) {
                realBookingsByBucket.getOrPut(bucket) { mutableListOf() }.add(booking)
            }
        }

        return (0 until count).map { i ->
            calendar.time = referenceDate
            val dayOffset = (count - 1 - i) * stepDays
            calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)
            val dateLabel = dateFormat.format(calendar.time)
            val fullDateStr = fullDateFormat.format(calendar.time)

            val bucketBookings = realBookingsByBucket[i] ?: emptyList()

            if (!includeStudioBaseline) {
                val activeBookings = bucketBookings.filter {
                    val s = it.status.trim().lowercase()
                    s != "cancelled" && s != "canceled" && s != "rejected"
                }
                val realConfirmed = activeBookings.count {
                    val s = it.status.trim().lowercase()
                    s == "confirmed" || s == "approved" || s == "completed"
                }
                val realPending = (activeBookings.size - realConfirmed).coerceAtLeast(0)
                val finalConfirmed = realConfirmed
                val finalPending = if (confirmedOnly) 0 else realPending
                BookingTrendDataPoint(
                    dayIndex = i,
                    dateLabel = dateLabel,
                    fullDateStr = fullDateStr,
                    totalBookings = finalConfirmed + finalPending,
                    confirmedBookings = finalConfirmed,
                    pendingBookings = finalPending
                )
            } else {
                val realCount = bucketBookings.size
                val base = basePattern.getOrElse(i) { 5 }
                val total = base + realCount

                val confirmed = if (confirmedOnly) total else {
                    val realConfirmed = bucketBookings.count {
                        val s = it.status.trim().lowercase()
                        s == "confirmed" || s == "approved" || s == "completed"
                    }
                    (total * 0.72f).toInt().coerceAtLeast(realConfirmed)
                }
                val pending = if (confirmedOnly) 0 else (total - confirmed).coerceAtLeast(0)

                BookingTrendDataPoint(
                    dayIndex = i,
                    dateLabel = dateLabel,
                    fullDateStr = fullDateStr,
                    totalBookings = if (confirmedOnly) confirmed else total,
                    confirmedBookings = confirmed,
                    pendingBookings = pending
                )
            }
        }
    }

    fun computePopularServices(
        bookings: List<ShootBooking>,
        includeStudioBaseline: Boolean = true
    ): List<ServiceAnalyticsItem> {
        // Base catalog of Star Artist Studio services
        val catalog = listOf(
            Triple("Fashion Portfolio Photography", "फैशन पोर्टफोलियो फोटोग्राफी", "Photography"),
            Triple("4K Cinema Video Production", "4K सिनेमा वीडियो प्रोडक्शन", "Cinema & Reels"),
            Triple("Model Grooming & Management", "मॉडल ग्रूमिंग व लॉन्चिंग", "Talent Management"),
            Triple("Royal Pre-Wedding & Bridal", "रॉयल प्री-वेडिंग व ब्राइडल", "Wedding & Stills"),
            Triple("Commercial Brand Shoot", "कमर्शियल ब्रांड शूट", "Commercial")
        )

        // Count real Firestore bookings matching studio service types
        val counts = mutableMapOf<String, Int>()
        catalog.forEach { counts[it.first] = 0 }

        bookings.forEach { booking ->
            val raw = booking.serviceType.trim().lowercase()
            val matchedName = when {
                raw.contains("wedding") || raw.contains("bridal") || raw.contains("pre-wedding") ->
                    "Royal Pre-Wedding & Bridal"
                raw.contains("commercial") || raw.contains("brand") || raw.contains("product") ->
                    "Commercial Brand Shoot"
                raw.contains("model") || raw.contains("groom") || raw.contains("talent") ->
                    "Model Grooming & Management"
                raw.contains("video") || raw.contains("cinema") || raw.contains("reel") || raw.contains("film") ->
                    "4K Cinema Video Production"
                raw.contains("photo") || raw.contains("fashion") || raw.contains("portfolio") || raw.contains("portrait") || raw.contains("editorial") ->
                    "Fashion Portfolio Photography"
                else -> when (canonicalBookingServiceBucket(booking.serviceType)) {
                    "Modeling" -> "Model Grooming & Management"
                    "Video Production" -> "4K Cinema Video Production"
                    "Photography" -> "Fashion Portfolio Photography"
                    else -> "Commercial Brand Shoot"
                }
            }
            counts[matchedName] = (counts[matchedName] ?: 0) + 1
        }

        // Add studio operational baseline volume only when requested
        val baseVolumes = mapOf(
            "Fashion Portfolio Photography" to 28,
            "4K Cinema Video Production" to 22,
            "Model Grooming & Management" to 19,
            "Royal Pre-Wedding & Bridal" to 15,
            "Commercial Brand Shoot" to 11
        )

        val totalVolume = catalog.sumOf {
            (counts[it.first] ?: 0) + if (includeStudioBaseline) (baseVolumes[it.first] ?: 10) else 0
        }

        val palette = listOf(
            Color(0xFFD4AF37), // Studio Gold Primary
            Color(0xFFE5A93C), // Amber Gold
            Color(0xFF38BDF8), // Studio Cinema Cyan
            Color(0xFFA57D12), // Deep Gold
            Color(0xFF9C7A2C)  // Antique Bronze
        )

        val growthTags = listOf("+28% Top", "+22% Hot", "+15% Rising", "+12% Bridal", "+9% Brand")
        val bays = listOf("Bay A (Strobe Suite)", "Bay C (4K Cinema Floor)", "Talent Academy Bay", "Outdoor Set / Royal Hall", "Studio Cyclorama")
        val hours = listOf("3.5 hrs", "6.0 hrs", "4.0 hrs", "5.5 hrs", "4.5 hrs")

        return catalog.mapIndexed { idx, (en, hi, cat) ->
            val count = (counts[en] ?: 0) + if (includeStudioBaseline) (baseVolumes[en] ?: 10) else 0
            val percent = if (totalVolume > 0) (count.toFloat() / totalVolume * 100f) else 0f
            ServiceAnalyticsItem(
                serviceName = en,
                serviceNameHi = hi,
                category = cat,
                requestCount = count,
                percentage = percent,
                accentColor = palette.getOrElse(idx) { Color(0xFFD4AF37) },
                growthTag = growthTags.getOrElse(idx) { "+10%" },
                primaryBay = bays.getOrElse(idx) { "Studio Floor" },
                avgSessionHours = hours.getOrElse(idx) { "3 hrs" }
            )
        }.sortedByDescending { it.requestCount }
    }

    fun computeKpis(
        bookings: List<ShootBooking>,
        trendPoints: List<BookingTrendDataPoint>,
        popularServices: List<ServiceAnalyticsItem>,
        includeStudioBaseline: Boolean = true
    ): AnalyticsKpis {
        val totalTracked = if (includeStudioBaseline) {
            trendPoints.sumOf { it.totalBookings }
        } else {
            bookings.size
        }
        val confirmedTotal = if (includeStudioBaseline) {
            trendPoints.sumOf { it.confirmedBookings }
        } else {
            bookings.count {
                val s = it.status.trim().lowercase()
                s == "confirmed" || s == "approved" || s == "completed"
            }
        }
        val confRate = if (totalTracked > 0) ((confirmedTotal.toFloat() / totalTracked) * 100).toInt().coerceIn(0, 100) else 0
        val topService = if (includeStudioBaseline) {
            popularServices.firstOrNull()?.serviceName ?: "Fashion Portfolio"
        } else {
            popularServices.firstOrNull { it.requestCount > 0 }?.serviceName ?: "No Bookings Yet"
        }

        val peakDay = if (includeStudioBaseline) {
            "Saturday & Sunday"
        } else {
            val dayFmt = SimpleDateFormat("EEE", Locale.ENGLISH)
            val busiest = bookings
                .mapNotNull { resolveBookingTimestampMs(it) }
                .groupingBy { dayFmt.format(Date(it)) }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
            busiest ?: "—"
        }

        return AnalyticsKpis(
            totalBookingsTracked = totalTracked,
            confirmationRatePercent = confRate,
            peakActivityDay = peakDay,
            topRequestedService = topService
        )
    }

    /**
     * Parses a booking's Firestore createdAt timestamp or preferredDate string into epoch millis.
     */
    fun resolveBookingTimestampMs(booking: ShootBooking): Long? {
        booking.createdAt?.toDate()?.time?.let { return it }

        val dateStr = booking.preferredDate.trim()
        if (dateStr.isEmpty()) return null

        val patterns = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "MMM dd, yyyy",
            "MMM d, yyyy",
            "yyyy/MM/dd"
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                    isLenient = false
                }.parse(dateStr)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    /**
     * Computes actual booking counts per studio service type over the last 30 days
     * relative to [referenceDate] (inclusive).
     */
    fun computeServiceBookingsLast30Days(
        bookings: List<ShootBooking>,
        referenceDate: Date = Date()
    ): List<ServiceLast30DaysMetric> {
        val nowMs = referenceDate.time
        val thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L
        val windowStartMs = nowMs - thirtyDaysMs
        val windowEndMs = nowMs + 24L * 60L * 60L * 1000L // allow slight client/server clock skew within 24h

        val recentBookings = bookings.filter { booking ->
            val ts = resolveBookingTimestampMs(booking) ?: return@filter false
            ts in windowStartMs..windowEndMs
        }

        val grouped = recentBookings.groupBy { canonicalBookingServiceBucket(it.serviceType) }

        val descriptors = listOf(
            Triple("Modeling", "मॉडलिंग", Color(0xFFFBBF24)),
            Triple("Photography", "फ़ोटोग्राफ़ी", Color(0xFFD4AF37)),
            Triple("Video Production", "वीडियो प्रोडक्शन", Color(0xFF38BDF8))
        )

        val metrics = descriptors.map { (nameEn, nameHi, color) ->
            val bucketBookings = grouped[nameEn].orEmpty()
            buildService30DaysMetric(
                serviceKey = nameEn.lowercase().replace(" ", "_"),
                labelEn = nameEn,
                labelHi = nameHi,
                accentColor = color,
                bucketBookings = bucketBookings
            )
        }.toMutableList()

        val otherBookings = grouped["Other"].orEmpty()
        if (otherBookings.isNotEmpty()) {
            metrics.add(
                buildService30DaysMetric(
                    serviceKey = "other",
                    labelEn = "Other",
                    labelHi = "अन्य",
                    accentColor = Color(0xFF9CA3AF),
                    bucketBookings = otherBookings
                )
            )
        }

        return metrics
    }

    private fun buildService30DaysMetric(
        serviceKey: String,
        labelEn: String,
        labelHi: String,
        accentColor: Color,
        bucketBookings: List<ShootBooking>
    ): ServiceLast30DaysMetric {
        val pending = bucketBookings.count { isPendingBookingStatus(it.status) }
        val confirmed = bucketBookings.count {
            val s = it.status.trim().lowercase()
            s == "confirmed" || s == "approved"
        }
        val completed = bucketBookings.count { it.status.trim().equals("completed", ignoreCase = true) }
        val cancelled = bucketBookings.count {
            val s = it.status.trim().lowercase()
            s == "cancelled" || s == "canceled" || s == "rejected"
        }
        return ServiceLast30DaysMetric(
            serviceKey = serviceKey,
            labelEn = labelEn,
            labelHi = labelHi,
            totalCount = bucketBookings.size,
            pendingCount = pending,
            confirmedCount = confirmed,
            completedCount = completed,
            cancelledCount = cancelled,
            accentColor = accentColor
        )
    }
}

/**
 * Visual Analytics Dashboard with Recharts-inspired Interactive Area & Bar Charts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioVisualAnalyticsDashboard(
    bookings: List<ShootBooking>,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    includeStudioBaseline: Boolean = true,
    onBookService: ((String) -> Unit)? = null
) {
    val s = StudioStrings(language)
    val isLight = StudioTheme.isLight

    var selectedPeriod by remember { mutableStateOf(AnalyticsPeriod.SEVEN_DAYS) }
    var confirmedOnly by remember { mutableStateOf(false) }
    var selectedTrendPoint by remember { mutableStateOf<BookingTrendDataPoint?>(null) }
    var selectedService by remember { mutableStateOf<ServiceAnalyticsItem?>(null) }

    // Aggregate trends and metrics
    val trendPoints = remember(bookings, selectedPeriod, confirmedOnly, includeStudioBaseline) {
        StudioAnalyticsEngine.generateTrendPoints(
            bookings = bookings,
            period = selectedPeriod,
            confirmedOnly = confirmedOnly,
            includeStudioBaseline = includeStudioBaseline
        )
    }

    val popularServices = remember(bookings, includeStudioBaseline) {
        StudioAnalyticsEngine.computePopularServices(
            bookings = bookings,
            includeStudioBaseline = includeStudioBaseline
        )
    }

    val kpis = remember(bookings, trendPoints, popularServices, includeStudioBaseline) {
        StudioAnalyticsEngine.computeKpis(
            bookings = bookings,
            trendPoints = trendPoints,
            popularServices = popularServices,
            includeStudioBaseline = includeStudioBaseline
        )
    }

    val last30DaysServiceMetrics = remember(bookings) {
        StudioAnalyticsEngine.computeServiceBookingsLast30Days(bookings)
    }
    var selected30DayServiceKey by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioBlack)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("studio_visual_analytics_dashboard")
    ) {

        // =====================================================================
        // 1. RECHARTS HEADER BANNER & TIME PERIOD SELECTOR
        // =====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(GoldPrimary, StudioOutline, GoldPrimary.copy(alpha = 0.5f)))),
                    RoundedCornerShape(16.dp)
                )
                .testTag("analytics_header_card"),
            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldContainer)
                                .border(1.dp, GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Visual Analytics",
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = s.analyticsHeaderTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldLight
                            )
                            Text(
                                text = if (includeStudioBaseline) {
                                    "Recharts Engine • Firestore + Benchmark"
                                } else {
                                    "Recharts Engine • Live Firestore Telemetry"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    // Recharts Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioElevatedSurface)
                            .border(0.8.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "RECHARTS",
                            color = GoldPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Time Period Filter Pill Buttons (Recharts Button Group Style, 48dp accessible touch targets)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsPeriod.values().forEach { period ->
                        val isSelected = selectedPeriod == period
                        Surface(
                            onClick = {
                                selectedPeriod = period
                                selectedTrendPoint = null
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("period_chip_${period.name.lowercase()}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) GoldPrimary else StudioElevatedSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) GoldLight else StudioOutline
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) period.labelHi else period.labelEn,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else TextPrimary
                                )
                            }
                        }
                    }

                    // Confirmed Only Toggle Chip
                    Surface(
                        onClick = { confirmedOnly = !confirmedOnly },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("filter_confirmed_only_chip"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (confirmedOnly) {
                            if (isLight) Color(0xFFE8F5E9) else Color(0xFF1E3A2F)
                        } else {
                            StudioElevatedSurface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (confirmedOnly) SuccessGreen else StudioOutline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (confirmedOnly) Icons.Default.CheckCircle else Icons.Default.FilterList,
                                contentDescription = null,
                                tint = if (confirmedOnly) SuccessGreen else GoldPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI) "स्वीकृत" else "Confirmed",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (confirmedOnly) SuccessGreen else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =====================================================================
        // 2. EXECUTIVE KPI CARDS (Recharts Metrics Tiles)
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // KPI 1: Total Bookings Tracked
            AnalyticsKpiCard(
                title = if (language == AppLanguage.HINDI) "कुल बुकिंग्स" else "Total Bookings",
                value = "${kpis.totalBookingsTracked}",
                subtext = if (includeStudioBaseline) {
                    if (language == AppLanguage.HINDI) "स्टूडियो ट्रैक किया गया" else "Tracked volume"
                } else {
                    if (language == AppLanguage.HINDI) "लाइव Firestore" else "Live Firestore"
                },
                icon = Icons.Default.ShowChart,
                modifier = Modifier.weight(1f),
                testTag = "kpi_total_bookings"
            )

            // KPI 2: Confirmation Rate
            AnalyticsKpiCard(
                title = if (language == AppLanguage.HINDI) "स्वीकृति दर" else "Confirm Rate",
                value = "${kpis.confirmationRatePercent}%",
                subtext = if (kpis.confirmationRatePercent >= 50) {
                    if (language == AppLanguage.HINDI) "मजबूत दर" else "Healthy rate"
                } else {
                    if (language == AppLanguage.HINDI) "सक्रिय अनुपात" else "Active ratio"
                },
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f),
                accentColor = SuccessGreen,
                testTag = "kpi_confirmation_rate"
            )

            // KPI 3: Peak Booking Days
            AnalyticsKpiCard(
                title = if (language == AppLanguage.HINDI) "चरम दिन" else "Peak Days",
                value = if (kpis.peakActivityDay == "Saturday & Sunday") "Sat / Sun" else kpis.peakActivityDay,
                subtext = if (includeStudioBaseline) "Weekend rush" else "Busiest day",
                icon = Icons.Default.CalendarToday,
                modifier = Modifier.weight(1f),
                testTag = "kpi_peak_days"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =====================================================================
        // 2B. LAST 30 DAYS BOOKINGS PER SERVICE TYPE (NATIVE COMPOSE BAR CHART)
        // =====================================================================
        ServiceBookings30DaysChartCard(
            metrics = last30DaysServiceMetrics,
            language = language,
            selectedServiceKey = selected30DayServiceKey,
            onSelectService = { key ->
                selected30DayServiceKey = if (selected30DayServiceKey == key) null else key
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // =====================================================================
        // 3. RECHARTS AREA CHART: BOOKING TRENDS OVER TIME
        // =====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, StudioOutline), RoundedCornerShape(16.dp))
                .testTag("recharts_booking_trends_card"),
            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Section Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(GoldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = s.bookingTrendsTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI)
                                    "दैनिक व साप्ताहिक आरक्षण प्रवाह (टैप करें विवरण हेतु)"
                                else
                                    "Interactive session flow (Tap points for Recharts tooltip)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioElevatedSurface)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AREA CHART",
                            color = GoldLight,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Recharts-Style Interactive Canvas Area Chart
                RechartsAreaTrendChart(
                    dataPoints = trendPoints,
                    selectedPoint = selectedTrendPoint,
                    onSelectPoint = { point -> selectedTrendPoint = point },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .testTag("recharts_area_trend_canvas")
                )

                // Interactive Recharts Tooltip Overlay
                selectedTrendPoint?.let { point ->
                    Spacer(modifier = Modifier.height(10.dp))
                    RechartsTooltipCard(
                        point = point,
                        language = language,
                        onDismiss = { selectedTrendPoint = null }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Chart Legend Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "स्वीकृत शूट्स" else "Confirmed Shoots",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(TextSecondary)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "लंबित अनुरोध" else "Pending Inquiries",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =====================================================================
        // 4. RECHARTS BAR CHART: POPULAR SERVICE REQUESTS
        // =====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, StudioOutline), RoundedCornerShape(16.dp))
                .testTag("recharts_popular_services_card"),
            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(GoldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = s.popularServicesTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI)
                                    "पैकेज मांग व प्रतिशत अनुपात"
                                else
                                    "Client package volume & demand share",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioElevatedSurface)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BAR CHART",
                            color = GoldLight,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Recharts Horizontal Bar Chart Rows
                popularServices.forEach { service ->
                    RechartsServiceBarItem(
                        item = service,
                        language = language,
                        isSelected = selectedService == service,
                        onSelect = {
                            selectedService = if (selectedService == service) null else service
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Interactive Selected Service Detail Card
                selectedService?.let { service ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, GoldPrimary, RoundedCornerShape(12.dp))
                            .testTag("selected_service_detail_card"),
                        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) service.serviceNameHi else service.serviceName,
                                    color = GoldLight,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = service.growthTag,
                                    color = SuccessGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Primary Bay: ${service.primaryBay}",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "Avg Duration: ${service.avgSessionHours}",
                                    color = GoldPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (onBookService != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    onClick = { onBookService(service.serviceName) },
                                    color = GoldPrimary,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("analytics_book_selected_service_button")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "यह सेवा बुक करें" else "Book ${service.category}",
                                            color = MaterialTheme.colorScheme.onPrimary,
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

        Spacer(modifier = Modifier.height(14.dp))

        // =====================================================================
        // 5. RECHARTS RADIAL / DONUT CATEGORY MIX
        // =====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, StudioOutline), RoundedCornerShape(16.dp))
                .testTag("recharts_service_distribution_card"),
            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(GoldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.serviceDistributionTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Recharts Donut Ring Canvas
                    RechartsDonutChart(
                        services = popularServices,
                        modifier = Modifier
                            .size(110.dp)
                            .testTag("recharts_donut_canvas")
                    )

                    // Recharts Donut Legend
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        popularServices.take(4).forEach { item ->
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
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(item.accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.category,
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${item.percentage.toInt()}%",
                                    color = GoldLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts Metric KPI Card with gold accent.
 */
@Composable
private fun AnalyticsKpiCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = GoldPrimary,
    testTag: String = ""
) {
    Card(
        modifier = modifier
            .border(0.8.dp, StudioOutline, RoundedCornerShape(12.dp))
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = GoldLight,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = if (accentColor == SuccessGreen) SuccessGreen else TextSecondary,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Recharts-style Area Trend Chart with smooth cubic Bezier curve, gradient fill,
 * and touch scrubbing for interactive tooltips.
 */
@Composable
private fun RechartsAreaTrendChart(
    dataPoints: List<BookingTrendDataPoint>,
    selectedPoint: BookingTrendDataPoint?,
    onSelectPoint: (BookingTrendDataPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) return

    val maxVal = remember(dataPoints) { (dataPoints.maxOfOrNull { it.totalBookings } ?: 6).coerceAtLeast(6) }
    val isLight = StudioTheme.isLight
    val lineColor = GoldPrimary
    val haloColor = GoldLight
    val dotCenterColor = StudioCardSurface
    val gridColor = if (isLight) Color(0xFFE2DAC8) else Color(0xFF262116)

    BoxWithConstraints(modifier = modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val bottomMargin = 40f
        val topMargin = 20f
        val leftMargin = 30f
        val rightMargin = 20f

        val plotW = w - leftMargin - rightMargin
        val plotH = h - topMargin - bottomMargin

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dataPoints) {
                    detectTapGestures { offset ->
                        val divisor = (plotW / (dataPoints.size - 1).coerceAtLeast(1)).coerceAtLeast(1f)
                        val index = ((offset.x - leftMargin) / divisor)
                            .toInt()
                            .coerceIn(0, dataPoints.size - 1)
                        onSelectPoint(dataPoints[index])
                    }
                }
                .pointerInput(dataPoints) {
                    detectDragGestures { change, _ ->
                        val divisor = (plotW / (dataPoints.size - 1).coerceAtLeast(1)).coerceAtLeast(1f)
                        val index = ((change.position.x - leftMargin) / divisor)
                            .toInt()
                            .coerceIn(0, dataPoints.size - 1)
                        onSelectPoint(dataPoints[index])
                    }
                }
        ) {
            // 1. Horizontal dashed grid lines (Recharts Grid)
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = topMargin + (plotH * (i / gridLines.toFloat()))
                drawLine(
                    color = gridColor,
                    start = Offset(leftMargin, y),
                    end = Offset(w - rightMargin, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            }

            // 2. Compute coordinates of points
            val stepX = plotW / (dataPoints.size - 1).coerceAtLeast(1)
            val points = dataPoints.mapIndexed { idx, dp ->
                val x = leftMargin + (idx * stepX)
                val normalizedY = (dp.totalBookings.toFloat() / maxVal).coerceIn(0f, 1f)
                val y = topMargin + plotH - (normalizedY * plotH)
                Offset(x, y)
            }

            // 3. Build Smooth Bezier Path (Area & Line)
            val strokePath = Path()
            val fillPath = Path()

            if (points.isNotEmpty()) {
                strokePath.moveTo(points.first().x, points.first().y)
                fillPath.moveTo(points.first().x, topMargin + plotH)
                fillPath.lineTo(points.first().x, points.first().y)

                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val controlX = (p0.x + p1.x) / 2f
                    strokePath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                    fillPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                }

                fillPath.lineTo(points.last().x, topMargin + plotH)
                fillPath.close()

                // Draw Gradient Fill (Recharts Area)
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.42f),
                            lineColor.copy(alpha = 0.12f),
                            lineColor.copy(alpha = 0f)
                        ),
                        startY = topMargin,
                        endY = topMargin + plotH
                    )
                )

                // Draw Stroke Curve Line
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // 4. Draw Circular Milestone Nodes
                points.forEachIndexed { idx, pt ->
                    val isPointSelected = selectedPoint == dataPoints[idx]

                    if (isPointSelected) {
                        // Vertical Cursor Hairline
                        drawLine(
                            color = haloColor,
                            start = Offset(pt.x, topMargin),
                            end = Offset(pt.x, topMargin + plotH),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                        // Glowing Outer Halo
                        drawCircle(
                            color = lineColor.copy(alpha = 0.35f),
                            radius = 12f,
                            center = pt
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 7f,
                            center = pt
                        )
                        drawCircle(
                            color = dotCenterColor,
                            radius = 3.5f,
                            center = pt
                        )
                    } else {
                        drawCircle(
                            color = dotCenterColor,
                            radius = 5f,
                            center = pt
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 5f,
                            center = pt,
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }
        }

        // X-Axis Date Labels Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelStep = if (dataPoints.size > 8) 2 else 1
            dataPoints.forEachIndexed { index, dp ->
                if (index % labelStep == 0 || index == dataPoints.size - 1) {
                    Text(
                        text = dp.dateLabel,
                        color = if (selectedPoint == dp) GoldPrimary else TextSecondary,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (selectedPoint == dp) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Floating Recharts-style Tooltip Card showing date, total volume, and status breakdown.
 */
@Composable
private fun RechartsTooltipCard(
    point: BookingTrendDataPoint,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, GoldPrimary, RoundedCornerShape(10.dp))
            .clickable { onDismiss() }
            .testTag("recharts_tooltip_card"),
        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = point.fullDateStr,
                    color = GoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Confirmed: ${point.confirmedBookings}",
                        color = SuccessGreen,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pending: ${point.pendingBookings}",
                        color = TextSecondary,
                        fontSize = 9.5.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(GoldPrimary)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${point.totalBookings} SHOOTS",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * Single service row in the Recharts popular services bar chart.
 */
@Composable
private fun RechartsServiceBarItem(
    item: ServiceAnalyticsItem,
    language: AppLanguage,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val isLight = StudioTheme.isLight
    val animatedProgress by animateFloatAsState(
        targetValue = (item.percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "bar_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) StudioElevatedSurface else Color.Transparent)
            .clickable { onSelect() }
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag("service_bar_${item.category.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (language == AppLanguage.HINDI) item.serviceNameHi else item.serviceName,
                color = if (isSelected) GoldLight else TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${item.requestCount} requests",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${item.percentage.toInt()}%",
                    color = GoldPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Progress Bar (Recharts Bar Shape)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (isLight) Color(0xFFE8E2D2) else Color(0xFF221E14))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                item.accentColor.copy(alpha = 0.85f),
                                item.accentColor
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Recharts-style Donut Chart showing service category distribution.
 */
@Composable
private fun RechartsDonutChart(
    services: List<ServiceAnalyticsItem>,
    modifier: Modifier = Modifier
) {
    val trackColor = StudioOutline.copy(alpha = 0.35f)
    Canvas(modifier = modifier) {
        val strokeWidth = 18f
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        val totalPct = services.sumOf { it.percentage.toDouble() }
        if (totalPct <= 0.1) {
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            return@Canvas
        }

        var startAngle = -90f
        services.forEach { item ->
            if (item.percentage > 0f) {
                val sweep = (item.percentage / 100f) * 360f
                drawArc(
                    color = item.accentColor,
                    startAngle = startAngle,
                    sweepAngle = (sweep - 2f).coerceAtLeast(2f), // Subtle gap between slices
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }
    }
}

/**
 * Interactive native Jetpack Compose Canvas bar chart showing the number of Firestore bookings
 * per service type over the last 30 days.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceBookings30DaysChartCard(
    metrics: List<ServiceLast30DaysMetric>,
    language: AppLanguage,
    selectedServiceKey: String?,
    onSelectService: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHindi = language == AppLanguage.HINDI
    val total30Days = remember(metrics) { metrics.sumOf { it.totalCount } }
    val maxCount = remember(metrics) { (metrics.maxOfOrNull { it.totalCount } ?: 0).coerceAtLeast(4) }
    val selectedMetric = remember(metrics, selectedServiceKey) {
        metrics.firstOrNull { it.serviceKey == selectedServiceKey }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)), RoundedCornerShape(16.dp))
            .testTag("service_30d_chart_card"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
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
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(GoldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isHindi) "सेवा अनुसार बुकिंग (पिछले 30 दिन)" else "Bookings per Service (Last 30 Days)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                        Text(
                            text = if (isHindi) {
                                "पिछले 30 दिनों की वास्तविक Firestore बुकिंग संख्या"
                            } else {
                                "Actual Firestore bookings over the rolling 30-day window"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.5.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GoldContainer)
                        .border(0.8.dp, GoldPrimary.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("service_30d_total_badge")
                ) {
                    Text(
                        text = if (isHindi) "$total30Days कुल" else "$total30Days BOOKINGS",
                        color = GoldLight,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (total30Days == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioElevatedSurface)
                        .testTag("service_30d_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isHindi) {
                            "पिछले 30 दिनों में कोई बुकिंग दर्ज नहीं हुई है।"
                        } else {
                            "No bookings recorded over the last 30 days."
                        },
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                // Y-axis max / mid / 0 indicator + Compose Canvas Column Bar Chart
                val isLight = StudioTheme.isLight
                val gridLineColor = if (isLight) Color(0xFFE2DAC8) else Color(0xFF2B2416)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Y-axis ticks
                    Column(
                        modifier = Modifier
                            .height(150.dp)
                            .padding(end = 6.dp, top = 8.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = maxCount.toString(),
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = (maxCount / 2).toString(),
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "0",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Bar Plot Canvas
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp)
                            .pointerInput(metrics) {
                                detectTapGestures { offset ->
                                    if (metrics.isNotEmpty()) {
                                        val slotW = size.width.toFloat() / metrics.size
                                        val tappedIdx = (offset.x / slotW)
                                            .toInt()
                                            .coerceIn(0, metrics.size - 1)
                                        onSelectService(metrics[tappedIdx].serviceKey)
                                    }
                                }
                            }
                            .semantics {
                                contentDescription = if (isHindi) {
                                    "पिछले 30 दिनों के सेवा अनुसार बुकिंग बार चार्ट, कुल $total30Days बुकिंग"
                                } else {
                                    "Bar chart of bookings per service over the last 30 days, $total30Days total bookings"
                                }
                            }
                            .testTag("service_30d_bar_chart_canvas")
                    ) {
                        val topPad = 14f
                        val bottomPad = 14f
                        val plotH = (size.height - topPad - bottomPad).coerceAtLeast(10f)

                        for (g in 0..2) {
                            val y = topPad + plotH * (g / 2f)
                            drawLine(
                                color = gridLineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }

                        val slotWidth = size.width / metrics.size.coerceAtLeast(1)
                        val barWidth = (slotWidth * 0.44f).coerceAtMost(72f)

                        metrics.forEachIndexed { idx, item ->
                            val ratio = (item.totalCount.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
                            val barH = if (item.totalCount > 0) (ratio * plotH).coerceAtLeast(10f) else 3f
                            val barLeft = idx * slotWidth + (slotWidth - barWidth) / 2f
                            val barTop = topPad + plotH - barH
                            val isSelected = selectedServiceKey == item.serviceKey

                            if (isSelected) {
                                drawRoundRect(
                                    color = item.accentColor.copy(alpha = 0.22f),
                                    topLeft = Offset(barLeft - 6f, topPad),
                                    size = Size(barWidth + 12f, plotH),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                            }

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        item.accentColor,
                                        item.accentColor.copy(alpha = 0.65f)
                                    ),
                                    startY = barTop,
                                    endY = topPad + plotH
                                ),
                                topLeft = Offset(barLeft, barTop),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Interactive Service Columns / Legend Chips (aligned beneath bars)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    metrics.forEach { item ->
                        val isSelected = selectedServiceKey == item.serviceKey
                        val chipLabel = if (isHindi) item.labelHi else item.labelEn
                        Surface(
                            onClick = { onSelectService(item.serviceKey) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) item.accentColor.copy(alpha = 0.2f) else StudioElevatedSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) item.accentColor else StudioOutline.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .semantics(mergeDescendants = true) {
                                    contentDescription = if (isHindi) {
                                        "$chipLabel: पिछले 30 दिनों में ${item.totalCount} बुकिंग"
                                    } else {
                                        "$chipLabel: ${item.totalCount} bookings in last 30 days"
                                    }
                                }
                                .testTag("service_30d_bar_chip_${item.serviceKey}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = item.totalCount.toString(),
                                    color = item.accentColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = chipLabel,
                                    color = TextPrimary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            selectedMetric?.let { detail ->
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, detail.accentColor, RoundedCornerShape(12.dp))
                        .testTag("service_30d_detail_card"),
                    colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isHindi) {
                                "${detail.labelHi} — पिछले 30 दिन (${detail.totalCount} बुकिंग)"
                            } else {
                                "${detail.labelEn} — Last 30 Days (${detail.totalCount} bookings)"
                            },
                            color = detail.accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isHindi) "लंबित: ${detail.pendingCount}" else "Pending: ${detail.pendingCount}",
                                color = Color(0xFFFBBF24),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isHindi) "स्वीकृत: ${detail.confirmedCount}" else "Confirmed: ${detail.confirmedCount}",
                                color = SuccessGreen,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isHindi) "पूर्ण: ${detail.completedCount}" else "Completed: ${detail.completedCount}",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isHindi) "रद्द: ${detail.cancelledCount}" else "Cancelled: ${detail.cancelledCount}",
                                color = Color(0xFFEF4444),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
