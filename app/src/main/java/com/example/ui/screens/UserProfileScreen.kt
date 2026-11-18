package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.data.local.entity.ArtistBioEntity
import com.example.data.local.entity.ProjectDraftEntity
import com.example.data.model.AppLanguage
import com.example.data.model.ServicePackage
import com.example.data.model.ServicePackageCategory
import com.example.data.model.ServiceReview
import com.example.data.model.ShootBooking
import com.example.data.model.StudioServicesData
import com.example.data.model.UserProfile
import com.example.ui.components.ArtistBioAndSpecialtyCard
import com.example.ui.components.ArtistContactInfoCard
import com.example.ui.components.ArtistHeroIdentityCard
import com.example.ui.components.ArtistPortfolioLinksCard
import com.example.ui.components.ArtistProfileTopBar
import com.example.ui.components.BookingApprovalStageCard
import com.example.ui.components.BookingReviewStatusBlock
import com.example.ui.components.LanguageToggleComponent
import com.example.ui.components.ProfilePhotoComponent
import com.example.ui.components.ServiceReviewDialog
import com.example.ui.components.ThemeToggleComponent
import com.example.ui.components.computeArtistProfileCompleteness
import com.example.ui.components.encodeHeadshotUriToDataUrl
import com.example.ui.components.normalizePortfolioLink
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

val artistSpecialtyOptions = listOf(
    "Fashion Model",
    "Photographer",
    "Videographer",
    "Music Artist",
    "Makeup Artist",
    "Studio Client"
)

/**
 * Pure validation helper for artist profile updates before persisting to Cloud Firestore.
 */
fun validateArtistProfileInput(
    displayName: String,
    phoneNumber: String,
    bio: String
): String? {
    if (displayName.trim().length < 2) {
        return "Display name must be at least 2 characters."
    }
    val trimmedPhone = phoneNumber.trim()
    if (trimmedPhone.isNotEmpty() && (trimmedPhone.length < 8 || trimmedPhone.length > 30)) {
        return "Contact number must be between 8 and 30 characters."
    }
    if (bio.trim().length > 500) {
        return "Artist bio must be 500 characters or less."
    }
    return null
}

/**
 * Formats an artist's contact details, portfolio links, and bio into a shareable directory summary.
 */
fun buildArtistProfileSummaryText(
    displayName: String,
    specialty: String,
    city: String,
    phoneNumber: String,
    email: String,
    instagramHandle: String,
    portfolioLinks: List<String>,
    bio: String
): String {
    return buildString {
        append(displayName.trim().ifBlank { "Star Artist Member" })
        if (specialty.isNotBlank()) append(" • ${specialty.trim()}")
        if (city.isNotBlank()) append("\nLocation: ${city.trim()}")
        if (phoneNumber.isNotBlank()) append("\nPhone / WhatsApp: ${phoneNumber.trim()}")
        if (email.isNotBlank()) append("\nEmail: ${email.trim()}")
        if (instagramHandle.isNotBlank()) append("\nInstagram: ${instagramHandle.trim()}")
        if (portfolioLinks.isNotEmpty()) {
            append("\nPortfolio Links:")
            portfolioLinks.forEach { append("\n- $it") }
        }
        if (bio.isNotBlank()) append("\nBio: ${bio.trim()}")
    }
}

@Composable
fun UserProfileScreen(
    userProfile: UserProfile?,
    userEmail: String?,
    currentUserId: String,
    modelsCount: Int,
    bookingsCount: Int,
    myBookings: List<ShootBooking>,
    savedProjects: List<ProjectDraftEntity>,
    myReviews: List<ServiceReview> = emptyList(),
    favoritePackageIds: Set<String> = emptySet(),
    localArtistBio: ArtistBioEntity? = null,
    isEmailVerified: Boolean,
    isSubmitting: Boolean,
    isStudioLight: Boolean,
    language: AppLanguage,
    onBack: () -> Unit,
    onSaveLocalBio: (
        specialty: String,
        bioText: String,
        onComplete: (Boolean, String) -> Unit
    ) -> Unit = { _, _, onComplete -> onComplete(true, "Professional bio saved locally on your device!") },
    onUpdateArtistProfile: (
        displayName: String,
        avatarUrl: String,
        phoneNumber: String,
        city: String,
        instagramHandle: String,
        specialty: String,
        bio: String,
        portfolioLinks: List<String>,
        onComplete: (Boolean, String) -> Unit
    ) -> Unit,
    onSetProfilePhoto: (
        avatarUrl: String,
        onComplete: (Boolean, String) -> Unit
    ) -> Unit = { _, onComplete -> onComplete(true, "Photo updated") },
    onUploadPortfolioPhoto: ((
        title: String,
        category: String,
        mediaType: String,
        mediaUrl: String,
        isDemo: Boolean
    ) -> Unit)? = null,
    onSubmitReview: (
        bookingId: String,
        serviceType: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit
    ) -> Unit = { _, _, _, _, _ -> },
    onToggleFavoritePackage: (ServicePackage) -> Unit = {},
    onBookService: (String) -> Unit = {},
    onOpenMyBookings: () -> Unit = {},
    onOpenServiceCatalog: () -> Unit = {},
    onToggleTheme: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onOpenPrivacy: () -> Unit,
    onDeleteMyData: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val firebaseUser = remember(context) {
        runCatching {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                Firebase.auth.currentUser
            } else {
                null
            }
        }.getOrNull()
    }
    val isHindi = language == AppLanguage.HINDI

    val initialDisplayName = userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: firebaseUser?.displayName?.takeIf { it.isNotBlank() }
        ?: userEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
        ?: "Studio Artist"
    val initialAvatar = userProfile?.effectiveAvatarUrl?.takeIf { it.isNotBlank() }
        ?: firebaseUser?.photoUrl?.toString()?.takeIf { it.isNotBlank() }
        ?: ""
    val initialPhone = userProfile?.phoneNumber.orEmpty()
    val initialCity = userProfile?.city.orEmpty()
    val initialInstagram = userProfile?.instagramHandle.orEmpty()
    val initialPortfolioLinks = userProfile?.portfolioLinks.orEmpty()
    val initialSpecialty = userProfile?.specialty?.takeIf { it.isNotBlank() }
        ?: localArtistBio?.specialty.orEmpty()
    val initialBio = userProfile?.bio?.takeIf { it.isNotBlank() }
        ?: localArtistBio?.bioText.orEmpty()

    var baselineDisplayName by rememberSaveable(currentUserId) { mutableStateOf(initialDisplayName) }
    var baselineAvatar by rememberSaveable(currentUserId) { mutableStateOf(initialAvatar) }
    var baselinePhone by rememberSaveable(currentUserId) { mutableStateOf(initialPhone) }
    var baselineCity by rememberSaveable(currentUserId) { mutableStateOf(initialCity) }
    var baselineInstagram by rememberSaveable(currentUserId) { mutableStateOf(initialInstagram) }
    var baselinePortfolioLinks by rememberSaveable(currentUserId) { mutableStateOf(initialPortfolioLinks) }
    var baselineSpecialty by rememberSaveable(currentUserId) { mutableStateOf(initialSpecialty) }
    var baselineBio by rememberSaveable(currentUserId) { mutableStateOf(initialBio) }

    var displayName by rememberSaveable(currentUserId) { mutableStateOf(initialDisplayName) }
    var avatarUrl by rememberSaveable(currentUserId) { mutableStateOf(initialAvatar) }
    var phoneNumber by rememberSaveable(currentUserId) { mutableStateOf(initialPhone) }
    var city by rememberSaveable(currentUserId) { mutableStateOf(initialCity) }
    var instagramHandle by rememberSaveable(currentUserId) { mutableStateOf(initialInstagram) }
    var portfolioLinks by rememberSaveable(currentUserId) { mutableStateOf(initialPortfolioLinks) }
    var newPortfolioUrlDraft by rememberSaveable(currentUserId) { mutableStateOf("") }
    var portfolioLinkError by remember { mutableStateOf<String?>(null) }
    var specialty by rememberSaveable(currentUserId) { mutableStateOf(initialSpecialty) }
    var bio by rememberSaveable(currentUserId) { mutableStateOf(initialBio) }

    val hasUnsavedChanges = displayName.trim() != baselineDisplayName.trim() ||
        avatarUrl.trim() != baselineAvatar.trim() ||
        phoneNumber.trim() != baselinePhone.trim() ||
        city.trim() != baselineCity.trim() ||
        instagramHandle.trim() != baselineInstagram.trim() ||
        portfolioLinks != baselinePortfolioLinks ||
        specialty.trim() != baselineSpecialty.trim() ||
        bio.trim() != baselineBio.trim()

    // Hydrate offline-first from Room database as soon as localArtistBio emits
    var initializedFromLocalRoom by rememberSaveable(currentUserId) { mutableStateOf(false) }
    LaunchedEffect(localArtistBio) {
        if (localArtistBio != null && !initializedFromLocalRoom) {
            if (localArtistBio.bioText.isNotBlank() && bio.isBlank()) {
                bio = localArtistBio.bioText
                baselineBio = localArtistBio.bioText
            }
            if (localArtistBio.specialty.isNotBlank() && specialty.isBlank()) {
                specialty = localArtistBio.specialty
                baselineSpecialty = localArtistBio.specialty
            }
            initializedFromLocalRoom = true
        }
    }

    // Sync state when remote Firestore userProfile finishes loading or updates remotely without local unsaved edits
    var initializedFromRemote by rememberSaveable(currentUserId) { mutableStateOf(false) }
    LaunchedEffect(userProfile) {
        if (userProfile != null && (!initializedFromRemote || !hasUnsavedChanges)) {
            val remoteDisplayName = userProfile.displayName.takeIf { it.isNotBlank() } ?: baselineDisplayName
            val remoteAvatar = userProfile.effectiveAvatarUrl
            val remotePhone = userProfile.phoneNumber
            val remoteCity = userProfile.city
            val remoteInstagram = userProfile.instagramHandle
            val remoteLinks = userProfile.portfolioLinks
            val remoteSpecialty = userProfile.specialty.ifBlank {
                if (!initializedFromRemote) localArtistBio?.specialty.orEmpty() else ""
            }
            val remoteBio = userProfile.bio.ifBlank {
                if (!initializedFromRemote) localArtistBio?.bioText.orEmpty() else ""
            }

            if (!initializedFromRemote) {
                if (userProfile.displayName.isNotBlank()) {
                    displayName = remoteDisplayName
                    baselineDisplayName = remoteDisplayName
                }
                if (remoteAvatar.isNotBlank()) {
                    avatarUrl = remoteAvatar
                    baselineAvatar = remoteAvatar
                }
                if (remotePhone.isNotBlank()) {
                    phoneNumber = remotePhone
                    baselinePhone = remotePhone
                }
                if (remoteCity.isNotBlank()) {
                    city = remoteCity
                    baselineCity = remoteCity
                }
                if (remoteInstagram.isNotBlank()) {
                    instagramHandle = remoteInstagram
                    baselineInstagram = remoteInstagram
                }
                if (remoteLinks.isNotEmpty()) {
                    portfolioLinks = remoteLinks
                    baselinePortfolioLinks = remoteLinks
                }
                if (userProfile.specialty.isNotBlank()) {
                    specialty = userProfile.specialty
                    baselineSpecialty = userProfile.specialty
                }
                if (userProfile.bio.isNotBlank() && (localArtistBio?.bioText.isNullOrBlank() || bio.isBlank())) {
                    bio = userProfile.bio
                    baselineBio = userProfile.bio
                }
            } else {
                displayName = remoteDisplayName
                baselineDisplayName = remoteDisplayName
                avatarUrl = remoteAvatar
                baselineAvatar = remoteAvatar
                phoneNumber = remotePhone
                baselinePhone = remotePhone
                city = remoteCity
                baselineCity = remoteCity
                instagramHandle = remoteInstagram
                baselineInstagram = remoteInstagram
                portfolioLinks = remoteLinks
                baselinePortfolioLinks = remoteLinks
                specialty = remoteSpecialty
                baselineSpecialty = remoteSpecialty
                bio = remoteBio
                baselineBio = remoteBio
            }
            initializedFromRemote = true
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var localError by remember { mutableStateOf<String?>(null) }
    var displayNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var saveFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPortfolioUploadDialog by remember { mutableStateOf(false) }
    var startPortfolioUploadWithCamera by remember { mutableStateOf(false) }
    var reviewTargetBooking by remember { mutableStateOf<ShootBooking?>(null) }

    if (showPortfolioUploadDialog && onUploadPortfolioPhoto != null) {
        UploadMediaDialog(
            language = language,
            isSubmitting = isSubmitting,
            initialStartCamera = startPortfolioUploadWithCamera,
            onDismiss = {
                showPortfolioUploadDialog = false
                startPortfolioUploadWithCamera = false
            },
            onUpload = { title, cat, type, url, isDemo ->
                onUploadPortfolioPhoto(title, cat, type, url, isDemo)
                showPortfolioUploadDialog = false
                startPortfolioUploadWithCamera = false
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

    val completeness = remember(
        displayName, avatarUrl, phoneNumber, city, instagramHandle, portfolioLinks, specialty, bio
    ) {
        computeArtistProfileCompleteness(
            displayName = displayName,
            avatarUrl = avatarUrl,
            phoneNumber = phoneNumber,
            city = city,
            instagramHandle = instagramHandle,
            portfolioLinks = portfolioLinks,
            specialty = specialty,
            bio = bio
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val encoded = encodeHeadshotUriToDataUrl(context, uri)
            if (encoded != null) {
                avatarUrl = encoded
                localError = null
                onSetProfilePhoto(encoded) { success, message ->
                    if (success) {
                        baselineAvatar = encoded
                        saveFeedbackMessage = message
                    } else {
                        localError = message
                    }
                }
            } else {
                localError = if (isHindi) "छवि संसाधित करने में समस्या आई।" else "Unable to process selected image."
            }
        }
    }

    val favoritePackages = remember(favoritePackageIds) {
        StudioServicesData.packages.filter { favoritePackageIds.contains(it.id) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("user_profile_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .testTag("user_profile_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ArtistProfileTopBar(
                    isHindi = isHindi,
                    isEmailVerified = isEmailVerified,
                    hasUnsavedChanges = hasUnsavedChanges,
                    onBack = onBack
                )
            }

            item {
                ArtistHeroIdentityCard(
                    displayName = displayName,
                    avatarUrl = avatarUrl,
                    specialty = specialty,
                    emailText = userEmail ?: firebaseUser?.email ?: "Member Account",
                    city = city,
                    phoneNumber = phoneNumber,
                    portfolioCount = portfolioLinks.size + if (instagramHandle.isNotBlank() && portfolioLinks.none { "instagram.com" in it.lowercase() }) 1 else 0,
                    completeness = completeness,
                    modelsCount = modelsCount,
                    bookingsCount = bookingsCount,
                    projectsCount = savedProjects.size,
                    favoritesCount = favoritePackages.size,
                    isHindi = isHindi,
                    bio = bio,
                    onPickPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = StudioCardSurface,
                    contentColor = GoldPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GoldPrimary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, StudioOutline, RoundedCornerShape(12.dp))
                        .testTag("user_profile_tabs")
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("profile_tab_edit"),
                        text = {
                            Text(
                                text = if (isHindi) "प्रोफ़ाइल" else "Profile",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("profile_tab_activity"),
                        text = {
                            Text(
                                text = if (isHindi) "बुकिंग्स (${myBookings.size})" else "Activity (${myBookings.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.testTag("profile_tab_favorites"),
                        text = {
                            Text(
                                text = if (isHindi) "पसंदीदा (${favoritePackages.size})" else "Favorites (${favoritePackages.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        modifier = Modifier.testTag("profile_tab_account"),
                        text = {
                            Text(
                                text = if (isHindi) "खाता" else "Account",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            if (selectedTab == 0) {
                item {
                    ProfilePhotoComponent(
                        displayName = displayName,
                        avatarUrl = avatarUrl,
                        isHindi = isHindi,
                        isSaving = isSubmitting,
                        onAvatarUrlChanged = {
                            avatarUrl = it
                            localError = null
                            saveFeedbackMessage = null
                        },
                        onSetProfilePhoto = { newAvatarUrl, onDone ->
                            onSetProfilePhoto(newAvatarUrl) { ok, msg ->
                                if (ok) {
                                    baselineAvatar = newAvatarUrl.trim()
                                }
                                onDone(ok, msg)
                            }
                        }
                    )
                }

                item {
                    ArtistContactInfoCard(
                        displayName = displayName,
                        onDisplayNameChange = {
                            displayName = it
                            displayNameError = null
                            localError = null
                            saveFeedbackMessage = null
                        },
                        phoneNumber = phoneNumber,
                        onPhoneChange = {
                            phoneNumber = it
                            phoneError = null
                            localError = null
                            saveFeedbackMessage = null
                        },
                        city = city,
                        onCityChange = {
                            city = it
                            saveFeedbackMessage = null
                        },
                        accountEmail = userEmail ?: firebaseUser?.email ?: "Member Account",
                        isEmailVerified = isEmailVerified,
                        isHindi = isHindi,
                        displayNameError = displayNameError,
                        phoneError = phoneError
                    )
                }

                item {
                    ArtistPortfolioLinksCard(
                        instagramHandle = instagramHandle,
                        onInstagramChange = {
                            instagramHandle = it
                            saveFeedbackMessage = null
                        },
                        portfolioLinks = portfolioLinks,
                        newLinkDraft = newPortfolioUrlDraft,
                        onNewLinkDraftChange = {
                            newPortfolioUrlDraft = it
                            portfolioLinkError = null
                            saveFeedbackMessage = null
                        },
                        linkError = portfolioLinkError,
                        onAddLink = {
                            val normalized = normalizePortfolioLink(newPortfolioUrlDraft)
                            when {
                                normalized == null -> {
                                    portfolioLinkError = if (isHindi)
                                        "कृपया मान्य पोर्टफोलियो लिंक (उदा. https://behance.net/...) दर्ज करें।"
                                    else
                                        "Enter a valid link (e.g. https://behance.net/name or @handle)."
                                }
                                portfolioLinks.any { it.equals(normalized, ignoreCase = true) } -> {
                                    portfolioLinkError = if (isHindi) "यह लिंक पहले से जुड़ा हुआ है।" else "This link is already added."
                                }
                                portfolioLinks.size >= 10 -> {
                                    portfolioLinkError = if (isHindi) "अधिकतम 10 लिंक जोड़े जा सकते हैं।" else "Maximum 10 portfolio links allowed."
                                }
                                else -> {
                                    portfolioLinks = portfolioLinks + normalized
                                    newPortfolioUrlDraft = ""
                                    portfolioLinkError = null
                                    saveFeedbackMessage = null
                                }
                            }
                        },
                        onRemoveLink = { target ->
                            portfolioLinks = portfolioLinks.filterNot { it == target }
                            saveFeedbackMessage = null
                        },
                        onOpenLink = { url ->
                            val safeUrl = normalizePortfolioLink(url) ?: url
                            runCatching { uriHandler.openUri(safeUrl) }
                        },
                        isHindi = isHindi
                    )
                }

                if (onUploadPortfolioPhoto != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
                                .testTag("profile_portfolio_capture_card"),
                            colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (isHindi) "स्टूडियो पोर्टफोलियो फ़ोटो अपलोड" else "Studio Portfolio Photo Upload",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                }
                                Text(
                                    text = if (isHindi) {
                                        "सीधे अपने कैमरे से उच्च-गुणवत्ता पोर्टफोलियो शॉट लें या गैलरी से चुनकर स्टूडियो पोर्टफोलियो में अपलोड करें।"
                                    } else {
                                        "Capture a high-quality portfolio photo directly with your camera or pick an existing shot to submit to the studio gallery."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )
                                Button(
                                    onClick = {
                                        startPortfolioUploadWithCamera = true
                                        showPortfolioUploadDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldPrimary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("profile_capture_portfolio_button")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isHindi) "कैमरा से पोर्टफोलियो फ़ोटो अपलोड करें" else "Capture & Upload Portfolio Photo",
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    ArtistBioAndSpecialtyCard(
                        specialty = specialty,
                        specialtyOptions = artistSpecialtyOptions,
                        onSpecialtySelect = {
                            specialty = it
                            saveFeedbackMessage = null
                        },
                        bio = bio,
                        onBioChange = {
                            bio = it
                            localError = null
                            saveFeedbackMessage = null
                        },
                        isHindi = isHindi,
                        localArtistBio = localArtistBio,
                        onSaveLocalBio = onSaveLocalBio
                    )
                }

                item {
                    val lastSyncedLabel = remember(userProfile?.updatedAt, hasUnsavedChanges, isHindi) {
                        val updatedDate = userProfile?.updatedAt?.toDate()
                        when {
                            hasUnsavedChanges -> if (isHindi) "Cloud Firestore में सहेजे जाने के लिए तैयार बदलाव" else "Unsaved edits — tap Save to sync with Cloud Firestore"
                            updatedDate != null -> {
                                val fmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                if (isHindi) "Firestore सिंक: ${fmt.format(updatedDate)}" else "Synced to Firestore • ${fmt.format(updatedDate)}"
                            }
                            else -> if (isHindi) "Cloud Firestore प्रोफ़ाइल सिंक सक्रिय" else "Connected to Cloud Firestore"
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_firestore_sync_card"),
                            shape = RoundedCornerShape(12.dp),
                            color = StudioElevatedSurface,
                            border = BorderStroke(
                                1.dp,
                                if (hasUnsavedChanges) GoldPrimary.copy(alpha = 0.55f) else StudioOutline
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = if (hasUnsavedChanges) GoldPrimary else SuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = lastSyncedLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hasUnsavedChanges) GoldLight else TextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        val shareText = buildArtistProfileSummaryText(
                                            displayName = displayName,
                                            specialty = specialty,
                                            city = city,
                                            phoneNumber = phoneNumber,
                                            email = userEmail ?: firebaseUser?.email.orEmpty(),
                                            instagramHandle = instagramHandle,
                                            portfolioLinks = portfolioLinks,
                                            bio = bio
                                        )
                                        clipboardManager.setText(AnnotatedString(shareText))
                                        saveFeedbackMessage = if (isHindi)
                                            "कलाकार प्रोफ़ाइल सारांश क्लिपबोर्ड पर कॉपी किया गया!"
                                        else
                                            "Artist profile summary copied to clipboard!"
                                    },
                                    modifier = Modifier.testTag("profile_copy_summary_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = if (isHindi) "प्रोफ़ाइल कॉपी करें" else "Copy Profile Summary",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHindi) "कॉपी" else "Copy Card",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = localError != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ErrorRed.copy(alpha = 0.14f))
                                    .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                                    .testTag("profile_error_banner"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = localError.orEmpty(),
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = saveFeedbackMessage != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SuccessGreen.copy(alpha = 0.14f))
                                    .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                                    .testTag("profile_save_success_banner"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = saveFeedbackMessage.orEmpty(),
                                    color = SuccessGreen,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (hasUnsavedChanges) {
                                OutlinedButton(
                                    onClick = {
                                        displayName = baselineDisplayName
                                        avatarUrl = baselineAvatar
                                        phoneNumber = baselinePhone
                                        city = baselineCity
                                        instagramHandle = baselineInstagram
                                        portfolioLinks = baselinePortfolioLinks
                                        newPortfolioUrlDraft = ""
                                        portfolioLinkError = null
                                        displayNameError = null
                                        phoneError = null
                                        specialty = baselineSpecialty
                                        bio = baselineBio
                                        localError = null
                                        saveFeedbackMessage = null
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 50.dp)
                                        .testTag("profile_reset_button"),
                                    border = BorderStroke(1.dp, StudioOutline),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "रीसेट" else "Reset",
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val pendingLink = newPortfolioUrlDraft.trim()
                                    val finalLinks = if (pendingLink.isNotEmpty()) {
                                        val normalizedPending = normalizePortfolioLink(pendingLink)
                                        if (normalizedPending == null) {
                                            portfolioLinkError = if (isHindi)
                                                "सहेजने से पहले मान्य पोर्टफोलियो लिंक दर्ज करें।"
                                            else
                                                "Enter a valid portfolio link or clear the URL field before saving."
                                            return@Button
                                        }
                                        (portfolioLinks + normalizedPending).distinct().take(10)
                                    } else {
                                        portfolioLinks
                                    }

                                    val validationError = validateArtistProfileInput(
                                        displayName = displayName,
                                        phoneNumber = phoneNumber,
                                        bio = bio
                                    )
                                    if (validationError != null) {
                                        displayNameError = if (displayName.trim().length < 2) validationError else null
                                        val trimmedPhone = phoneNumber.trim()
                                        phoneError = if (trimmedPhone.isNotEmpty() && (trimmedPhone.length < 8 || trimmedPhone.length > 30)) {
                                            validationError
                                        } else {
                                            null
                                        }
                                        localError = validationError
                                        saveFeedbackMessage = null
                                    } else {
                                        displayNameError = null
                                        phoneError = null
                                        localError = null
                                        val trimmedName = displayName.trim()
                                        val trimmedAvatar = avatarUrl.trim()
                                        val trimmedPhone = phoneNumber.trim()
                                        val trimmedCity = city.trim()
                                        val trimmedInstagram = instagramHandle.trim()
                                        val trimmedSpecialty = specialty.trim()
                                        val trimmedBio = bio.trim()
                                        portfolioLinks = finalLinks
                                        newPortfolioUrlDraft = ""
                                        portfolioLinkError = null
                                        onUpdateArtistProfile(
                                            trimmedName,
                                            trimmedAvatar,
                                            trimmedPhone,
                                            trimmedCity,
                                            trimmedInstagram,
                                            trimmedSpecialty,
                                            trimmedBio,
                                            finalLinks
                                        ) { success, message ->
                                            if (success) {
                                                baselineDisplayName = trimmedName
                                                baselineAvatar = trimmedAvatar
                                                baselinePhone = trimmedPhone
                                                baselineCity = trimmedCity
                                                baselineInstagram = trimmedInstagram
                                                baselinePortfolioLinks = finalLinks
                                                baselineSpecialty = trimmedSpecialty
                                                baselineBio = trimmedBio
                                                saveFeedbackMessage = message
                                            } else {
                                                localError = message
                                            }
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp)
                                    .testTag("profile_save_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isHindi) "सहेजा जा रहा है..." else "Saving...",
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isHindi) "प्रोफ़ाइल सहेजें" else "Save Profile Changes",
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedTab == 1) {
                item {
                    ProfileActivityTabContent(
                        myBookings = myBookings,
                        savedProjects = savedProjects,
                        myReviews = myReviews,
                        language = language,
                        isHindi = isHindi,
                        onReviewBooking = { reviewTargetBooking = it },
                        onOpenMyBookings = onOpenMyBookings,
                        onBookNewShoot = { onBookService("Photography (High-Fashion Editorial)") }
                    )
                }
            }

            if (selectedTab == 2) {
                item {
                    ProfileFavoritesTabContent(
                        favoritePackageIds = favoritePackageIds,
                        language = language,
                        isHindi = isHindi,
                        onToggleFavorite = onToggleFavoritePackage,
                        onBookPackage = { pkg -> onBookService(pkg.recommendedServiceType) },
                        onOpenCatalog = onOpenServiceCatalog
                    )
                }
            }

            if (selectedTab == 3) {
                item {
                    ProfileAccountTabContent(
                        isStudioLight = isStudioLight,
                        language = language,
                        isHindi = isHindi,
                        isSubmitting = isSubmitting,
                        onToggleTheme = onToggleTheme,
                        onSelectLanguage = onSelectLanguage,
                        onOpenPrivacy = onOpenPrivacy,
                        onSignOut = onSignOut,
                        onRequestDelete = { showDeleteConfirmDialog = true }
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "डेटा हटाने की पुष्टि" else "Confirm Data Deletion",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = if (isHindi)
                        "क्या आप वाकई अपनी प्रोफ़ाइल, बुकिंग और मॉडल आवेदन डेटा स्थायी रूप से हटाना चाहते हैं?"
                    else
                        "Permanently delete your profile, shoot bookings, and model applications from Cloud Firestore?",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteMyData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isHindi) "हाँ, हटाएं" else "Yes, Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel", color = TextSecondary)
                }
            },
            containerColor = StudioCardSurface
        )
    }
}

@Composable
private fun ProfileActivityTabContent(
    myBookings: List<ShootBooking>,
    savedProjects: List<ProjectDraftEntity>,
    myReviews: List<ServiceReview>,
    language: AppLanguage,
    isHindi: Boolean,
    onReviewBooking: (ShootBooking) -> Unit,
    onOpenMyBookings: () -> Unit = {},
    onBookNewShoot: () -> Unit = {}
) {
    val partitioned = remember(myBookings) {
        BookingScheduleHelper.partitionAndFilterBookings(myBookings)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Appointments Summary & Action Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioOutline, RoundedCornerShape(14.dp))
                .testTag("profile_appointments_summary_card"),
            colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
            shape = RoundedCornerShape(14.dp)
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
                    Column {
                        Text(
                            text = if (isHindi) "अपॉइंटमेंट इतिहास" else "Appointment History",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Text(
                            text = if (isHindi)
                                "${partitioned.upcoming.size} आगामी • ${partitioned.past.size} पिछले सत्र"
                            else
                                "${partitioned.upcoming.size} Upcoming • ${partitioned.past.size} Past Sessions",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onOpenMyBookings,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("profile_open_my_bookings_button"),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.65f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isHindi) "शेड्यूल" else "Full Schedule",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                        }
                    }
                }

                Button(
                    onClick = onBookNewShoot,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("profile_book_new_shoot_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "नया स्टूडियो शूट बुक करें" else "Book a New Studio Shoot",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            }
        }

        // 1. UPCOMING APPOINTMENTS SECTION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_upcoming_appointments_section"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Upcoming, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isHindi)
                        "आगामी अपॉइंटमेंट्स (${partitioned.upcoming.size})"
                    else
                        "Upcoming Appointments (${partitioned.upcoming.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
            }
        }

        if (partitioned.upcoming.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline, RoundedCornerShape(14.dp))
                    .testTag("profile_upcoming_empty_card"),
                colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.EventAvailable, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(30.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi) "कोई आगामी अपॉइंटमेंट निर्धारित नहीं है।" else "No upcoming studio appointments.",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi) "ऊपर से नया शूट बुक करें या कैटलॉग देखें।" else "Book a session above or from your saved favorite packages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            partitioned.upcoming.forEach { booking ->
                val existingReview = myReviews.find { it.bookingId == booking.id }
                val dayBadge = BookingScheduleHelper.relativeDayLabel(booking, language)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_upcoming_booking_${booking.id}"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = GoldContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dayBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnGoldContainer
                            )
                            Text(
                                text = "${booking.preferredDate}${if (booking.preferredTime.isNotBlank()) " • ${booking.preferredTime}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    BookingApprovalStageCard(booking = booking, language = language)
                    BookingReviewStatusBlock(
                        booking = booking,
                        existingReview = existingReview,
                        language = language,
                        onOpenReviewDialog = { onReviewBooking(booking) }
                    )
                }
            }
        }

        // 2. PAST APPOINTMENT HISTORY SECTION
        if (partitioned.past.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_past_appointments_section"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isHindi)
                        "पिछला अपॉइंटमेंट इतिहास (${partitioned.past.size})"
                    else
                        "Past Appointment History (${partitioned.past.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
            }

            partitioned.past.forEach { booking ->
                val existingReview = myReviews.find { it.bookingId == booking.id }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_past_booking_${booking.id}"),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    BookingApprovalStageCard(booking = booking, language = language)
                    BookingReviewStatusBlock(
                        booking = booking,
                        existingReview = existingReview,
                        language = language,
                        onOpenReviewDialog = { onReviewBooking(booking) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isHindi) "सहेजे गए प्रोजेक्ट्स (${savedProjects.size})" else "Saved Photography Drafts (${savedProjects.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary
        )

        if (savedProjects.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isHindi) "कोई सहेजा गया प्रोजेक्ट नहीं।" else "No saved AI or moodboard drafts yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            savedProjects.forEach { project ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioOutline, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = project.draftTitle,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                        Text(
                            text = project.projectType,
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary
                        )
                        if (project.conceptNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = project.conceptNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileFavoritesTabContent(
    favoritePackageIds: Set<String>,
    language: AppLanguage,
    isHindi: Boolean,
    onToggleFavorite: (ServicePackage) -> Unit,
    onBookPackage: (ServicePackage) -> Unit,
    onOpenCatalog: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ServicePackageCategory.ALL) }

    val savedFavorites = remember(favoritePackageIds, selectedCategory) {
        StudioServicesData.filterAndSortPackages(
            category = selectedCategory,
            favoritesOnly = true,
            favoriteIds = favoritePackageIds
        )
    }

    val discoverPackages = remember(selectedCategory) {
        StudioServicesData.filterAndSortPackages(category = selectedCategory)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_favorites_tab_content"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Category Filter Row for Saved & Discoverable Packages
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ServicePackageCategory.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = {
                        Text(
                            text = if (isHindi) cat.titleHi else cat.titleEn,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
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
                        .testTag("profile_favorites_category_${cat.id}")
                )
            }
        }

        // Section 1: Saved Favorite Packages
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_saved_favorites_section"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isHindi)
                        "सहेजे गए पसंदीदा पैकेज (${savedFavorites.size})"
                    else
                        "Saved Favorite Packages (${savedFavorites.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
            }
            TextButton(
                onClick = onOpenCatalog,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("profile_open_catalog_button")
            ) {
                Text(
                    text = if (isHindi) "कैटलॉग देखें" else "Full Catalog",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
            }
        }

        if (savedFavorites.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline, RoundedCornerShape(14.dp))
                    .testTag("profile_favorites_empty_card"),
                colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(30.dp))
                    Text(
                        text = if (isHindi) "अभी कोई पसंदीदा पैकेज सहेजा नहीं गया" else "No saved favorite packages yet",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isHindi)
                            "नीचे फोटोग्राफी, मॉडलिंग या वीडियो प्रोडक्शन पैकेज पर दिल का आइकन टैप करके सहेजें।"
                        else
                            "Tap Save on any Photography, Modeling, or Video Production package below to bookmark it to your account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            savedFavorites.forEach { pkg ->
                val pkgTitle = if (isHindi) pkg.titleHi else pkg.titleEn
                val pkgCategory = if (isHindi) pkg.category.titleHi else pkg.category.titleEn
                val pkgPrice = if (isHindi) pkg.priceFormattedHi else pkg.priceFormattedEn
                val pkgDuration = if (isHindi) pkg.durationHi else pkg.durationEn
                val pkgSummary = if (isHindi) pkg.summaryHi else pkg.summaryEn

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .testTag("profile_favorite_card_${pkg.id}"),
                    colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = GoldContainer,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, GoldPrimary)
                            ) {
                                Text(
                                    text = pkgCategory.uppercase(),
                                    color = OnGoldContainer,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = pkgPrice,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldPrimary
                            )
                        }

                        Text(
                            text = pkgTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = pkgDuration, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }

                        Text(
                            text = pkgSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onBookPackage(pkg) },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("profile_favorite_book_${pkg.id}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "शूट बुक करें" else "Book Shoot",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            OutlinedButton(
                                onClick = { onToggleFavorite(pkg) },
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .testTag("profile_remove_fav_${pkg.id}"),
                                border = BorderStroke(1.dp, StudioOutline),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Favorite, contentDescription = if (isHindi) "हटाएं" else "Remove favorite", tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHindi) "हटाएं" else "Remove",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Quick Package Discovery & Bookmarking by Category
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isHindi) "पैकेज ब्राउज़ व सेव करें" else "Discover & Save Studio Packages",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = GoldLight,
            modifier = Modifier.testTag("profile_discover_packages_section")
        )

        discoverPackages.forEach { pkg ->
            val isSaved = favoritePackageIds.contains(pkg.id)
            val pkgTitle = if (isHindi) pkg.titleHi else pkg.titleEn
            val pkgCategory = if (isHindi) pkg.category.titleHi else pkg.category.titleEn
            val pkgPrice = if (isHindi) pkg.priceFormattedHi else pkg.priceFormattedEn

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_discover_row_${pkg.id}"),
                shape = RoundedCornerShape(12.dp),
                color = StudioElevatedSurface,
                border = BorderStroke(1.dp, if (isSaved) GoldPrimary.copy(alpha = 0.5f) else StudioOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "$pkgCategory • $pkgPrice",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Text(
                            text = pkgTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onToggleFavorite(pkg) },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("profile_quick_fav_${pkg.id}")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isSaved) {
                                if (isHindi) "$pkgTitle पसंदीदा से हटाएं" else "Remove $pkgTitle from favorites"
                            } else {
                                if (isHindi) "$pkgTitle पसंदीदा में सहेजें" else "Save $pkgTitle to favorites"
                            },
                            tint = if (isSaved) GoldPrimary else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAccountTabContent(
    isStudioLight: Boolean,
    language: AppLanguage,
    isHindi: Boolean,
    isSubmitting: Boolean,
    onToggleTheme: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onOpenPrivacy: () -> Unit,
    onSignOut: () -> Unit,
    onRequestDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioOutline, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isStudioLight) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = GoldPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHindi) "थीम मोड" else "Theme & Contrast",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isStudioLight) "Studio Light" else "Luxury Black & Gold",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                ThemeToggleComponent(isStudioLight = isStudioLight, onToggle = onToggleTheme)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioOutline, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isHindi) "भाषा चुनें" else "App Language",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                LanguageToggleComponent(
                    currentLanguage = language,
                    onLanguageSelected = onSelectLanguage
                )
            }
        }

        OutlinedButton(
            onClick = onOpenPrivacy,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            border = BorderStroke(1.dp, StudioOutline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = GoldPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isHindi) "गोपनीयता नीति और शर्तें" else "Privacy Policy & Consent",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("profile_screen_sign_out_button"),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = GoldPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isHindi) "साइन आउट करें" else "Sign Out",
                color = GoldPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            onClick = onRequestDelete,
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("profile_screen_delete_data_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isHindi) "मेरा सभी डेटा हटाएं" else "Delete My Cloud Data",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
