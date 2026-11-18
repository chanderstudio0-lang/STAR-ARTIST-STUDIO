package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import com.example.ui.components.MultiStepBookingForm
import com.example.ui.components.ServiceBookingFormComponent
import com.example.ui.components.VoiceBookingAssistantPanel
import com.example.ui.components.VoiceToTextInputComponent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.ServiceReview
import com.example.data.model.ShootBooking
import com.example.data.model.UserProfile
import com.example.ui.components.BookingReviewStatusBlock
import com.example.ui.components.BookingsSkeletonList
import com.example.ui.components.InteractiveBookingCalendar
import com.example.ui.components.ServiceReviewDialog
import com.example.ui.components.StudioReviewsShowcaseCard
import com.example.ui.theme.ErrorRed
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
import com.example.ui.viewmodel.VoiceBookingPrefill
import com.example.util.ParsedVoiceBookingCommand
import com.example.util.StudioContactDetails
import com.example.util.StudioStrings
import com.example.util.VoiceBookingCommandParser
import androidx.compose.runtime.LaunchedEffect
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class PrimaryServiceCategory(val label: String, val icon: ImageVector) {
    MODELING("Modeling", Icons.Default.Person),
    PHOTOGRAPHY("Photography", Icons.Default.CameraAlt),
    VIDEO("Video Production", Icons.Default.Videocam)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShootBookingScreen(
    language: AppLanguage,
    myBookings: List<ShootBooking>,
    isSubmitting: Boolean,
    isLoading: Boolean = false,
    userProfile: UserProfile? = null,
    preselectedService: String? = null,
    onConsumePreselectedService: () -> Unit = {},
    voiceBookingPrefill: VoiceBookingPrefill? = null,
    onConsumeVoicePrefill: () -> Unit = {},
    onParseVoiceCommand: suspend (String) -> ParsedVoiceBookingCommand = { transcript ->
        VoiceBookingCommandParser.parseLocally(transcript)
    },
    myReviews: List<ServiceReview> = emptyList(),
    studioReviews: List<ServiceReview> = emptyList(),
    isEmailVerified: Boolean = runCatching { Firebase.auth.currentUser?.isEmailVerified }.getOrNull() ?: false,
    onResendVerification: ((onResult: (Boolean, String) -> Unit) -> Unit)? = null,
    onRefreshVerificationStatus: ((onComplete: (Boolean) -> Unit) -> Unit)? = null,
    onSubmitReview: (
        bookingId: String,
        serviceType: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit
    ) -> Unit = { _, _, _, _, _ -> },
    onSubmit: (
        clientName: String,
        contactNumber: String,
        serviceType: String,
        preferredDate: String,
        preferredTime: String,
        location: String,
        notes: String,
        onSuccess: (bookingId: String) -> Unit
    ) -> Unit,
    onOpenFullMyBookings: (() -> Unit)? = null
) {
    val s = StudioStrings(language)
    val coroutineScope = rememberCoroutineScope()
    val currentUser = remember { runCatching { Firebase.auth.currentUser }.getOrNull() }
    val userEmail = currentUser?.email ?: "member@starartist.com"

    var isVerifiedState by remember(isEmailVerified) { mutableStateOf(isEmailVerified) }
    var resendFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingVerification by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var bookingMode by remember { mutableIntStateOf(0) } // 0 = Guided 3-Step, 1 = Calendar & Voice
    var reviewTargetBooking by remember { mutableStateOf<ShootBooking?>(null) }

    val defaultClientName = userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.displayName.orEmpty()
    val defaultPhoneNumber = userProfile?.phoneNumber.orEmpty()

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val tomorrowStr = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        dateFormat.format(cal.time)
    }

    var calendarSelectedDate by remember { mutableStateOf(tomorrowStr) }
    var calendarSelectedTime by remember { mutableStateOf("10:00 AM (Morning Key Light)") }
    var activeInitialService by remember { mutableStateOf("Photography") }
    var initialLocationPrefill by remember { mutableStateOf("") }
    var initialNotesPrefill by remember { mutableStateOf("") }
    var voiceClientNamePrefill by remember { mutableStateOf("") }
    var voicePhonePrefill by remember { mutableStateOf("") }

    val effectiveClientName = voiceClientNamePrefill.ifBlank { defaultClientName }
    val effectivePhoneNumber = voicePhonePrefill.ifBlank { defaultPhoneNumber }

    LaunchedEffect(preselectedService) {
        if (!preselectedService.isNullOrBlank()) {
            activeInitialService = if (preselectedService.startsWith("Video (", ignoreCase = true)) {
                "Video Production ${preselectedService.removePrefix("Video ").trim()}"
            } else {
                preselectedService
            }
            selectedTab = 0
            onConsumePreselectedService()
        }
    }

    LaunchedEffect(voiceBookingPrefill) {
        val prefill = voiceBookingPrefill
        if (prefill != null) {
            if (prefill.serviceType.isNotBlank()) activeInitialService = prefill.serviceType
            if (prefill.preferredDate.isNotBlank()) calendarSelectedDate = prefill.preferredDate
            if (prefill.preferredTime.isNotBlank()) calendarSelectedTime = prefill.preferredTime
            if (prefill.location.isNotBlank()) initialLocationPrefill = prefill.location
            if (prefill.notes.isNotBlank()) initialNotesPrefill = prefill.notes
            if (prefill.clientName.isNotBlank()) voiceClientNamePrefill = prefill.clientName
            if (prefill.contactNumber.isNotBlank()) voicePhonePrefill = prefill.contactNumber
            selectedTab = 0
            bookingMode = 0
            onConsumeVoicePrefill()
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("shoot_booking_screen")
    ) {
        // Tab Header: Book Shoot vs My Bookings
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = StudioBlack,
            contentColor = GoldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = GoldPrimary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = if (selectedTab == 0) GoldPrimary else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "नई बुकिंग" else "Book Shoot",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) GoldPrimary else Color.Gray
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (selectedTab == 1) GoldPrimary else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = (if (language == AppLanguage.HINDI) "मेरी बुकिंग्स" else "My Bookings") + " (${myBookings.size})",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) GoldPrimary else Color.Gray
                        )
                    }
                }
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("booking_form"),
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Email Verification Gate Banner for Unverified Users
                if (!isVerifiedState) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(GoldPrimary, Color(0xFFE65100)))),
                                    RoundedCornerShape(16.dp)
                                )
                                .testTag("email_verification_required_banner")
                                .testTag("email_verification_gate"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF221706)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(GoldPrimary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MarkEmailUnread,
                                            contentDescription = "Unverified Email Warning",
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (language == AppLanguage.HINDI)
                                                "शूट बुकिंग के लिए ईमेल सत्यापन आवश्यक है"
                                            else
                                                "Email Verification Required to Book Shoots",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldLight
                                        )
                                        Text(
                                            text = userEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            modifier = Modifier.testTag("unverified_user_email")
                                        )
                                    }
                                }

                                Text(
                                    text = if (language == AppLanguage.HINDI)
                                        "स्टेज स्लॉट, 4K उपकरण और मॉडलिंग शेड्यूल आरक्षित करने के लिए अपने ईमेल पते की पुष्टि करें। कृपया इनबॉक्स में सत्यापन लिंक पर क्लिक करें।"
                                    else
                                        "To reserve studio lighting bays, 4K camera gear, and verified model schedules, clients must confirm their email before submitting booking requests.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD4C8B0),
                                    lineHeight = 16.sp
                                )

                                if (resendFeedbackMessage != null) {
                                    Text(
                                        text = resendFeedbackMessage ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("verification_feedback_text")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (onResendVerification != null) {
                                                onResendVerification { _, msg ->
                                                    resendFeedbackMessage = msg
                                                }
                                            } else {
                                                coroutineScope.launch {
                                                    try {
                                                        currentUser?.sendEmailVerification()?.await()
                                                        resendFeedbackMessage = if (language == AppLanguage.HINDI)
                                                            "सत्यापन लिंक $userEmail पर भेजा गया!"
                                                        else
                                                            "Verification link sent to $userEmail!"
                                                    } catch (e: Exception) {
                                                        resendFeedbackMessage = e.localizedMessage ?: "Failed to resend."
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GoldPrimary,
                                            contentColor = Color(0xFF141000)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("resend_verification_button")
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "पुनः भेजें" else "Resend Email",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            isCheckingVerification = true
                                            if (onRefreshVerificationStatus != null) {
                                                onRefreshVerificationStatus { verified ->
                                                    isVerifiedState = verified
                                                    isCheckingVerification = false
                                                    resendFeedbackMessage = if (verified)
                                                        "Email confirmed! Booking unlocked."
                                                    else
                                                        "Email not yet confirmed. Please click link in inbox."
                                                }
                                            } else {
                                                coroutineScope.launch {
                                                    try {
                                                        currentUser?.reload()?.await()
                                                        val verified = currentUser?.isEmailVerified ?: false
                                                        isVerifiedState = verified
                                                        isCheckingVerification = false
                                                        resendFeedbackMessage = if (verified)
                                                            "Email confirmed! Booking unlocked."
                                                        else
                                                            "Email not yet confirmed. Please click link in inbox."
                                                    } catch (e: Exception) {
                                                        isCheckingVerification = false
                                                        resendFeedbackMessage = e.localizedMessage ?: "Check failed."
                                                    }
                                                }
                                            }
                                        },
                                        border = BorderStroke(1.dp, GoldPrimary),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("refresh_verification_status_button"),
                                        enabled = !isCheckingVerification
                                    ) {
                                        if (isCheckingVerification) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = GoldPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (language == AppLanguage.HINDI) "स्थिति जांचें" else "Check Status",
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

                // Header Banner: Studio Booking Welcome Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(1.dp, Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.6f), StudioOutline))),
                                RoundedCornerShape(16.dp)
                            ),
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
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GoldContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "RESERVE PRODUCTION SLOT",
                                        color = GoldLight,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Text(
                                    text = "10 AM - 8 PM",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = s.bookingTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = s.bookingSub,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFC0C0C0),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Booking Mode Switcher (Guided 3-Step Form vs Voice Booking vs Calendar & Voice Slot Form)
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("booking_mode_switcher"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = bookingMode == 0,
                                onClick = { bookingMode = 0 },
                                label = {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "गाइडेड 3-चरण" else "Guided 3-Step",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.EventAvailable,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = StudioBlack,
                                    selectedLeadingIconColor = StudioBlack,
                                    containerColor = StudioCardSurface,
                                    labelColor = Color.LightGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = bookingMode == 0,
                                    borderColor = if (bookingMode == 0) GoldPrimary else StudioOutline
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("mode_guided_booking_chip")
                            )
                        }

                        item {
                            FilterChip(
                                selected = bookingMode == 2,
                                onClick = { bookingMode = 2 },
                                label = {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "वॉइस बुकिंग" else "Voice Booking",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = StudioBlack,
                                    selectedLeadingIconColor = StudioBlack,
                                    containerColor = StudioCardSurface,
                                    labelColor = Color.LightGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = bookingMode == 2,
                                    borderColor = if (bookingMode == 2) GoldPrimary else StudioOutline
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("mode_voice_booking_chip")
                            )
                        }

                        item {
                            FilterChip(
                                selected = bookingMode == 1,
                                onClick = { bookingMode = 1 },
                                label = {
                                    Text(
                                        text = if (language == AppLanguage.HINDI) "कैलेंडर व स्लॉट" else "Calendar & Voice",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = StudioBlack,
                                    selectedLeadingIconColor = StudioBlack,
                                    containerColor = StudioCardSurface,
                                    labelColor = Color.LightGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = bookingMode == 1,
                                    borderColor = if (bookingMode == 1) GoldPrimary else StudioOutline
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("mode_calendar_booking_chip")
                            )
                        }
                    }
                }

                when (bookingMode) {
                    0 -> {
                        // Guided Multi-Step Booking Form with Profile Prefill, Verification Gate & Real Firestore Doc ID
                        item {
                            MultiStepBookingForm(
                                language = language,
                                initialServiceType = activeInitialService,
                                initialDate = calendarSelectedDate,
                                initialTime = calendarSelectedTime,
                                initialLocation = initialLocationPrefill,
                                initialNotes = initialNotesPrefill,
                                defaultClientName = effectiveClientName,
                                defaultPhoneNumber = effectivePhoneNumber,
                                isEmailVerified = isVerifiedState,
                                onResendVerification = onResendVerification?.let { resend ->
                                    {
                                        resend { _, msg ->
                                            resendFeedbackMessage = msg
                                        }
                                    }
                                },
                                onRefreshVerificationStatus = onRefreshVerificationStatus?.let { refresh ->
                                    {
                                        isCheckingVerification = true
                                        refresh { verified ->
                                            isVerifiedState = verified
                                            isCheckingVerification = false
                                            resendFeedbackMessage = if (verified)
                                                "Email confirmed! Booking unlocked."
                                            else
                                                "Email not yet confirmed. Please click link in inbox."
                                        }
                                    }
                                },
                                isSubmitting = isSubmitting,
                                onSaveToFirestore = { name, phone, sType, date, time, loc, sNotes, onSuccessWithId ->
                                    onSubmit(name, phone, sType, date, time, loc, sNotes) { realDocId ->
                                        onSuccessWithId(realDocId)
                                    }
                                },
                                onViewBookings = {
                                    selectedTab = 1
                                }
                            )
                        }
                    }
                    2 -> {
                        // Voice-Controlled Booking Assistant (Mic + NLU Command Parser + Direct Confirm or Form Prefill)
                        item {
                            VoiceBookingAssistantPanel(
                                language = language,
                                defaultClientName = effectiveClientName,
                                defaultPhoneNumber = effectivePhoneNumber,
                                isEmailVerified = isVerifiedState,
                                isSubmitting = isSubmitting,
                                onParseCommand = onParseVoiceCommand,
                                onSubmitBooking = { name, phone, sType, date, time, loc, sNotes, onSuccessWithId ->
                                    onSubmit(name, phone, sType, date, time, loc, sNotes) { realDocId ->
                                        onSuccessWithId(realDocId)
                                    }
                                },
                                onApplyToFullForm = { prefill ->
                                    if (prefill.serviceType.isNotBlank()) activeInitialService = prefill.serviceType
                                    if (prefill.preferredDate.isNotBlank()) calendarSelectedDate = prefill.preferredDate
                                    if (prefill.preferredTime.isNotBlank()) calendarSelectedTime = prefill.preferredTime
                                    if (prefill.location.isNotBlank()) initialLocationPrefill = prefill.location
                                    if (prefill.notes.isNotBlank()) initialNotesPrefill = prefill.notes
                                    if (prefill.clientName.isNotBlank()) voiceClientNamePrefill = prefill.clientName
                                    if (prefill.contactNumber.isNotBlank()) voicePhonePrefill = prefill.contactNumber
                                    bookingMode = 0
                                },
                                onViewBookings = {
                                    selectedTab = 1
                                }
                            )
                        }
                    }
                    else -> {
                        // Calendar Grid + Single-Page Quick Session Request + Voice Notes
                        item {
                            InteractiveBookingCalendar(
                                language = language,
                                selectedDate = calendarSelectedDate,
                                selectedTimeSlot = calendarSelectedTime,
                                onDateSelected = { date ->
                                    calendarSelectedDate = date
                                },
                                onTimeSlotSelected = { slot ->
                                    calendarSelectedTime = slot
                                }
                            )
                        }

                        item {
                            ServiceBookingFormComponent(
                                language = language,
                                initialServiceType = activeInitialService,
                                initialDate = calendarSelectedDate,
                                initialTime = calendarSelectedTime,
                                initialLocation = initialLocationPrefill,
                                initialNotes = initialNotesPrefill,
                                defaultClientName = effectiveClientName,
                                defaultPhoneNumber = effectivePhoneNumber,
                                isEmailVerified = isVerifiedState,
                                onDateChanged = { calendarSelectedDate = it },
                                onTimeChanged = { calendarSelectedTime = it },
                                onViewBookings = { selectedTab = 1 },
                                isSubmitting = isSubmitting,
                                onSaveToFirestore = { name, phone, sType, date, time, loc, sNotes, onSuccessWithId ->
                                    onSubmit(name, phone, sType, date, time, loc, sNotes) { realDocId ->
                                        onSuccessWithId(realDocId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // My Bookings Tab
            if (isLoading) {
                BookingsSkeletonList()
            } else if (myBookings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(StudioElevatedSurface)
                                .border(1.dp, StudioOutline, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "आपने अभी तक कोई शूट बुक नहीं किया है।"
                            else
                                "No shoot bookings found for your account.",
                            color = Color(0xFFC0C0C0),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "अपनी मनपसंद तारीख और समय स्लॉट आरक्षित करने के लिए नया शूट बुक करें।"
                            else
                                "Reserve your preferred date and 4K production time slot.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { selectedTab = 0 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color(0xFF141000)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "शूट बुक करें" else "Book a Shoot",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                var selectedStatusFilter by remember { mutableStateOf("All") }
                val filteredBookings = remember(myBookings, selectedStatusFilter) {
                    if (selectedStatusFilter == "All") {
                        myBookings
                    } else {
                        myBookings.filter {
                            normalizeBookingStatusKey(it.status) == selectedStatusFilter
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = (if (language == AppLanguage.HINDI) "आपकी आरक्षित शूटिंग्स" else "Your Reserved Shootings") + " (${myBookings.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold
                                )
                                if (onOpenFullMyBookings != null) {
                                    OutlinedButton(
                                        onClick = onOpenFullMyBookings,
                                        border = BorderStroke(1.dp, GoldPrimary),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("open_full_my_bookings_btn")
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "विस्तृत दृश्य" else "Full Schedule",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.testTag("booking_status_filter_row")
                            ) {
                                val filterOptions = listOf("All", "Confirmed", "Pending", "Completed", "Cancelled")
                                items(filterOptions) { option ->
                                    val count = if (option == "All") {
                                        myBookings.size
                                    } else {
                                        myBookings.count { normalizeBookingStatusKey(it.status) == option }
                                    }
                                    val selected = selectedStatusFilter == option
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedStatusFilter = option },
                                        label = {
                                            Text(
                                                text = "$option ($count)",
                                                fontSize = 11.5.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoldPrimary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            containerColor = StudioCardSurface,
                                            labelColor = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selected,
                                            borderColor = if (selected) GoldPrimary else StudioOutline
                                        ),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("shoot_booking_filter_${option.lowercase(Locale.ROOT)}")
                                    )
                                }
                            }
                        }
                    }

                    if (filteredBookings.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, StudioOutline, RoundedCornerShape(14.dp)),
                                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.HINDI)
                                        "चयनित स्थिति में कोई बुकिंग नहीं है।"
                                    else
                                        "No bookings match the selected status filter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(filteredBookings, key = { it.id.ifEmpty { "${it.preferredDate}_${it.serviceType}" } }) { booking ->
                            val existingReview = myReviews.find { it.bookingId == booking.id }
                            BookingStatusCard(
                                booking = booking,
                                language = language,
                                existingReview = existingReview,
                                onOpenReviewDialog = { reviewTargetBooking = booking }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        StudioReviewsShowcaseCard(
                            reviews = studioReviews,
                            language = language
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateQuickChip(
    label: String,
    dateVal: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) GoldPrimary else StudioElevatedSurface)
            .border(
                1.dp,
                if (isSelected) GoldLight else StudioOutline,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) StudioBlack else Color(0xFFC0C0C0),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

data class BookingStatusVisual(
    val canonicalStatus: String,
    val label: String,
    val icon: ImageVector,
    val accentColor: Color
)

fun normalizeBookingStatusKey(rawStatus: String): String {
    return when (rawStatus.trim().lowercase(Locale.ROOT)) {
        "confirmed", "approved" -> "Confirmed"
        "completed", "done" -> "Completed"
        "cancelled", "canceled", "rejected" -> "Cancelled"
        else -> "Pending"
    }
}

@Composable
fun resolveBookingStatusVisual(
    rawStatus: String,
    language: AppLanguage = AppLanguage.ENGLISH
): BookingStatusVisual {
    val canonical = normalizeBookingStatusKey(rawStatus)
    val isHindi = language == AppLanguage.HINDI
    val isLight = StudioTheme.isLight
    return when (canonical) {
        "Confirmed" -> BookingStatusVisual(
            canonicalStatus = "Confirmed",
            label = if (isHindi) "पुष्ट (Confirmed)" else "Confirmed",
            icon = Icons.Default.CheckCircle,
            accentColor = if (isLight) Color(0xFF1B7A34) else SuccessGreen
        )
        "Completed" -> BookingStatusVisual(
            canonicalStatus = "Completed",
            label = if (isHindi) "पूर्ण (Completed)" else "Completed",
            icon = Icons.Default.TaskAlt,
            accentColor = if (isLight) GoldDark else GoldLight
        )
        "Cancelled" -> BookingStatusVisual(
            canonicalStatus = "Cancelled",
            label = if (isHindi) "रद्द (Cancelled)" else "Cancelled",
            icon = Icons.Default.Cancel,
            accentColor = if (isLight) Color(0xFFB91C1C) else ErrorRed
        )
        else -> BookingStatusVisual(
            canonicalStatus = "Pending",
            label = if (isHindi) "लंबित (Pending)" else "Pending",
            icon = Icons.Default.Schedule,
            accentColor = if (isLight) Color(0xFFB45309) else Color(0xFFF59E0B)
        )
    }
}

@Composable
fun BookingStatusBadge(
    status: String,
    language: AppLanguage = AppLanguage.ENGLISH,
    modifier: Modifier = Modifier
) {
    val visual = resolveBookingStatusVisual(status, language)
    Surface(
        color = visual.accentColor.copy(alpha = 0.16f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, visual.accentColor),
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Booking status: ${visual.label}"
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = visual.accentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = visual.label,
                color = visual.accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
fun BookingStatusCard(
    booking: ShootBooking,
    language: AppLanguage = AppLanguage.ENGLISH,
    existingReview: ServiceReview? = null,
    onOpenReviewDialog: () -> Unit = {}
) {
    val statusVisual = resolveBookingStatusVisual(booking.status, language)
    val statusColor = statusVisual.accentColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(statusColor.copy(alpha = 0.55f), StudioOutline)),
                RoundedCornerShape(14.dp)
            )
            .testTag("booking_card_${booking.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = booking.serviceType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    BookingStatusBadge(
                        status = booking.status,
                        language = language,
                        modifier = Modifier.testTag("booking_status_badge_${booking.id}")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Client: ${booking.clientName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Phone: ${booking.contactNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

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
                        color = GoldLight,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = booking.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                if (booking.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioElevatedSurface)
                            .border(0.5.dp, StudioOutline, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Instructions: ${booking.notes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                BookingReviewStatusBlock(
                    booking = booking,
                    existingReview = existingReview,
                    language = language,
                    onOpenReviewDialog = onOpenReviewDialog
                )
            }
        }
    }
}
