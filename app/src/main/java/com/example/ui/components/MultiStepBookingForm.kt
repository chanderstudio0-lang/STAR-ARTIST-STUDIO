package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.material.icons.filled.MarkEmailUnread
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
import androidx.compose.runtime.mutableIntStateOf
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
 * Service categories supported in Star Artist Studio.
 */
enum class MultiStepServiceCategory(val label: String, val icon: ImageVector) {
    PHOTOGRAPHY("Photography", Icons.Default.CameraAlt),
    MODELING("Modeling", Icons.Default.Person),
    PRODUCTION("Production", Icons.Default.Videocam)
}

fun normalizeBookingPhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length == 12 && digits.startsWith("91")) {
        digits.takeLast(10)
    } else {
        digits.take(15)
    }
}

/**
 * Multi-Step Booking Form:
 * Step 1: Service Type (Photography, Modeling, Production / Video Production)
 * Step 2: Preferred Date & Time
 * Step 3: User Details & Session Review
 * Step 4: Subtle Lottie-Style Success Animation & Fade-in Confirmation Message
 */
@Composable
fun MultiStepBookingForm(
    language: AppLanguage = AppLanguage.ENGLISH,
    initialServiceType: String = "Photography",
    initialDate: String = "",
    initialTime: String = "10:00 AM (Morning Key Light Bay)",
    initialLocation: String = "",
    initialNotes: String = "",
    defaultClientName: String = "",
    defaultPhoneNumber: String = "",
    isEmailVerified: Boolean = true,
    onResendVerification: (() -> Unit)? = null,
    onRefreshVerificationStatus: (() -> Unit)? = null,
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
    onViewBookings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser = remember { runCatching { Firebase.auth.currentUser }.getOrNull() }

    // Current Step Index (0, 1, 2, 3)
    var currentStep by remember { mutableIntStateOf(0) }

    // Form Field States
    var selectedCategory by remember {
        mutableStateOf(
            when {
                initialServiceType.startsWith("Modeling", ignoreCase = true) -> MultiStepServiceCategory.MODELING
                initialServiceType.startsWith("Production", ignoreCase = true) ||
                        initialServiceType.startsWith("Video", ignoreCase = true) -> MultiStepServiceCategory.PRODUCTION
                else -> MultiStepServiceCategory.PHOTOGRAPHY
            }
        )
    }
    var serviceType by remember { mutableStateOf(initialServiceType.ifBlank { "Photography" }) }

    LaunchedEffect(initialServiceType) {
        if (initialServiceType.isNotBlank()) {
            selectedCategory = when {
                initialServiceType.startsWith("Modeling", ignoreCase = true) -> MultiStepServiceCategory.MODELING
                initialServiceType.startsWith("Production", ignoreCase = true) ||
                        initialServiceType.startsWith("Video", ignoreCase = true) -> MultiStepServiceCategory.PRODUCTION
                else -> MultiStepServiceCategory.PHOTOGRAPHY
            }
            serviceType = initialServiceType
        }
    }

    // Date State
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

    // Time State
    val timeSlotOptions = listOf(
        "10:00 AM (Morning Key Light Bay)",
        "12:00 PM (Midday High-Key Flash)",
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

    // Validation & Success Feedback State
    var stepValidationError by remember { mutableStateOf<String?>(null) }
    var confirmedBookingId by remember { mutableStateOf("") }

    if (showDatePickerModal) {
        BookingDatePickerModal(
            initialDate = selectedDate,
            language = language,
            onDateSelected = { formatted ->
                selectedDate = formatted
                stepValidationError = null
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
                stepValidationError = null
            },
            onDismiss = { showTimePickerModal = false }
        )
    }

    // User Details State (prefilled smoothly from authenticated profile)
    var clientName by remember {
        mutableStateOf(
            defaultClientName.ifBlank { currentUser?.displayName.orEmpty() }
        )
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

    val isNameValid by remember { derivedStateOf { clientName.trim().length >= 2 } }
    val isPhoneValid by remember { derivedStateOf { contactNumber.trim().length in 8..15 } }
    val isDateValid by remember { derivedStateOf { selectedDate.trim().isNotEmpty() } }

    val photographyPackages = listOf(
        "Photography",
        "Photography (High-Fashion Editorial)",
        "Photography (Royal Pre-Wedding & Bridal)",
        "Photography (Commercial Product & Catalog)",
        "Photography (Portrait & Glamour Stills)"
    )
    val modelingPackages = listOf(
        "Modeling",
        "Modeling (Portfolio & Comp-Card Shoot)",
        "Modeling (Agency Audition Profile)",
        "Modeling (Runway & Lookbook Session)"
    )
    val productionPackages = listOf(
        "Video Production",
        "Production",
        "Video Production (4K Cinema Music Video Direction)",
        "Video Production (Commercial Brand Ad Film)",
        "Video Production (Fashion Model Reel in 4K HDR)"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.2.dp, Brush.verticalGradient(listOf(GoldPrimary, StudioOutline))),
                RoundedCornerShape(20.dp)
            )
            .testTag("multi_step_booking_form"),
        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =================================================================
            // MULTI-STEP PROGRESS STEPPER (Steps 1, 2, 3)
            // =================================================================
            if (currentStep < 3) {
                MultiStepProgressIndicator(
                    currentStep = currentStep,
                    language = language,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_step_indicator")
                )
            }

            // Step Validation Error Alert
            if (stepValidationError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF331616)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stepValidationError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // =================================================================
            // STEP CONTENT (Animated Transition)
            // =================================================================
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "step_content_transition"
            ) { targetStep ->
                when (targetStep) {
                    0 -> {
                        // -----------------------------------------------------
                        // STEP 1: SERVICE TYPE (Photography, Modeling, Production)
                        // -----------------------------------------------------
                        StepServiceTypeContent(
                            language = language,
                            selectedCategory = selectedCategory,
                            serviceType = serviceType,
                            photographyPackages = photographyPackages,
                            modelingPackages = modelingPackages,
                            productionPackages = productionPackages,
                            onCategorySelected = { cat ->
                                selectedCategory = cat
                                serviceType = if (cat == MultiStepServiceCategory.PRODUCTION) {
                                    "Video Production"
                                } else {
                                    cat.label
                                }
                                stepValidationError = null
                            },
                            onServiceTypeSelected = { pkg ->
                                serviceType = pkg
                                stepValidationError = null
                            },
                            onNext = {
                                if (serviceType.isBlank()) {
                                    stepValidationError = if (language == AppLanguage.HINDI) "कृपया सेवा चुनें" else "Please select a service type."
                                } else {
                                    stepValidationError = null
                                    currentStep = 1
                                }
                            }
                        )
                    }
                    1 -> {
                        // -----------------------------------------------------
                        // STEP 2: PREFERRED DATE & TIME
                        // -----------------------------------------------------
                        StepDateTimeContent(
                            language = language,
                            selectedDate = selectedDate,
                            selectedTime = selectedTime,
                            todayStr = todayStr,
                            tomorrowStr = tomorrowStr,
                            nextWeekendStr = nextWeekendStr,
                            timeSlotOptions = timeSlotOptions,
                            onDateChange = {
                                selectedDate = it
                                stepValidationError = null
                            },
                            onTimeChange = {
                                selectedTime = it
                                stepValidationError = null
                            },
                            onOpenDatePicker = { showDatePickerModal = true },
                            onOpenTimePicker = { showTimePickerModal = true },
                            onBack = {
                                stepValidationError = null
                                currentStep = 0
                            },
                            onNext = {
                                if (selectedDate.trim().isBlank() || selectedTime.trim().isBlank()) {
                                    stepValidationError = if (language == AppLanguage.HINDI) "कृपया सत्र की तारीख व समय चुनें" else "Please specify session date and time slot."
                                } else {
                                    stepValidationError = null
                                    currentStep = 2
                                }
                            }
                        )
                    }
                    2 -> {
                        // -----------------------------------------------------
                        // STEP 3: USER DETAILS & SESSION REVIEW
                        // -----------------------------------------------------
                        StepUserDetailsReviewContent(
                            language = language,
                            clientName = clientName,
                            contactNumber = contactNumber,
                            serviceType = serviceType,
                            date = selectedDate,
                            time = selectedTime,
                            location = location,
                            notes = notes,
                            isSubmitting = isSubmitting,
                            isEmailVerified = isEmailVerified,
                            onResendVerification = onResendVerification,
                            onRefreshVerificationStatus = onRefreshVerificationStatus,
                            isNameValid = isNameValid,
                            isPhoneValid = isPhoneValid,
                            onNameChange = {
                                clientName = it
                                stepValidationError = null
                            },
                            onPhoneChange = {
                                contactNumber = it
                                stepValidationError = null
                            },
                            onLocationChange = { location = it },
                            onNotesChange = { notes = it },
                            onBack = {
                                stepValidationError = null
                                currentStep = 1
                            },
                            onSubmit = {
                                if (currentUser != null && !isEmailVerified) {
                                    stepValidationError = if (language == AppLanguage.HINDI)
                                        "बुकिंग दर्ज करने से पहले ऊपर दिए गए बटन से अपना ईमेल सत्यापित करें।"
                                    else
                                        "Please verify your email address before reserving a studio session."
                                } else if (!isNameValid || !isPhoneValid || selectedDate.isBlank()) {
                                    stepValidationError = if (language == AppLanguage.HINDI)
                                        "कृपया मान्य नाम (2+ अक्षर) व 8–15 अंकों का फोन नंबर दर्ज करें"
                                    else
                                        "Please enter a valid client name (2+ chars) and contact phone number (8–15 digits)."
                                } else {
                                    stepValidationError = null
                                    onSaveToFirestore(
                                        clientName.trim(),
                                        contactNumber.trim(),
                                        serviceType.trim(),
                                        selectedDate.trim(),
                                        selectedTime.trim(),
                                        location.trim(),
                                        notes.trim()
                                    ) { newBookingId ->
                                        confirmedBookingId = newBookingId
                                        currentStep = 3
                                    }
                                }
                            }
                        )
                    }
                    3 -> {
                        // -----------------------------------------------------
                        // STEP 4: SUBTLE LOTTIE-STYLE ANIMATION & CONFIRMATION
                        // -----------------------------------------------------
                        LottieSuccessAnimationComponent(
                            clientName = clientName,
                            serviceType = serviceType,
                            date = selectedDate,
                            time = selectedTime,
                            location = location,
                            bookingId = confirmedBookingId,
                            language = language,
                            onViewTimeline = onViewBookings,
                            onBookAnother = {
                                currentStep = 0
                                selectedDate = tomorrowStr
                                notes = ""
                            },
                            onWhatsAppConsult = {
                                val summary = "Hello Star Artist Studio! Inquiring about booking #$confirmedBookingId for a $serviceType session on $selectedDate at $selectedTime. Client: $clientName."
                                val url = StudioContactDetails.buildWhatsAppUrl(summary)
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3-Step Visual Progress Stepper.
 */
@Composable
private fun MultiStepProgressIndicator(
    currentStep: Int,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        if (language == AppLanguage.HINDI) "1. सेवा" else "1. Service",
        if (language == AppLanguage.HINDI) "2. दिनांक/समय" else "2. Date & Time",
        if (language == AppLanguage.HINDI) "3. विवरण" else "3. Details"
    )

    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, title ->
            val isCompleted = currentStep > index
            val isActive = currentStep == index

            val nodeBgColor = when {
                isCompleted -> SuccessGreen
                isActive -> GoldPrimary
                else -> Color(0xFF242018)
            }
            val nodeTextColor = when {
                isCompleted -> Color.Black
                isActive -> StudioBlack
                else -> Color.Gray
            }
            val titleColor = when {
                isActive -> GoldLight
                isCompleted -> Color.White
                else -> Color.Gray
            }

            // Step Node & Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("step_node_$index")
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(nodeBgColor)
                        .border(1.dp, if (isActive) GoldLight else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = StudioBlack, modifier = Modifier.size(14.dp))
                    } else {
                        Text(
                            text = "${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = nodeTextColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                    color = titleColor
                )
            }

            // Connector Line between steps
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 6.dp)
                        .background(if (currentStep > index) GoldPrimary else Color.DarkGray)
                )
            }
        }
    }
}

/**
 * Step 1 Content: Service Type (Photography, Modeling, Production)
 */
@Composable
private fun StepServiceTypeContent(
    language: AppLanguage,
    selectedCategory: MultiStepServiceCategory,
    serviceType: String,
    photographyPackages: List<String>,
    modelingPackages: List<String>,
    productionPackages: List<String>,
    onCategorySelected: (MultiStepServiceCategory) -> Unit,
    onServiceTypeSelected: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("step_service_type"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == AppLanguage.HINDI) "चरण 1: सेवा प्रकार चुनें (Photography / Modeling / Production)" else "Step 1: Choose Service Type (Photography, Modeling, Production)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GoldLight,
                fontSize = 13.5.sp
            )
        }

        // 3 Main Service Category Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("service_type_selector"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MultiStepServiceCategory.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                val typeTag = when (cat) {
                    MultiStepServiceCategory.PHOTOGRAPHY -> "service_type_photography"
                    MultiStepServiceCategory.MODELING -> "service_type_modeling"
                    MultiStepServiceCategory.PRODUCTION -> "service_type_production"
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 72.dp)
                        .border(
                            if (isSelected) 1.5.dp else 1.2.dp,
                            if (isSelected) GoldPrimary else StudioOutline,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onCategorySelected(cat) }
                        .testTag(typeTag),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) GoldContainer else StudioCardSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = cat.label,
                            tint = if (isSelected) GoldPrimary else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cat.label,
                            color = if (isSelected) GoldLight else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Specialized Package Selection Chips
        Text(
            text = if (language == AppLanguage.HINDI) "विशिष्ट सत्र पैकेज चुनें:" else "Select Specific Shoot Package:",
            color = Color.LightGray,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold
        )

        val activePackages = when (selectedCategory) {
            MultiStepServiceCategory.PHOTOGRAPHY -> photographyPackages
            MultiStepServiceCategory.MODELING -> modelingPackages
            MultiStepServiceCategory.PRODUCTION -> productionPackages
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(activePackages) { pkg ->
                val isSelected = serviceType == pkg
                FilterChip(
                    selected = isSelected,
                    onClick = { onServiceTypeSelected(pkg) },
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

        Spacer(modifier = Modifier.height(6.dp))

        // Next Button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("multi_step_next_button"),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = if (language == AppLanguage.HINDI) "आगे बढ़ें: तारीख व समय (Next: Date & Time)" else "Next: Select Date & Time",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Step 2 Content: Preferred Date & Time
 */
@Composable
private fun StepDateTimeContent(
    language: AppLanguage,
    selectedDate: String,
    selectedTime: String,
    todayStr: String,
    tomorrowStr: String,
    nextWeekendStr: String,
    timeSlotOptions: List<String>,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onOpenDatePicker: () -> Unit,
    onOpenTimePicker: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("step_date_time"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == AppLanguage.HINDI) "चरण 2: पसंदीदा तारीख व समय (Date & Time)" else "Step 2: Choose Preferred Date & Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GoldLight,
                fontSize = 13.5.sp
            )
        }

        // Material 3 Date Picker Trigger Button
        OutlinedButton(
            onClick = onOpenDatePicker,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("booking_date_picker_button"),
            border = BorderStroke(1.dp, GoldPrimary),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight)
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (selectedDate.isNotBlank()) {
                    if (language == AppLanguage.HINDI) "तारीख: $selectedDate (कैलेंडर से बदलें)" else "Date: $selectedDate (Open Date Picker)"
                } else {
                    if (language == AppLanguage.HINDI) "कैलेंडर से तारीख चुनें" else "Open Date Picker"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        // Quick Date Chips
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
                        .clickable { onDateChange(dateVal) }
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

        // Direct Date Input Field
        OutlinedTextField(
            value = selectedDate,
            onValueChange = onDateChange,
            label = { Text("Date (YYYY-MM-DD)") },
            placeholder = { Text(tomorrowStr) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("booking_date_input"),
            shape = RoundedCornerShape(10.dp),
            colors = multiStepFieldColors()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Material 3 Time Picker Trigger Button
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccessTime, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == AppLanguage.HINDI) "समय पिकर या लाइटिंग बे स्लॉट चुनें:" else "Time Picker or Lighting Bay Slot:",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        OutlinedButton(
            onClick = onOpenTimePicker,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("booking_time_picker_button"),
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

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("booking_time_selector"),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(timeSlotOptions) { slot ->
                val isSelected = selectedTime == slot
                FilterChip(
                    selected = isSelected,
                    onClick = { onTimeChange(slot) },
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

        OutlinedTextField(
            value = selectedTime,
            onValueChange = onTimeChange,
            label = { Text("Time Slot") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("booking_time_input"),
            shape = RoundedCornerShape(10.dp),
            colors = multiStepFieldColors()
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Back and Next Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("multi_step_back_button"),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (language == AppLanguage.HINDI) "पीछे (Back)" else "Back", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("multi_step_next_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = StudioBlack),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.HINDI) "आगे बढ़ें: ग्राहक विवरण" else "Next: Client Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * Step 3 Content: User Details & Session Review
 */
@Composable
private fun StepUserDetailsReviewContent(
    language: AppLanguage,
    clientName: String,
    contactNumber: String,
    serviceType: String,
    date: String,
    time: String,
    location: String,
    notes: String,
    isSubmitting: Boolean,
    isEmailVerified: Boolean,
    onResendVerification: (() -> Unit)?,
    onRefreshVerificationStatus: (() -> Unit)?,
    isNameValid: Boolean,
    isPhoneValid: Boolean,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val locationOptions = listOf(
        "Studio (Talwara Khurd - 4K Lighting Bay)",
        "Outdoor (Sirsa & Ellenabad Region)",
        "Client Location (Custom Set)"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("step_user_details"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == AppLanguage.HINDI) "चरण 3: ग्राहक विवरण व समीक्षा (Details & Review)" else "Step 3: Client Details & Session Review",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GoldLight,
                fontSize = 13.5.sp
            )
        }

        // Client Name Input
        OutlinedTextField(
            value = clientName,
            onValueChange = onNameChange,
            label = { Text("Client Full Name") },
            placeholder = { Text("e.g. Gurpreet Singh") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            isError = clientName.isNotEmpty() && !isNameValid,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("booking_clientname_input"),
            shape = RoundedCornerShape(10.dp),
            colors = multiStepFieldColors()
        )

        // Contact Phone Input
        OutlinedTextField(
            value = contactNumber,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }
                if (digits.length <= 15) onPhoneChange(digits)
            },
            label = { Text("Contact Phone Number") },
            placeholder = { Text("e.g. 9812890081") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            isError = contactNumber.isNotEmpty() && !isPhoneValid,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("booking_phone_input"),
            shape = RoundedCornerShape(10.dp),
            colors = multiStepFieldColors()
        )

        // Shoot Location Selector Chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (language == AppLanguage.HINDI) "शूटिंग स्थान (Shoot Location):" else "Shoot Location:",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(locationOptions) { loc ->
                    val isSelected = location == loc
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLocationChange(loc) },
                        label = { Text(loc, fontSize = 11.sp) },
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
                        modifier = Modifier.testTag("location_chip_${loc.take(6).replace(" ", "")}")
                    )
                }
            }
        }

        // Special Notes
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Session Notes & Lighting Concepts") },
            placeholder = { Text("e.g. Royal jewelry set, slow-mo 4K gimbal sequence") },
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("booking_notes_input"),
            shape = RoundedCornerShape(10.dp),
            colors = multiStepFieldColors()
        )

        // Session Review Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("booking_session_review_card"),
            colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, StudioOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.HINDI) "सत्र सारांश (Booking Summary):" else "Session Review Summary:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
                Text(
                    text = "• Service: $serviceType",
                    fontSize = 11.5.sp,
                    color = TextPrimary
                )
                Text(
                    text = "• Scheduled: $date at $time",
                    fontSize = 11.5.sp,
                    color = TextPrimary
                )
                Text(
                    text = "• Client: ${clientName.ifBlank { "Client" }} (${contactNumber.ifBlank { "Phone" }})",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )
                Text(
                    text = "• Location: $location",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        if (!isEmailVerified) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GoldContainer),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MarkEmailUnread, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "बुकिंग सहेजने के लिए ईमेल सत्यापित करें" else "Verify email before reserving studio slot",
                            color = TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (onResendVerification != null || onRefreshVerificationStatus != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onResendVerification != null) {
                                OutlinedButton(
                                    onClick = onResendVerification,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp),
                                    border = BorderStroke(1.dp, GoldPrimary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "ईमेल भेजें" else "Resend Link",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (onRefreshVerificationStatus != null) {
                                OutlinedButton(
                                    onClick = onRefreshVerificationStatus,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp),
                                    border = BorderStroke(1.dp, GoldPrimary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "स्थिति जांचें" else "Check Verified",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Back and Submit to Firestore Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 50.dp)
                    .testTag("multi_step_back_button"),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (language == AppLanguage.HINDI) "पीछे" else "Back", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .weight(1.8f)
                    .defaultMinSize(minHeight = 50.dp)
                    .testTag("multi_step_submit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEmailVerified) GoldPrimary else GoldDark,
                    contentColor = if (isEmailVerified) MaterialTheme.colorScheme.onPrimary else Color.White
                ),
                shape = RoundedCornerShape(10.dp),
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
                            if (language == AppLanguage.HINDI) "ईमेल सत्यापित करें" else "Verify Email First"
                        } else {
                            if (language == AppLanguage.HINDI) "सत्र अनुरोध सहेजें (Book Session)" else "Confirm & Save Booking"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun multiStepFieldColors() = OutlinedTextFieldDefaults.colors(
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
