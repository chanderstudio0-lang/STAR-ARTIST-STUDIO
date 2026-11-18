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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
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
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Validates that a profile photo string is a supported URL or compact Base64 data URI within
 * Cloud Firestore's 500,000-byte security rule bound.
 */
fun isValidProfilePhotoUrl(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return false
    if (trimmed.length > 500_000) return false
    val lower = trimmed.lowercase()
    return lower.startsWith("https://") ||
        lower.startsWith("http://") ||
        lower.startsWith("data:image/")
}

/**
 * Creates a scoped FileProvider cache file for capturing a camera headshot without requesting
 * external storage permissions.
 */
fun createHeadshotCaptureDestination(context: Context): Pair<File, Uri> {
    val headshotsDir = File(context.cacheDir, "headshots").apply { mkdirs() }
    val photoFile = File(headshotsDir, "headshot_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
    return photoFile to uri
}

/**
 * Decodes an image Uri (camera or gallery), applies EXIF orientation correction so portraits stay
 * upright, downsamples cleanly to fit Firestore profile limits, and encodes as a JPEG data URI.
 */
fun encodeHeadshotUriToDataUrl(
    context: Context,
    uri: Uri,
    maxDimension: Int = 384,
    jpegQuality: Int = 82
): String? {
    return try {
        val orientation = context.contentResolver.openInputStream(uri)?.use { exifStream ->
            runCatching {
                ExifInterface(exifStream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { imgStream ->
            BitmapFactory.decodeStream(imgStream)
        } ?: return null

        val orientedBitmap = rotateBitmapByExifOrientation(decodedBitmap, orientation)
        val width = orientedBitmap.width
        val height = orientedBitmap.height
        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(width, height)
            val newW = (width * scale).toInt().coerceAtLeast(1)
            val newH = (height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(orientedBitmap, newW, newH, true)
        } else {
            orientedBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outputStream)
        val encoded = "data:image/jpeg;base64," + Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        if (encoded.length <= 500_000) encoded else null
    } catch (e: Exception) {
        null
    }
}

private fun rotateBitmapByExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
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
 * Material 3 'Profile Photo' component allowing artists to capture a professional headshot directly
 * with the device camera, choose from the Android Photo Picker, pick a studio preset, or supply a URL,
 * and immediately set it on their Cloud Firestore user profile.
 */
@Composable
fun ProfilePhotoComponent(
    displayName: String,
    avatarUrl: String,
    isHindi: Boolean,
    isSaving: Boolean = false,
    onAvatarUrlChanged: (String) -> Unit,
    onSetProfilePhoto: (
        newAvatarUrl: String,
        onComplete: (Boolean, String) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingCaptureFile by remember { mutableStateOf<File?>(null) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var photoStatusMessage by remember { mutableStateOf<String?>(null) }
    var photoErrorMessage by remember { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val capturedUri = pendingCaptureUri
        val capturedFile = pendingCaptureFile
        if (success && capturedUri != null) {
            val encoded = encodeHeadshotUriToDataUrl(context, capturedUri)
            capturedFile?.delete()
            pendingCaptureFile = null
            pendingCaptureUri = null
            if (encoded != null) {
                photoErrorMessage = null
                onAvatarUrlChanged(encoded)
                onSetProfilePhoto(encoded) { savedOk, msg ->
                    if (savedOk) {
                        photoStatusMessage = msg
                        photoErrorMessage = null
                    } else {
                        photoErrorMessage = msg
                    }
                }
            } else {
                photoErrorMessage = if (isHindi)
                    "कैप्चर किया गया हेडशॉट संसाधित नहीं हो सका। कृपया पुनः प्रयास करें।"
                else
                    "Unable to process captured headshot. Please retake."
            }
        } else {
            capturedFile?.delete()
            pendingCaptureFile = null
            pendingCaptureUri = null
        }
    }

    fun launchCameraCapture() {
        photoErrorMessage = null
        photoStatusMessage = null
        try {
            val (file, uri) = createHeadshotCaptureDestination(context)
            pendingCaptureFile = file
            pendingCaptureUri = uri
            takePictureLauncher.launch(uri)
        } catch (_: ActivityNotFoundException) {
            pendingCaptureFile?.delete()
            pendingCaptureFile = null
            pendingCaptureUri = null
            photoErrorMessage = if (isHindi)
                "इस डिवाइस पर कैमरा ऐप उपलब्ध नहीं है। कृपया गैलरी से फोटो चुनें।"
            else
                "No camera app found on this device. Please use Choose Photo instead."
        } catch (e: Exception) {
            pendingCaptureFile?.delete()
            pendingCaptureFile = null
            pendingCaptureUri = null
            photoErrorMessage = e.localizedMessage ?: "Unable to open camera."
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            launchCameraCapture()
        } else {
            photoErrorMessage = if (isHindi)
                "हेडशॉट लेने के लिए कैमरा अनुमति आवश्यक है। आप गैलरी से भी फोटो चुन सकते हैं।"
            else
                "Camera permission is required to capture a headshot. You can also choose a photo from gallery."
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val encoded = encodeHeadshotUriToDataUrl(context, uri)
            if (encoded != null) {
                photoErrorMessage = null
                onAvatarUrlChanged(encoded)
                onSetProfilePhoto(encoded) { savedOk, msg ->
                    if (savedOk) {
                        photoStatusMessage = msg
                        photoErrorMessage = null
                    } else {
                        photoErrorMessage = msg
                    }
                }
            } else {
                photoErrorMessage = if (isHindi)
                    "चयनित छवि संसाधित करने में समस्या आई।"
                else
                    "Unable to process selected image."
            }
        }
    }

    fun requestCameraOrLaunch() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
            launchCameraCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
            .testTag("profile_photo_component"),
        colors = CardDefaults.elevatedCardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row + Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isHindi) "प्रोफ़ाइल फ़ोटो और हेडशॉट" else "Profile Photo & Headshot",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Text(
                            text = if (isHindi)
                                "कैमरे से प्रोफ़ेशनल हेडशॉट लें या गैलरी से चुनें"
                            else
                                "Capture a studio headshot with camera or pick a photo",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                AssistChip(
                    onClick = { requestCameraOrLaunch() },
                    label = {
                        Text(
                            text = if (avatarUrl.isNotBlank()) {
                                if (isHindi) "हेडशॉट सेट" else "Headshot Set"
                            } else {
                                if (isHindi) "कैमरा तैयार" else "Camera Ready"
                            },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (avatarUrl.isNotBlank()) SuccessGreen.copy(alpha = 0.14f) else GoldContainer,
                        labelColor = if (avatarUrl.isNotBlank()) SuccessGreen else OnGoldContainer
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (avatarUrl.isNotBlank()) SuccessGreen.copy(alpha = 0.5f) else GoldPrimary.copy(alpha = 0.5f)
                    )
                )
            }

            // Headshot Frame Preview + Framing Guidance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clickable { requestCameraOrLaunch() }
                        .testTag("profile_headshot_frame"),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = if (isHindi) "कलाकार हेडशॉट" else "Artist Professional Headshot",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, GoldPrimary, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(GoldPrimary, GoldDark)))
                                .padding(2.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(StudioBlack),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayName.ifBlank { "S" }.take(1).uppercase(),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldPrimary
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(GoldPrimary)
                            .border(2.dp, StudioElevatedSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = if (isHindi) "कैमरा हेडशॉट लें" else "Take Camera Headshot",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = StudioCardSurface,
                    border = BorderStroke(1.dp, StudioOutline)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TipsAndUpdates,
                            contentDescription = null,
                            tint = GoldLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isHindi)
                                "सुझाव: कास्टिंग निर्देशकों के लिए साफ सामने की रोशनी और साधारण पृष्ठभूमि में हेडशॉट लें।"
                            else
                                "Studio Tip: Face soft natural or ring lighting against a clean backdrop for casting-ready headshots.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Primary Camera + Gallery + Clear Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { requestCameraOrLaunch() },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("profile_take_headshot_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "अपडेट..." else "Setting...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (avatarUrl.isNotBlank()) {
                                if (isHindi) "नया हेडशॉट लें" else "Retake Headshot"
                            } else {
                                if (isHindi) "हेडशॉट लें" else "Take Headshot"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        photoErrorMessage = null
                        photoStatusMessage = null
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("profile_pick_avatar_button"),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "गैलरी चुनें" else "Choose Photo",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (avatarUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            photoErrorMessage = null
                            onAvatarUrlChanged("")
                            onSetProfilePhoto("") { savedOk, msg ->
                                if (savedOk) {
                                    photoStatusMessage = msg
                                } else {
                                    photoErrorMessage = msg
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("profile_clear_avatar_button"),
                        border = BorderStroke(1.dp, StudioOutline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = if (isHindi) "फ़ोटो हटाएं" else "Clear Photo",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Inline Camera / Upload Status Feedback
            AnimatedVisibility(
                visible = photoErrorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ErrorRed.copy(alpha = 0.14f))
                        .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                        .testTag("profile_photo_error_banner"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = photoErrorMessage.orEmpty(),
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            AnimatedVisibility(
                visible = photoStatusMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SuccessGreen.copy(alpha = 0.14f))
                        .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                        .testTag("profile_photo_success_banner"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = photoStatusMessage.orEmpty(),
                        color = SuccessGreen,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Studio Avatar Presets Row
            Text(
                text = if (isHindi) "या स्टूडियो हेडशॉट प्रीसेट चुनें:" else "Or pick a Studio Headshot Preset:",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                studioAvatarPresets.forEachIndexed { index, presetUrl ->
                    val isSelected = avatarUrl == presetUrl
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) GoldPrimary else StudioOutline,
                                shape = CircleShape
                            )
                            .clickable {
                                photoErrorMessage = null
                                onAvatarUrlChanged(presetUrl)
                                onSetProfilePhoto(presetUrl) { savedOk, msg ->
                                    if (savedOk) photoStatusMessage = msg else photoErrorMessage = msg
                                }
                            }
                            .testTag("profile_avatar_preset_$index")
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(presetUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Studio Avatar Preset ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            OutlinedTextField(
                value = avatarUrl,
                onValueChange = {
                    photoErrorMessage = null
                    photoStatusMessage = null
                    onAvatarUrlChanged(it)
                },
                label = { Text(if (isHindi) "कस्टम हेडशॉट URL (वैकल्पिक)" else "Custom Headshot URL (Optional)") },
                placeholder = { Text("https://...") },
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null, tint = GoldPrimary)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_avatar_url_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = StudioOutline,
                    focusedLabelColor = GoldPrimary,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = GoldPrimary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }
    }
}
