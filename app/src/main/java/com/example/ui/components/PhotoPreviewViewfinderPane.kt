package com.example.ui.components

import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.AppLanguage
import com.example.ui.screens.FramingAspectGuide
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Interactive CameraX viewfinder pane for taking quick reference shots with composition grid,
 * aspect framing masks, flash cycle, front/rear lens flip, zoom selector, and tap-to-focus.
 */
@Composable
fun PhotoPreviewViewfinderPane(
    language: AppLanguage,
    hasCameraPermission: Boolean,
    deviceHasCamera: Boolean,
    aspectGuide: FramingAspectGuide,
    showRuleOfThirds: Boolean,
    isProcessingShot: Boolean,
    capturedCount: Int,
    onRequestPermission: () -> Unit,
    onLaunchSystemCamera: () -> Unit,
    onLaunchPhotoPicker: () -> Unit,
    onCapturedFileUri: (
        encodedPhoto: EncodedPortfolioPhoto,
        lensLabel: String,
        flashLabel: String,
        zoomLabel: String
    ) -> Unit,
    onOpenLatestReview: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHindi = language == AppLanguage.HINDI
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    if (!hasCameraPermission) {
        ElevatedCard(
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                .testTag("photo_preview_permission_card"),
            colors = CardDefaults.elevatedCardColors(containerColor = StudioCardSurface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(GoldContainer)
                        .border(1.5.dp, GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = if (isHindi)
                        "संदर्भ शॉट्स के लिए लाइव कैमरा प्रीव्यू सक्षम करें"
                    else
                        "Enable Live Camera Reference Viewfinder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isHindi)
                        "पोर्टफोलियो प्रोजेक्ट्स और सेवा कैटलॉग पैकेज के लिए लाइटिंग टेस्ट, पोज़ और प्रॉप्स संदर्भ शॉट तुरंत कैप्चर करें।"
                    else
                        "Capture quick lighting tests, framing guides, pose studies, and catalog package reference shots directly using your device camera.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .semantics(mergeDescendants = true) {}
                        .testTag("photo_preview_grant_permission_btn")
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "कैमरा अनुमति दें" else "Enable Live Camera",
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onLaunchSystemCamera,
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("photo_preview_system_camera_btn")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "सिस्टम कैमरा" else "System Cam",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    OutlinedButton(
                        onClick = onLaunchPhotoPicker,
                        border = BorderStroke(1.dp, StudioOutline),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("photo_preview_gallery_fallback_btn")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "फोटो चुनें" else "Photo Library",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
        return
    }

    if (!deviceHasCamera) {
        ElevatedCard(
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, StudioOutline, RoundedCornerShape(18.dp))
                .testTag("photo_preview_no_camera_card"),
            colors = CardDefaults.elevatedCardColors(containerColor = StudioCardSurface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.VideocamOff, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(36.dp))
                Text(
                    text = if (isHindi)
                        "इस डिवाइस पर कैमरा सेंसर उपलब्ध नहीं है"
                    else
                        "Camera sensor unavailable on this device",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isHindi)
                        "आप अभी भी सिस्टम कैमरा या फोटो लाइब्रेरी से संदर्भ इमेज जोड़ सकते हैं।"
                    else
                        "Use System Camera or Android Photo Library below to stage reference shots for your portfolio or catalog.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onLaunchSystemCamera,
                        border = BorderStroke(1.dp, GoldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("photo_preview_system_camera_btn")
                    ) {
                        Text(if (isHindi) "सिस्टम कैमरा" else "System Camera", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onLaunchPhotoPicker,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("photo_preview_gallery_fallback_btn")
                    ) {
                        Text(if (isHindi) "फोटो लाइब्रेरी" else "Pick Photo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
        return
    }

    // Live CameraX state
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(3f) }
    var hasMultipleCameras by remember { mutableStateOf(false) }
    var boundImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var focusTapPoint by remember { mutableStateOf<Offset?>(null) }
    var isTakingPhoto by remember { mutableStateOf(false) }
    var localBindError by remember { mutableStateOf<String?>(null) }

    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(focusTapPoint) {
        if (focusTapPoint != null) {
            delay(1100)
            focusTapPoint = null
        }
    }

    LaunchedEffect(flashMode) {
        boundImageCapture?.flashMode = flashMode
    }

    DisposableEffect(lifecycleOwner, lensFacing) {
        var activeProvider: ProcessCameraProvider? = null
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                activeProvider = provider
                val hasBack = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                val hasFront = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                hasMultipleCameras = hasBack && hasFront

                val targetFacing = when {
                    lensFacing == CameraSelector.LENS_FACING_FRONT && hasFront -> CameraSelector.LENS_FACING_FRONT
                    hasBack -> CameraSelector.LENS_FACING_BACK
                    hasFront -> CameraSelector.LENS_FACING_FRONT
                    else -> {
                        localBindError = if (isHindi) "कोई उपलब्ध कैमरा लेंस नहीं मिला।" else "No accessible camera lens found."
                        return@addListener
                    }
                }
                val selector = CameraSelector.Builder().requireLensFacing(targetFacing).build()
                val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
                val preview = Preview.Builder()
                    .setTargetRotation(rotation)
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setFlashMode(flashMode)
                    .setTargetRotation(rotation)
                    .build()

                provider.unbindAll()
                val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                boundCamera = camera
                boundImageCapture = capture
                maxZoomRatio = (camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 3f).coerceAtLeast(1f)
                zoomRatio = 1f
                localBindError = null
            } catch (e: Exception) {
                localBindError = e.localizedMessage ?: "Camera preview initialization failed."
            }
        }, mainExecutor)

        onDispose {
            runCatching { activeProvider?.unbindAll() }
            boundCamera = null
            boundImageCapture = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { cameraExecutor.shutdown() }
        }
    }

    // Capture Material theme / overlay colors outside Canvas DrawScope
    val reticleGridColor = GoldPrimary.copy(alpha = 0.45f)
    val guideBracketColor = GoldLight
    val outsideGuideScrim = Color.Black.copy(alpha = 0.56f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .border(1.5.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                .pointerInput(boundCamera, maxZoomRatio) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        if (kotlin.math.abs(zoomChange - 1f) > 0.01f) {
                            val nextZoom = (zoomRatio * zoomChange).coerceIn(1f, maxZoomRatio)
                            zoomRatio = nextZoom
                            runCatching { boundCamera?.cameraControl?.setZoomRatio(nextZoom) }
                        }
                    }
                }
                .pointerInput(boundCamera) {
                    detectTapGestures { offset ->
                        focusTapPoint = offset
                        runCatching {
                            val meteringPoint = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                            val action = FocusMeteringAction.Builder(meteringPoint)
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                            boundCamera?.cameraControl?.startFocusAndMetering(action)
                        }
                    }
                }
                .testTag("photo_preview_viewfinder_box")
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Composition Grid & Aspect Framing Guide Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val targetRatio = aspectGuide.targetRatio
                val frameRect: Pair<Offset, Size> = if (targetRatio == null) {
                    Offset.Zero to size
                } else {
                    val containerRatio = w / h.coerceAtLeast(1f)
                    if (containerRatio > targetRatio) {
                        val fw = h * targetRatio
                        val left = (w - fw) / 2f
                        Offset(left, 0f) to Size(fw, h)
                    } else {
                        val fh = w / targetRatio
                        val top = (h - fh) / 2f
                        Offset(0f, top) to Size(w, fh)
                    }
                }

                val (origin, frameSize) = frameRect

                // Dim outer region when using 1:1, 4:5, or 16:9 crop framing guides
                if (targetRatio != null) {
                    if (origin.y > 0f) {
                        drawRect(outsideGuideScrim, Offset.Zero, Size(w, origin.y))
                        drawRect(outsideGuideScrim, Offset(0f, origin.y + frameSize.height), Size(w, h - (origin.y + frameSize.height)))
                    }
                    if (origin.x > 0f) {
                        drawRect(outsideGuideScrim, Offset(0f, origin.y), Size(origin.x, frameSize.height))
                        drawRect(outsideGuideScrim, Offset(origin.x + frameSize.width, origin.y), Size(w - (origin.x + frameSize.width), frameSize.height))
                    }
                    drawRect(
                        color = guideBracketColor.copy(alpha = 0.85f),
                        topLeft = origin,
                        size = frameSize,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Rule of thirds lines inside active framing rectangle
                if (showRuleOfThirds) {
                    val strokePx = 1.dp.toPx()
                    for (i in 1..2) {
                        val x = origin.x + (frameSize.width * i / 3f)
                        drawLine(
                            color = reticleGridColor,
                            start = Offset(x, origin.y),
                            end = Offset(x, origin.y + frameSize.height),
                            strokeWidth = strokePx
                        )
                        val y = origin.y + (frameSize.height * i / 3f)
                        drawLine(
                            color = reticleGridColor,
                            start = Offset(origin.x, y),
                            end = Offset(origin.x + frameSize.width, y),
                            strokeWidth = strokePx
                        )
                    }
                }
            }

            // Animated Tap-to-Focus Gold Reticle
            focusTapPoint?.let { pt ->
                val ringRadiusPx = with(LocalDensity.current) { 26.dp.toPx() }
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (pt.x - ringRadiusPx).roundToInt(),
                                (pt.y - ringRadiusPx).roundToInt()
                            )
                        }
                        .size(52.dp)
                        .border(2.dp, GoldPrimary, CircleShape)
                        .testTag("photo_preview_focus_reticle")
                )
            }

            // Top Viewfinder HUD: Flash, Frame Badge, Camera Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                            else -> ImageCapture.FLASH_MODE_AUTO
                        }
                    },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics(mergeDescendants = true) {}
                        .testTag("photo_preview_flash_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = when (flashMode) {
                                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                                else -> Icons.Default.FlashAuto
                            },
                            contentDescription = if (isHindi) "फ़्लैश मोड बदलें" else "Toggle Flash Mode",
                            tint = Color(0xFFF6E79D),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = when (flashMode) {
                                ImageCapture.FLASH_MODE_ON -> "ON"
                                ImageCapture.FLASH_MODE_OFF -> "OFF"
                                else -> "AUTO"
                            },
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, GoldDark.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = "${if (isHindi) aspectGuide.labelHi else aspectGuide.labelEn} • ${String.format("%.1fx", zoomRatio)}",
                        color = GoldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("photo_preview_hud_status")
                    )
                }

                Surface(
                    onClick = {
                        if (hasMultipleCameras) {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        }
                    },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("photo_preview_flip_btn")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = if (isHindi) "कैमरा लेंस बदलें" else "Switch Front or Rear Camera",
                            tint = if (hasMultipleCameras) Color(0xFFF6E79D) else Color.Gray
                        )
                    }
                }
            }

            // Bottom Zoom Preset Pills over Viewfinder
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(1.0f to "1x", 1.5f to "1.5x", 2.0f to "2x").forEach { (ratio, label) ->
                    val target = ratio.coerceAtMost(maxZoomRatio)
                    val active = kotlin.math.abs(zoomRatio - target) < 0.2f
                    Surface(
                        onClick = {
                            zoomRatio = target
                            runCatching { boundCamera?.cameraControl?.setZoomRatio(target) }
                        },
                        shape = CircleShape,
                        color = if (active) GoldPrimary else Color.Black.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, if (active) GoldPrimary else GoldPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                            .testTag("photo_preview_zoom_${label.replace(".", "_")}")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(
                                text = label,
                                color = if (active) Color(0xFF141000) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            if (isTakingPhoto || isProcessingShot) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.68f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GoldPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHindi) "HD संदर्भ शॉट तैयार हो रहा है..." else "Processing HD reference shot...",
                            color = GoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (localBindError != null) {
            Text(
                text = localBindError.orEmpty(),
                color = ErrorRed,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.testTag("photo_preview_camera_error")
            )
        }

        // Tactile Studio Shutter Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onLaunchPhotoPicker,
                border = BorderStroke(1.dp, StudioOutline),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("photo_preview_pick_library_btn")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isHindi) "लाइब्रेरी" else "Library",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Center Gold Tactile Shutter Trigger
            Surface(
                onClick = {
                    val capture = boundImageCapture
                    if (capture == null) {
                        onLaunchSystemCamera()
                        return@Surface
                    }
                    isTakingPhoto = true
                    localBindError = null
                    val (file, uri) = createPortfolioCaptureDestination(context)
                    val metadata = ImageCapture.Metadata().apply {
                        isReversedHorizontal = (lensFacing == CameraSelector.LENS_FACING_FRONT)
                    }
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file)
                        .setMetadata(metadata)
                        .build()
                    val lensName = if (lensFacing == CameraSelector.LENS_FACING_FRONT) "Front Cam" else "Rear Cam"
                    val flashName = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> "Flash On"
                        ImageCapture.FLASH_MODE_OFF -> "Flash Off"
                        else -> "Flash Auto"
                    }
                    val zoomTxt = String.format("%.1fx", zoomRatio)

                    capture.takePicture(
                        outputOptions,
                        mainExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                coroutineScope.launch {
                                    val encoded = withContext(Dispatchers.IO) {
                                        encodePortfolioPhotoUriToDataUrl(context, uri)
                                    }
                                    runCatching { file.delete() }
                                    isTakingPhoto = false
                                    if (encoded != null) {
                                        onCapturedFileUri(encoded, lensName, flashName, zoomTxt)
                                    } else {
                                        onError(
                                            if (isHindi) "फोटो एन्कोड करने में त्रुटि हुई।"
                                            else "Could not process captured photo."
                                        )
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                runCatching { file.delete() }
                                isTakingPhoto = false
                                localBindError = exception.localizedMessage ?: "Capture failed."
                                onError(localBindError.orEmpty())
                            }
                        }
                    )
                },
                enabled = !isTakingPhoto && !isProcessingShot,
                shape = CircleShape,
                color = GoldPrimary,
                border = BorderStroke(3.dp, GoldLight),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(72.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("photo_preview_shutter_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = if (isHindi) "संदर्भ फोटो खींचें" else "Take Reference Shot",
                        tint = Color(0xFF141000),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = if (capturedCount > 0) onOpenLatestReview else onLaunchSystemCamera,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("photo_preview_review_or_syscam_btn")
            ) {
                Icon(
                    imageVector = if (capturedCount > 0) Icons.Default.Collections else Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (capturedCount > 0) {
                        if (isHindi) "समीक्षा ($capturedCount)" else "Review ($capturedCount)"
                    } else {
                        if (isHindi) "सिस्टम कैम" else "Sys Cam"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
            }
        }
    }
}
