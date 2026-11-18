package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ArtistBioEntity
import com.example.data.local.entity.PortfolioProjectEntity
import com.example.data.local.entity.PortfolioProjectMediaType
import com.example.data.local.entity.ProjectDraftEntity
import com.example.data.local.entity.ServiceCatalogReferenceEntity
import com.example.data.local.entity.StudioInfoEntity
import com.example.data.model.AppLanguage
import com.example.data.model.AppointmentNotification
import com.example.data.model.BlogPost
import com.example.data.model.GalleryItem
import com.example.data.model.ModelRegistration
import com.example.data.model.ServiceReview
import com.example.data.model.ShootBooking
import com.example.data.model.StudioAnnouncement
import com.example.data.model.StudioServicesData
import com.example.data.model.UserProfile
import com.example.data.repository.ArtistBioRepository
import com.example.data.repository.ArtistDraftsRepository
import com.example.data.repository.PortfolioProjectsRepository
import com.example.data.repository.ServiceCatalogReferenceRepository
import com.example.data.repository.StudioInfoRepository
import com.example.data.repository.StudioRepository
import com.example.notification.StudioNotificationManager
import com.example.util.ParsedVoiceBookingCommand
import com.example.util.StudioConstants
import com.example.util.StudioStrings
import com.example.util.VoiceBookingCommandParser
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface StudioScreen {
    data object Home : StudioScreen
    data object ModelRegistration : StudioScreen
    data object ShootBooking : StudioScreen
    data object Gallery : StudioScreen
    data object Portfolio : StudioScreen
    data object Blog : StudioScreen
    data object AiStudio : StudioScreen
    data object AdminDashboard : StudioScreen
    data object Contact : StudioScreen
    data object PrivacyAndConsent : StudioScreen
    data object UserProfile : StudioScreen
    data object ServiceCatalog : StudioScreen
    data object MyBookings : StudioScreen
    data object PortfolioProjects : StudioScreen
    data object Analytics : StudioScreen
    data object Notifications : StudioScreen
    data object PhotoPreview : StudioScreen
    data object ProjectStatusTracker : StudioScreen
    data object MoodBoardGenerator : StudioScreen
    data object MoodBoard : StudioScreen
}

enum class PhotoPreviewDestination {
    PORTFOLIO_ARCHIVE,
    SERVICE_CATALOG,
    STUDIO_GALLERY,
    MOOD_BOARD
}

data class VoiceBookingPrefill(
    val serviceType: String,
    val preferredDate: String,
    val preferredTime: String,
    val location: String,
    val notes: String,
    val clientName: String = "",
    val contactNumber: String = ""
)

class StudioViewModel(
    private val repository: StudioRepository,
    val currentUserId: String,
    private val studioInfoRepository: StudioInfoRepository? = null,
    private val artistDraftsRepository: ArtistDraftsRepository? = null,
    private val artistBioRepository: ArtistBioRepository? = null,
    private val portfolioProjectsRepository: PortfolioProjectsRepository? = null,
    private val serviceCatalogReferenceRepository: ServiceCatalogReferenceRepository? = null
) : ViewModel() {

    // Offline-cached studio info from Room database
    val cachedStudioInfo: StateFlow<StudioInfoEntity> = (studioInfoRepository?.studioInfo
        ?: flowOf(StudioInfoRepository.defaultStudioInfo()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = StudioInfoRepository.defaultStudioInfo()
        )

    // Locally persisted artist professional bio & specialty from Room database
    val localArtistBio: StateFlow<ArtistBioEntity?> = (artistBioRepository?.observeBio(currentUserId)
        ?: flowOf(null))
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _isStudioLightMode = MutableStateFlow(false)
    val isStudioLightMode: StateFlow<Boolean> = _isStudioLightMode.asStateFlow()

    private val _currentScreen = MutableStateFlow<StudioScreen>(StudioScreen.Home)
    val currentScreen: StateFlow<StudioScreen> = _currentScreen.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isEmailVerified = MutableStateFlow(Firebase.auth.currentUser?.isEmailVerified ?: false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    // Status snackbar / feedback
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _preselectedServiceType = MutableStateFlow<String?>(null)
    val preselectedServiceType: StateFlow<String?> = _preselectedServiceType.asStateFlow()

    private val _voiceBookingPrefill = MutableStateFlow<VoiceBookingPrefill?>(null)
    val voiceBookingPrefill: StateFlow<VoiceBookingPrefill?> = _voiceBookingPrefill.asStateFlow()

    private val _photoPreviewInitialTarget = MutableStateFlow(PhotoPreviewDestination.PORTFOLIO_ARCHIVE)
    val photoPreviewInitialTarget: StateFlow<PhotoPreviewDestination> = _photoPreviewInitialTarget.asStateFlow()

    private val _photoPreviewInitialPackageId = MutableStateFlow<String?>(null)
    val photoPreviewInitialPackageId: StateFlow<String?> = _photoPreviewInitialPackageId.asStateFlow()

    private val _moodBoardTargetProjectId = MutableStateFlow<Long?>(null)
    val moodBoardTargetProjectId: StateFlow<Long?> = _moodBoardTargetProjectId.asStateFlow()

    val approvedGallery: StateFlow<List<GalleryItem>> = repository.observeApprovedGallery()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myGalleryItems: StateFlow<List<GalleryItem>> = repository.observeMyGalleryItems(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val visiblePortfolioGallery: StateFlow<List<GalleryItem>> = kotlinx.coroutines.flow.combine(
        myGalleryItems,
        approvedGallery
    ) { mine, approved ->
        val seenIds = HashSet<String>()
        val merged = mutableListOf<GalleryItem>()
        // Prioritize user's newly captured/uploaded portfolio photos first
        mine.filter { !it.status.equals("rejected", ignoreCase = true) }
            .forEach { item ->
                if (seenIds.add(item.id)) merged.add(item)
            }
        approved.forEach { item ->
            if (seenIds.add(item.id)) merged.add(item)
        }
        merged
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myModels: StateFlow<List<ModelRegistration>> = repository.observeMyModelRegistrations(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myBookings: StateFlow<List<ShootBooking>> = repository.observeMyBookings(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myReviews: StateFlow<List<ServiceReview>> = repository.observeMyReviews(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val studioReviews: StateFlow<List<ServiceReview>> = repository.observeReviews()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Firebase Cloud Messaging: Studio Announcements (topic) & Per-User Appointment Status Updates
    val studioAnnouncements: StateFlow<List<StudioAnnouncement>> = repository.observeStudioAnnouncements()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myAppointmentNotifications: StateFlow<List<AppointmentNotification>> =
        repository.observeUserNotifications(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = myAppointmentNotifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val _fcmToken = MutableStateFlow("")
    val fcmToken: StateFlow<String> = _fcmToken.asStateFlow()

    val announcementsOptIn: StateFlow<Boolean> = _userProfile
        .map { it?.announcementsOptIn ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val appointmentPushOptIn: StateFlow<Boolean> = _userProfile
        .map { it?.appointmentPushOptIn ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val favoritePackageIds: StateFlow<Set<String>> = _userProfile
        .map { it?.favoritePackageIds?.toSet().orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    fun toggleFavoriteServicePackage(packageId: String) {
        val cleanId = packageId.trim()
        if (cleanId.isEmpty() || currentUserId.isBlank()) return

        val previousProfile = _userProfile.value
        val existingIds = previousProfile?.favoritePackageIds.orEmpty()
        val wasFavorite = existingIds.contains(cleanId)
        val optimisticIds = if (wasFavorite) {
            existingIds.filterNot { it == cleanId }
        } else {
            (existingIds + cleanId).distinct().take(30)
        }
        if (previousProfile != null) {
            _userProfile.value = previousProfile.copy(favoritePackageIds = optimisticIds)
        }

        viewModelScope.launch {
            val result = repository.toggleFavoriteServicePackage(currentUserId, cleanId)
            if (result.isSuccess) {
                val syncedIds = result.getOrNull() ?: optimisticIds
                _userProfile.value = _userProfile.value?.copy(favoritePackageIds = syncedIds)
                val pkgTitle = StudioServicesData.findPackageById(cleanId)?.let {
                    if (_language.value == AppLanguage.HINDI) it.titleHi else it.titleEn
                } ?: cleanId
                showSnackbar(
                    if (wasFavorite) {
                        if (_language.value == AppLanguage.HINDI) "$pkgTitle को पसंदीदा से हटाया गया"
                        else "$pkgTitle removed from saved favorites"
                    } else {
                        if (_language.value == AppLanguage.HINDI) "$pkgTitle पसंदीदा में सहेजा गया!"
                        else "$pkgTitle saved to your favorites!"
                    }
                )
            } else {
                if (previousProfile != null) {
                    _userProfile.value = previousProfile
                }
                showSnackbar(
                    result.exceptionOrNull()?.localizedMessage ?: if (_language.value == AppLanguage.HINDI)
                        "पसंदीदा पैकेज अपडेट करने में त्रुटि हुई।"
                    else
                        "Failed to update saved favorite package."
                )
            }
        }
    }

    // Saved photography projects and drafts for the authenticated client
    val savedProjects: StateFlow<List<ProjectDraftEntity>> = (artistDraftsRepository?.allDrafts ?: flowOf(emptyList()))
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun toggleProjectFavorite(project: ProjectDraftEntity) {
        viewModelScope.launch {
            artistDraftsRepository?.toggleFavorite(project)
        }
    }

    fun deleteSavedProject(project: ProjectDraftEntity) {
        viewModelScope.launch {
            artistDraftsRepository?.deleteDraft(project)
            showSnackbar(
                if (_language.value == AppLanguage.HINDI) "प्रोजेक्ट हटा दिया गया" else "Project removed from saved list"
            )
        }
    }

    fun saveNewProject(title: String, type: String, conceptNotes: String, budget: Long = 0L) {
        viewModelScope.launch {
            artistDraftsRepository?.saveDraft(
                ProjectDraftEntity(
                    draftTitle = title,
                    projectType = type,
                    conceptNotes = conceptNotes,
                    estimatedBudgetInr = budget
                )
            )
            showSnackbar(
                if (_language.value == AppLanguage.HINDI) "प्रोजेक्ट सफलतापूर्वक सहेज लिया गया" else "Project saved to your profile"
            )
        }
    }

    // Room-persisted Photography & Video Portfolio Projects with image/video thumbnails
    val portfolioProjects: StateFlow<List<PortfolioProjectEntity>> =
        (portfolioProjectsRepository?.allProjects ?: flowOf(emptyList()))
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun savePortfolioProject(
        title: String,
        mediaType: String,
        category: String,
        thumbnailUrl: String,
        mediaUrl: String,
        clientName: String,
        cameraGear: String,
        durationSeconds: Int,
        shotCount: Int,
        resolutionTag: String,
        description: String,
        onSaved: () -> Unit = {}
    ) {
        val cleanTitle = title.trim()
        val cleanThumb = thumbnailUrl.trim().ifEmpty { mediaUrl.trim() }
        if (cleanTitle.length < 2) {
            showSnackbar(
                if (_language.value == AppLanguage.HINDI) "कृपया प्रोजेक्ट का शीर्षक दर्ज करें।"
                else "Please enter a project title (at least 2 characters)."
            )
            return
        }
        if (cleanThumb.isEmpty()) {
            showSnackbar(
                if (_language.value == AppLanguage.HINDI) "कृपया प्रोजेक्ट थंबनेल या मीडिया चुनें।"
                else "Please select a project photo or video thumbnail."
            )
            return
        }
        viewModelScope.launch {
            val isVideo = mediaType.equals(PortfolioProjectMediaType.VIDEO, ignoreCase = true)
            portfolioProjectsRepository?.saveProject(
                PortfolioProjectEntity(
                    title = cleanTitle,
                    mediaType = if (isVideo) PortfolioProjectMediaType.VIDEO else PortfolioProjectMediaType.PHOTO,
                    category = category.trim().ifEmpty { if (isVideo) "Music Video" else "Editorial" },
                    description = description.trim(),
                    thumbnailUrl = cleanThumb,
                    mediaUrl = mediaUrl.trim().ifEmpty { cleanThumb },
                    clientName = clientName.trim().ifEmpty { "Star Artist Studio" },
                    cameraGear = cameraGear.trim().ifEmpty {
                        if (isVideo) "Sony FX3 • 4K Cinema" else "Sony A7R V • 85mm GM"
                    },
                    durationSeconds = if (isVideo) durationSeconds.coerceAtLeast(15) else 0,
                    shotCount = if (isVideo) 1 else shotCount.coerceAtLeast(1),
                    resolutionTag = resolutionTag.trim().ifEmpty {
                        if (isVideo) "4K 60fps" else "61MP RAW"
                    }
                )
            )
            showSnackbar(
                if (_language.value == AppLanguage.HINDI) "पोर्टफोलियो प्रोजेक्ट लोकल डेटाबेस में सहेजा गया!"
                else "Portfolio project saved to local Room archive!"
            )
            onSaved()
        }
    }

    fun togglePortfolioProjectFavorite(project: PortfolioProjectEntity) {
        viewModelScope.launch {
            portfolioProjectsRepository?.toggleFavorite(project)
        }
    }

    fun deletePortfolioProject(project: PortfolioProjectEntity) {
        viewModelScope.launch {
            portfolioProjectsRepository?.deleteProject(project)
            showSnackbar(
                if (_language.value == AppLanguage.HINDI) "पोर्टफोलियो प्रोजेक्ट हटाया गया।"
                else "Portfolio project removed."
            )
        }
    }

    // Artist-captured quick reference shots attached to Service Catalog offerings (Room DB)
    val catalogReferenceShots: StateFlow<Map<String, List<ServiceCatalogReferenceEntity>>> =
        (serviceCatalogReferenceRepository?.referencesByPackage ?: flowOf(emptyMap()))
            .catch { emit(emptyMap()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    fun saveCatalogReferenceShot(
        packageId: String,
        title: String,
        notes: String,
        imageDataUrl: String,
        aspectGuide: String,
        resolutionTag: String,
        onSaved: () -> Unit = {}
    ) {
        val cleanPkgId = packageId.trim()
        val cleanUrl = imageDataUrl.trim()
        if (cleanPkgId.isEmpty() || cleanUrl.isEmpty()) {
            showSnackbar(
                if (_language.value == AppLanguage.HINDI) "कृपया पैकेज और संदर्भ फोटो चुनें।"
                else "Please select a service package and capture a reference shot."
            )
            return
        }
        viewModelScope.launch {
            serviceCatalogReferenceRepository?.saveReference(
                packageId = cleanPkgId,
                title = title.trim().ifEmpty { "Studio Reference Shot" },
                notes = notes.trim(),
                imageDataUrl = cleanUrl,
                aspectGuide = aspectGuide,
                resolutionTag = resolutionTag
            )
            val pkgName = StudioServicesData.findPackageById(cleanPkgId)?.let {
                if (_language.value == AppLanguage.HINDI) it.titleHi else it.titleEn
            } ?: cleanPkgId
            showSnackbar(
                if (_language.value == AppLanguage.HINDI)
                    "$pkgName कैटलॉग पैकेज में संदर्भ शॉट सहेजा गया!"
                else
                    "Reference shot attached to $pkgName in Service Catalog!"
            )
            onSaved()
        }
    }

    fun deleteCatalogReferenceShot(reference: ServiceCatalogReferenceEntity) {
        viewModelScope.launch {
            serviceCatalogReferenceRepository?.deleteReference(reference)
            showSnackbar(
                if (_language.value == AppLanguage.HINDI) "संदर्भ फोटो हटाई गई।"
                else "Reference shot removed from catalog package."
            )
        }
    }

    // Admin flows (strictly gated on verified admin role to prevent unauthorized reads)
    @OptIn(ExperimentalCoroutinesApi::class)
    val allModels: StateFlow<List<ModelRegistration>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllModelRegistrations().catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allBookings: StateFlow<List<ShootBooking>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllBookings().catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allGalleryItems: StateFlow<List<GalleryItem>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllGalleryItems().catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val blogPosts: StateFlow<List<BlogPost>> = repository.observeBlogPosts()
        .catch { emit(StudioRepository.defaultSeedBlogPosts()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StudioRepository.defaultSeedBlogPosts())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allBlogPostsForAdmin: StateFlow<List<BlogPost>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllBlogPostsForAdmin().catch { emit(StudioRepository.defaultSeedBlogPosts()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Loading state indicators for skeleton screens
    private val _isLoadingGallery = MutableStateFlow(true)
    val isLoadingGallery: StateFlow<Boolean> = _isLoadingGallery.asStateFlow()

    private val _isLoadingBlog = MutableStateFlow(false)
    val isLoadingBlog: StateFlow<Boolean> = _isLoadingBlog.asStateFlow()

    private val _isLoadingAdmin = MutableStateFlow(true)
    val isLoadingAdmin: StateFlow<Boolean> = _isLoadingAdmin.asStateFlow()

    private val _isLoadingUserData = MutableStateFlow(true)
    val isLoadingUserData: StateFlow<Boolean> = _isLoadingUserData.asStateFlow()

    init {
        loadUserProfile()
        viewModelScope.launch {
            portfolioProjectsRepository?.seedShowcaseProjectsIfEmpty()
        }
        viewModelScope.launch {
            repository.observeUserProfile(currentUserId)
                .catch { /* Handled gracefully via loadUserProfile */ }
                .collect { observed ->
                    if (observed != null) {
                        _userProfile.value = observed
                        if (observed.fcmToken.isNotBlank()) {
                            _fcmToken.value = observed.fcmToken
                        }
                    }
                }
        }
        viewModelScope.launch {
            repository.observeApprovedGallery().collect {
                _isLoadingGallery.value = false
            }
        }
        viewModelScope.launch {
            repository.observeMyBookings(currentUserId).collect {
                _isLoadingUserData.value = false
            }
        }
        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            _isAdmin
                .flatMapLatest { admin ->
                    if (admin) repository.observeAllModelRegistrations().catch { emit(emptyList()) }
                    else flowOf(emptyList())
                }
                .collect {
                    _isLoadingAdmin.value = false
                }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoadingGallery.value = true
            _isLoadingAdmin.value = true
            _isLoadingUserData.value = true
            kotlinx.coroutines.delay(600)
            _isLoadingGallery.value = false
            _isLoadingAdmin.value = false
            _isLoadingUserData.value = false
        }
    }

    fun refreshUserProfile() {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser ?: return@launch
            try {
                try {
                    user.reload().await()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Use current cached auth state if offline
                }
                _isEmailVerified.value = user.isEmailVerified
                val profile = repository.getOrCreateUserProfile(
                    userId = user.uid,
                    email = user.email.orEmpty(),
                    displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Star Artist Member",
                    photoUrl = user.photoUrl?.toString().orEmpty()
                )
                _userProfile.value = profile
                if (profile.fcmToken.isNotBlank()) {
                    _fcmToken.value = profile.fcmToken
                }
                if (profile.bio.isNotBlank() && artistBioRepository?.getBioOnce(user.uid) == null) {
                    artistBioRepository?.saveBio(
                        userId = user.uid,
                        specialty = profile.specialty,
                        bioText = profile.bio,
                        syncedToCloud = true
                    )
                }
                val adminCheck = repository.isUserAdmin(user.uid)
                _isAdmin.value = adminCheck

                // If admin, seed initial demo gallery if not yet populated
                if (adminCheck) {
                    repository.seedInitialDemoGalleryIfNeeded(user.uid)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isAdmin.value = false
                _isLoadingAdmin.value = false
            }
        }
    }

    fun toggleLanguage() {
        _language.value = if (_language.value == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
    }

    fun setLanguage(newLanguage: AppLanguage) {
        _language.value = newLanguage
    }

    fun toggleTheme() {
        _isStudioLightMode.value = !_isStudioLightMode.value
    }

    fun setStudioLightMode(isLight: Boolean) {
        _isStudioLightMode.value = isLight
    }

    fun navigateTo(screen: StudioScreen) {
        _currentScreen.value = screen
    }

    fun openPhotoPreview(
        target: PhotoPreviewDestination = PhotoPreviewDestination.PORTFOLIO_ARCHIVE,
        packageId: String? = null,
        moodBoardProjectId: Long? = null
    ) {
        _photoPreviewInitialTarget.value = target
        _photoPreviewInitialPackageId.value = packageId
        if (moodBoardProjectId != null || target == PhotoPreviewDestination.MOOD_BOARD) {
            _moodBoardTargetProjectId.value = moodBoardProjectId
        }
        _currentScreen.value = StudioScreen.PhotoPreview
    }

    fun setMoodBoardTargetProjectId(projectId: Long?) {
        _moodBoardTargetProjectId.value = projectId
    }

    fun navigateToBookingWithService(serviceType: String) {
        _preselectedServiceType.value = serviceType
        _currentScreen.value = StudioScreen.ShootBooking
    }

    fun consumePreselectedServiceType() {
        _preselectedServiceType.value = null
    }

    fun navigateToBookingWithPrefill(prefill: VoiceBookingPrefill) {
        _voiceBookingPrefill.value = prefill
        _currentScreen.value = StudioScreen.ShootBooking
    }

    fun consumeVoiceBookingPrefill() {
        _voiceBookingPrefill.value = null
    }

    suspend fun parseVoiceBookingCommand(transcript: String): ParsedVoiceBookingCommand {
        return VoiceBookingCommandParser.parseWithGeminiAndFallback(transcript)
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun updateUserProfile(
        displayName: String,
        avatarUrl: String = "",
        phoneNumber: String? = null,
        city: String? = null,
        instagramHandle: String? = null,
        specialty: String? = null,
        bio: String? = null,
        portfolioLinks: List<String>? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val trimmedName = displayName.trim()
        if (trimmedName.isEmpty()) {
            val msg = if (_language.value == AppLanguage.HINDI) "नाम खाली नहीं हो सकता" else "Display name cannot be blank."
            showSnackbar(msg)
            onResult(false, msg)
            return
        }
        val trimmedPhone = phoneNumber?.trim()
        if (!trimmedPhone.isNullOrEmpty() && (trimmedPhone.length < 8 || trimmedPhone.length > 30)) {
            val msg = if (_language.value == AppLanguage.HINDI)
                "कृपया मान्य संपर्क नंबर (कम से कम 8 अंक) दर्ज करें।"
            else
                "Please enter a valid contact number (at least 8 characters)."
            showSnackbar(msg)
            onResult(false, msg)
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.updateUserProfile(
                userId = currentUserId,
                displayName = trimmedName,
                avatarUrl = avatarUrl.trim(),
                phoneNumber = phoneNumber,
                city = city,
                instagramHandle = instagramHandle,
                specialty = specialty,
                bio = bio,
                portfolioLinks = portfolioLinks
            )
            _isSubmitting.value = false
            if (result.isSuccess) {
                val updated = result.getOrNull()
                if (updated != null) {
                    _userProfile.value = updated
                }
                if (bio != null || specialty != null) {
                    val resolvedBio = bio?.trim() ?: updated?.bio.orEmpty()
                    val resolvedSpecialty = specialty?.trim() ?: updated?.specialty.orEmpty()
                    if (resolvedBio.isNotBlank() || resolvedSpecialty.isNotBlank()) {
                        artistBioRepository?.saveBio(
                            userId = currentUserId,
                            specialty = resolvedSpecialty,
                            bioText = resolvedBio,
                            syncedToCloud = true
                        )
                    } else {
                        artistBioRepository?.deleteBio(currentUserId)
                    }
                }
                val successMsg = if (_language.value == AppLanguage.HINDI)
                    "प्रोफ़ाइल, संपर्क व पोर्टफोलियो लिंक सफलतापूर्वक सहेजे गए!"
                else
                    "Artist profile, contact & portfolio saved!"
                showSnackbar(successMsg)
                onResult(true, successMsg)
            } else {
                val errMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to update profile."
                showSnackbar(errMsg)
                onResult(false, errMsg)
            }
        }
    }

    fun saveLocalArtistBio(
        specialty: String,
        bioText: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val trimmedBio = bioText.trim()
        if (trimmedBio.isEmpty()) {
            val msg = if (_language.value == AppLanguage.HINDI)
                "सहेजने से पहले कृपया अपना प्रोफ़ेशनल बायो लिखें।"
            else
                "Please enter your professional bio before saving."
            showSnackbar(msg)
            onResult(false, msg)
            return
        }
        if (trimmedBio.length > ArtistBioRepository.MAX_BIO_LENGTH) {
            val msg = if (_language.value == AppLanguage.HINDI)
                "बायो 500 अक्षरों से अधिक नहीं हो सकता।"
            else
                "Artist bio must be 500 characters or less."
            showSnackbar(msg)
            onResult(false, msg)
            return
        }
        viewModelScope.launch {
            artistBioRepository?.saveBio(
                userId = currentUserId,
                specialty = specialty,
                bioText = trimmedBio,
                syncedToCloud = false
            )
            val successMsg = if (_language.value == AppLanguage.HINDI)
                "बायो आपके डिवाइस (Room) में सफलतापूर्वक सहेजा गया!"
            else
                "Professional bio saved locally on your device!"
            showSnackbar(successMsg)
            onResult(true, successMsg)
        }
    }

    fun updateProfilePhoto(
        avatarUrl: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val trimmed = avatarUrl.trim()
        if (trimmed.length > 500_000) {
            val msg = if (_language.value == AppLanguage.HINDI)
                "फ़ोटो का आकार बहुत बड़ा है।"
            else
                "Captured photo exceeds size limit. Please retake."
            showSnackbar(msg)
            onResult(false, msg)
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.updateProfilePhoto(
                userId = currentUserId,
                avatarUrl = trimmed
            )
            _isSubmitting.value = false
            if (result.isSuccess) {
                result.getOrNull()?.let { _userProfile.value = it }
                val successMsg = if (trimmed.isEmpty()) {
                    if (_language.value == AppLanguage.HINDI) "प्रोफ़ाइल फ़ोटो हटाया गया।" else "Profile photo cleared."
                } else {
                    if (_language.value == AppLanguage.HINDI) "प्रोफ़ाइल हेडशॉट सफलतापूर्वक अपडेट किया गया!" else "Professional headshot saved to your profile!"
                }
                showSnackbar(successMsg)
                onResult(true, successMsg)
            } else {
                val errMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to save profile photo."
                showSnackbar(errMsg)
                onResult(false, errMsg)
            }
        }
    }

    // --- FORM ACTIONS ---

    fun submitModelRegistration(
        fullName: String,
        ageStr: String,
        gender: String,
        city: String,
        phoneNumber: String,
        skills: String,
        portfolioUrl: String,
        consentGiven: Boolean,
        onSuccess: () -> Unit
    ) {
        val s = StudioStrings(_language.value)
        val nameTrimmed = fullName.trim()
        val ageVal = ageStr.toLongOrNull() ?: 0L
        val phoneTrimmed = phoneNumber.trim()
        val cityTrimmed = city.trim()

        if (nameTrimmed.length < 2) {
            showSnackbar(if (_language.value == AppLanguage.HINDI) "कृपया पूरा नाम दर्ज करें।" else "Please enter your full name.")
            return
        }
        if (ageVal < 18) {
            showSnackbar(if (_language.value == AppLanguage.HINDI) "पंजीकरण के लिए न्यूनतम आयु 18 वर्ष आवश्यक है।" else "Minimum age of 18 years is required to register.")
            return
        }
        if (cityTrimmed.isEmpty()) {
            showSnackbar(if (_language.value == AppLanguage.HINDI) "कृपया अपना शहर दर्ज करें।" else "Please enter your city.")
            return
        }
        if (phoneTrimmed.length < 10) {
            showSnackbar(if (_language.value == AppLanguage.HINDI) "कृपया मान्य 10 अंकों का फोन नंबर दर्ज करें।" else "Please enter a valid 10-digit phone number.")
            return
        }
        if (!consentGiven) {
            showSnackbar(if (_language.value == AppLanguage.HINDI) "पंजीकरण के लिए सहमति अनिवार्य है।" else "Consent is mandatory for model registration.")
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            val model = ModelRegistration(
                fullName = nameTrimmed,
                age = ageVal,
                gender = gender,
                city = cityTrimmed,
                phoneNumber = phoneTrimmed,
                skills = skills.trim(),
                portfolioUrls = portfolioUrl.trim(),
                consentGiven = true,
                status = "pending"
            )
            val result = repository.submitModelRegistration(model)
            _isSubmitting.value = false
            if (result.isSuccess) {
                showSnackbar(s.regSuccessMsg)
                onSuccess()
            } else {
                showSnackbar(result.exceptionOrNull()?.localizedMessage ?: "Failed to submit model registration.")
            }
        }
    }

    fun submitShootBooking(
        clientName: String,
        contactNumber: String,
        serviceType: String,
        preferredDate: String,
        preferredTime: String,
        location: String,
        notes: String,
        onSuccess: (bookingId: String) -> Unit
    ) {
        val s = StudioStrings(_language.value)
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            showSnackbar(
                if (_language.value == AppLanguage.HINDI)
                    "शूट बुक करने के लिए कृपया साइन इन करें।"
                else
                    "Please sign in before submitting a studio booking request."
            )
            return
        }
        _isEmailVerified.value = currentUser.isEmailVerified
        if (!currentUser.isEmailVerified) {
            showSnackbar(
                if (_language.value == AppLanguage.HINDI)
                    "शूट बुक करने से पहले कृपया अपना ईमेल सत्यापित करें।"
                else
                    "Please verify your email address before booking studio shoot sessions."
            )
            return
        }

        val cleanName = clientName.trim()
        val cleanPhone = contactNumber.trim()
        val cleanService = serviceType.trim()
        val cleanDate = preferredDate.trim()
        val cleanTime = preferredTime.trim()

        if (cleanService.isEmpty()) {
            showSnackbar(if (_language.value == AppLanguage.HINDI) "कृपया सेवा प्रकार चुनें।" else "Please select a service type.")
            return
        }
        if (cleanName.length < 2) {
            showSnackbar(if (_language.value == AppLanguage.HINDI) "कृपया अपना नाम दर्ज करें।" else "Please enter client name.")
            return
        }
        if (cleanPhone.length !in 8..20) {
            showSnackbar(if (_language.value == AppLanguage.HINDI) "कृपया मान्य संपर्क नंबर दर्ज करें।" else "Please enter a valid contact number (8–20 digits).")
            return
        }
        if (cleanDate.isEmpty()) {
            showSnackbar(if (_language.value == AppLanguage.HINDI) "कृपया पसंदीदा तारीख दर्ज करें।" else "Please specify preferred date.")
            return
        }
        if (cleanTime.isEmpty()) {
            showSnackbar(if (_language.value == AppLanguage.HINDI) "कृपया सत्र का समय चुनें।" else "Please select preferred session time.")
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            val fcmToken = _fcmToken.value.ifBlank {
                StudioNotificationManager.getOrFetchFcmToken() ?: ""
            }
            val booking = ShootBooking(
                clientName = cleanName,
                contactNumber = cleanPhone,
                serviceType = cleanService,
                preferredDate = cleanDate,
                preferredTime = cleanTime,
                location = location.trim().ifEmpty { "Studio (Talwara Khurd)" },
                notes = notes.trim(),
                status = "Pending",
                fcmToken = fcmToken
            )
            val result = repository.createBooking(booking)
            _isSubmitting.value = false
            if (result.isSuccess) {
                val createdBookingId = result.getOrNull().orEmpty()
                if (createdBookingId.isNotBlank()) {
                    repository.createAppointmentNotification(
                        targetUserId = currentUser.uid,
                        bookingId = createdBookingId,
                        title = if (_language.value == AppLanguage.HINDI)
                            "बुकिंग अनुरोध प्राप्त हुआ"
                        else
                            "Shoot Booking Request Received",
                        message = if (_language.value == AppLanguage.HINDI)
                            "$cleanService के लिए $cleanDate ($cleanTime) का आपका अनुरोध समीक्षा के लिए सबमिट किया गया है।"
                        else
                            "Your request for $cleanService on $cleanDate ($cleanTime) is Pending studio review.",
                        status = "Pending",
                        serviceType = cleanService,
                        targetFcmToken = fcmToken
                    )
                }
                showSnackbar(s.bookingSuccessMsg)
                onSuccess(createdBookingId)
            } else {
                showSnackbar(result.exceptionOrNull()?.localizedMessage ?: "Failed to create booking.")
            }
        }
    }

    fun cancelMyBooking(bookingId: String, onComplete: (Boolean) -> Unit = {}) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            showSnackbar(
                if (_language.value == AppLanguage.HINDI)
                    "बुकिंग रद्द करने के लिए कृपया साइन इन करें।"
                else
                    "Please sign in to cancel your booking."
            )
            onComplete(false)
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.cancelUserBooking(bookingId)
            _isSubmitting.value = false
            if (result.isSuccess) {
                showSnackbar(
                    if (_language.value == AppLanguage.HINDI)
                        "आपकी बुकिंग रद्द कर दी गई है।"
                    else
                        "Your appointment has been cancelled."
                )
                onComplete(true)
            } else {
                showSnackbar(
                    result.exceptionOrNull()?.localizedMessage ?: if (_language.value == AppLanguage.HINDI)
                        "बुकिंग रद्द करने में त्रुटि हुई।"
                    else
                        "Failed to cancel booking."
                )
                onComplete(false)
            }
        }
    }

    fun refreshEmailVerification(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val user = Firebase.auth.currentUser
                user?.reload()?.await()
                val verified = user?.isEmailVerified ?: false
                _isEmailVerified.value = verified
                onResult(verified)
            } catch (e: Exception) {
                onResult(_isEmailVerified.value)
            }
        }
    }

    fun resendVerificationEmail(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val user = Firebase.auth.currentUser
                if (user != null) {
                    user.sendEmailVerification().await()
                    onResult(
                        true,
                        if (_language.value == AppLanguage.HINDI)
                            "${user.email} पर सत्यापन ईमेल भेजा गया!"
                        else
                            "Verification email sent to ${user.email}!"
                    )
                } else {
                    onResult(false, "No active user session found.")
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Failed to resend verification email.")
            }
        }
    }

    fun submitGalleryUpload(
        title: String,
        category: String,
        mediaType: String,
        mediaUrl: String,
        thumbnailUrl: String,
        isDemo: Boolean,
        onSuccess: () -> Unit
    ) {
        val cleanTitle = title.trim()
        val cleanMediaUrl = mediaUrl.trim()
        if (cleanTitle.isEmpty() || cleanMediaUrl.isEmpty()) {
            showSnackbar("Title and Portfolio Photo/URL are required.")
            return
        }
        val effectiveThumb = thumbnailUrl.trim().let { thumb ->
            when {
                cleanMediaUrl.startsWith("data:image/", ignoreCase = true) -> {
                    if (thumb == cleanMediaUrl || thumb.length > 200_000) "" else thumb
                }
                else -> thumb.ifEmpty { cleanMediaUrl }
            }
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val item = GalleryItem(
                title = cleanTitle,
                category = category,
                mediaType = mediaType,
                mediaUrl = cleanMediaUrl,
                thumbnailUrl = effectiveThumb,
                isDemo = isDemo,
                status = if (_isAdmin.value) "approved" else "pending"
            )
            val result = repository.addGalleryItem(item, autoApprove = _isAdmin.value)
            _isSubmitting.value = false
            if (result.isSuccess) {
                showSnackbar(
                    if (_isAdmin.value) "Portfolio photo published to studio gallery!"
                    else "Portfolio photo submitted for admin approval before public listing."
                )
                onSuccess()
            } else {
                showSnackbar(result.exceptionOrNull()?.localizedMessage ?: "Upload failed.")
            }
        }
    }

    // --- ADMIN ACTIONS ---

    private fun requireAdminAccess(): Boolean {
        if (!_isAdmin.value) {
            showSnackbar(
                if (_language.value == AppLanguage.HINDI)
                    "अनधिकृत पहुंच अस्वीकृत। यह क्रिया केवल अधिकृत एडमिन के लिए है।"
                else
                    "Unauthorized access denied. Admin privileges required."
            )
            return false
        }
        return true
    }

    fun updateModelStatus(modelId: String, status: String, adminNotes: String = "") {
        if (!requireAdminAccess()) return
        viewModelScope.launch {
            val res = repository.updateModelStatus(modelId, status, adminNotes)
            if (res.isSuccess) {
                val msg = when (status.trim().lowercase()) {
                    "approved" -> "Model manually verified and added to Official Studio Roster!"
                    "rejected" -> "Model application rejected with admin review notes."
                    else -> "Model application moved back to pending verification queue."
                }
                showSnackbar(msg)
            } else {
                showSnackbar("Failed to update status")
            }
        }
    }

    fun updateBookingStatus(bookingId: String, status: String) {
        updateBookingStatus(bookingId = bookingId, status = status, customNote = "")
    }

    fun updateBookingStatus(bookingId: String, status: String, customNote: String) {
        if (!requireAdminAccess()) return
        viewModelScope.launch {
            val targetBooking = allBookings.value.firstOrNull { it.id == bookingId }
            val res = repository.updateBookingStatus(bookingId, status)
            if (res.isSuccess) {
                if (targetBooking != null && targetBooking.userId.isNotBlank()) {
                    val (notifTitle, baseBody) = StudioNotificationManager.formatNotificationPayload(
                        clientName = targetBooking.clientName.ifBlank { "Client" },
                        serviceType = targetBooking.serviceType,
                        preferredDate = targetBooking.preferredDate,
                        newStatus = status
                    )
                    val trimmedNote = customNote.trim()
                    val fullMessage = if (trimmedNote.isNotEmpty()) {
                        "$baseBody\nNote: $trimmedNote"
                    } else {
                        baseBody
                    }
                    repository.createAppointmentNotification(
                        targetUserId = targetBooking.userId,
                        bookingId = bookingId,
                        title = notifTitle,
                        message = fullMessage,
                        status = status,
                        serviceType = targetBooking.serviceType,
                        targetFcmToken = targetBooking.fcmToken
                    )
                }
                val msg = if (_language.value == AppLanguage.HINDI) {
                    "बुकिंग स्थिति '$status' में बदल दी गई और ग्राहक को सूचित किया गया"
                } else {
                    "Booking status changed to $status & client notified"
                }
                showSnackbar(msg)
            } else {
                val msg = if (_language.value == AppLanguage.HINDI) {
                    "बुकिंग स्थिति अपडेट करने में विफल"
                } else {
                    "Failed to update booking status"
                }
                showSnackbar(msg)
            }
        }
    }

    fun deleteBooking(bookingId: String) {
        if (!requireAdminAccess()) return
        viewModelScope.launch {
            val res = repository.deleteBooking(bookingId)
            if (res.isSuccess) {
                val msg = if (_language.value == AppLanguage.HINDI) {
                    "बुकिंग अनुरोध हटा दिया गया।"
                } else {
                    "Booking request deleted."
                }
                showSnackbar(msg)
            } else {
                val msg = if (_language.value == AppLanguage.HINDI) {
                    "बुकिंग अनुरोध हटाने में विफल"
                } else {
                    "Failed to delete booking request"
                }
                showSnackbar(msg)
            }
        }
    }

    fun updateGalleryStatus(itemId: String, status: String) {
        if (!requireAdminAccess()) return
        viewModelScope.launch {
            val res = repository.updateGalleryStatus(itemId, status)
            if (res.isSuccess) {
                showSnackbar("Gallery status updated to $status")
            } else {
                showSnackbar("Failed to update gallery status")
            }
        }
    }

    fun deleteGalleryItem(itemId: String) {
        if (!requireAdminAccess()) return
        viewModelScope.launch {
            val res = repository.deleteGalleryItem(itemId)
            if (res.isSuccess) {
                showSnackbar("Gallery item deleted.")
            } else {
                showSnackbar("Failed to delete item")
            }
        }
    }

    // --- DATA DELETION REQUEST ---

    fun requestDataDeletion(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.deleteUserData(currentUserId)
            _isSubmitting.value = false
            if (res.isSuccess) {
                artistBioRepository?.deleteBio(currentUserId)
                showSnackbar("All your data and submissions have been permanently deleted.")
                onComplete()
            } else {
                showSnackbar("Data deletion failed: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    // --- OFFLINE STUDIO INFO CACHING ---

    fun refreshStudioInfoCache() {
        viewModelScope.launch {
            studioInfoRepository?.refreshCache()
            showSnackbar("Studio info cache updated locally.")
        }
    }

    // --- BLOG & PHOTOGRAPHY TIPS POSTS ---

    fun publishBlogPost(
        title: String,
        category: String,
        content: String,
        summary: String,
        imageUrl: String,
        tags: String,
        status: String = "published",
        onSuccess: () -> Unit = {}
    ) {
        if (!requireAdminAccess()) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.createBlogPost(
                title = title,
                category = category,
                content = content,
                summary = summary,
                imageUrl = imageUrl,
                tags = tags,
                status = status
            )
            _isSubmitting.value = false
            if (result.isSuccess) {
                showSnackbar(
                    if (_language.value == AppLanguage.HINDI)
                        "ब्लॉग पोस्ट सफलतापूर्वक प्रकाशित हुआ!"
                    else
                        "Blog post published successfully!"
                )
                onSuccess()
            } else {
                showSnackbar(result.exceptionOrNull()?.message ?: "Failed to publish blog post")
            }
        }
    }

    fun updateBlogPost(
        postId: String,
        title: String,
        category: String,
        content: String,
        summary: String,
        imageUrl: String,
        tags: String,
        status: String,
        onSuccess: () -> Unit = {}
    ) {
        if (!requireAdminAccess()) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.updateBlogPost(
                postId = postId,
                title = title,
                category = category,
                content = content,
                summary = summary,
                imageUrl = imageUrl,
                tags = tags,
                status = status
            )
            _isSubmitting.value = false
            if (result.isSuccess) {
                showSnackbar(
                    if (_language.value == AppLanguage.HINDI)
                        "ब्लॉग पोस्ट अपडेट हो गया!"
                    else
                        "Blog post updated successfully!"
                )
                onSuccess()
            } else {
                showSnackbar(result.exceptionOrNull()?.message ?: "Failed to update blog post")
            }
        }
    }

    fun deleteBlogPost(postId: String, onSuccess: () -> Unit = {}) {
        if (!requireAdminAccess()) return
        viewModelScope.launch {
            val result = repository.deleteBlogPost(postId)
            if (result.isSuccess) {
                showSnackbar(
                    if (_language.value == AppLanguage.HINDI)
                        "ब्लॉग पोस्ट हटा दिया गया।"
                    else
                        "Blog post deleted successfully."
                )
                onSuccess()
            } else {
                showSnackbar(result.exceptionOrNull()?.message ?: "Failed to delete blog post")
            }
        }
    }

    // --- SERVICE RATINGS & REVIEWS ---

    fun submitServiceReview(
        bookingId: String,
        serviceType: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit = {}
    ) {
        val cleanComment = comment.trim()
        if (bookingId.trim().isEmpty()) {
            showSnackbar(
                if (_language.value == AppLanguage.HINDI) "बुकिंग आईडी अमान्य है।"
                else "Invalid booking reference."
            )
            return
        }
        if (rating !in 1..5) {
            showSnackbar(
                if (_language.value == AppLanguage.HINDI) "कृपया 1 से 5 स्टार रेटिंग चुनें।"
                else "Please select a star rating from 1 to 5."
            )
            return
        }
        if (cleanComment.length < 5) {
            showSnackbar(
                if (_language.value == AppLanguage.HINDI) "कृपया कम से कम 5 अक्षरों की समीक्षा लिखें।"
                else "Please write at least 5 characters for your review."
            )
            return
        }
        if (cleanComment.length > 1000) {
            showSnackbar(
                if (_language.value == AppLanguage.HINDI) "समीक्षा 1000 अक्षरों से कम होनी चाहिए।"
                else "Review must be 1000 characters or fewer."
            )
            return
        }

        val currentProfile = _userProfile.value
        val currentUser = Firebase.auth.currentUser
        val reviewerName = currentProfile?.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: "Studio Artist"
        val reviewerAvatarUrl = currentProfile?.effectiveAvatarUrl?.takeIf { it.isNotBlank() }
            ?: currentUser?.photoUrl?.toString().orEmpty()

        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.submitServiceReview(
                bookingId = bookingId,
                serviceType = serviceType,
                rating = rating,
                comment = cleanComment,
                reviewerName = reviewerName,
                reviewerAvatarUrl = reviewerAvatarUrl
            )
            _isSubmitting.value = false
            if (result.isSuccess) {
                showSnackbar(
                    if (_language.value == AppLanguage.HINDI)
                        "आपकी रेटिंग व समीक्षा सफलतापूर्वक सहेजी गई!"
                    else
                        "Thank you! Your star rating and review have been saved."
                )
                onSuccess()
            } else {
                showSnackbar(
                    result.exceptionOrNull()?.localizedMessage
                        ?: if (_language.value == AppLanguage.HINDI) "समीक्षा सहेजने में त्रुटि हुई।"
                        else "Failed to submit review."
                )
            }
        }
    }

    // --- FIREBASE CLOUD MESSAGING: ANNOUNCEMENTS, PREFERENCES & READ TRACKING ---

    fun publishStudioAnnouncement(
        title: String,
        message: String,
        category: String = "Studio Update",
        priority: String = "normal",
        actionRoute: String = "home",
        onSuccess: () -> Unit = {}
    ) {
        if (!requireAdminAccess()) return
        val authorName = _userProfile.value?.displayName?.takeIf { it.isNotBlank() } ?: "Star Artist Studio"
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.publishStudioAnnouncement(
                title = title,
                message = message,
                category = category,
                priority = priority,
                actionRoute = actionRoute,
                authorName = authorName
            )
            _isSubmitting.value = false
            if (result.isSuccess) {
                showSnackbar(
                    if (_language.value == AppLanguage.HINDI)
                        "स्टूडियो घोषणा सभी सब्सक्राइबर्स को प्रसारित हो गई!"
                    else
                        "Studio announcement published to subscribed users!"
                )
                onSuccess()
            } else {
                showSnackbar(result.exceptionOrNull()?.localizedMessage ?: "Failed to publish announcement.")
            }
        }
    }

    fun deleteStudioAnnouncement(announcementId: String) {
        if (!requireAdminAccess()) return
        viewModelScope.launch {
            val result = repository.deleteStudioAnnouncement(announcementId)
            if (result.isSuccess) {
                showSnackbar(
                    if (_language.value == AppLanguage.HINDI) "घोषणा हटाई गई।" else "Announcement removed."
                )
            } else {
                showSnackbar(result.exceptionOrNull()?.localizedMessage ?: "Failed to remove announcement.")
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        if (notificationId.isBlank() || currentUserId.isBlank()) return
        viewModelScope.launch {
            repository.markNotificationRead(currentUserId, notificationId)
        }
    }

    fun markAllNotificationsRead() {
        if (currentUserId.isBlank()) return
        val unreadIds = myAppointmentNotifications.value.filter { !it.isRead }.map { it.id }
        if (unreadIds.isEmpty()) return
        viewModelScope.launch {
            val res = repository.markAllNotificationsRead(currentUserId, unreadIds)
            if (res.isSuccess) {
                showSnackbar(
                    if (_language.value == AppLanguage.HINDI) "सभी सूचनाएं पढ़ी हुई चिह्नित की गईं"
                    else "All appointment alerts marked as read"
                )
            }
        }
    }

    fun refreshFcmToken(context: android.content.Context) {
        viewModelScope.launch {
            val token = StudioNotificationManager.syncFcmTopicsAndToken(
                context = context,
                userId = currentUserId,
                announcementsOptIn = announcementsOptIn.value,
                appointmentPushOptIn = appointmentPushOptIn.value
            )
            if (!token.isNullOrBlank()) {
                _fcmToken.value = token
                showSnackbar(
                    if (_language.value == AppLanguage.HINDI) "FCM पुश टोकन और टॉपिक सिंक हो गए!"
                    else "FCM device token & topic subscriptions synced!"
                )
            } else {
                showSnackbar(
                    if (_language.value == AppLanguage.HINDI) "पुश टॉपिक सिंक किए गए।"
                    else "Push notification topics synced."
                )
            }
        }
    }

    fun updatePushPreferences(
        context: android.content.Context,
        announcementsEnabled: Boolean,
        appointmentPushEnabled: Boolean
    ) {
        viewModelScope.launch {
            val token = StudioNotificationManager.syncFcmTopicsAndToken(
                context = context,
                userId = currentUserId,
                announcementsOptIn = announcementsEnabled,
                appointmentPushOptIn = appointmentPushEnabled
            )
            if (!token.isNullOrBlank()) {
                _fcmToken.value = token
            }
            val result = repository.updateNotificationPreferences(
                userId = currentUserId,
                announcementsOptIn = announcementsEnabled,
                appointmentPushOptIn = appointmentPushEnabled,
                fcmToken = token
            )
            if (result.isSuccess) {
                _userProfile.value = _userProfile.value?.copy(
                    announcementsOptIn = announcementsEnabled,
                    appointmentPushOptIn = appointmentPushEnabled,
                    fcmToken = token ?: _userProfile.value?.fcmToken.orEmpty()
                )
                showSnackbar(
                    if (_language.value == AppLanguage.HINDI) "पुश नोटिफिकेशन प्राथमिकताएं सहेजी गईं"
                    else "FCM notification preferences updated"
                )
            }
        }
    }
}

