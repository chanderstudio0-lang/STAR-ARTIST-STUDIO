package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
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
import com.example.ui.theme.WhatsAppGreen
import com.example.util.StudioContactDetails
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Service categories available for studio booking.
 */
enum class BookingServiceType(val label: String, val icon: ImageVector) {
    MODELING("Modeling", Icons.Default.Person),
    PHOTOGRAPHY("Photography", Icons.Default.CameraAlt),
    VIDEO_PRODUCTION("Video Production", Icons.Default.Videocam)
}

/**
 * Service Booking Form Component.
 * Captures Date, Time, and Service Type (Modeling, Photography, Video Production)
 * along with client contact details, and submits the request to Cloud Firestore.
 */
@Composable
fun ServiceBookingFormComponent(
    language: AppLanguage = AppLanguage.ENGLISH,
    initialServiceType: String = "Photography",
    initialDate: String = "",
    initialTime: String = "10:00 AM (Morning Key Light)",
    initialLocation: String = "",
    initialNotes: String = "",
    defaultClientName: String = "",
    defaultPhoneNumber: String = "",
    isEmailVerified: Boolean = true,
    onDateChanged: (String) -> Unit = {},
    onTimeChanged: (String) -> Unit = {},
    onViewBookings: () -> Unit = {},
    isSubmitting: Boolean = false,
    onSaveToFirestore: (
        clientName: String,
        contactNumber: String,
        serviceType: String,
        date: String,
        time: String,
        location: String,
        notes: String,
        onSuccess: (bookingId: String) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser = remember { runCatching { Firebase.auth.currentUser }.getOrNull() }

    // 1. Service Type State
    var selectedCategory by remember {
        mutableStateOf(
            when {
                initialServiceType.startsWith("Modeling", ignoreCase = true) -> BookingServiceType.MODELING
                initialServiceType.startsWith("Video", ignoreCase = true) ||
                        initialServiceType.startsWith("Production", ignoreCase = true) -> BookingServiceType.VIDEO_PRODUCTION
                else -> BookingServiceType.PHOTOGRAPHY
            }
        )
    }
    var serviceType by remember { mutableStateOf(initialServiceType.ifBlank { "Photography" }) }

    LaunchedEffect(initialServiceType) {
        if (initialServiceType.isNotBlank()) {
            selectedCategory = when {
                initialServiceType.startsWith("Modeling", ignoreCase = true) -> BookingServiceType.MODELING
                initialServiceType.startsWith("Video", ignoreCase = true) ||
                        initialServiceType.startsWith("Production", ignoreCase = true) -> BookingServiceType.VIDEO_PRODUCTION
                else -> BookingServiceType.PHOTOGRAPHY
            }
            serviceType = initialServiceType
        }
    }

    // 2. Date State
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayStr = remember { dateFormat.format(Calendar.getInstance().time) }
    val tomorrowStr = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        dateFormat.format(cal.time)
    }
    val nextWeekendStr = remember {
        val cal = Calendar.getInstance().apply {
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        dateFormat.format(cal.time)
    }
    var selectedDate by remember { mutableStateOf(initialDate.ifBlank { tomorrowStr }) }
    LaunchedEffect(initialDate) {
        if (initialDate.isNotBlank() && initialDate != selectedDate) {
            selectedDate = initialDate
        }
    }

    var showDatePickerModal by remember { mutableStateOf(false) }
    var showTimePickerModal by remember { mutableStateOf(false) }

    // 3. Time State
    val timeSlotOptions = listOf(
        "10:00 AM (Morning Key Light)",
        "12:00 PM (Midday Flash Stage)",
        "02:30 PM (Editorial Softbox Bay)",
        "04:30 PM (Golden Hour Outdoor)",
        "06:30 PM (Dark & Rim Mood Studio)"
    )
    var selectedTime by remember { mutableStateOf(initialTime.ifBlank { timeSlotOptions.first() }) }
    LaunchedEffect(initialTime) {
        if (initialTime.isNotBlank() && initialTime != selectedTime) {
            selectedTime = initialTime
        }
    }

    if (showDatePickerModal) {
        BookingDatePickerModal(
            initialDate = selectedDate,
            language = language,
            onDateSelected = { formatted ->
                selectedDate = formatted
                onDateChanged(formatted)
            },
            onDismiss = { showDatePickerModal = false }
        )
    }

    if (showTimePickerModal) {
        BookingTimePickerModal(
            initialTime = selectedTime,
            language = language,
            onTimeSelected = { formattedTime ->
                selectedTime = formattedTime
                onTimeChanged(formattedTime)
            },
            onDismiss = { showTimePickerModal = false }
        )
    }

    // 4. Client Info State (prefilled smoothly from authenticated profile)
    var clientName by remember {
        mutableStateOf(defaultClientName.ifBlank { currentUser?.displayName.orEmpty() })
    }
    var contactNumber by remember {
        mutableStateOf(normalizeBookingPhone(defaultPhoneNumber))
    }
    LaunchedEffect(defaultClientName, defaultPhoneNumber) {
        if (clientName.isBlank() && defaultClientName.isNotBlank()) {
            clientName = defaultClientName
        }
        if (contactNumber.isBlank() && defaultPhoneNumber.isNotBlank()) {
            contactNumber = normalizeBookingPhone(defaultPhoneNumber)
        }
    }
    var location by remember {
        mutableStateOf(initialLocation.ifBlank { "Studio (Talwara Khurd - 4K Lighting Bay)" })
    }
    var notes by remember { mutableStateOf(initialNotes) }
    LaunchedEffect(initialLocation, initialNotes) {
        if (initialLocation.isNotBlank()) {
            location = initialLocation
        }
        if (initialNotes.isNotBlank()) {
            notes = initialNotes
        }
    }

    // Submission and feedback states
    var showErrors by remember { mutableStateOf(false) }
    var savedBookingId by remember { mutableStateOf("") }
    var isSuccessSubmitted by remember { mutableStateOf(false) }

    val isNameValid by remember { derivedStateOf { clientName.trim().length >= 2 } }
    val isPhoneValid by remember { derivedStateOf { contactNumber.trim().length in 8..15 } }
    val isDateValid by remember { derivedStateOf { selectedDate.trim().isNotEmpty() } }
    val isTimeValid by remember { derivedStateOf { selectedTime.trim().isNotEmpty() } }
    val isFormValid by remember { derivedStateOf { isNameValid && isPhoneValid && isDateValid && isTimeValid && serviceType.trim().isNotEmpty() } }

    val modelingPackages = listOf(
        "Modeling",
        "Modeling (Portfolio & Comp-Card Shoot)",
        "Modeling (Agency Audition Profile)",
        "Modeling (Runway & Lookbook Session)"
    )
    val photographyPackages = listOf(
        "Photography",
        "Photography (High-Fashion Editorial)",
        "Photography (Royal Pre-Wedding & Bridal)",
        "Photography (Commercial Product & Catalog)"
    )
    val videoPackages = listOf(
        "Video Production",
        "Video Production (4K Cinema Music Video Direction)",
        "Video Production (Commercial Brand Ad Film)",
        "Video Production (Fashion Model Reel in 4K HDR)"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.2.dp, Brush.verticalGradient(listOf(GoldPrimary, StudioOutline))),
                RoundedCornerShape(18.dp)
            )
            .testTag("service_booking_form"),
        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Component Title and Studio Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldContainer)
                            .border(1.dp, GoldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (language == AppLanguage.HINDI) "स्टूडियो सत्र बुक करें" else "Book Studio Session",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                        Text(
                            text = if (language == AppLanguage.HINDI) "तारीख, समय व सेवा प्रकार चुनें" else "Select Date, Time & Service Type",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GoldContainer)
                        .border(0.8.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "FIRESTORE SYNC",
                        color = GoldLight,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Success Confirmation Banner
            AnimatedVisibility(visible = isSuccessSubmitted, enter = fadeIn(), exit = fadeOut()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SuccessGreen, RoundedCornerShape(12.dp))
                        .testTag("booking_success_confirmation"),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.14f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "बुकिंग अनुरोध सहेजा गया!" else "Booking Request Saved to Firestore!",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (savedBookingId.isNotBlank()) {
                                        if (language == AppLanguage.HINDI) "संदर्भ आईडी: $savedBookingId" else "Reference ID: $savedBookingId"
                                    } else {
                                        if (language == AppLanguage.HINDI) "स्टूडियो एडमिन द्वारा समीक्षा उपरांत पुष्टि की जाएगी।" else "Your session is scheduled for review by the studio admin."
                                    },
                                    color = SuccessGreen,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onViewBookings,
                            border = BorderStroke(1.dp, SuccessGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("booking_success_track_btn")
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "मेरी बुकिंग्स में देखें" else "Track in My Bookings",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 1. SERVICE TYPE SELECTOR (Modeling, Photography, Video Production)
            // =================================================================
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "1. सेवा प्रकार (Service Type)" else "1. Choose Service Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        fontSize = 12.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Primary 3 Category Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_type_selector"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BookingServiceType.values().forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val typeTag = when (cat) {
                            BookingServiceType.MODELING -> "service_type_modeling"
                            BookingServiceType.PHOTOGRAPHY -> "service_type_photography"
                            BookingServiceType.VIDEO_PRODUCTION -> "service_type_video_production"
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 64.dp)
                                .border(
                                    BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) GoldPrimary else StudioOutline
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedCategory = cat
                                    serviceType = cat.label
                                }
                                .testTag(typeTag),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) GoldContainer else StudioCardSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.label,
                                    tint = if (isSelected) GoldPrimary else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cat.label,
                                    color = if (isSelected) GoldLight else TextPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sub-packages chips
                val activePackages = when (selectedCategory) {
                    BookingServiceType.MODELING -> modelingPackages
                    BookingServiceType.PHOTOGRAPHY -> photographyPackages
                    BookingServiceType.VIDEO_PRODUCTION -> videoPackages
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(activePackages) { pkg ->
                        val isSelected = serviceType == pkg
                        FilterChip(
                            selected = isSelected,
                            onClick = { serviceType = pkg },
                            label = { Text(pkg, fontSize = 11.5.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = StudioBlack,
                                containerColor = StudioCardSurface,
                                labelColor = Color.LightGray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) GoldPrimary else StudioOutline
                            ),
                            modifier = Modifier.testTag("service_option_${pkg.hashCode()}")
                        )
                    }
                }
            }

            // =================================================================
            // 2. DATE SELECTOR
            // =================================================================
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "2. सत्र की तारीख (Session Date)" else "2. Select Session Date",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        fontSize = 12.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // M3 Date Picker Modal Trigger Button
                OutlinedButton(
                    onClick = { showDatePickerModal = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("service_booking_date_picker_button"),
                    border = BorderStroke(1.dp, GoldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedDate.isNotBlank()) {
                            if (language == AppLanguage.HINDI) "तारीख: $selectedDate (कैलेंडर से चुनें)" else "Date: $selectedDate (Open Date Picker)"
                        } else {
                            if (language == AppLanguage.HINDI) "कैलेंडर से तारीख चुनें" else "Open Date Picker"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Date Chips: Today, Tomorrow, Weekend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Today" to todayStr,
                        "Tomorrow" to tomorrowStr,
                        "Saturday" to nextWeekendStr
                    ).forEach { (label, dateVal) ->
                        val isSelected = selectedDate == dateVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) GoldContainer else StudioCardSurface)
                                .border(1.dp, if (isSelected) GoldPrimary else StudioOutline, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedDate = dateVal
                                    onDateChanged(dateVal)
                                }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$label\n$dateVal",
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) GoldLight else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Direct date text field
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = {
                        selectedDate = it
                        onDateChanged(it)
                    },
                    label = { Text("Date (YYYY-MM-DD)") },
                    placeholder = { Text(tomorrowStr) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_booking_date_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = studioBookingFieldColors()
                )
            }

            // =================================================================
            // 3. TIME PICKER & LIGHTING BAY SLOTS
            // =================================================================
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "3. समय पिकर व स्लॉट (Session Time)" else "3. Select Session Time",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        fontSize = 12.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // M3 Time Picker Modal Trigger Button
                OutlinedButton(
                    onClick = { showTimePickerModal = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("service_booking_time_picker_button"),
                    border = BorderStroke(1.dp, GoldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) {
                            "समय: $selectedTime (टाइम पिकर खोलें)"
                        } else {
                            "Time: $selectedTime (Open Time Picker)"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Time Slot Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_booking_time_selector"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(timeSlotOptions) { slot ->
                        val isSelected = selectedTime == slot
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedTime = slot
                                onTimeChanged(slot)
                            },
                            label = { Text(slot, fontSize = 11.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
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
                                .testTag("time_slot_chip_${slot.take(5).replace(":", "")}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Direct time input
                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = {
                        selectedTime = it
                        onTimeChanged(it)
                    },
                    label = { Text("Preferred Time Slot") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_booking_time_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = studioBookingFieldColors()
                )
            }

            // =================================================================
            // 4. CLIENT CONTACT & LOCATION
            // =================================================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Client Full Name") },
                    placeholder = { Text("e.g. Ramesh Kumar") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    isError = showErrors && !isNameValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_booking_client_name"),
                    shape = RoundedCornerShape(10.dp),
                    colors = studioBookingFieldColors()
                )

                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.length <= 15) contactNumber = digits
                    },
                    label = { Text("Contact Phone Number") },
                    placeholder = { Text("e.g. 9812890081") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    isError = showErrors && !isPhoneValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_booking_phone_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = studioBookingFieldColors()
                )

                // Location Chips
                val locationChoices = listOf(
                    "Studio (Talwara Khurd - 4K Lighting Bay)",
                    "Outdoor (Sirsa & Ellenabad Region)",
                    "Client Location (Custom Set)"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(locationChoices) { loc ->
                        val isSelected = location == loc
                        FilterChip(
                            selected = isSelected,
                            onClick = { location = loc },
                            label = { Text(loc, fontSize = 11.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                            } else {
                                { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(13.dp)) }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = StudioCardSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) GoldPrimary else StudioOutline
                            )
                        )
                    }
                }

                // Voice-to-Text Requirement Input + Notes Field
                VoiceToTextInputComponent(
                    currentText = notes,
                    onTextAppended = { notes = it },
                    language = language,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Session Requirements & Lighting Notes") },
                    placeholder = { Text("e.g. 3 costume changes, royal bridal jewelry set, slow-mo 4K gimbal sequence") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_booking_notes_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = studioBookingFieldColors()
                )
            }

            // Validation Summary
            if (showErrors && !isFormValid) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_booking_validation_error"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "कृपया नाम, फोन, तारीख व समय पूर्ण करें" else "Please complete client name, phone number (8–15 digits), date, and time.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            // =================================================================
            // 5. SUBMIT TO FIRESTORE BUTTON
            // =================================================================
            Button(
                onClick = {
                    if (currentUser != null && !isEmailVerified) {
                        showErrors = true
                        return@Button
                    }
                    if (!isFormValid) {
                        showErrors = true
                    } else {
                        showErrors = false
                        onSaveToFirestore(
                            clientName.trim(),
                            contactNumber.trim(),
                            serviceType.trim(),
                            selectedDate.trim(),
                            selectedTime.trim(),
                            location.trim(),
                            notes.trim()
                        ) { newBookingId ->
                            savedBookingId = newBookingId
                            isSuccessSubmitted = true
                            notes = ""
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 50.dp)
                    .testTag("service_booking_submit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEmailVerified) GoldPrimary else GoldDark,
                    contentColor = if (isEmailVerified) MaterialTheme.colorScheme.onPrimary else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isEmailVerified) Icons.Default.EventAvailable else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!isEmailVerified) {
                            if (language == AppLanguage.HINDI) "ईमेल सत्यापित करें (बुकिंग के लिए)" else "Verify Email First"
                        } else {
                            if (language == AppLanguage.HINDI) "सत्र अनुरोध फायरस्टोर में सहेजें" else "Submit Booking Request to Firestore"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }

            // Instant WhatsApp Consultation Shortcut
            Button(
                onClick = {
                    val summary = "Hello Star Artist Studio! I would like to book a $serviceType session on $selectedDate at $selectedTime. Client: $clientName ($contactNumber)."
                    val url = StudioContactDetails.buildWhatsAppUrl(summary)
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("booking_whatsapp_consult_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (language == AppLanguage.HINDI) "व्हाट्सएप पर तत्काल परामर्श" else "Instant WhatsApp Consultation",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun studioBookingFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GoldPrimary,
    unfocusedBorderColor = StudioOutline,
    focusedLabelColor = GoldPrimary,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = GoldPrimary,
    focusedContainerColor = StudioCardSurface,
    unfocusedContainerColor = StudioCardSurface
)
