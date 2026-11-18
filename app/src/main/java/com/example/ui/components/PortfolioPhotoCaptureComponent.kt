package com.example.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors

/**
 * Encoded high-quality portfolio photo ready for Firestore gallery upload and preview.
 */
data class EncodedPortfolioPhoto(
    val dataUrl: String,
    val width: Int,
    val height: Int,
    val byteLength: Int
)

/**
 * Validates that a portfolio media string is an HTTPS/HTTP URL or a Base64 data URI
 * within Firestore's 850,000-byte gallery security rule bound.
 */
fun isValidPortfolioMediaUrl(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty() || trimmed.length > 850_000) return false
    val lower = trimmed.lowercase()
    return lower.startsWith("https://") ||
        lower.startsWith("http://") ||
        lower.startsWith("data:image/")
}

/**
 * Checks whether the device reports at least one camera hardware feature.
 */
fun hasCameraHardware(context: Context): Boolean {
    val pm = context.packageManager
    return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ||
        pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
}

/**
 * Creates a scoped FileProvider cache destination inside `cacheDir/portfolio/` without
 * requiring broad external storage permissions.
 */
fun createPortfolioCaptureDestination(context: Context): Pair<File, Uri> {
    val portfolioDir = File(context.cacheDir, "portfolio").apply { mkdirs() }
    val photoFile = File(portfolioDir, "portfolio_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
    return photoFile to uri
}

/**
 * Decodes a portfolio image Uri (from CameraX, system camera, or Photo Picker), applies EXIF
 * rotation correction, scales down only when exceeding [maxDimension] (1440px HD), and encodes as
 * high-quality JPEG within [maxDataUrlBytes] so it fits reliably in Firestore.
 */
fun encodePortfolioPhotoUriToDataUrl(
    context: Context,
    uri: Uri,
    maxDimension: Int = 1440,
    maxDataUrlBytes: Int = 780_000
): EncodedPortfolioPhoto? {
    return try {
        val orientation = context.contentResolver.openInputStream(uri)?.use { exifStream ->
            runCatching {
                ExifInterface(exifStream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        // Decode bounds first so massive 48MP photos are subsampled efficiently before Bitmap alloc
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { boundsStream ->
            BitmapFactory.decodeStream(boundsStream, null, boundsOpts)
        }
        val rawMax = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        var sampleSize = 1
        while (rawMax > 0 && (rawMax / sampleSize) > maxDimension * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { imgStream ->
            BitmapFactory.decodeStream(imgStream, null, decodeOpts)
        } ?: return null

        val orientedBitmap = rotatePortfolioBitmapByExif(decodedBitmap, orientation)
        if (orientedBitmap !== decodedBitmap) {
            decodedBitmap.recycle()
        }

        val scaledBitmap = scaleBitmapToMaxEdge(orientedBitmap, maxDimension)
        if (scaledBitmap !== orientedBitmap) {
            orientedBitmap.recycle()
        }

        val candidateTargets = listOf(
            scaledBitmap to 92,
            scaledBitmap to 86,
            scaledBitmap to 80,
            scaledBitmap to 72,
            scaledBitmap to 64
        )
        for ((bmp, quality) in candidateTargets) {
            val encoded = compressBitmapToDataUrl(bmp, quality)
            if (encoded.length <= maxDataUrlBytes) {
                val res = EncodedPortfolioPhoto(
                    dataUrl = encoded,
                    width = bmp.width,
                    height = bmp.height,
                    byteLength = encoded.length
                )
                bmp.recycle()
                return res
            }
        }

        // Secondary fallback dimension for extraordinarily dense textures
        val compactBitmap = scaleBitmapToMaxEdge(scaledBitmap, 1080)
        if (compactBitmap !== scaledBitmap) {
            scaledBitmap.recycle()
        }
        for (quality in listOf(78, 68, 56)) {
            val encoded = compressBitmapToDataUrl(compactBitmap, quality)
            if (encoded.length <= maxDataUrlBytes) {
                val res = EncodedPortfolioPhoto(
                    dataUrl = encoded,
                    width = compactBitmap.width,
                    height = compactBitmap.height,
                    byteLength = encoded.length
                )
                compactBitmap.recycle()
                return res
            }
        }
        compactBitmap.recycle()
        null
    } catch (_: Exception) {
        null
    }
}

private fun scaleBitmapToMaxEdge(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxDimension && height <= maxDimension) return bitmap
    val scale = maxDimension.toFloat() / maxOf(width, height).coerceAtLeast(1)
    val newW = (width * scale).toInt().coerceAtLeast(1)
    val newH = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
}

private fun compressBitmapToDataUrl(bitmap: Bitmap, jpegQuality: Int): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outputStream)
    return "data:image/jpeg;base64," + Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

private fun rotatePortfolioBitmapByExif(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        else -> return bitmap
    }
    return runCatching {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }.getOrDefault(bitmap)
}

/**
 * Full-featured in-app Studio Camera Capture modal powered by CameraX (CAPTURE_MODE_MAXIMIZE_QUALITY)
 * with explicit runtime `CAMERA` permission handling, flash/lens controls, system camera fallback,
 * and Photo Picker fallback.
 */
@Composable
fun StudioCameraCaptureDialog(
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onPhotoCaptured: (EncodedPortfolioPhoto) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedOnce by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var isCapturingOrEncoding by remember { mutableStateOf(false) }
    var boundImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var hasMultipleCameras by remember { mutableStateOf(true) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(4f) }
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    var pendingReviewPhoto by remember { mutableStateOf<EncodedPortfolioPhoto?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    var pendingExternalCaptureFile by remember { mutableStateOf<File?>(null) }
    var pendingExternalCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { cameraProviderRef?.unbindAll() }
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(focusTapOffset) {
        if (focusTapOffset != null) {
            delay(900)
            focusTapOffset = null
        }
    }

    fun processUriAndReturn(uri: Uri, tempFile: File?, reviewBeforeConfirm: Boolean = true) {
        isCapturingOrEncoding = true
        cameraError = null
        coroutineScope.launch {
            val encoded = withContext(Dispatchers.IO) {
                encodePortfolioPhotoUriToDataUrl(context, uri)
            }
            tempFile?.delete()
            isCapturingOrEncoding = false
            if (encoded != null) {
                if (reviewBeforeConfirm) {
                    pendingReviewPhoto = encoded
                } else {
                    onPhotoCaptured(encoded)
                }
            } else {
                cameraError = if (isHindi) {
                    "कैप्चर की गई फ़ोटो संसाधित नहीं हो सकी। कृपया पुनः प्रयास करें।"
                } else {
                    "Unable to process captured photo. Please retake or pick another image."
                }
            }
        }
    }

    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uri = pendingExternalCaptureUri
        val file = pendingExternalCaptureFile
        pendingExternalCaptureUri = null
        pendingExternalCaptureFile = null
        if (success && uri != null) {
            processUriAndReturn(uri, file)
        } else {
            file?.delete()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            processUriAndReturn(uri, null)
        }
    }

    fun launchSystemCameraFallback() {
        cameraError = null
        try {
            val (file, uri) = createPortfolioCaptureDestination(context)
            pendingExternalCaptureFile = file
            pendingExternalCaptureUri = uri
            systemCameraLauncher.launch(uri)
        } catch (_: ActivityNotFoundException) {
            pendingExternalCaptureFile?.delete()
            pendingExternalCaptureFile = null
            pendingExternalCaptureUri = null
            cameraError = if (isHindi) {
                "इस डिवाइस पर कैमरा ऐप उपलब्ध नहीं है। कृपया गैलरी से चुनें।"
            } else {
                "No camera app available on this device. Please choose a photo from gallery."
            }
        } catch (e: Exception) {
            pendingExternalCaptureFile?.delete()
            pendingExternalCaptureFile = null
            pendingExternalCaptureUri = null
            cameraError = e.localizedMessage ?: "Unable to launch system camera."
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        hasCameraPermission = granted
        permissionDeniedOnce = !granted
        if (!granted) {
            cameraError = if (isHindi) {
                "स्टूडियो पोर्टफोलियो फ़ोटो लेने के लिए कैमरा अनुमति आवश्यक है।"
            } else {
                "Camera permission is required to capture portfolio photos directly."
            }
        } else {
            cameraError = null
        }
    }

    Dialog(
        onDismissRequest = { if (!isCapturingOrEncoding) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .border(1.2.dp, GoldPrimary.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .testTag("studio_camera_capture_dialog"),
            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
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
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GoldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "स्टूडियो कैमरा कैप्चर" else "Studio Camera Capture",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldLight
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HighQuality,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (isHindi) "HD 1440px पोर्टफोलियो मोड" else "HD 1440px Portfolio Quality",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { if (!isCapturingOrEncoding) onDismiss() },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("camerax_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isHindi) "बंद करें" else "Close Camera",
                            tint = TextSecondary
                        )
                    }
                }

                if (!hasCameraPermission) {
                    // Explicit Camera Permission Gate Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("camera_permission_request_card"),
                        shape = RoundedCornerShape(16.dp),
                        color = StudioElevatedSurface,
                        border = BorderStroke(1.dp, StudioOutline)
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
                                    .border(1.dp, GoldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Text(
                                text = if (isHindi) "कैमरा हार्डवेयर अनुमति सक्षम करें" else "Enable Camera Hardware Permission",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = if (isHindi) {
                                    "उच्च-गुणवत्ता वाले पोर्टफोलियो फ़ोटो सीधे ऐप से खींचने और अपलोड करने के लिए कैमरा एक्सेस की अनुमति दें।"
                                } else {
                                    "Allow camera access to capture high-quality studio portfolio photos directly inside Star Artist Studio."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            if (permissionDeniedOnce && cameraError != null) {
                                Text(
                                    text = cameraError.orEmpty(),
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Button(
                                onClick = {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = StudioBlack
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .semantics(mergeDescendants = true) {}
                                    .testTag("grant_camera_permission_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi) "कैमरा अनुमति दें" else "Enable Camera Access",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                border = BorderStroke(1.dp, StudioOutline),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("camera_fallback_pick_photo_button")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi) "गैलरी से चुनें" else "Choose from Gallery Instead",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else if (pendingReviewPhoto != null) {
                    val reviewedPhoto = pendingReviewPhoto!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("camerax_review_panel"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(StudioBlack)
                                .border(1.dp, GoldPrimary.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(reviewedPhoto.dataUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = if (isHindi) "कैप्चर की गई फ़ोटो समीक्षा" else "Captured photo review",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("camerax_review_image")
                            )

                            Surface(
                                color = Color.Black.copy(alpha = 0.78f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "HD ${reviewedPhoto.width}×${reviewedPhoto.height} • ${(reviewedPhoto.byteLength / 1024).coerceAtLeast(1)} KB",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pendingReviewPhoto = null },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StudioOutline),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .semantics(mergeDescendants = true) {}
                                    .testTag("camerax_retake_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "पुनः लें" else "Retake",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    pendingReviewPhoto = null
                                    onPhotoCaptured(reviewedPhoto)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.25f)
                                    .heightIn(min = 48.dp)
                                    .semantics(mergeDescendants = true) {}
                                    .testTag("camerax_use_photo_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "पोर्टफोलियो में उपयोग करें" else "Use This Photo",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                } else {
                    // Granted: CameraX Viewfinder + Tap-to-Focus + Zoom + Capture Controls
                    val cameraAvailable = remember(context) { hasCameraHardware(context) }

                    if (cameraAvailable) {
                        LaunchedEffect(previewViewRef, lensFacing, hasCameraPermission, lifecycleOwner) {
                            val previewView = previewViewRef ?: return@LaunchedEffect
                            if (!hasCameraPermission) return@LaunchedEffect
                            val future = ProcessCameraProvider.getInstance(context)
                            future.addListener({
                                try {
                                    val provider = future.get()
                                    cameraProviderRef = provider

                                    val hasBack = runCatching { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)
                                    val hasFront = runCatching { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)
                                    hasMultipleCameras = hasBack && hasFront

                                    val effectiveLens = when {
                                        lensFacing == CameraSelector.LENS_FACING_BACK && hasBack -> CameraSelector.LENS_FACING_BACK
                                        lensFacing == CameraSelector.LENS_FACING_FRONT && hasFront -> CameraSelector.LENS_FACING_FRONT
                                        hasBack -> CameraSelector.LENS_FACING_BACK
                                        hasFront -> CameraSelector.LENS_FACING_FRONT
                                        else -> lensFacing
                                    }
                                    if (effectiveLens != lensFacing) {
                                        lensFacing = effectiveLens
                                    }

                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
                                    val imageCapture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                        .setFlashMode(flashMode)
                                        .setTargetRotation(rotation)
                                        .build()

                                    val selector = CameraSelector.Builder()
                                        .requireLensFacing(effectiveLens)
                                        .build()

                                    provider.unbindAll()
                                    val camera = provider.bindToLifecycle(
                                        lifecycleOwner,
                                        selector,
                                        preview,
                                        imageCapture
                                    )
                                    boundCamera = camera
                                    boundImageCapture = imageCapture
                                    val zoomStateVal = camera.cameraInfo.zoomState.value
                                    maxZoomRatio = (zoomStateVal?.maxZoomRatio ?: 4f).coerceAtLeast(1.5f)
                                    zoomRatio = (zoomStateVal?.zoomRatio ?: 1f).coerceIn(1f, maxZoomRatio)
                                    cameraError = null
                                } catch (e: Exception) {
                                    boundCamera = null
                                    boundImageCapture = null
                                    cameraError = if (isHindi) {
                                        "इन-ऐप कैमरा प्रीव्यू उपलब्ध नहीं है। सिस्टम कैमरा या गैलरी का उपयोग करें।"
                                    } else {
                                        "In-app camera preview unavailable on this device (${e.localizedMessage ?: "hardware busy"}). Use System Camera below."
                                    }
                                }
                            }, mainExecutor)
                        }

                        LaunchedEffect(flashMode, boundImageCapture) {
                            boundImageCapture?.flashMode = flashMode
                        }

                        val reticleHalfPx = with(LocalDensity.current) { 24.dp.toPx() }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(StudioBlack)
                                .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
                                .pointerInput(boundCamera, previewViewRef) {
                                    detectTapGestures { offset ->
                                        val camera = boundCamera ?: return@detectTapGestures
                                        val preview = previewViewRef ?: return@detectTapGestures
                                        focusTapOffset = offset
                                        val point = preview.meteringPointFactory.createPoint(offset.x, offset.y)
                                        val action = FocusMeteringAction.Builder(
                                            point,
                                            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                        )
                                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                            .build()
                                        runCatching { camera.cameraControl.startFocusAndMetering(action) }
                                    }
                                }
                                .pointerInput(boundCamera, maxZoomRatio) {
                                    detectTransformGestures { _, _, zoom, _ ->
                                        val camera = boundCamera ?: return@detectTransformGestures
                                        if (zoom != 1f) {
                                            val updatedZoom = (zoomRatio * zoom).coerceIn(1f, maxZoomRatio)
                                            zoomRatio = updatedZoom
                                            runCatching { camera.cameraControl.setZoomRatio(updatedZoom) }
                                        }
                                    }
                                }
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).apply {
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    }.also { view ->
                                        previewViewRef = view
                                    }
                                },
                                update = {},
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("camerax_viewfinder_preview")
                            )

                            // Tap-to-focus gold reticle overlay
                            focusTapOffset?.let { tap ->
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                (tap.x - reticleHalfPx).roundToInt(),
                                                (tap.y - reticleHalfPx).roundToInt()
                                            )
                                        }
                                        .size(48.dp)
                                        .border(1.5.dp, GoldPrimary, CircleShape)
                                )
                            }

                            // Top overlay controls (Flash, Zoom 1x/2x, Lens Flip)
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
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .testTag("camerax_flash_toggle_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                                        Icon(
                                            imageVector = when (flashMode) {
                                                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                                ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                                                else -> Icons.Default.FlashAuto
                                            },
                                            contentDescription = "Toggle Camera Flash",
                                            tint = Color(0xFFF6E79D)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        val nextZoom = if (zoomRatio >= 1.8f) 1f else 2f.coerceAtMost(maxZoomRatio)
                                        zoomRatio = nextZoom
                                        runCatching { boundCamera?.cameraControl?.setZoomRatio(nextZoom) }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.Black.copy(alpha = 0.72f),
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .testTag("camerax_zoom_toggle_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(
                                            text = String.format("%.1fx", zoomRatio),
                                            color = Color(0xFFF6E79D),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
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
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .testTag("camerax_switch_lens_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.FlipCameraAndroid,
                                            contentDescription = if (isHindi) "कैमरा बदलें" else "Switch Camera Lens",
                                            tint = if (hasMultipleCameras) Color(0xFFF6E79D) else Color.Gray
                                        )
                                    }
                                }
                            }

                            if (isCapturingOrEncoding) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.7f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = GoldPrimary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (isHindi) "HD फ़ोटो तैयार हो रही है..." else "Optimizing HD photo...",
                                            color = Color(0xFFF6E79D),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = StudioElevatedSurface,
                            border = BorderStroke(1.dp, StudioOutline)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.VideocamOff, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                                Text(
                                    text = if (isHindi) "कोई कैमरा हार्डवेयर नहीं मिला। नीचे से गैलरी या सिस्टम कैमरा चुनें।" else "No camera sensor detected on this device. Use System Camera or Gallery below.",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (cameraError != null) {
                        Text(
                            text = cameraError.orEmpty(),
                            color = ErrorRed,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.testTag("camerax_error_text")
                        )
                    }

                    // Shutter & System Camera Fallback Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { launchSystemCameraFallback() },
                            enabled = !isCapturingOrEncoding,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("camerax_system_camera_fallback_button")
                        ) {
                            Text(
                                text = if (isHindi) "सिस्टम कैमरा" else "System Camera",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Button(
                            onClick = {
                                val capture = boundImageCapture
                                if (capture == null) {
                                    launchSystemCameraFallback()
                                    return@Button
                                }
                                isCapturingOrEncoding = true
                                cameraError = null
                                val (file, uri) = createPortfolioCaptureDestination(context)
                                val metadata = ImageCapture.Metadata().apply {
                                    isReversedHorizontal = (lensFacing == CameraSelector.LENS_FACING_FRONT)
                                }
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(file)
                                    .setMetadata(metadata)
                                    .build()
                                capture.takePicture(
                                    outputOptions,
                                    mainExecutor,
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            processUriAndReturn(uri, file, reviewBeforeConfirm = true)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            file.delete()
                                            isCapturingOrEncoding = false
                                            cameraError = exception.localizedMessage ?: "Capture failed. Try System Camera."
                                        }
                                    }
                                )
                            },
                            enabled = !isCapturingOrEncoding,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.25f)
                                .heightIn(min = 52.dp)
                                .semantics(mergeDescendants = true) {}
                                .testTag("camerax_shutter_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "फोटो खींचें" else "Capture Photo",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
