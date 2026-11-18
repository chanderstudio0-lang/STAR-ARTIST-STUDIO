package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.ShootBooking
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline

/**
 * Pure helper checks whether a Firestore booking status represents a pending request.
 * Blank status values are treated as pending so uninitialized records are never missed.
 */
fun isPendingBookingStatus(status: String): Boolean {
    val clean = status.trim()
    return clean.isEmpty() || clean.equals("pending", ignoreCase = true)
}

/**
 * Classifies free-form service types into canonical studio service buckets.
 */
fun canonicalBookingServiceBucket(serviceType: String): String {
    val normalized = serviceType.lowercase()
    return when {
        normalized.contains("model") -> "Modeling"
        normalized.contains("video") || normalized.contains("reel") || normalized.contains("cinema") || normalized.contains("film") -> "Video Production"
        normalized.contains("photo") || normalized.contains("portrait") || normalized.contains("fashion") ||
            normalized.contains("bridal") || normalized.contains("editorial") || normalized.contains("portfolio") -> "Photography"
        else -> "Other"
    }
}

/**
 * Counts all pending studio booking requests grouped by primary service category.
 */
fun countPendingBookingsByService(bookings: List<ShootBooking>): Map<String, Int> {
    val counts = linkedMapOf(
        "Modeling" to 0,
        "Photography" to 0,
        "Video Production" to 0,
        "Other" to 0
    )
    bookings.filter { isPendingBookingStatus(it.status) }.forEach { booking ->
        val bucket = canonicalBookingServiceBucket(booking.serviceType)
        counts[bucket] = (counts[bucket] ?: 0) + 1
    }
    return counts
}

/**
 * Filters booking requests by status tab, service category, and search query.
 */
fun filterAdminBookingRequests(
    bookings: List<ShootBooking>,
    statusFilter: String,
    serviceFilter: String = "ALL",
    searchQuery: String = ""
): List<ShootBooking> {
    val trimmedQuery = searchQuery.trim()
    return bookings.filter { booking ->
        val statusMatches = when (statusFilter.uppercase()) {
            "PENDING" -> isPendingBookingStatus(booking.status)
            "CONFIRMED" -> booking.status.trim().lowercase() in listOf("confirmed", "approved")
            "COMPLETED" -> booking.status.trim().equals("completed", ignoreCase = true)
            "CANCELLED" -> booking.status.trim().lowercase() in listOf("cancelled", "rejected")
            else -> true
        }
        val serviceMatches = when (serviceFilter.uppercase()) {
            "MODELING" -> canonicalBookingServiceBucket(booking.serviceType) == "Modeling"
            "PHOTOGRAPHY" -> canonicalBookingServiceBucket(booking.serviceType) == "Photography"
            "VIDEO_PRODUCTION" -> canonicalBookingServiceBucket(booking.serviceType) == "Video Production"
            else -> true
        }
        val searchMatches = trimmedQuery.isEmpty() ||
            booking.clientName.contains(trimmedQuery, ignoreCase = true) ||
            booking.contactNumber.contains(trimmedQuery, ignoreCase = true) ||
            booking.serviceType.contains(trimmedQuery, ignoreCase = true) ||
            booking.preferredDate.contains(trimmedQuery, ignoreCase = true) ||
            booking.preferredTime.contains(trimmedQuery, ignoreCase = true) ||
            booking.location.contains(trimmedQuery, ignoreCase = true) ||
            booking.notes.contains(trimmedQuery, ignoreCase = true)

        statusMatches && serviceMatches && searchMatches
    }
}

/**
 * Dedicated Material 3 Admin Dashboard component for viewing and managing
 * pending and lifecycle studio session booking requests from Cloud Firestore.
 */
@Composable
fun AdminBookingRequestsComponent(
    bookings: List<ShootBooking>,
    isLoading: Boolean,
    language: AppLanguage,
    onUpdateStatus: (bookingId: String, status: String) -> Unit,
    onDeleteBooking: (bookingId: String) -> Unit = {},
    initialFilter: String? = null,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        AdminSkeletonList()
        return
    }

    val isHindi = language == AppLanguage.HINDI
    val pendingCount = remember(bookings) { bookings.count { isPendingBookingStatus(it.status) } }
    val confirmedCount = remember(bookings) { bookings.count { it.status.trim().lowercase() in listOf("confirmed", "approved") } }
    val completedCount = remember(bookings) { bookings.count { it.status.trim().equals("completed", ignoreCase = true) } }
    val cancelledCount = remember(bookings) { bookings.count { it.status.trim().lowercase() in listOf("cancelled", "rejected") } }
    val pendingByService = remember(bookings) { countPendingBookingsByService(bookings) }

    var hasUserSelectedStatus by remember { mutableStateOf(initialFilter != null) }
    var bookingFilter by remember {
        mutableStateOf(
            initialFilter ?: if (pendingCount > 0) "PENDING" else "ALL"
        )
    }

    // Automatically focus the pending queue when async Firestore data arrives unless the admin picked another tab.
    LaunchedEffect(pendingCount, hasUserSelectedStatus) {
        if (!hasUserSelectedStatus && initialFilter == null) {
            bookingFilter = if (pendingCount > 0) "PENDING" else "ALL"
        }
    }

    var serviceFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val filteredBookings = remember(bookings, bookingFilter, serviceFilter, searchQuery) {
        filterAdminBookingRequests(
            bookings = bookings,
            statusFilter = bookingFilter,
            serviceFilter = serviceFilter,
            searchQuery = searchQuery
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_booking_requests_component")
    ) {
        // Pinned Search Bar to filter pending & active booking requests by customer name or service type
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = if (isHindi) "बुकिंग खोजें" else "Search booking requests",
                    tint = GoldPrimary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("admin_booking_search_clear")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isHindi) "खोज साफ़ करें" else "Clear search",
                            tint = Color.Gray
                        )
                    }
                }
            },
            placeholder = {
                Text(
                    text = if (isHindi) {
                        "ग्राहक के नाम या सेवा प्रकार से खोजें..."
                    } else {
                        "Search by customer name or service type..."
                    },
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = StudioOutline,
                focusedContainerColor = StudioElevatedSurface,
                unfocusedContainerColor = StudioElevatedSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = GoldPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .semantics {
                    contentDescription = if (isHindi) {
                        "ग्राहक नाम या सेवा प्रकार द्वारा लंबित बुकिंग खोजें"
                    } else {
                        "Search pending booking requests by customer name or service type"
                    }
                }
                .testTag("admin_booking_search_input")
        )

        if (searchQuery.isNotBlank()) {
            val scopeLabel = when (bookingFilter) {
                "PENDING" -> if (isHindi) "लंबित अनुरोध" else "pending request(s)"
                "CONFIRMED" -> if (isHindi) "पुष्ट बुकिंग" else "confirmed booking(s)"
                "COMPLETED" -> if (isHindi) "पूर्ण बुकिंग" else "completed booking(s)"
                "CANCELLED" -> if (isHindi) "रद्द बुकिंग" else "cancelled booking(s)"
                else -> if (isHindi) "बुकिंग" else "booking(s)"
            }
            Text(
                text = if (isHindi) {
                    "\"${searchQuery.trim()}\" के लिए ${filteredBookings.size} $scopeLabel मिले"
                } else {
                    "Showing ${filteredBookings.size} $scopeLabel matching \"${searchQuery.trim()}\""
                },
                style = MaterialTheme.typography.labelSmall,
                color = GoldLight,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 2.dp)
                    .testTag("admin_booking_search_summary")
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .testTag("admin_bookings_lazy_list"),
            contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Pending Studio Session Queue Header Banner
            item(key = "pending_header") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    if (pendingCount > 0) Color(0xFFFBBF24) else GoldPrimary.copy(alpha = 0.5f),
                                    StudioOutline
                                )
                            )
                        ),
                        RoundedCornerShape(14.dp)
                    )
                    .testTag("admin_pending_bookings_header"),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(if (pendingCount > 0) Color(0xFFFBBF24) else Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "स्टूडियो सत्र बुकिंग कतार" else "Pending Session Requests",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight
                                )
                                Text(
                                    text = if (pendingCount > 0) {
                                        if (isHindi) "$pendingCount अनुरोध स्वीकृति की प्रतीक्षा में हैं" else "$pendingCount request(s) awaiting slot confirmation"
                                    } else {
                                        if (isHindi) "सभी लंबित अनुरोध संभाले जा चुके हैं" else "All pending booking requests handled"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFBDBDBD)
                                )
                            }
                        }

                        if (bookingFilter != "PENDING" && pendingCount > 0) {
                            OutlinedButton(
                                onClick = {
                                    hasUserSelectedStatus = true
                                    bookingFilter = "PENDING"
                                },
                                border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("admin_jump_to_pending_bookings")
                            ) {
                                Text(
                                    text = if (isHindi) "लंबित देखें ($pendingCount)" else "Pending ($pendingCount)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (bookings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .testTag("admin_booking_status_summary_row"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BookingStatusChip(
                                statusStr = "Pending",
                                language = language,
                                count = pendingCount,
                                onClick = {
                                    hasUserSelectedStatus = true
                                    bookingFilter = "PENDING"
                                },
                                modifier = Modifier.testTag("admin_summary_chip_pending")
                            )
                            BookingStatusChip(
                                statusStr = "Confirmed",
                                language = language,
                                count = confirmedCount,
                                onClick = {
                                    hasUserSelectedStatus = true
                                    bookingFilter = "CONFIRMED"
                                },
                                modifier = Modifier.testTag("admin_summary_chip_confirmed")
                            )
                            BookingStatusChip(
                                statusStr = "Cancelled",
                                language = language,
                                count = cancelledCount,
                                onClick = {
                                    hasUserSelectedStatus = true
                                    bookingFilter = "CANCELLED"
                                },
                                modifier = Modifier.testTag("admin_summary_chip_cancelled")
                            )
                        }
                    }

                    if (pendingCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val modelingPending = pendingByService["Modeling"] ?: 0
                            val photoPending = pendingByService["Photography"] ?: 0
                            val videoPending = pendingByService["Video Production"] ?: 0
                            val otherPending = pendingByService["Other"] ?: 0

                            ServiceCountPill(
                                label = if (isHindi) "मॉडलिंग" else "Modeling",
                                count = modelingPending
                            )
                            ServiceCountPill(
                                label = if (isHindi) "फ़ोटोग्राफ़ी" else "Photography",
                                count = photoPending
                            )
                            ServiceCountPill(
                                label = if (isHindi) "वीडियो प्रोडक्शन" else "Video Production",
                                count = videoPending
                            )
                            if (otherPending > 0) {
                                ServiceCountPill(
                                    label = if (isHindi) "अन्य" else "Other",
                                    count = otherPending
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Status Filter Bar
        item(key = "status_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusFilterChip(
                    label = if (isHindi) "सभी" else "All",
                    count = bookings.size,
                    isSelected = bookingFilter == "ALL",
                    indicatorColor = null,
                    onClick = {
                        hasUserSelectedStatus = true
                        bookingFilter = "ALL"
                    },
                    testTag = "admin_booking_filter_all"
                )
                StatusFilterChip(
                    label = if (isHindi) "लंबित" else "Pending",
                    count = pendingCount,
                    isSelected = bookingFilter == "PENDING",
                    indicatorColor = Color(0xFFFBBF24),
                    onClick = {
                        hasUserSelectedStatus = true
                        bookingFilter = "PENDING"
                    },
                    testTag = "admin_booking_filter_pending"
                )
                StatusFilterChip(
                    label = if (isHindi) "पुष्ट / स्वीकृत" else "Confirmed",
                    count = confirmedCount,
                    isSelected = bookingFilter == "CONFIRMED",
                    indicatorColor = Color(0xFF10B981),
                    onClick = {
                        hasUserSelectedStatus = true
                        bookingFilter = "CONFIRMED"
                    },
                    testTag = "admin_booking_filter_confirmed"
                )
                StatusFilterChip(
                    label = if (isHindi) "पूर्ण" else "Completed",
                    count = completedCount,
                    isSelected = bookingFilter == "COMPLETED",
                    indicatorColor = Color(0xFF38BDF8),
                    onClick = {
                        hasUserSelectedStatus = true
                        bookingFilter = "COMPLETED"
                    },
                    testTag = "admin_booking_filter_completed"
                )
                StatusFilterChip(
                    label = if (isHindi) "रद्द / अस्वीकृत" else "Cancelled",
                    count = cancelledCount,
                    isSelected = bookingFilter == "CANCELLED",
                    indicatorColor = Color(0xFFEF4444),
                    onClick = {
                        hasUserSelectedStatus = true
                        bookingFilter = "CANCELLED"
                    },
                    testTag = "admin_booking_filter_cancelled"
                )
            }
        }

        // 4. Service Category Filter Bar
        item(key = "service_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val serviceOptions = listOf(
                    Triple("ALL", if (isHindi) "सभी सेवाएं" else "All Services", "admin_booking_service_filter_all"),
                    Triple("MODELING", if (isHindi) "मॉडलिंग" else "Modeling", "admin_booking_service_filter_modeling"),
                    Triple("PHOTOGRAPHY", if (isHindi) "फ़ोटोग्राफ़ी" else "Photography", "admin_booking_service_filter_photography"),
                    Triple("VIDEO_PRODUCTION", if (isHindi) "वीडियो प्रोडक्शन" else "Video Production", "admin_booking_service_filter_video_production")
                )
                serviceOptions.forEach { (key, label, tag) ->
                    val selected = serviceFilter == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) GoldContainer else StudioCardSurface)
                            .border(
                                1.dp,
                                if (selected) GoldPrimary else StudioOutline.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { serviceFilter = key }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag(tag)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) GoldLight else Color(0xFFB8B8B8)
                        )
                    }
                }
            }
        }

        // 5. Empty States or Booking Request Cards
        if (bookings.isEmpty()) {
            item(key = "empty_all") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = GoldPrimary.copy(alpha = 0.7f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isHindi) "कोई शूट बुकिंग अनुरोध अभी उपलब्ध नहीं है।" else "No studio session booking requests recorded yet.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (filteredBookings.isEmpty()) {
            item(key = "empty_filtered") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val emptyMsg = if (bookingFilter == "PENDING" && serviceFilter == "ALL" && searchQuery.isBlank()) {
                            if (isHindi) "कोई लंबित बुकिंग अनुरोध शेष नहीं है।" else "All caught up! No pending studio session requests."
                        } else {
                            if (isHindi) "वर्तमान फ़िल्टर से मेल खाती कोई बुकिंग नहीं मिली।" else "No bookings match your active filters."
                        }
                        Text(
                            text = emptyMsg,
                            color = Color(0xFFC8C8C8),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (bookingFilter == "PENDING" && serviceFilter == "ALL" && searchQuery.isBlank()) {
                            OutlinedButton(
                                onClick = {
                                    hasUserSelectedStatus = true
                                    bookingFilter = "ALL"
                                },
                                border = BorderStroke(1.dp, GoldPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("admin_booking_show_all_button")
                            ) {
                                Text(
                                    text = if (isHindi) "सभी बुकिंग देखें (${bookings.size})" else "Show All Bookings (${bookings.size})",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    hasUserSelectedStatus = true
                                    bookingFilter = "ALL"
                                    serviceFilter = "ALL"
                                    searchQuery = ""
                                },
                                border = BorderStroke(1.dp, GoldPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("admin_booking_clear_filters_button")
                            ) {
                                Text(
                                    text = if (isHindi) "फ़िल्टर साफ़ करें" else "Clear Filters",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = filteredBookings,
                key = { it.id.ifBlank { "${it.clientName}_${it.preferredDate}_${it.preferredTime}" } }
            ) { booking ->
                AdminBookingCard(
                    booking = booking,
                    language = language,
                    onUpdateStatus = onUpdateStatus,
                    onDeleteBooking = onDeleteBooking
                )
            }
        }
    }
    }
}

@Composable
private fun ServiceCountPill(
    label: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StudioElevatedSurface)
            .border(1.dp, StudioOutline.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = Color(0xFFD0D0D0),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = count.toString(),
            fontSize = 10.5.sp,
            color = if (count > 0) Color(0xFFFBBF24) else Color.Gray,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun AdminBookingCard(
    booking: ShootBooking,
    language: AppLanguage = AppLanguage.ENGLISH,
    onUpdateStatus: (String, String) -> Unit,
    onDeleteBooking: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isHindi = language == AppLanguage.HINDI
    val statuses = listOf("Pending", "Confirmed", "Completed", "Cancelled")
    val itemStatus = AdminItemStatus.from(booking.status)
    val isPending = isPendingBookingStatus(booking.status)

    var showDeclineDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            containerColor = StudioCardSurface,
            title = {
                Text(
                    text = if (isHindi) "बुकिंग अस्वीकार करें?" else "Decline Booking Request?",
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isHindi) {
                        "क्या आप ${booking.clientName} (${booking.preferredDate}) का बुकिंग अनुरोध रद्द करना चाहते हैं?"
                    } else {
                        "Decline and cancel ${booking.clientName}'s studio session request for ${booking.preferredDate}?"
                    },
                    color = Color(0xFFD0D0D0)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeclineDialog = false
                        onUpdateStatus(booking.id, "Cancelled")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("booking_decline_confirm_${booking.id}")
                ) {
                    Text(if (isHindi) "हाँ, अस्वीकार करें" else "Decline Slot")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeclineDialog = false },
                    modifier = Modifier.testTag("booking_decline_cancel_${booking.id}")
                ) {
                    Text(
                        text = if (isHindi) "रद्द करें" else "Keep Pending",
                        color = GoldPrimary
                    )
                }
            },
            modifier = Modifier.testTag("booking_decline_dialog_${booking.id}")
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = StudioCardSurface,
            title = {
                Text(
                    text = if (isHindi) "बुकिंग रिकॉर्ड हटाएं?" else "Delete Booking Record?",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isHindi) {
                        "यह क्रिया Firestore से ${booking.clientName} का बुकिंग रिकॉर्ड स्थायी रूप से हटा देगी।"
                    } else {
                        "This permanently removes ${booking.clientName}'s booking request from Firestore."
                    },
                    color = Color(0xFFD0D0D0)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteBooking(booking.id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("booking_delete_confirm_${booking.id}")
                ) {
                    Text(if (isHindi) "हटाएं" else "Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.testTag("booking_delete_cancel_${booking.id}")
                ) {
                    Text(
                        text = if (isHindi) "रद्द करें" else "Cancel",
                        color = GoldPrimary
                    )
                }
            },
            modifier = Modifier.testTag("booking_delete_dialog_${booking.id}")
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            itemStatus.primaryColor.copy(alpha = 0.7f),
                            itemStatus.borderColor.copy(alpha = 0.4f),
                            StudioOutline
                        )
                    )
                ),
                RoundedCornerShape(14.dp)
            )
            .testTag("admin_booking_card_${booking.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Service Type & Visual Status Indicator Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.serviceType.ifBlank { "Studio Session" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${booking.clientName} • ${booking.contactNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD0D0D0)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                BookingStatusChip(
                    statusStr = booking.status,
                    language = language,
                    modifier = Modifier.testTag("booking_status_chip_${booking.id}")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Booking Slot, Location & Quick Contact / Delete Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${booking.preferredDate} • ${booking.preferredTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFB0B0B0),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = booking.location.ifBlank { "Star Artist Studio" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB0B0B0)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (booking.contactNumber.isNotBlank()) {
                        IconButton(
                            onClick = {
                                runCatching {
                                    val dialIntent = Intent(
                                        Intent.ACTION_DIAL,
                                        Uri.parse("tel:${booking.contactNumber.trim()}")
                                    )
                                    context.startActivity(dialIntent)
                                }
                            },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("booking_call_button_${booking.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = if (isHindi) "क्लाइंट को कॉल करें" else "Call ${booking.clientName}",
                                tint = GoldPrimary
                            )
                        }
                    }

                    if (booking.id.isNotBlank()) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("booking_delete_button_${booking.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = if (isHindi) "बुकिंग हटाएं" else "Delete booking",
                                tint = Color(0xFFEF4444).copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            if (booking.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isHindi) "नोट्स: ${booking.notes}" else "Notes: ${booking.notes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            // One-Tap Decision Bar for Pending Requests
            if (isPending) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onUpdateStatus(booking.id, "Confirmed") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = StudioBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("booking_confirm_action_${booking.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "स्लॉट पक्का करें" else "Confirm Slot",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    OutlinedButton(
                        onClick = { showDeclineDialog = true },
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("booking_decline_action_${booking.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "अस्वीकार करें" else "Decline",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isHindi) "स्थिति प्रबंधित करें:" else "Update Booking Status:",
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Visual Color-Coded Status Selection Chips (Pending / Confirmed / Completed / Cancelled)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statuses.forEach { st ->
                    val isSelected = booking.status.equals(st, ignoreCase = true) ||
                        (st == "Pending" && isPendingBookingStatus(booking.status))
                    val stInfo = AdminItemStatus.from(st)

                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) stInfo.primaryColor else stInfo.backgroundColor.copy(alpha = 0.45f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) stInfo.primaryColor else stInfo.borderColor.copy(alpha = 0.7f),
                                CircleShape
                            )
                            .clickable { onUpdateStatus(booking.id, st) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("booking_status_${st.lowercase()}_${booking.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) StudioBlack else stInfo.primaryColor)
                            )
                            Icon(
                                imageVector = stInfo.icon,
                                contentDescription = null,
                                tint = if (isSelected) StudioBlack else stInfo.primaryColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = st,
                                color = if (isSelected) StudioBlack else stInfo.primaryColor,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
