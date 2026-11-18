package com.example.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AppLanguage
import com.example.data.model.StudioServicesData
import com.example.ui.components.EncodedPortfolioPhoto
import com.example.ui.components.PhotoPreviewReviewPanel
import com.example.ui.components.PhotoPreviewViewfinderPane
import com.example.ui.components.createPortfolioCaptureDestination
import com.example.ui.components.encodePortfolioPhotoUriToDataUrl
import com.example.ui.components.hasCameraHardware
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PhotoPreviewDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Full-screen adaptive CameraX Photo Preview & Reference Shot studio screen.
 * Allows artists to frame shots with rule-of-thirds & aspect masks, capture quick reference photos,
 * review captured takes in a session filmstrip, and save directly into the Portfolio Archive (Room),
 * Service Catalog package references, or Studio Gallery (Firestore).
 */
@Composable
fun PhotoPreviewScreen(
    language: AppLanguage = AppLanguage.ENGLISH,
    initialDestination: PhotoPreviewDestination = PhotoPreviewDestination.PORTFOLIO_ARCHIVE,
    initialPackageId: String? = null,
    isSubmitting: Boolean = false,
    initialPermissionGrantedOverride: Boolean? = null,
    initialPreviewShots: List<CapturedReferenceShot> = emptyList(),
    onBack: () -> Unit = {},
    onSaveToPortfolioArchive: (
        title: String,
        category: String,
        dataUrl: String,
        cameraGear: String,
        resolutionTag: String,
        description: String,
        onSaved: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onSaveToServiceCatalog: (
        packageId: String,
        title: String,
        notes: String,
        imageDataUrl: String,
        aspectGuide: String,
        resolutionTag: String,
        onSaved: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onPublishToStudioGallery: (
        title: String,
        category: String,
        dataUrl: String,
        onSaved: () -> Unit
    ) -> Unit = { _, _, _, _ -> },
    onSaveToMoodBoard: (
        title: String,
        sectionTag: String,
        notes: String,
        dataUrl: String,
        onSaved: () -> Unit
    ) -> Unit = { _, _, _, _, _ -> },
    onOpenDestinationScreen: (PhotoPreviewDestination) -> Unit = {},
    onShowMessage: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isHindi = language == AppLanguage.HINDI

    var hasCameraPermission by remember {
        mutableStateOf(
            initialPermissionGrantedOverride ?: (
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                )
        )
    }
    val deviceHasCamera = remember { hasCameraHardware(context) }

    var selectedAspectGuide by remember { mutableStateOf(FramingAspectGuide.PORTRAIT_4_5) }
    var showRuleOfThirds by remember { mutableStateOf(true) }
    var isProcessingShot by remember { mutableStateOf(false) }

    val sessionShots = remember {
        mutableStateListOf<CapturedReferenceShot>().apply {
            addAll(initialPreviewShots)
        }
    }
    var selectedShot by remember { mutableStateOf(initialPreviewShots.firstOrNull()) }

    var selectedDestination by remember(initialDestination) { mutableStateOf(initialDestination) }
    var selectedCategory by remember { mutableStateOf("Fashion") }
    var selectedPackageId by remember(initialPackageId) {
        mutableStateOf(initialPackageId ?: StudioServicesData.packages.firstOrNull()?.id.orEmpty())
    }
    var title by remember {
        mutableStateOf(
            if (initialPreviewShots.isNotEmpty()) {
                PhotoPreviewDefaults.defaultShotTitle(
                    destination = initialDestination,
                    category = "Fashion",
                    selectedPackageId = initialPackageId ?: StudioServicesData.packages.firstOrNull()?.id.orEmpty(),
                    shotNumber = 1,
                    language = language
                )
            } else {
                ""
            }
        )
    }
    var notes by remember { mutableStateOf("") }
    var lastSavedDestination by remember { mutableStateOf<PhotoPreviewDestination?>(null) }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    fun registerCapturedPhoto(
        encoded: EncodedPortfolioPhoto,
        lensLabel: String = "Rear Cam",
        flashLabel: String = "Flash Auto",
        zoomLabel: String = "1.0x"
    ) {
        val nextId = System.currentTimeMillis() + sessionShots.size
        val shot = CapturedReferenceShot(
            id = nextId,
            encodedPhoto = encoded,
            aspectGuide = selectedAspectGuide,
            lensLabel = lensLabel,
            flashModeLabel = flashLabel,
            zoomLabel = zoomLabel
        )
        sessionShots.add(0, shot)
        selectedShot = shot
        lastSavedDestination = null
        if (title.isBlank()) {
            title = PhotoPreviewDefaults.defaultShotTitle(
                destination = selectedDestination,
                category = selectedCategory,
                selectedPackageId = selectedPackageId,
                shotNumber = sessionShots.size,
                language = language
            )
        }
    }

    fun processImageUri(uri: Uri, tempFile: File?, sourceLabel: String) {
        isProcessingShot = true
        coroutineScope.launch {
            val encoded = withContext(Dispatchers.IO) {
                encodePortfolioPhotoUriToDataUrl(context, uri)
            }
            tempFile?.delete()
            isProcessingShot = false
            if (encoded != null) {
                registerCapturedPhoto(encoded, lensLabel = sourceLabel)
            } else {
                onShowMessage(
                    if (isHindi) "इमेज प्रोसेस करने में समस्या आई। कृपया पुनः प्रयास करें।"
                    else "Unable to process selected photo. Please try another shot."
                )
            }
        }
    }

    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        val uri = pendingCameraUri
        pendingCameraFile = null
        pendingCameraUri = null
        if (success && uri != null) {
            processImageUri(uri, file, sourceLabel = "System Cam")
        } else {
            file?.delete()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            processImageUri(uri, tempFile = null, sourceLabel = "Photo Library")
        }
    }

    fun launchSystemCamera() {
        val (file, uri) = createPortfolioCaptureDestination(context)
        pendingCameraFile = file
        pendingCameraUri = uri
        try {
            systemCameraLauncher.launch(uri)
        } catch (_: ActivityNotFoundException) {
            file.delete()
            onShowMessage(
                if (isHindi) "इस डिवाइस पर सिस्टम कैमरा ऐप नहीं मिला।"
                else "No system camera app available on this device."
            )
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            onShowMessage(
                if (isHindi) "लाइव प्रीव्यू के लिए कैमरा अनुमति आवश्यक है या फोटो लाइब्रेरी उपयोग करें।"
                else "Camera permission denied. You can still use Photo Library or grant permission anytime."
            )
        }
    }

    fun handleSave() {
        val active = selectedShot ?: return
        val effectiveTitle = title.trim().ifEmpty {
            PhotoPreviewDefaults.defaultShotTitle(
                destination = selectedDestination,
                category = selectedCategory,
                selectedPackageId = selectedPackageId,
                shotNumber = sessionShots.size.coerceAtLeast(1),
                language = language
            )
        }
        val effectiveGuide = selectedAspectGuide
        val cleanNotes = notes.trim().ifEmpty {
            "${active.lensLabel} • ${active.flashModeLabel} • ${effectiveGuide.labelEn} (${active.zoomLabel})"
        }
        when (selectedDestination) {
            PhotoPreviewDestination.PORTFOLIO_ARCHIVE -> {
                onSaveToPortfolioArchive(
                    effectiveTitle,
                    selectedCategory,
                    active.encodedPhoto.dataUrl,
                    "${active.lensLabel} • ${active.zoomLabel}",
                    "${active.encodedPhoto.width}×${active.encodedPhoto.height} • ${effectiveGuide.labelEn}",
                    cleanNotes
                ) {
                    lastSavedDestination = PhotoPreviewDestination.PORTFOLIO_ARCHIVE
                }
            }
            PhotoPreviewDestination.SERVICE_CATALOG -> {
                onSaveToServiceCatalog(
                    selectedPackageId,
                    effectiveTitle,
                    cleanNotes,
                    active.encodedPhoto.dataUrl,
                    effectiveGuide.labelEn,
                    "${active.encodedPhoto.width}×${active.encodedPhoto.height}"
                ) {
                    lastSavedDestination = PhotoPreviewDestination.SERVICE_CATALOG
                }
            }
            PhotoPreviewDestination.STUDIO_GALLERY -> {
                onPublishToStudioGallery(
                    effectiveTitle,
                    selectedCategory,
                    active.encodedPhoto.dataUrl
                ) {
                    lastSavedDestination = PhotoPreviewDestination.STUDIO_GALLERY
                }
            }
            PhotoPreviewDestination.MOOD_BOARD -> {
                onSaveToMoodBoard(
                    effectiveTitle,
                    "Lighting Ref",
                    cleanNotes,
                    active.encodedPhoto.dataUrl
                ) {
                    lastSavedDestination = PhotoPreviewDestination.MOOD_BOARD
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("photo_preview_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Director Bar
            Surface(
                color = StudioElevatedSurface,
                border = BorderStroke(0.5.dp, StudioOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("photo_preview_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isHindi) "वापस जाएं" else "Back",
                                tint = GoldPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = if (isHindi) "फोटो प्रीव्यू स्टूडियो" else "Photo Preview Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isHindi)
                                    "पोर्टफोलियो व कैटलॉग के लिए लाइव संदर्भ कैमरा"
                                else
                                    "CameraX Reference Cam • Portfolio & Catalog",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { showRuleOfThirds = !showRuleOfThirds },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(if (showRuleOfThirds) GoldContainer else StudioElevatedSurface)
                                .testTag("photo_preview_grid_toggle")
                        ) {
                            Icon(
                                imageVector = if (showRuleOfThirds) Icons.Default.GridOn else Icons.Default.GridOff,
                                contentDescription = if (isHindi) "रूल ऑफ थर्ड्स ग्रिड" else "Toggle Rule of Thirds Grid",
                                tint = if (showRuleOfThirds) GoldPrimary else TextSecondary
                            )
                        }
                    }
                }
            }

            // Aspect Framing Guide Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                FramingAspectGuide.entries.forEach { guide ->
                    val isSelected = selectedAspectGuide == guide
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedAspectGuide = guide },
                        label = {
                            Text(
                                text = if (isHindi) guide.labelHi else guide.labelEn,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldContainer,
                            selectedLabelColor = OnGoldContainer,
                            containerColor = StudioElevatedSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) GoldPrimary else StudioOutline
                        ),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .semantics(mergeDescendants = true) {}
                            .testTag("photo_preview_aspect_${guide.id}")
                    )
                }
            }

            // Responsive Adaptive Body (Stacked on Compact phones, Two-Pane on Expanded tablets/foldables)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                val isWideScreen = maxWidth >= 720.dp
                if (isWideScreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.05f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            PhotoPreviewViewfinderPane(
                                language = language,
                                hasCameraPermission = hasCameraPermission,
                                deviceHasCamera = deviceHasCamera,
                                aspectGuide = selectedAspectGuide,
                                showRuleOfThirds = showRuleOfThirds,
                                isProcessingShot = isProcessingShot,
                                capturedCount = sessionShots.size,
                                onRequestPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                onLaunchSystemCamera = { launchSystemCamera() },
                                onLaunchPhotoPicker = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onCapturedFileUri = { encoded, lens, flash, zoom ->
                                    registerCapturedPhoto(encoded, lens, flash, zoom)
                                },
                                onOpenLatestReview = {
                                    selectedShot = sessionShots.firstOrNull()
                                },
                                onError = { onShowMessage(it) }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.95f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            PhotoPreviewReviewPanel(
                                language = language,
                                sessionShots = sessionShots,
                                selectedShot = selectedShot,
                                selectedDestination = selectedDestination,
                                selectedCategory = selectedCategory,
                                selectedPackageId = selectedPackageId,
                                title = title,
                                notes = notes,
                                isSubmitting = isSubmitting,
                                lastSavedDestination = lastSavedDestination,
                                onSelectShot = { selectedShot = it },
                                onRetakePhoto = { selectedShot = null },
                                onSelectDestination = { selectedDestination = it },
                                onSelectCategory = { selectedCategory = it },
                                onSelectPackageId = { selectedPackageId = it },
                                onTitleChange = { title = it },
                                onNotesChange = { notes = it },
                                onSaveReferenceShot = { handleSave() },
                                onOpenSavedTarget = onOpenDestinationScreen
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 640.dp)
                            .align(Alignment.TopCenter)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PhotoPreviewViewfinderPane(
                            language = language,
                            hasCameraPermission = hasCameraPermission,
                            deviceHasCamera = deviceHasCamera,
                            aspectGuide = selectedAspectGuide,
                            showRuleOfThirds = showRuleOfThirds,
                            isProcessingShot = isProcessingShot,
                            capturedCount = sessionShots.size,
                            onRequestPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            onLaunchSystemCamera = { launchSystemCamera() },
                            onLaunchPhotoPicker = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onCapturedFileUri = { encoded, lens, flash, zoom ->
                                registerCapturedPhoto(encoded, lens, flash, zoom)
                            },
                            onOpenLatestReview = {
                                selectedShot = sessionShots.firstOrNull()
                            },
                            onError = { onShowMessage(it) }
                        )

                        PhotoPreviewReviewPanel(
                            language = language,
                            sessionShots = sessionShots,
                            selectedShot = selectedShot,
                            selectedDestination = selectedDestination,
                            selectedCategory = selectedCategory,
                            selectedPackageId = selectedPackageId,
                            title = title,
                            notes = notes,
                            isSubmitting = isSubmitting,
                            lastSavedDestination = lastSavedDestination,
                            onSelectShot = { selectedShot = it },
                            onRetakePhoto = { selectedShot = null },
                            onSelectDestination = { selectedDestination = it },
                            onSelectCategory = { selectedCategory = it },
                            onSelectPackageId = { selectedPackageId = it },
                            onTitleChange = { title = it },
                            onNotesChange = { notes = it },
                            onSaveReferenceShot = { handleSave() },
                            onOpenSavedTarget = onOpenDestinationScreen
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
