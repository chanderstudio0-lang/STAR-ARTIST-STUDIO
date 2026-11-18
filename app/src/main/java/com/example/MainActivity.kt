package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.AppDatabase
import com.example.data.local.entity.PortfolioProjectMediaType
import com.example.data.repository.ArtistBioRepository
import com.example.data.repository.ArtistDraftsRepository
import com.example.data.repository.MoodBoardRepository
import com.example.data.repository.PortfolioProjectsRepository
import com.example.data.repository.ServiceCatalogReferenceRepository
import com.example.data.repository.StudioInfoRepository
import com.example.data.repository.StudioRepository
import com.example.data.repository.StudioSessionProjectsRepository
import com.example.ui.ai.AiStudioViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.StudioBottomNavigation
import com.example.ui.components.StudioTopAppBar
import com.example.ui.components.UserProfileDialog
import com.example.ui.components.VoiceBookingModalDialog
import com.example.ui.components.catalogReviewBookingId
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.BlogScreen
import com.example.ui.screens.ContactScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ModelRegistrationScreen
import com.example.ui.screens.MoodBoardArtScreen
import com.example.ui.screens.MoodBoardWorkspaceScreen
import com.example.ui.screens.MyBookingsScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PhotoPreviewScreen
import com.example.ui.screens.PortfolioProjectsScreen
import com.example.ui.screens.PortfolioScreen
import com.example.ui.screens.PrivacyScreen
import com.example.ui.screens.ProjectStatusTrackerScreen
import com.example.ui.screens.ServiceCatalogScreen
import com.example.ui.screens.ShootBookingScreen
import com.example.ui.screens.StudioAnalyticsScreen
import com.example.ui.screens.UserProfileScreen
import com.example.notification.SessionReminderScheduler
import com.example.notification.StudioNotificationManager
import com.example.ui.theme.StarArtistTheme
import com.example.ui.theme.StudioBlack
import com.example.ui.viewmodel.MoodBoardOrganizerViewModel
import com.example.ui.viewmodel.MoodBoardTile
import com.example.ui.viewmodel.MoodBoardViewModel
import com.example.ui.viewmodel.PhotoPreviewDestination
import com.example.ui.viewmodel.ProjectStatusViewModel
import com.example.ui.viewmodel.ProjectTrackerViewMode
import com.example.ui.viewmodel.StudioScreen
import com.example.ui.viewmodel.StudioViewModel
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AuthRedirectTarget {
    LOGIN,
    HOME
}

/**
 * Resolves whether the app should redirect to the Login screen or Home screen
 * given the current Firebase authentication status and optional email-verification hold.
 */
fun resolveAuthRedirectTarget(
    isAuthenticated: Boolean,
    holdAuthForVerification: Boolean = false
): AuthRedirectTarget {
    return if (isAuthenticated && !holdAuthForVerification) {
        AuthRedirectTarget.HOME
    } else {
        AuthRedirectTarget.LOGIN
    }
}

class MainActivity : ComponentActivity() {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _holdAuthForVerification = MutableStateFlow(false)
    val holdAuthForVerification: StateFlow<Boolean> = _holdAuthForVerification.asStateFlow()

    private val _openMyBookingsTrigger = MutableStateFlow(false)
    val openMyBookingsTrigger: StateFlow<Boolean> = _openMyBookingsTrigger.asStateFlow()

    private val _openAnnouncementsTrigger = MutableStateFlow(false)
    val openAnnouncementsTrigger: StateFlow<Boolean> = _openAnnouncementsTrigger.asStateFlow()

    internal val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        handleAuthStateChange(firebaseAuth.currentUser)
    }

    internal fun handleAuthStateChange(user: FirebaseUser?) {
        val wasAuthenticated = _currentUser.value != null
        _currentUser.value = user
        if (user == null) {
            // Ensure any pending email-verification hold and local session alarms clear upon sign-out without blocking UI thread.
            _holdAuthForVerification.value = false
            if (wasAuthenticated) {
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching { SessionReminderScheduler.clearAllReminders(applicationContext) }
                }
            }
        }
    }

    internal fun setHoldAuthForVerification(hold: Boolean) {
        _holdAuthForVerification.value = hold
    }

    private fun inspectLaunchIntent(launchIntent: Intent?) {
        if (launchIntent?.getBooleanExtra(StudioNotificationManager.EXTRA_OPEN_MY_BOOKINGS, false) == true) {
            _openMyBookingsTrigger.value = true
        }
        if (launchIntent?.getBooleanExtra(StudioNotificationManager.EXTRA_OPEN_ANNOUNCEMENTS, false) == true) {
            _openAnnouncementsTrigger.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure edge-to-edge before setContent/insets dispatch to avoid double SurfaceSyncGroup passes
        enableEdgeToEdge()

        // STEP 1: Mandatory App Check contract wiring with launch intent
        configureAppCheck(this, intent)
        inspectLaunchIntent(intent)

        // Synchronize initial auth status without blocking UI thread
        runCatching { handleAuthStateChange(Firebase.auth.currentUser) }

        // Pre-warm notification channels & Room DB off the main thread so first frame finishes fast
        val appContext = applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { StudioNotificationManager.createNotificationChannels(appContext) }
            runCatching { AppDatabase.getDatabase(appContext) }
        }

        setContent {
            var isStudioLightMode by rememberSaveable { mutableStateOf(false) }
            val authenticatedUser by currentUser.collectAsState()
            val isVerificationHeld by holdAuthForVerification.collectAsState()
            val shouldOpenMyBookings by openMyBookingsTrigger.collectAsState()
            val shouldOpenAnnouncements by openAnnouncementsTrigger.collectAsState()

            StarArtistTheme(isStudioLight = isStudioLightMode) {
                MainAppNavigation(
                    currentUser = authenticatedUser,
                    holdAuthForVerification = isVerificationHeld,
                    isStudioLightMode = isStudioLightMode,
                    openMyBookingsOnStart = shouldOpenMyBookings,
                    openAnnouncementsOnStart = shouldOpenAnnouncements,
                    onConsumeOpenMyBookings = { _openMyBookingsTrigger.value = false },
                    onConsumeOpenAnnouncements = { _openAnnouncementsTrigger.value = false },
                    onToggleTheme = { isStudioLightMode = !isStudioLightMode },
                    onHoldAuthForVerification = { hold -> setHoldAuthForVerification(hold) },
                    onAuthSuccess = {
                        setHoldAuthForVerification(false)
                        runCatching { handleAuthStateChange(Firebase.auth.currentUser) }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        inspectLaunchIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        runCatching {
            val firebaseAuth = Firebase.auth
            handleAuthStateChange(firebaseAuth.currentUser)
            firebaseAuth.addAuthStateListener(authStateListener)
        }
    }

    override fun onStop() {
        runCatching {
            Firebase.auth.removeAuthStateListener(authStateListener)
        }
        super.onStop()
    }
}

@Composable
fun MainAppNavigation(
    currentUser: FirebaseUser?,
    holdAuthForVerification: Boolean,
    isStudioLightMode: Boolean,
    openMyBookingsOnStart: Boolean = false,
    openAnnouncementsOnStart: Boolean = false,
    onConsumeOpenMyBookings: () -> Unit = {},
    onConsumeOpenAnnouncements: () -> Unit = {},
    onToggleTheme: () -> Unit,
    onHoldAuthForVerification: (Boolean) -> Unit,
    onAuthSuccess: () -> Unit
) {
    AuthRedirectHost(
        isAuthenticated = currentUser != null,
        currentUserId = currentUser?.uid.orEmpty(),
        holdAuthForVerification = holdAuthForVerification,
        loginScreen = {
            AuthScreen(
                isStudioLight = isStudioLightMode,
                onToggleTheme = onToggleTheme,
                onHoldAuthForVerification = onHoldAuthForVerification,
                onAuthSuccess = onAuthSuccess
            )
        },
        homeScreen = { uid ->
            StudioMainContent(
                currentUserId = uid,
                isStudioLightMode = isStudioLightMode,
                openMyBookingsOnStart = openMyBookingsOnStart,
                openAnnouncementsOnStart = openAnnouncementsOnStart,
                onConsumeOpenMyBookings = onConsumeOpenMyBookings,
                onConsumeOpenAnnouncements = onConsumeOpenAnnouncements,
                onToggleTheme = onToggleTheme
            )
        }
    )
}

@Composable
fun AuthRedirectHost(
    isAuthenticated: Boolean,
    currentUserId: String,
    holdAuthForVerification: Boolean,
    loginScreen: @Composable () -> Unit,
    homeScreen: @Composable (userId: String) -> Unit
) {
    val target = resolveAuthRedirectTarget(
        isAuthenticated = isAuthenticated,
        holdAuthForVerification = holdAuthForVerification
    )
    when (target) {
        AuthRedirectTarget.LOGIN -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("auth_redirect_login_container")
            ) {
                loginScreen()
            }
        }
        AuthRedirectTarget.HOME -> {
            androidx.compose.runtime.key(currentUserId) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("auth_redirect_home_container")
                ) {
                    homeScreen(currentUserId)
                }
            }
        }
    }
}

@Composable
fun StudioMainContent(
    currentUserId: String,
    isStudioLightMode: Boolean,
    openMyBookingsOnStart: Boolean = false,
    openAnnouncementsOnStart: Boolean = false,
    onConsumeOpenMyBookings: () -> Unit = {},
    onConsumeOpenAnnouncements: () -> Unit = {},
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Allow VRI[MainActivity] to finish its initial SurfaceSyncGroup draw immediately before
    // initializing Room databases and Firestore listeners off the first UI frame.
    var isStartupWarmedUp by remember(currentUserId) { mutableStateOf(false) }
    LaunchedEffect(currentUserId) {
        androidx.compose.runtime.withFrameNanos { }
        withContext(Dispatchers.IO) {
            runCatching {
                val appContext = context.applicationContext
                AppDatabase.getDatabase(appContext)
                val databaseId = appContext.getString(R.string.firestore_database_id)
                FirebaseFirestore.getInstance(databaseId)
            }
        }
        isStartupWarmedUp = true
    }

    if (!isStartupWarmedUp) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StudioBlack)
                .testTag("studio_startup_shell")
        )
        return
    }

    // Resolve ViewModel via factory using R.string.firestore_database_id
    val studioViewModel: StudioViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val localDb = AppDatabase.getDatabase(app)
                val studioInfoRepo = StudioInfoRepository(localDb.studioInfoDao())
                val draftsRepo = ArtistDraftsRepository(localDb.projectDraftDao(), localDb.sessionHistoryDao())
                val bioRepo = ArtistBioRepository(localDb.artistBioDao())
                val portfolioProjectsRepo = PortfolioProjectsRepository(localDb.portfolioProjectDao())
                val catalogReferenceRepo = ServiceCatalogReferenceRepository(localDb.serviceCatalogReferenceDao())
                StudioViewModel(
                    repository = StudioRepository(db),
                    currentUserId = currentUserId,
                    studioInfoRepository = studioInfoRepo,
                    artistDraftsRepository = draftsRepo,
                    artistBioRepository = bioRepo,
                    portfolioProjectsRepository = portfolioProjectsRepo,
                    serviceCatalogReferenceRepository = catalogReferenceRepo
                )
            }
        }
    )

    val projectStatusViewModel: ProjectStatusViewModel = viewModel(
        key = "project_status_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val localDb = AppDatabase.getDatabase(app)
                val sessionProjectsRepo = StudioSessionProjectsRepository(localDb.studioSessionProjectDao())
                ProjectStatusViewModel(sessionProjectsRepo)
            }
        }
    )

    val aiViewModel: AiStudioViewModel = viewModel()

    val moodBoardViewModel: MoodBoardViewModel = viewModel(
        key = "mood_board_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val localDb = AppDatabase.getDatabase(app)
                MoodBoardViewModel(
                    moodBoardRepository = MoodBoardRepository(
                        dao = localDb.moodBoardConceptDao(),
                        appContext = app.applicationContext,
                        referenceDao = localDb.moodBoardReferenceDao()
                    ),
                    draftsRepository = ArtistDraftsRepository(localDb.projectDraftDao(), localDb.sessionHistoryDao())
                )
            }
        }
    )

    val moodBoardOrganizerViewModel: MoodBoardOrganizerViewModel = viewModel(
        key = "mood_board_organizer_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val localDb = AppDatabase.getDatabase(app)
                MoodBoardOrganizerViewModel(
                    moodBoardRepository = MoodBoardRepository(
                        dao = localDb.moodBoardConceptDao(),
                        appContext = app.applicationContext,
                        referenceDao = localDb.moodBoardReferenceDao()
                    ),
                    sessionProjectsRepository = StudioSessionProjectsRepository(localDb.studioSessionProjectDao()),
                    serviceCatalogReferenceRepository = ServiceCatalogReferenceRepository(localDb.serviceCatalogReferenceDao()),
                    portfolioProjectsRepository = PortfolioProjectsRepository(localDb.portfolioProjectDao())
                )
            }
        }
    )

    var showProfileDialog by remember { mutableStateOf(false) }
    var showVoiceBookingDialog by remember { mutableStateOf(false) }
    val userProfile by studioViewModel.userProfile.collectAsState()

    val language by studioViewModel.language.collectAsState()
    val currentScreen by studioViewModel.currentScreen.collectAsState()
    val isAdmin by studioViewModel.isAdmin.collectAsState()
    val isSubmitting by studioViewModel.isSubmitting.collectAsState()
    val snackbarMessage by studioViewModel.snackbarMessage.collectAsState()

    val myModels by studioViewModel.myModels.collectAsState()
    val myBookings by studioViewModel.myBookings.collectAsState()
    val myReviews by studioViewModel.myReviews.collectAsState()
    val studioReviews by studioViewModel.studioReviews.collectAsState()
    val savedProjects by studioViewModel.savedProjects.collectAsState()
    val portfolioProjects by studioViewModel.portfolioProjects.collectAsStateWithLifecycle()
    val visiblePortfolioGallery by studioViewModel.visiblePortfolioGallery.collectAsState()

    val allModels by studioViewModel.allModels.collectAsState()
    val allBookings by studioViewModel.allBookings.collectAsState()
    val allGalleryItems by studioViewModel.allGalleryItems.collectAsState()
    val blogPosts by studioViewModel.blogPosts.collectAsState()
    val allBlogPostsForAdmin by studioViewModel.allBlogPostsForAdmin.collectAsState()

    val isLoadingGallery by studioViewModel.isLoadingGallery.collectAsState()
    val isLoadingAdmin by studioViewModel.isLoadingAdmin.collectAsState()
    val isLoadingUserData by studioViewModel.isLoadingUserData.collectAsState()
    val cachedStudioInfo by studioViewModel.cachedStudioInfo.collectAsState()
    val localArtistBio by studioViewModel.localArtistBio.collectAsState()
    val isEmailVerified by studioViewModel.isEmailVerified.collectAsState()
    val preselectedBookingService by studioViewModel.preselectedServiceType.collectAsState()
    val voiceBookingPrefill by studioViewModel.voiceBookingPrefill.collectAsState()
    val studioAnnouncements by studioViewModel.studioAnnouncements.collectAsState()
    val myAppointmentNotifications by studioViewModel.myAppointmentNotifications.collectAsState()
    val unreadNotificationsCount by studioViewModel.unreadNotificationsCount.collectAsState()
    val fcmToken by studioViewModel.fcmToken.collectAsState()
    val announcementsOptIn by studioViewModel.announcementsOptIn.collectAsState()
    val appointmentPushOptIn by studioViewModel.appointmentPushOptIn.collectAsState()
    val favoritePackageIds by studioViewModel.favoritePackageIds.collectAsState()
    val catalogReferenceShots by studioViewModel.catalogReferenceShots.collectAsStateWithLifecycle()
    val photoPreviewInitialTarget by studioViewModel.photoPreviewInitialTarget.collectAsState()
    val photoPreviewInitialPackageId by studioViewModel.photoPreviewInitialPackageId.collectAsState()

    // Handle back button: if on a sub-screen, return to Home
    BackHandler(enabled = currentScreen !is StudioScreen.Home) {
        studioViewModel.navigateTo(StudioScreen.Home)
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            studioViewModel.clearSnackbar()
        }
    }

    val projectStatusFeedback by projectStatusViewModel.statusFeedback.collectAsStateWithLifecycle()
    LaunchedEffect(projectStatusFeedback) {
        projectStatusFeedback?.let {
            snackbarHostState.showSnackbar(it)
            projectStatusViewModel.clearStatusFeedback()
        }
    }

    val moodBoardTargetProjectId by studioViewModel.moodBoardTargetProjectId.collectAsState()
    val ongoingMoodBoardProjects by moodBoardOrganizerViewModel.ongoingProjects.collectAsStateWithLifecycle()

    val moodBoardFeedback by moodBoardViewModel.feedbackMessage.collectAsStateWithLifecycle()
    LaunchedEffect(moodBoardFeedback) {
        moodBoardFeedback?.let {
            snackbarHostState.showSnackbar(it)
            moodBoardViewModel.clearFeedback()
        }
    }

    val moodBoardOrgFeedback by moodBoardOrganizerViewModel.feedbackMessage.collectAsStateWithLifecycle()
    LaunchedEffect(moodBoardOrgFeedback) {
        moodBoardOrgFeedback?.let {
            snackbarHostState.showSnackbar(it)
            moodBoardOrganizerViewModel.clearFeedback()
        }
    }

    // Request POST_NOTIFICATIONS on Android 13+ when the user has an active Confirmed booking
    var hasPromptedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        hasPromptedNotificationPermission = true
    }

    // Ensure authenticated session redirects straight to Home (or MyBookings / Notifications when tapped from push notification)
    LaunchedEffect(currentUserId, openMyBookingsOnStart, openAnnouncementsOnStart) {
        when {
            openMyBookingsOnStart -> {
                studioViewModel.navigateTo(StudioScreen.MyBookings)
                onConsumeOpenMyBookings()
            }
            openAnnouncementsOnStart -> {
                studioViewModel.navigateTo(StudioScreen.Notifications)
                onConsumeOpenAnnouncements()
            }
            else -> {
                studioViewModel.navigateTo(StudioScreen.Home)
            }
        }
    }

    // Keep FCM token & topic subscriptions synchronized with user preferences after initial frames settle
    LaunchedEffect(currentUserId, announcementsOptIn, appointmentPushOptIn) {
        androidx.compose.runtime.withFrameNanos { }
        withContext(Dispatchers.IO) {
            StudioNotificationManager.syncFcmTopicsAndToken(
                context = context,
                userId = currentUserId,
                announcementsOptIn = announcementsOptIn,
                appointmentPushOptIn = appointmentPushOptIn
            )
        }
    }

    // Monitor live studio announcements for newly published broadcasts while user has opted in
    var knownAnnouncementIds by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(studioAnnouncements, announcementsOptIn) {
        val currentIds = studioAnnouncements.map { it.id }.toSet()
        val prev = knownAnnouncementIds
        if (prev != null && announcementsOptIn) {
            studioAnnouncements
                .filter { it.id.isNotBlank() && it.id !in prev }
                .forEach { newAnnouncement ->
                    StudioNotificationManager.showStudioAnnouncementNotification(
                        context = context,
                        announcement = newAnnouncement
                    )
                }
        }
        knownAnnouncementIds = currentIds
    }

    // Real-time client listener:
    // 1. Monitor myBookings status transitions from Pending -> Confirmed / Completed for immediate notification
    // 2. Sync AlarmManager 24-hour advance reminders for all Confirmed upcoming studio sessions
    var previousBookingsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(myBookings) {
        if (previousBookingsMap.isNotEmpty()) {
            myBookings.forEach { currentBooking ->
                val prevStatus = previousBookingsMap[currentBooking.id]
                if (prevStatus != null && !prevStatus.equals(currentBooking.status, ignoreCase = true)) {
                    StudioNotificationManager.checkAndNotifyStatusTransition(
                        context = context,
                        booking = currentBooking,
                        oldStatus = prevStatus,
                        newStatus = currentBooking.status
                    )
                }
            }
        }
        previousBookingsMap = myBookings.associate { it.id to it.status }

        // Schedule / update / cancel 24h local alarms via AlarmManager API on background IO thread
        withContext(Dispatchers.IO) {
            SessionReminderScheduler.syncConfirmedSessionReminders(context, myBookings)
        }

        // Request POST_NOTIFICATIONS runtime permission on Android 13+ once user has a confirmed booking
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPromptedNotificationPermission &&
            !StudioNotificationManager.hasPostNotificationPermission(context) &&
            myBookings.any { it.status.equals("Confirmed", ignoreCase = true) }
        ) {
            hasPromptedNotificationPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showProfileDialog) {
        UserProfileDialog(
            userProfile = userProfile,
            userEmail = Firebase.auth.currentUser?.email,
            currentUserId = currentUserId,
            modelsCount = myModels.size,
            bookingsCount = myBookings.size,
            myBookings = myBookings,
            savedProjects = savedProjects,
            onToggleProjectFavorite = { project -> studioViewModel.toggleProjectFavorite(project) },
            onDeleteSavedProject = { project -> studioViewModel.deleteSavedProject(project) },
            onSaveNewProject = { title, type, notes -> studioViewModel.saveNewProject(title, type, notes) },
            onSignOut = {
                showProfileDialog = false
                val credentialManager = CredentialManager.create(context)
                signOutUser(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = {},
                    scope = coroutineScope
                )
            },
            isStudioLight = isStudioLightMode,
            onToggleTheme = onToggleTheme,
            isSubmitting = isSubmitting,
            language = language,
            onDismiss = { showProfileDialog = false },
            onDeleteMyData = {
                studioViewModel.requestDataDeletion {
                    showProfileDialog = false
                }
            },
            onUpdateProfile = { newName, newAvatarUrl ->
                studioViewModel.updateUserProfile(newName, newAvatarUrl)
            }
        )
    }

    if (showVoiceBookingDialog) {
        VoiceBookingModalDialog(
            language = language,
            defaultClientName = userProfile?.displayName?.takeIf { it.isNotBlank() }
                ?: Firebase.auth.currentUser?.displayName.orEmpty(),
            defaultPhoneNumber = userProfile?.phoneNumber.orEmpty(),
            isEmailVerified = isEmailVerified,
            isSubmitting = isSubmitting,
            onParseCommand = { transcript ->
                studioViewModel.parseVoiceBookingCommand(transcript)
            },
            onSubmitBooking = { clientName, phone, service, date, time, loc, notes, onSuccessWithId ->
                studioViewModel.submitShootBooking(
                    clientName = clientName,
                    contactNumber = phone,
                    serviceType = service,
                    preferredDate = date,
                    preferredTime = time,
                    location = loc,
                    notes = notes,
                    onSuccess = onSuccessWithId
                )
            },
            onApplyToFullForm = { prefill ->
                studioViewModel.navigateToBookingWithPrefill(prefill)
            },
            onViewBookings = {
                studioViewModel.navigateTo(StudioScreen.MyBookings)
            },
            onDismiss = { showVoiceBookingDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = StudioBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (currentScreen !is StudioScreen.PrivacyAndConsent && currentScreen !is StudioScreen.PhotoPreview) {
                StudioTopAppBar(
                    language = language,
                    isAdmin = isAdmin,
                    isStudioLight = isStudioLightMode,
                    avatarUrl = userProfile?.effectiveAvatarUrl,
                    unreadNotificationsCount = unreadNotificationsCount,
                    onToggleTheme = onToggleTheme,
                    onToggleLanguage = { studioViewModel.toggleLanguage() },
                    onOpenNotifications = { studioViewModel.navigateTo(StudioScreen.Notifications) },
                    onOpenPrivacy = { studioViewModel.navigateTo(StudioScreen.PrivacyAndConsent) },
                    onOpenProfile = { studioViewModel.navigateTo(StudioScreen.UserProfile) },
                    onSignOut = {
                        val credentialManager = CredentialManager.create(context)
                        signOutUser(
                            context = context,
                            credentialManager = credentialManager,
                            onSignOutComplete = {
                                // Handled automatically by FirebaseAuth listener
                            },
                            scope = coroutineScope
                        )
                    }
                )
            }
        },
        bottomBar = {
            if (currentScreen !is StudioScreen.PrivacyAndConsent && currentScreen !is StudioScreen.PhotoPreview) {
                StudioBottomNavigation(
                    currentScreen = currentScreen,
                    isAdmin = isAdmin,
                    language = language,
                    isStudioLight = isStudioLightMode,
                    onToggleTheme = onToggleTheme,
                    onNavigate = { studioViewModel.navigateTo(it) },
                    onToggleLanguage = { studioViewModel.toggleLanguage() },
                    onSelectLanguage = { studioViewModel.setLanguage(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StudioBlack)
        ) {
            when (currentScreen) {
                is StudioScreen.Home -> {
                    HomeScreen(
                        language = language,
                        isAdmin = isAdmin,
                        onNavigate = { destination ->
                            if (destination is StudioScreen.ProjectStatusTracker) {
                                projectStatusViewModel.setViewMode(ProjectTrackerViewMode.BOARD)
                            }
                            studioViewModel.navigateTo(destination)
                        },
                        onBookService = { service -> studioViewModel.navigateToBookingWithService(service) },
                        onOpenVoiceBooking = { showVoiceBookingDialog = true },
                        cachedStudioInfo = cachedStudioInfo,
                        studioReviews = studioReviews,
                        latestAnnouncement = studioAnnouncements.firstOrNull(),
                        unreadNotificationsCount = unreadNotificationsCount,
                        favoritePackageIds = favoritePackageIds,
                        onToggleFavoritePackage = { pkg ->
                            studioViewModel.toggleFavoriteServicePackage(pkg.id)
                        },
                        onOpenProjectCalendar = {
                            projectStatusViewModel.setViewMode(ProjectTrackerViewMode.CALENDAR)
                            studioViewModel.navigateTo(StudioScreen.ProjectStatusTracker)
                        }
                    )
                }
                is StudioScreen.ModelRegistration -> {
                    ModelRegistrationScreen(
                        language = language,
                        myModels = myModels,
                        isSubmitting = isSubmitting,
                        isLoading = isLoadingUserData,
                        onSubmit = { name, age, gender, city, phone, skills, portfolio, consent, onSuccess ->
                            studioViewModel.submitModelRegistration(
                                fullName = name,
                                ageStr = age,
                                gender = gender,
                                city = city,
                                phoneNumber = phone,
                                skills = skills,
                                portfolioUrl = portfolio,
                                consentGiven = consent,
                                onSuccess = onSuccess
                            )
                        }
                    )
                }
                is StudioScreen.ShootBooking -> {
                    ShootBookingScreen(
                        language = language,
                        myBookings = myBookings,
                        isSubmitting = isSubmitting,
                        isLoading = isLoadingUserData,
                        userProfile = userProfile,
                        preselectedService = preselectedBookingService,
                        onConsumePreselectedService = { studioViewModel.consumePreselectedServiceType() },
                        voiceBookingPrefill = voiceBookingPrefill,
                        onConsumeVoicePrefill = { studioViewModel.consumeVoiceBookingPrefill() },
                        onParseVoiceCommand = { transcript -> studioViewModel.parseVoiceBookingCommand(transcript) },
                        myReviews = myReviews,
                        studioReviews = studioReviews,
                        isEmailVerified = isEmailVerified,
                        onResendVerification = { onResult ->
                            studioViewModel.resendVerificationEmail(onResult)
                        },
                        onRefreshVerificationStatus = { onComplete ->
                            studioViewModel.refreshEmailVerification(onComplete)
                        },
                        onSubmitReview = { bookingId, serviceType, rating, comment, onSuccess ->
                            studioViewModel.submitServiceReview(
                                bookingId = bookingId,
                                serviceType = serviceType,
                                rating = rating,
                                comment = comment,
                                onSuccess = onSuccess
                            )
                        },
                        onSubmit = { clientName, phone, service, date, time, loc, notes, onSuccessWithId ->
                            studioViewModel.submitShootBooking(
                                clientName = clientName,
                                contactNumber = phone,
                                serviceType = service,
                                preferredDate = date,
                                preferredTime = time,
                                location = loc,
                                notes = notes,
                                onSuccess = onSuccessWithId
                            )
                        },
                        onOpenFullMyBookings = {
                            studioViewModel.navigateTo(StudioScreen.MyBookings)
                        }
                    )
                }
                is StudioScreen.Gallery, is StudioScreen.Portfolio -> {
                    PortfolioScreen(
                        language = language,
                        portfolioItems = visiblePortfolioGallery,
                        isSubmitting = isSubmitting,
                        isLoading = isLoadingGallery,
                        onUploadMedia = { title, cat, type, url, isDemo ->
                            studioViewModel.submitGalleryUpload(
                                title = title,
                                category = cat,
                                mediaType = type,
                                mediaUrl = url,
                                thumbnailUrl = url,
                                isDemo = isDemo,
                                onSuccess = {}
                            )
                        },
                        onNavigateToBooking = {
                            studioViewModel.navigateTo(StudioScreen.ShootBooking)
                        },
                        onOpenProjectArchive = {
                            studioViewModel.navigateTo(StudioScreen.PortfolioProjects)
                        },
                        onOpenPhotoPreview = {
                            studioViewModel.openPhotoPreview(PhotoPreviewDestination.STUDIO_GALLERY)
                        }
                    )
                }
                is StudioScreen.PortfolioProjects -> {
                    PortfolioProjectsScreen(
                        language = language,
                        projects = portfolioProjects,
                        onBack = { studioViewModel.navigateTo(StudioScreen.Home) },
                        onToggleFavorite = { project ->
                            studioViewModel.togglePortfolioProjectFavorite(project)
                        },
                        onDeleteProject = { project ->
                            studioViewModel.deletePortfolioProject(project)
                        },
                        onAddProject = { title, mediaType, category, thumbUrl, mediaUrl, clientName, cameraGear, durationSecs, shots, resolution, desc ->
                            studioViewModel.savePortfolioProject(
                                title = title,
                                mediaType = mediaType,
                                category = category,
                                thumbnailUrl = thumbUrl,
                                mediaUrl = mediaUrl,
                                clientName = clientName,
                                cameraGear = cameraGear,
                                durationSeconds = durationSecs,
                                shotCount = shots,
                                resolutionTag = resolution,
                                description = desc
                            )
                        },
                        onBookSimilarShoot = { serviceType ->
                            studioViewModel.navigateToBookingWithService(serviceType)
                        },
                        onOpenPhotoPreview = {
                            studioViewModel.openPhotoPreview(PhotoPreviewDestination.PORTFOLIO_ARCHIVE)
                        }
                    )
                }
                is StudioScreen.ProjectStatusTracker -> {
                    ProjectStatusTrackerScreen(
                        language = language,
                        viewModel = projectStatusViewModel,
                        onBack = { studioViewModel.navigateTo(StudioScreen.Home) },
                        onOpenMoodBoard = { projectId ->
                            moodBoardOrganizerViewModel.selectProject(projectId)
                            studioViewModel.navigateTo(StudioScreen.MoodBoard)
                        }
                    )
                }
                is StudioScreen.MoodBoard -> {
                    MoodBoardWorkspaceScreen(
                        language = language,
                        viewModel = moodBoardOrganizerViewModel,
                        onBack = { studioViewModel.navigateTo(StudioScreen.Home) },
                        onOpenConceptGenerator = { projectId ->
                            moodBoardViewModel.setTargetSessionProject(projectId)
                            studioViewModel.navigateTo(StudioScreen.MoodBoardGenerator)
                        },
                        onOpenCameraReference = { projectId ->
                            studioViewModel.openPhotoPreview(
                                target = PhotoPreviewDestination.MOOD_BOARD,
                                moodBoardProjectId = projectId
                            )
                        },
                        onRemixConceptInAiStudio = { tile ->
                            moodBoardViewModel.loadConceptIntoCanvasForRemix(tile.entity)
                            studioViewModel.navigateTo(StudioScreen.MoodBoardGenerator)
                        }
                    )
                }
                is StudioScreen.MoodBoardGenerator -> {
                    MoodBoardArtScreen(
                        language = language,
                        viewModel = moodBoardViewModel,
                        ongoingProjects = ongoingMoodBoardProjects,
                        onOpenMoodBoardWorkspace = {
                            val activeProj = moodBoardViewModel.targetSessionProjectId.value
                            if (activeProj != null) {
                                moodBoardOrganizerViewModel.selectProject(activeProj)
                            }
                            studioViewModel.navigateTo(StudioScreen.MoodBoard)
                        },
                        onBack = { studioViewModel.navigateTo(StudioScreen.Home) }
                    )
                }
                is StudioScreen.PhotoPreview -> {
                    PhotoPreviewScreen(
                        language = language,
                        initialDestination = photoPreviewInitialTarget,
                        initialPackageId = photoPreviewInitialPackageId,
                        isSubmitting = isSubmitting,
                        onBack = { studioViewModel.navigateTo(StudioScreen.Home) },
                        onSaveToPortfolioArchive = { title, category, dataUrl, cameraGear, resolutionTag, description, onSaved ->
                            studioViewModel.savePortfolioProject(
                                title = title,
                                mediaType = PortfolioProjectMediaType.PHOTO,
                                category = category,
                                thumbnailUrl = dataUrl,
                                mediaUrl = dataUrl,
                                clientName = userProfile?.displayName?.takeIf { it.isNotBlank() } ?: "Star Artist Studio",
                                cameraGear = cameraGear,
                                durationSeconds = 0,
                                shotCount = 1,
                                resolutionTag = resolutionTag,
                                description = description,
                                onSaved = onSaved
                            )
                        },
                        onSaveToServiceCatalog = { packageId, title, notes, imageDataUrl, aspectGuide, resolutionTag, onSaved ->
                            studioViewModel.saveCatalogReferenceShot(
                                packageId = packageId,
                                title = title,
                                notes = notes,
                                imageDataUrl = imageDataUrl,
                                aspectGuide = aspectGuide,
                                resolutionTag = resolutionTag,
                                onSaved = onSaved
                            )
                        },
                        onPublishToStudioGallery = { title, category, dataUrl, onSaved ->
                            studioViewModel.submitGalleryUpload(
                                title = title,
                                category = category,
                                mediaType = "photo",
                                mediaUrl = dataUrl,
                                thumbnailUrl = "",
                                isDemo = false,
                                onSuccess = onSaved
                            )
                        },
                        onSaveToMoodBoard = { title, sectionTag, notes, dataUrl, onSaved ->
                            moodBoardOrganizerViewModel.addReferenceFromCameraShot(
                                dataUrl = dataUrl,
                                title = title,
                                sectionTag = sectionTag,
                                notes = notes,
                                targetProjectId = moodBoardTargetProjectId
                            )
                            onSaved()
                        },
                        onOpenDestinationScreen = { dest ->
                            when (dest) {
                                PhotoPreviewDestination.PORTFOLIO_ARCHIVE ->
                                    studioViewModel.navigateTo(StudioScreen.PortfolioProjects)
                                PhotoPreviewDestination.SERVICE_CATALOG ->
                                    studioViewModel.navigateTo(StudioScreen.ServiceCatalog)
                                PhotoPreviewDestination.STUDIO_GALLERY ->
                                    studioViewModel.navigateTo(StudioScreen.Portfolio)
                                PhotoPreviewDestination.MOOD_BOARD -> {
                                    moodBoardTargetProjectId?.let { pid ->
                                        moodBoardOrganizerViewModel.selectProject(pid)
                                    }
                                    studioViewModel.navigateTo(StudioScreen.MoodBoard)
                                }
                            }
                        },
                        onShowMessage = { msg -> studioViewModel.showSnackbar(msg) }
                    )
                }
                is StudioScreen.AiStudio -> {
                    AiStudioScreen(
                        language = language,
                        aiViewModel = aiViewModel,
                        onNavigateToBooking = {
                            studioViewModel.navigateTo(StudioScreen.ShootBooking)
                        },
                        onOpenMoodBoardArt = {
                            studioViewModel.navigateTo(StudioScreen.MoodBoardGenerator)
                        },
                        onOpenMoodBoardWorkspace = {
                            studioViewModel.navigateTo(StudioScreen.MoodBoard)
                        }
                    )
                }
                is StudioScreen.Blog -> {
                    BlogScreen(
                        language = language,
                        blogPosts = blogPosts,
                        isAdmin = isAdmin,
                        isSubmitting = isSubmitting,
                        onPublishPost = { title, cat, content, summary, imageUrl, tags, status ->
                            studioViewModel.publishBlogPost(title, cat, content, summary, imageUrl, tags, status)
                        },
                        onUpdatePost = { id, title, cat, content, summary, imageUrl, tags, status ->
                            studioViewModel.updateBlogPost(id, title, cat, content, summary, imageUrl, tags, status)
                        },
                        onDeletePost = { id ->
                            studioViewModel.deleteBlogPost(id)
                        },
                        onNavigateToBooking = {
                            studioViewModel.navigateTo(StudioScreen.ShootBooking)
                        }
                    )
                }
                is StudioScreen.AdminDashboard -> {
                    AdminDashboardScreen(
                        language = language,
                        isAdmin = isAdmin,
                        isLoading = isLoadingAdmin,
                        allModels = allModels,
                        allBookings = allBookings,
                        allGalleryItems = allGalleryItems,
                        allBlogPosts = allBlogPostsForAdmin,
                        allAnnouncements = studioAnnouncements,
                        onUpdateModelStatus = { id, st, notes ->
                            studioViewModel.updateModelStatus(id, st, notes)
                        },
                        onUpdateBookingStatus = { id, st ->
                            studioViewModel.updateBookingStatus(id, st)
                        },
                        onDeleteBooking = { id ->
                            studioViewModel.deleteBooking(id)
                        },
                        onUpdateGalleryStatus = { id, st ->
                            studioViewModel.updateGalleryStatus(id, st)
                        },
                        onDeleteGalleryItem = { id ->
                            studioViewModel.deleteGalleryItem(id)
                        },
                        onPublishPost = { title, cat, content, summary, imageUrl, tags, status ->
                            studioViewModel.publishBlogPost(title, cat, content, summary, imageUrl, tags, status)
                        },
                        onUpdatePost = { id, title, cat, content, summary, imageUrl, tags, status ->
                            studioViewModel.updateBlogPost(id, title, cat, content, summary, imageUrl, tags, status)
                        },
                        onDeletePost = { id ->
                            studioViewModel.deleteBlogPost(id)
                        },
                        onPublishAnnouncement = { title, message, category, priority, actionRoute, onSuccess ->
                            studioViewModel.publishStudioAnnouncement(
                                title = title,
                                message = message,
                                category = category,
                                priority = priority,
                                actionRoute = actionRoute,
                                onSuccess = onSuccess
                            )
                        },
                        onDeleteAnnouncement = { announcementId ->
                            studioViewModel.deleteStudioAnnouncement(announcementId)
                        },
                        onAdminAuthSuccess = {
                            studioViewModel.refreshUserProfile()
                        },
                        onNavigateToHome = {
                            studioViewModel.navigateTo(StudioScreen.Home)
                        },
                        onOpenFullAnalytics = {
                            studioViewModel.navigateTo(StudioScreen.Analytics)
                        },
                        onSignOut = {
                            val credentialManager = CredentialManager.create(context)
                            signOutUser(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {
                                    studioViewModel.refreshUserProfile()
                                },
                                scope = coroutineScope
                            )
                        }
                    )
                }
                is StudioScreen.Analytics -> {
                    StudioAnalyticsScreen(
                        language = language,
                        bookings = if (isAdmin) allBookings else myBookings,
                        isAdmin = isAdmin,
                        isLoading = if (isAdmin) isLoadingAdmin else isLoadingUserData,
                        onBack = { studioViewModel.navigateTo(StudioScreen.Home) },
                        onRefresh = { studioViewModel.refreshData() },
                        onBookShoot = { service ->
                            studioViewModel.navigateToBookingWithService(service)
                        }
                    )
                }
                is StudioScreen.Notifications -> {
                    NotificationsScreen(
                        language = language,
                        isAdmin = isAdmin,
                        fcmToken = fcmToken,
                        announcementsOptIn = announcementsOptIn,
                        appointmentPushOptIn = appointmentPushOptIn,
                        announcements = studioAnnouncements,
                        appointmentNotifications = myAppointmentNotifications,
                        isSubmitting = isSubmitting,
                        onBack = { studioViewModel.navigateTo(StudioScreen.Home) },
                        onSyncFcm = { studioViewModel.refreshFcmToken(context) },
                        onUpdatePushPreferences = { annOpt, apptOpt ->
                            studioViewModel.updatePushPreferences(context, annOpt, apptOpt)
                        },
                        onMarkNotificationRead = { notifId ->
                            studioViewModel.markNotificationRead(notifId)
                        },
                        onMarkAllNotificationsRead = {
                            studioViewModel.markAllNotificationsRead()
                        },
                        onPublishAnnouncement = { title, message, category, priority, actionRoute, onSuccess ->
                            studioViewModel.publishStudioAnnouncement(
                                title = title,
                                message = message,
                                category = category,
                                priority = priority,
                                actionRoute = actionRoute,
                                onSuccess = onSuccess
                            )
                        },
                        onDeleteAnnouncement = { announcementId ->
                            studioViewModel.deleteStudioAnnouncement(announcementId)
                        },
                        onNavigateToScreen = { targetScreen ->
                            studioViewModel.navigateTo(targetScreen)
                        }
                    )
                }
                is StudioScreen.Contact -> {
                    ContactScreen(
                        language = language,
                        cachedStudioInfo = cachedStudioInfo,
                        onRefreshCache = { studioViewModel.refreshStudioInfoCache() }
                    )
                }
                is StudioScreen.ServiceCatalog -> {
                    ServiceCatalogScreen(
                        language = language,
                        favoritePackageIds = favoritePackageIds,
                        catalogReferenceShots = catalogReferenceShots,
                        studioReviews = studioReviews,
                        myReviews = myReviews,
                        isSubmittingReview = isSubmitting,
                        onToggleFavoritePackage = { pkg ->
                            studioViewModel.toggleFavoriteServicePackage(pkg.id)
                        },
                        onOpenReferenceCamera = { pkgId ->
                            studioViewModel.openPhotoPreview(
                                target = PhotoPreviewDestination.SERVICE_CATALOG,
                                packageId = pkgId
                            )
                        },
                        onDeleteReferenceShot = { ref ->
                            studioViewModel.deleteCatalogReferenceShot(ref)
                        },
                        onSubmitServiceReview = { pkgId, serviceType, rating, comment, onSuccess ->
                            studioViewModel.submitServiceReview(
                                bookingId = catalogReviewBookingId(pkgId),
                                serviceType = serviceType,
                                rating = rating,
                                comment = comment,
                                onSuccess = onSuccess
                            )
                        },
                        onBack = { studioViewModel.navigateTo(StudioScreen.Home) },
                        onBookService = { serviceWithTier ->
                            studioViewModel.navigateToBookingWithService(serviceWithTier)
                        }
                    )
                }
                is StudioScreen.MyBookings -> {
                    MyBookingsScreen(
                        language = language,
                        myBookings = myBookings,
                        myReviews = myReviews,
                        isLoading = isLoadingUserData,
                        isSubmitting = isSubmitting,
                        onBack = { studioViewModel.navigateTo(StudioScreen.Home) },
                        onRefresh = { studioViewModel.refreshData() },
                        onBookNewShoot = { studioViewModel.navigateTo(StudioScreen.ShootBooking) },
                        onBookAgain = { serviceType ->
                            studioViewModel.navigateToBookingWithService(serviceType)
                        },
                        onCancelBooking = { bookingId ->
                            studioViewModel.cancelMyBooking(bookingId)
                        },
                        onSubmitReview = { bookingId, serviceType, rating, comment, onSuccess ->
                            studioViewModel.submitServiceReview(
                                bookingId = bookingId,
                                serviceType = serviceType,
                                rating = rating,
                                comment = comment,
                                onSuccess = onSuccess
                            )
                        }
                    )
                }
                is StudioScreen.PrivacyAndConsent -> {
                    PrivacyScreen(
                        language = language,
                        isSubmitting = isSubmitting,
                        userProfile = userProfile,
                        userEmail = Firebase.auth.currentUser?.email,
                        currentUserId = currentUserId,
                        modelsCount = myModels.size,
                        bookingsCount = myBookings.size,
                        onBack = { studioViewModel.navigateTo(StudioScreen.Home) },
                        onRequestDataDeletion = { onComplete ->
                            studioViewModel.requestDataDeletion(onComplete)
                        }
                    )
                }
                is StudioScreen.UserProfile -> {
                    UserProfileScreen(
                        userProfile = userProfile,
                        userEmail = Firebase.auth.currentUser?.email,
                        currentUserId = currentUserId,
                        modelsCount = myModels.size,
                        bookingsCount = myBookings.size,
                        myBookings = myBookings,
                        savedProjects = savedProjects,
                        myReviews = myReviews,
                        favoritePackageIds = favoritePackageIds,
                        localArtistBio = localArtistBio,
                        isEmailVerified = isEmailVerified,
                        isSubmitting = isSubmitting,
                        isStudioLight = isStudioLightMode,
                        language = language,
                        onBack = { studioViewModel.navigateTo(StudioScreen.Home) },
                        onSaveLocalBio = { specialty, bioText, onComplete ->
                            studioViewModel.saveLocalArtistBio(
                                specialty = specialty,
                                bioText = bioText,
                                onResult = onComplete
                            )
                        },
                        onUpdateArtistProfile = { name, avatar, phone, city, instagram, specialty, bio, portfolioLinks, onComplete ->
                            studioViewModel.updateUserProfile(
                                displayName = name,
                                avatarUrl = avatar,
                                phoneNumber = phone,
                                city = city,
                                instagramHandle = instagram,
                                specialty = specialty,
                                bio = bio,
                                portfolioLinks = portfolioLinks,
                                onResult = onComplete
                            )
                        },
                        onSetProfilePhoto = { newAvatarUrl, onComplete ->
                            studioViewModel.updateProfilePhoto(
                                avatarUrl = newAvatarUrl,
                                onResult = onComplete
                            )
                        },
                        onUploadPortfolioPhoto = { title, cat, type, url, isDemo ->
                            studioViewModel.submitGalleryUpload(
                                title = title,
                                category = cat,
                                mediaType = type,
                                mediaUrl = url,
                                thumbnailUrl = "",
                                isDemo = isDemo,
                                onSuccess = {}
                            )
                        },
                        onSubmitReview = { bookingId, serviceType, rating, comment, onSuccess ->
                            studioViewModel.submitServiceReview(
                                bookingId = bookingId,
                                serviceType = serviceType,
                                rating = rating,
                                comment = comment,
                                onSuccess = onSuccess
                            )
                        },
                        onToggleFavoritePackage = { pkg ->
                            studioViewModel.toggleFavoriteServicePackage(pkg.id)
                        },
                        onBookService = { service ->
                            studioViewModel.navigateToBookingWithService(service)
                        },
                        onOpenMyBookings = {
                            studioViewModel.navigateTo(StudioScreen.MyBookings)
                        },
                        onOpenServiceCatalog = {
                            studioViewModel.navigateTo(StudioScreen.ServiceCatalog)
                        },
                        onToggleTheme = onToggleTheme,
                        onSelectLanguage = { studioViewModel.setLanguage(it) },
                        onOpenPrivacy = { studioViewModel.navigateTo(StudioScreen.PrivacyAndConsent) },
                        onDeleteMyData = {
                            studioViewModel.requestDataDeletion {
                                studioViewModel.navigateTo(StudioScreen.Home)
                            }
                        },
                        onSignOut = {
                            val credentialManager = CredentialManager.create(context)
                            signOutUser(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {},
                                scope = coroutineScope
                            )
                        }
                    )
                }
            }
        }
    }
}
