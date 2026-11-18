package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.ProjectDraftEntity
import com.example.data.model.AppLanguage
import com.example.data.model.ShootBooking
import com.example.data.model.UserProfile
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
import com.example.util.StudioContactDetails
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Curated luxury studio avatar presets for client profiles.
 */
val studioAvatarPresets = listOf(
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=256&fit=crop&crop=faces",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=256&fit=crop&crop=faces",
    "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=256&fit=crop&crop=faces",
    "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=256&fit=crop&crop=faces"
)

/**
 * Reads an image Uri from the Photo Picker, downsamples to max dimension to preserve Firestore
 * document size limits, and compresses to a Base64 data URL.
 */
fun convertUriToDownsampledDataUrl(context: android.content.Context, uri: Uri, maxDimension: Int = 256): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(width, height)
            val newW = (width * scale).toInt().coerceAtLeast(1)
            val newH = (height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(originalBitmap, newW, newH, true)
        } else {
            originalBitmap
        }
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

/**
 * Visual status tracking indicator card for studio bookings, displaying the current approval stage
 * (Pending Review, Confirmed & Scheduled, Completed, or Cancelled) along with a 3-step progress timeline.
 */
@Composable
fun BookingApprovalStageCard(
    booking: ShootBooking,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isPending = booking.status.equals("Pending", ignoreCase = true)
    val isConfirmed = booking.status.equals("Confirmed", ignoreCase = true)
    val isCompleted = booking.status.equals("Completed", ignoreCase = true)
    val isCancelled = booking.status.equals("Cancelled", ignoreCase = true)

    val statusBadgeColor = when {
        isCancelled -> Color(0xFFE57373)
        isCompleted -> GoldLight
        isConfirmed -> Color(0xFF81C784)
        else -> Color(0xFFFFD54F) // Pending amber
    }

    val statusContainerColor = when {
        isCancelled -> Color(0xFF331515)
        isCompleted -> Color(0xFF2E2408)
        isConfirmed -> Color(0xFF162E18)
        else -> Color(0xFF2E2405)
    }

    val statusLabel = when {
        isCancelled -> if (language == AppLanguage.HINDI) "रद्द (Cancelled)" else "Cancelled"
        isCompleted -> if (language == AppLanguage.HINDI) "पूर्ण (Completed)" else "Completed"
        isConfirmed -> if (language == AppLanguage.HINDI) "स्वीकृत व निर्धारित (Confirmed)" else "Confirmed & Scheduled"
        else -> if (language == AppLanguage.HINDI) "समीक्षा जारी (Pending Review)" else "Pending Review"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.dp,
                    if (isConfirmed || isCompleted) GoldPrimary.copy(alpha = 0.5f) else StudioOutline
                ),
                RoundedCornerShape(12.dp)
            )
            .testTag("booking_stage_card_${booking.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Service Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.serviceType.ifBlank { "Production Shoot" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${booking.preferredDate} • ${booking.preferredTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFAAAAAA),
                        fontSize = 10.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusContainerColor)
                        .border(1.dp, statusBadgeColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusBadgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3-Step Approval Timeline Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stage 1: Request Submitted
                val stage1Completed = !isCancelled
                StageStepNode(
                    stepNumber = "1",
                    label = if (language == AppLanguage.HINDI) "अनुरोध" else "Submitted",
                    isActive = true,
                    isCompleted = stage1Completed,
                    nodeColor = if (isCancelled) Color(0xFFE57373) else Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )

                // Connector 1 -> 2
                val connector1Active = !isCancelled
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .height(2.dp)
                        .background(if (connector1Active) GoldPrimary.copy(alpha = 0.8f) else Color.DarkGray)
                )

                // Stage 2: Studio Review
                val stage2Completed = isConfirmed || isCompleted
                val stage2Active = isPending
                StageStepNode(
                    stepNumber = "2",
                    label = if (language == AppLanguage.HINDI) "समीक्षा" else "Review",
                    isActive = stage2Active || stage2Completed,
                    isCompleted = stage2Completed,
                    nodeColor = when {
                        isCancelled -> Color(0xFFE57373)
                        stage2Completed -> Color(0xFF4CAF50)
                        stage2Active -> Color(0xFFFFD54F)
                        else -> Color.Gray
                    },
                    modifier = Modifier.weight(1f)
                )

                // Connector 2 -> 3
                val connector2Active = isConfirmed || isCompleted
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .height(2.dp)
                        .background(if (connector2Active) Color(0xFF4CAF50) else Color.DarkGray)
                )

                // Stage 3: Confirmed & Scheduled
                val stage3Completed = isCompleted
                val stage3Active = isConfirmed
                StageStepNode(
                    stepNumber = "3",
                    label = if (isCompleted) {
                        if (language == AppLanguage.HINDI) "पूर्ण" else "Completed"
                    } else {
                        if (language == AppLanguage.HINDI) "पुष्टि" else "Confirmed"
                    },
                    isActive = stage3Active || stage3Completed,
                    isCompleted = stage3Completed,
                    nodeColor = when {
                        stage3Completed -> GoldLight
                        stage3Active -> Color(0xFF4CAF50)
                        else -> Color.Gray
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Guidance & Location Note
            val guidanceText = when {
                isCancelled -> if (language == AppLanguage.HINDI)
                    "यह बुकिंग अनुरोध रद्द कर दिया गया है। पुनः सहायता हेतु कॉल/व्हाट्सएप करें।"
                else
                    "This booking request was cancelled. Reach out via Call/WhatsApp for rescheduling."
                isCompleted -> if (language == AppLanguage.HINDI)
                    "शूटिंग व पोस्ट-प्रोडक्शन सफलतापूर्वक पूर्ण हुआ। धन्यवाद!"
                else
                    "Studio photoshoot & video production completed. Thank you!"
                isConfirmed -> if (language == AppLanguage.HINDI)
                    "आपकी बुकिंग स्वीकृत व निर्धारित है! कृपया निर्धारित समय से 15 मिनट पूर्व तलवाड़ा खुर्द स्टूडियो पहुंचें।"
                else
                    "Shoot confirmed & slot locked! Please arrive 15 minutes before scheduled slot at Talwara Khurd, Sirsa."
                else -> if (language == AppLanguage.HINDI)
                    "स्टूडियो एडमिन द्वारा स्लॉट और उपकरणों की समीक्षा जारी है। 9541024098 से व्हाट्सएप संदेश प्राप्त होगा।"
                else
                    "Studio team is reviewing your schedule and equipment setup. WhatsApp confirmation will follow."
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF141414))
                    .padding(8.dp)
            ) {
                Text(
                    text = guidanceText,
                    fontSize = 10.5.sp,
                    color = if (isConfirmed || isCompleted) GoldLight else Color(0xFFBBBBBB),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun StageStepNode(
    stepNumber: String,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    nodeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isActive) nodeColor.copy(alpha = 0.2f) else Color(0xFF222222))
                .border(1.5.dp, if (isActive) nodeColor else Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = nodeColor,
                    modifier = Modifier.size(12.dp)
                )
            } else {
                Text(
                    text = stepNumber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) nodeColor else Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) nodeColor else Color.Gray,
            maxLines = 1
        )
    }
}

/**
 * User Profile Content enabling clients to track saved photography projects and booking history.
 */
@Composable
fun UserProfileSectionContent(
    userProfile: UserProfile?,
    userEmail: String?,
    currentUserId: String,
    modelsCount: Int,
    bookingsCount: Int,
    myBookings: List<ShootBooking> = emptyList(),
    savedProjects: List<ProjectDraftEntity> = emptyList(),
    onToggleProjectFavorite: (ProjectDraftEntity) -> Unit = {},
    onDeleteSavedProject: (ProjectDraftEntity) -> Unit = {},
    onSaveNewProject: (String, String, String) -> Unit = { _, _, _ -> },
    onSignOut: () -> Unit = {},
    isStudioLight: Boolean = false,
    onToggleTheme: () -> Unit = {},
    isSubmitting: Boolean = false,
    language: AppLanguage = AppLanguage.ENGLISH,
    onDeleteMyData: () -> Unit = {},
    onUpdateProfile: (displayName: String, avatarUrl: String) -> Unit = { _, _ -> }
) {
    var selectedProfileTab by remember { mutableIntStateOf(0) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showCustomizeDialog by remember { mutableStateOf(false) }

    val firebaseUser = Firebase.auth.currentUser
    val clientDisplayName = userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: firebaseUser?.displayName?.takeIf { it.isNotBlank() }
        ?: userEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
        ?: "Studio Client"
    val effectiveAvatar = userProfile?.effectiveAvatarUrl?.takeIf { it.isNotBlank() }
        ?: firebaseUser?.photoUrl?.toString()?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_profile_section")
    ) {
        // =====================================================================
        // 1. CLIENT FIREBASE AUTH IDENTITY CARD
        // =====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(GoldPrimary, StudioOutline, GoldPrimary.copy(alpha = 0.5f)))),
                    RoundedCornerShape(16.dp)
                )
                .testTag("client_auth_identity_card"),
            colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Avatar with edit overlay
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clickable { showCustomizeDialog = true }
                            .testTag("avatar_container"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!effectiveAvatar.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(effectiveAvatar)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, GoldPrimary, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(GoldPrimary, GoldDark))
                                    )
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(StudioBlack),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = clientDisplayName.take(1).uppercase(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldLight
                                    )
                                }
                            }
                        }

                        // Small camera indicator badge
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(GoldPrimary)
                                .border(1.dp, StudioBlack, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Edit Avatar",
                                tint = StudioBlack,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = clientDisplayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GoldContainer)
                                    .border(0.8.dp, GoldPrimary, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "CLIENT",
                                    color = GoldPrimary,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = userEmail ?: firebaseUser?.email ?: "Authenticated via Google",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFDDDDDD),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Security Auth Status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                            )
                            Text(
                                text = "Firebase Auth • Google Verified",
                                color = SuccessGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Edit Profile Action Button
                    IconButton(
                        onClick = { showCustomizeDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = GoldLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Customize Profile Button Bar
                OutlinedButton(
                    onClick = { showCustomizeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("customize_profile_button"),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "प्रोफ़ाइल व अवतार संपादित करें" else "Edit Display Name & Avatar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats Banner: Saved Projects & Active Bookings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Saved Projects Count Pill
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141008)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StudioOutline)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "${savedProjects.size}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (language == AppLanguage.HINDI) "सहेजे गए प्रोजेक्ट" else "Saved Projects",
                                    fontSize = 9.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }

                    // Bookings Count Pill
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141008)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StudioOutline)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.EventAvailable, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "${myBookings.size}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (language == AppLanguage.HINDI) "शूट बुकिंग्स" else "Bookings",
                                    fontSize = 9.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // =====================================================================
        // 2. PROFILE SECTION TABS (Saved Projects, Booking History, Account)
        // =====================================================================
        TabRow(
            selectedTabIndex = selectedProfileTab,
            containerColor = StudioBlack,
            contentColor = GoldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedProfileTab]),
                    color = GoldPrimary
                )
            },
            modifier = Modifier.testTag("client_profile_tabs")
        ) {
            Tab(
                selected = selectedProfileTab == 0,
                onClick = { selectedProfileTab = 0 },
                text = {
                    Text(
                        text = if (language == AppLanguage.HINDI) "प्रोजेक्ट्स (${savedProjects.size})" else "Saved Projects (${savedProjects.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedProfileTab == 0) GoldPrimary else Color.Gray
                    )
                }
            )
            Tab(
                selected = selectedProfileTab == 1,
                onClick = { selectedProfileTab = 1 },
                text = {
                    Text(
                        text = if (language == AppLanguage.HINDI) "बुकिंग इतिहास (${myBookings.size})" else "Booking History (${myBookings.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedProfileTab == 1) GoldPrimary else Color.Gray
                    )
                }
            )
            Tab(
                selected = selectedProfileTab == 2,
                onClick = { selectedProfileTab = 2 },
                text = {
                    Text(
                        text = if (language == AppLanguage.HINDI) "खाता व सुरक्षा" else "Account & Privacy",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedProfileTab == 2) GoldPrimary else Color.Gray
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // =====================================================================
        // 3. TAB 0: SAVED PHOTOGRAPHY PROJECTS
        // =====================================================================
        if (selectedProfileTab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_saved_projects_section"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header with Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "सहेजे गए फोटोग्राफी प्रोजेक्ट्स" else "Saved Photography Projects",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        fontSize = 12.5.sp
                    )

                    Button(
                        onClick = { showNewProjectDialog = true },
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("profile_new_project_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = StudioBlack),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "नया प्रोजेक्ट" else "New Project",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (savedProjects.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StudioOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI)
                                    "अभी तक कोई प्रोजेक्ट सहेजा नहीं गया है।"
                                else
                                    "No saved photography projects yet.",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI)
                                    "अपने फोटोशूट विचार, पोशाक योजनाएं और लाइटिंग सेटअप्स यहाँ सहेजें।"
                                else
                                    "Save your photoshoot moodboards, costume plans, and lighting concepts here.",
                                color = Color.Gray,
                                fontSize = 10.5.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    savedProjects.forEach { project ->
                        SavedProjectCard(
                            project = project,
                            language = language,
                            onToggleFavorite = { onToggleProjectFavorite(project) },
                            onDelete = { onDeleteSavedProject(project) }
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 4. TAB 1: BOOKING HISTORY & APPROVAL TIMELINE
        // =====================================================================
        if (selectedProfileTab == 1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("booking_status_tracking_section"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "शूट बुकिंग इतिहास" else "Client Booking History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "${myBookings.size} total",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }

                if (myBookings.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StudioOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EventAvailable, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI)
                                    "कोई सक्रिय बुकिंग नहीं मिली।"
                                else
                                    "No shoot bookings recorded yet.",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI)
                                    "बुकिंग फॉर्म से नया शूट स्लॉट आरक्षित करें और अनुमोदन स्थिति यहाँ ट्रैक करें।"
                                else
                                    "Reserve your production session in the booking form to track review and confirmation here.",
                                color = Color.Gray,
                                fontSize = 10.5.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    myBookings.forEach { booking ->
                        BookingApprovalStageCard(
                            booking = booking,
                            language = language
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 5. TAB 2: ACCOUNT, SECURITY & DATA DELETION
        // =====================================================================
        if (selectedProfileTab == 2) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Theme Toggle Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, StudioOutline), RoundedCornerShape(14.dp))
                        .testTag("profile_theme_toggle_card"),
                    colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(GoldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isStudioLight) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "थीम मोड (Theme)" else "Theme & Contrast",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isStudioLight) "Studio Light" else "Luxury Black & Gold",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldLight,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        ThemeToggleComponent(
                            isStudioLight = isStudioLight,
                            onToggle = onToggleTheme
                        )
                    }
                }

                // Sign Out Button
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("profile_sign_out_button"),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "साइन आउट करें (Sign Out)" else "Sign Out of Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Data Sovereignty & Removal Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            RoundedCornerShape(14.dp)
                        )
                        .testTag("delete_my_data_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF221111)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "डेटा संप्रभुता व निष्कासन" else "Data Sovereignty & Removal",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "यह विकल्प आपके सभी मॉडल आवेदन, शूट बुकिंग्स और प्रोफ़ाइल रिकॉर्ड्स को स्थायी रूप से हटा देता है।"
                            else
                                "Permanently delete all your bookings, model submissions, and client records from Cloud Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD8B0B0),
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("delete_my_data_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "मेरा डेटा हटाएं" else "Delete My Data",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog to Create New Project
    if (showNewProjectDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newType by remember { mutableStateOf("Photography") }
        var newNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = {
                Text(
                    text = if (language == AppLanguage.HINDI) "नया फोटोग्राफी प्रोजेक्ट सहेजें" else "Save New Photography Project",
                    color = GoldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Project Title") },
                        placeholder = { Text("e.g. Royal Bridal Campaign") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it },
                        label = { Text("Concept & Lighting Notes") },
                        placeholder = { Text("e.g. Amber backlight, gold jewelry set, 4K slow motion") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onSaveNewProject(newTitle.trim(), newType, newNotes.trim())
                            showNewProjectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = StudioBlack),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Project", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = StudioElevatedSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, GoldPrimary, RoundedCornerShape(16.dp))
        )
    }

    // Confirmation Alert Dialog for Data Deletion
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "डेटा हटाने की पुष्टि" else "Confirm Data Deletion",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            },
            text = {
                Text(
                    text = if (language == AppLanguage.HINDI)
                        "क्या आप वाकई अपने सभी संबंधित दस्तावेज़ फायरस्टोर डेटाबेस से स्थायी रूप से हटाना चाहते हैं?\n\nयह क्रिया पूर्ववत नहीं की जा सकती।"
                    else
                        "Permanently purge all your associated bookings and profile documents from Cloud Firestore?\n\nThis action cannot be undone.",
                    color = Color(0xFFE0E0E0),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onDeleteMyData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_delete_everything_button")
                ) {
                    Text(if (language == AppLanguage.HINDI) "हाँ, सब कुछ हटाएं" else "Yes, Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel", color = Color.LightGray)
                }
            },
            containerColor = StudioCardSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        )
    }

    // Modal Dialog to Customize Display Name and Avatar Image
    if (showCustomizeDialog) {
        var editDisplayName by remember(clientDisplayName) { mutableStateOf(clientDisplayName) }
        var editAvatarUrl by remember(effectiveAvatar) { mutableStateOf(effectiveAvatar ?: "") }
        val context = LocalContext.current
        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                val dataUrl = convertUriToDownsampledDataUrl(context, uri)
                if (dataUrl != null) {
                    editAvatarUrl = dataUrl
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showCustomizeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "प्रोफ़ाइल अनुकूलन" else "Customize Profile",
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar Preview
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(StudioBlack)
                            .border(2.dp, GoldPrimary, CircleShape)
                            .testTag("avatar_preview_image"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (editAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(editAvatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = (editDisplayName.ifBlank { clientDisplayName }).take(1).uppercase(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldLight
                            )
                        }
                    }

                    // Upload Photo Button (Photo Picker)
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("upload_avatar_photo_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = StudioBlack),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "फ़ोटो अपलोड करें (Photo Picker)" else "Upload Avatar Photo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Presets Label & Row
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "स्टूडियो अवतार प्रीसेट" else "Studio Avatar Presets",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("avatar_presets_row"),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            studioAvatarPresets.forEachIndexed { index, presetUrl ->
                                val isSelected = editAvatarUrl == presetUrl
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) GoldLight else Color.DarkGray,
                                            shape = CircleShape
                                        )
                                        .clickable { editAvatarUrl = presetUrl }
                                        .testTag("avatar_preset_$index")
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(presetUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Studio Preset $index",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // Direct Avatar URL Input
                    OutlinedTextField(
                        value = editAvatarUrl,
                        onValueChange = { editAvatarUrl = it },
                        label = { Text(if (language == AppLanguage.HINDI) "इमेज URL या डेटा" else "Avatar URL / Data") },
                        placeholder = { Text("https://... or data:image/...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_avatar_url_input"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        }
                    )

                    // Display Name Input
                    OutlinedTextField(
                        value = editDisplayName,
                        onValueChange = { editDisplayName = it },
                        label = { Text(if (language == AppLanguage.HINDI) "प्रदर्शन नाम (Display Name)" else "Display Name") },
                        placeholder = { Text("e.g. Jasmeet Singh") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_display_name_input"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editDisplayName.isNotBlank()) {
                            onUpdateProfile(editDisplayName.trim(), editAvatarUrl.trim())
                            showCustomizeDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_profile_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = StudioBlack),
                    shape = RoundedCornerShape(8.dp),
                    enabled = editDisplayName.isNotBlank()
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "सहेजें (Save)" else "Save Profile",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCustomizeDialog = false },
                    modifier = Modifier.testTag("cancel_edit_profile_button")
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel",
                        color = Color.Gray
                    )
                }
            },
            containerColor = StudioElevatedSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .border(1.dp, GoldPrimary, RoundedCornerShape(16.dp))
                .testTag("profile_customization_dialog")
        )
    }
}

/**
 * Saved Project Card in the client profile.
 */
@Composable
private fun SavedProjectCard(
    project: ProjectDraftEntity,
    language: AppLanguage,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.8.dp, StudioOutline, RoundedCornerShape(12.dp))
            .testTag("saved_project_card_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = project.draftTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        fontSize = 12.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E170A))
                            .border(0.5.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = project.projectType,
                            color = GoldPrimary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (project.conceptNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = project.conceptNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCCCCCC),
                        fontSize = 10.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (project.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (project.isFavorite) Color(0xFFE53935) else Color.Gray,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * User Profile Modal Dialog launched when tapping the profile avatar button in the top app bar.
 */
@Composable
fun UserProfileDialog(
    userProfile: UserProfile?,
    userEmail: String?,
    currentUserId: String,
    modelsCount: Int,
    bookingsCount: Int,
    myBookings: List<ShootBooking> = emptyList(),
    savedProjects: List<ProjectDraftEntity> = emptyList(),
    onToggleProjectFavorite: (ProjectDraftEntity) -> Unit = {},
    onDeleteSavedProject: (ProjectDraftEntity) -> Unit = {},
    onSaveNewProject: (String, String, String) -> Unit = { _, _, _ -> },
    onSignOut: () -> Unit = {},
    isStudioLight: Boolean = false,
    onToggleTheme: () -> Unit = {},
    isSubmitting: Boolean = false,
    language: AppLanguage = AppLanguage.ENGLISH,
    onDismiss: () -> Unit = {},
    onDeleteMyData: () -> Unit = {},
    onUpdateProfile: (displayName: String, avatarUrl: String) -> Unit = { _, _ -> }
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "क्लाइंट प्रोफ़ाइल व प्रोजेक्ट्स" else "Client Profile & Studio Projects",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        fontSize = 14.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                UserProfileSectionContent(
                    userProfile = userProfile,
                    userEmail = userEmail,
                    currentUserId = currentUserId,
                    modelsCount = modelsCount,
                    bookingsCount = bookingsCount,
                    myBookings = myBookings,
                    savedProjects = savedProjects,
                    onToggleProjectFavorite = onToggleProjectFavorite,
                    onDeleteSavedProject = onDeleteSavedProject,
                    onSaveNewProject = onSaveNewProject,
                    onSignOut = onSignOut,
                    isStudioLight = isStudioLight,
                    onToggleTheme = onToggleTheme,
                    isSubmitting = isSubmitting,
                    language = language,
                    onDeleteMyData = onDeleteMyData,
                    onUpdateProfile = onUpdateProfile
                )
            }
        },
        confirmButton = {},
        containerColor = StudioBlack,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .testTag("user_profile_dialog")
    )
}
