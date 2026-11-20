package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.basicMarquee
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.ServicePackage
import com.example.data.model.ServiceReview
import com.example.data.model.StudioAnnouncement
import com.example.ui.components.ScrollToTopFab
import com.example.ui.components.ServicesOfferedGridSection
import com.example.ui.components.StudioAddressMapCard
import com.example.ui.components.StudioReviewsShowcaseCard
import com.example.ui.components.makeStudioCall
import com.example.ui.components.openWhatsApp
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.StudioScreen
import com.example.data.local.entity.StudioInfoEntity
import com.example.data.repository.StudioInfoRepository
import com.example.util.ShareHelper
import com.example.util.StudioConstants
import com.example.util.StudioContactDetails
import com.example.util.StudioStrings

@Composable
fun HomeScreen(
    language: AppLanguage,
    isAdmin: Boolean = false,
    onNavigate: (StudioScreen) -> Unit,
    onBookService: (String) -> Unit = { onNavigate(StudioScreen.ShootBooking) },
    onOpenVoiceBooking: () -> Unit = {},
    cachedStudioInfo: StudioInfoEntity = StudioInfoRepository.defaultStudioInfo(),
    studioReviews: List<ServiceReview> = emptyList(),
    latestAnnouncement: StudioAnnouncement? = null,
    unreadNotificationsCount: Int = 0,
    favoritePackageIds: Set<String> = emptySet(),
    onToggleFavoritePackage: (ServicePackage) -> Unit = {},
    onOpenProjectCalendar: () -> Unit = { onNavigate(StudioScreen.ProjectStatusTracker) }
) {
    val s = StudioStrings(language)
    val context = LocalContext.current
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen"),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
        // =====================================================================
        // 1. HERO BANNER
        // =====================================================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .testTag("hero_banner_section")
            ) {
                // Cinematic Hero Background Image (decoded asynchronously off the UI thread)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(R.drawable.studio_hero_banner_1793955301083)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Star Artist Studio Hero Production Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Premium Multi-Layer Dark and Gold Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    StudioBlack.copy(alpha = 0.55f),
                                    StudioBlack.copy(alpha = 0.92f),
                                    StudioBlack
                                )
                            )
                        )
                )

                // Hero Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Location & Establishment Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(GoldPrimary, GoldDark)
                                    )
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "EST. 125102 • SIRSA, HARYANA",
                                color = Color(0xFF141000),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF231F14))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "CINEMA & MODELING",
                                color = GoldLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Studio Title in Gold Primary
                    Text(
                        text = StudioConstants.STUDIO_NAME,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldPrimary,
                        letterSpacing = 1.5.sp
                    )

                    Text(
                        text = s.heroTagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary.copy(alpha = 0.95f),
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // =====================================================================
        // 1.5. HERO AUTO-SCROLLING MARQUEE (STUDIO AVAILABILITY & VERIFIED INFO)
        // =====================================================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (StudioTheme.isLight) Color(0xFFF6EEDC) else Color(0xFF19140A))
                    .border(BorderStroke(0.5.dp, GoldPrimary.copy(alpha = 0.35f)))
                    .padding(vertical = 8.dp, horizontal = 12.dp)
                    .testTag("hero_marquee_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = Int.MAX_VALUE),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "📍 स्टार आर्टिस्ट स्टूडियो • तलवाड़ा खुर्द, ऐलनाबाद, सिरसा, हरियाणा - 125102 • 📸 पेशेवर फोटोग्राफी और 4K सिनेमा वीडियो प्रोडक्शन • 🕒 समय: सुबह 10:00 से रात 8:00 (प्रतिदिन) • मोबाइल: 9812890081 • व्हाट्सऐप: 9541024098 •"
                        else
                            "📍 STAR ARTIST STUDIO • Talwara Khurd, Ellenabad, Sirsa, Haryana - 125102 • 📸 Professional Photography & 4K Cinema Video Production • 🕒 Hours: 10:00 AM - 8:00 PM (Everyday) • Mobile: 9812890081 • WhatsApp: 9541024098 •",
                        color = if (StudioTheme.isLight) GoldPrimary else GoldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }

        // =====================================================================
        // 1.75. LIVE STUDIO ANNOUNCEMENT & FCM ALERTS BANNER
        // =====================================================================
        if (latestAnnouncement != null || unreadNotificationsCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(
                            BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.65f)),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onNavigate(StudioScreen.Notifications) }
                        .testTag("home_latest_announcement_banner"),
                    colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GoldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (latestAnnouncement != null) Icons.Default.Campaign else Icons.Default.NotificationsActive,
                                    contentDescription = "Studio Announcement",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = latestAnnouncement?.let { "${it.category.uppercase()} • STUDIO ALERT" }
                                        ?: if (language == AppLanguage.HINDI)
                                            "$unreadNotificationsCount नया अपॉइंटमेंट अपडेट"
                                        else
                                            "$unreadNotificationsCount NEW APPOINTMENT UPDATE(S)",
                                    color = GoldPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp
                                )
                                Text(
                                    text = latestAnnouncement?.title ?: if (language == AppLanguage.HINDI)
                                        "अपनी शूट बुकिंग की ताज़ा स्थिति देखें"
                                    else
                                        "Tap to inspect real-time shoot status updates",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                            onClick = { onNavigate(StudioScreen.Notifications) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color(0xFF141000)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("home_all_alerts_button")
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "सभी अलर्ट" else "All Alerts",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 2. QUICK ACTION CALL & WHATSAPP ROW
        // =====================================================================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Direct Mobile Call Button
                Button(
                    onClick = { makeStudioCall(context, "tel:+919812890081") },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("call_button_home"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color(0xFF141000)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call Studio Mobile",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Call: 9812890081",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Direct WhatsApp Button
                Button(
                    onClick = { openWhatsApp(context, "https://wa.me/919541020948") },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("whatsapp_button_home"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "WhatsApp Studio",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WhatsApp: 9541024098",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // =====================================================================
        // 2.5. VOICE-CONTROLLED BOOKING QUICK LAUNCHER
        // =====================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .border(
                        BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(GoldPrimary, StudioOutline))),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onOpenVoiceBooking() }
                    .testTag("home_voice_book_button"),
                colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Book Shoot by Voice",
                                tint = StudioBlack,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.HINDI)
                                    "आवाज़ से शूट बुक करें (Voice Booking)"
                                else
                                    "Book Shoot by Voice Command",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldLight
                            )
                            Text(
                                text = if (language == AppLanguage.HINDI)
                                    "बोलें: \"कल शाम 4:30 बजे मॉडलिंग शूट बुक करें\""
                                else
                                    "Say: \"Book a modeling shoot tomorrow at 4:30 PM\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFC2C2C2),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // =====================================================================
        // 3. STUDIO INTRODUCTION SECTION
        // =====================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(1.dp, StudioOutline, RoundedCornerShape(18.dp))
                    .testTag("studio_introduction_section"),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Header with Gold Accent Line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(GoldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = s.studioIntroTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF282010))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "OFFICIAL PROFILE",
                                color = GoldPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tagline Highlight
                    Text(
                        text = s.studioIntroTagline,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldPrimary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Comprehensive Studio Introduction Body
                    Text(
                        text = s.studioIntroBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD6D6D6),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Studio Production Showcase Snippet
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioElevatedSurface)
                            .border(1.dp, Color(0xFF2E2616), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(R.drawable.studio_model_portrait_1793955328333)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Studio Talent Showcase",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "सिग्नेचर इन-हाउस प्रोडक्शन" else "Signature In-House Production",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "4K सिनेमैटिक लाइटिंग स्टेज, तलवाड़ा खुर्द" else "4K Cinematic Lighting Stage, Talwara Khurd",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFAAAAAA),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Creative Director Quote
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B1914))
                            .border(1.dp, GoldDark.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Director's Note",
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s.studioDirectorNote,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = GoldLight.copy(alpha = 0.95f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 4. GRID-BASED NAVIGATION TO STUDIO FEATURES
        // =====================================================================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("features_grid_section")
            ) {
                // Section Title & Subtitle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = s.quickNavTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    Text(
                        text = if (language == AppLanguage.HINDI) "सभी सुविधाएं" else "All Features",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = s.quickNavSub,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Grid Row 1: Bookings & Studio Gallery
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = s.navGridBookShoot,
                        subtitle = s.navGridBookShootSub,
                        badge = "INSTANT",
                        icon = Icons.Default.CameraAlt,
                        testTag = "nav_grid_bookings",
                        onClick = { onNavigate(StudioScreen.ShootBooking) }
                    )

                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = s.navGridGallery,
                        subtitle = s.navGridGallerySub,
                        badge = "4K WORKS",
                        icon = Icons.Default.PhotoLibrary,
                        testTag = "nav_grid_gallery",
                        onClick = { onNavigate(StudioScreen.Gallery) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grid Row 2: AI Studio & Model Registration (18+)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = s.navGridAiStudio,
                        subtitle = s.navGridAiStudioSub,
                        badge = "GEMINI AI",
                        icon = Icons.Default.AutoAwesome,
                        testTag = "nav_grid_ai_studio",
                        onClick = { onNavigate(StudioScreen.AiStudio) }
                    )

                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = s.navGridModelReg,
                        subtitle = s.navGridModelRegSub,
                        badge = "18+ ROSTER",
                        icon = Icons.Default.Person,
                        testTag = "nav_grid_model_reg",
                        onClick = { onNavigate(StudioScreen.ModelRegistration) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grid Row 3: Admin Console & Contact & Location
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = s.navGridAdmin,
                        subtitle = s.navGridAdminSub,
                        badge = if (isAdmin) "ADMIN VERIFIED" else "ADMIN ONLY",
                        icon = Icons.Default.AdminPanelSettings,
                        testTag = "nav_grid_admin",
                        onClick = { onNavigate(StudioScreen.AdminDashboard) }
                    )

                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = s.navGridContact,
                        subtitle = s.navGridContactSub,
                        badge = "TALWARA KHURD",
                        icon = Icons.Default.LocationOn,
                        testTag = "nav_grid_contact",
                        onClick = { onNavigate(StudioScreen.Contact) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grid Row 4: Studio Blog & Artist Profile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = if (language == AppLanguage.HINDI) "स्टूडियो ब्लॉग व टिप्स" else "Studio Blog & Tips",
                        subtitle = if (language == AppLanguage.HINDI) "समाचार, लाइटिंग व BTS" else "News, lighting & BTS",
                        badge = "NEWS & TIPS",
                        icon = Icons.Default.Newspaper,
                        testTag = "nav_grid_blog",
                        onClick = { onNavigate(StudioScreen.Blog) }
                    )

                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = if (language == AppLanguage.HINDI) "कलाकार प्रोफ़ाइल" else "Artist Profile",
                        subtitle = if (language == AppLanguage.HINDI) "संपर्क, पोर्टफोलियो व बायो" else "Contact, portfolio & bio",
                        badge = "MY PROFILE",
                        icon = Icons.Default.Person,
                        testTag = "nav_grid_profile",
                        onClick = { onNavigate(StudioScreen.UserProfile) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grid Row 5: Full Service Catalog & My Bookings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = if (language == AppLanguage.HINDI) "सेवा कैटलॉग व मूल्य" else "Service Catalog",
                        subtitle = if (language == AppLanguage.HINDI)
                            "फोटोग्राफी, मॉडलिंग व 4K टियर"
                        else
                            "Compare studio pricing tiers",
                        badge = "3 TIERS",
                        icon = Icons.Default.WorkspacePremium,
                        testTag = "nav_grid_service_catalog",
                        onClick = { onNavigate(StudioScreen.ServiceCatalog) }
                    )

                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = if (language == AppLanguage.HINDI) "मेरी बुकिंग्स" else "My Bookings",
                        subtitle = if (language == AppLanguage.HINDI)
                            "आगामी व पिछली शूटिंग स्थिति"
                        else
                            "Past & upcoming appointments",
                        badge = "LIVE SYNC",
                        icon = Icons.Default.EventAvailable,
                        testTag = "nav_grid_my_bookings",
                        onClick = { onNavigate(StudioScreen.MyBookings) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grid Row 6: Recharts Booking & Service Analytics + Project Portfolio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = if (language == AppLanguage.HINDI) "बुकिंग एनालिटिक्स" else "Booking Analytics",
                        subtitle = if (language == AppLanguage.HINDI)
                            "Recharts ट्रेंड व लोकप्रिय सेवाएं"
                        else
                            "Recharts trends & service demand",
                        badge = "FIRESTORE LIVE",
                        icon = Icons.Default.BarChart,
                        testTag = "nav_grid_analytics",
                        onClick = { onNavigate(StudioScreen.Analytics) }
                    )

                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = if (language == AppLanguage.HINDI) "प्रोजेक्ट पोर्टफोलियो" else "Project Portfolio",
                        subtitle = if (language == AppLanguage.HINDI)
                            "फोटो व 4K वीडियो थंबनेल (Room)"
                        else
                            "Photo & 4K video thumbnails",
                        badge = "ROOM ARCHIVE",
                        icon = Icons.Default.Videocam,
                        testTag = "nav_grid_project_portfolio",
                        onClick = { onNavigate(StudioScreen.PortfolioProjects) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grid Row 7: Photo Preview Studio Camera & Studio Notifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = if (language == AppLanguage.HINDI) "फोटो प्रीव्यू कैमरा" else "Photo Preview Cam",
                        subtitle = if (language == AppLanguage.HINDI)
                            "पोर्टफोलियो व कैटलॉग संदर्भ शॉट"
                        else
                            "Quick portfolio & catalog shots",
                        badge = "CAMERAX LIVE",
                        icon = Icons.Default.CameraAlt,
                        testTag = "nav_grid_photo_preview",
                        onClick = { onNavigate(StudioScreen.PhotoPreview) }
                    )

                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = if (language == AppLanguage.HINDI) "स्टूडियो अलर्ट व सूचना" else "Studio Alerts",
                        subtitle = if (language == AppLanguage.HINDI)
                            "बुकिंग अपडेट व स्टूडियो घोषणाएं"
                        else
                            "Booking status & announcements",
                        badge = "REAL-TIME",
                        icon = Icons.Default.NotificationsActive,
                        testTag = "nav_grid_notifications",
                        onClick = { onNavigate(StudioScreen.Notifications) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grid Row 8: Room Session Production Tracker & Mood Board Image AI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = if (language == AppLanguage.HINDI) "प्रोजेक्ट स्टेटस ट्रैकर" else "Session Tracker",
                        subtitle = if (language == AppLanguage.HINDI)
                            "योजना, शूटिंग व पूर्ण स्थिति (Room)"
                        else
                            "Planning, In Production & Completed",
                        badge = "ROOM WORKFLOW",
                        icon = Icons.Default.AssignmentTurnedIn,
                        testTag = "nav_grid_project_tracker",
                        onClick = { onNavigate(StudioScreen.ProjectStatusTracker) }
                    )

                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = if (language == AppLanguage.HINDI) "मूड बोर्ड आर्ट" else "Mood Board Art",
                        subtitle = if (language == AppLanguage.HINDI)
                            "AI कॉन्सेप्ट आर्ट जेनरेशन"
                        else
                            "AI concept art for project boards",
                        badge = "IMAGEN AI",
                        icon = Icons.Default.Palette,
                        testTag = "nav_grid_mood_board",
                        onClick = { onNavigate(StudioScreen.MoodBoardGenerator) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grid Row 9: Dedicated Project Mood Board Organizer & Monthly Studio Schedule Calendar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = if (language == AppLanguage.HINDI) "प्रोजेक्ट मूड बोर्ड" else "Mood Board",
                        subtitle = if (language == AppLanguage.HINDI)
                            "कॉन्सेप्ट आर्ट व रेफरेंस फ़ोटो बोर्ड"
                        else
                            "Organize art & ref photos",
                        badge = "ORGANIZE",
                        icon = Icons.Default.DashboardCustomize,
                        testTag = "nav_grid_mood_board_workspace",
                        onClick = { onNavigate(StudioScreen.MoodBoard) }
                    )

                    StudioGridButton(
                        modifier = Modifier.weight(1f),
                        title = if (language == AppLanguage.HINDI) "स्टूडियो कैलेंडर" else "Studio Calendar",
                        subtitle = if (language == AppLanguage.HINDI)
                            "मासिक शूट व डेडलाइन शेड्यूल"
                        else
                            "Monthly shoots & deadlines",
                        badge = "SCHEDULE",
                        icon = Icons.Default.CalendarMonth,
                        testTag = "nav_grid_project_calendar",
                        onClick = onOpenProjectCalendar
                    )
                }
            }
        }

        // =====================================================================
        // 5. OFFICIAL STUDIO DETAILS CARD (STRICT INVARIANT)
        // =====================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
                    .testTag("official_details_card"),
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(GoldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "आधिकारिक स्टूडियो पता" else "Official Studio Address",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF252014))
                                .clickable { onNavigate(StudioScreen.Contact) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "विवरण" else "Details",
                                    color = GoldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = GoldLight,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Strict Official Address (Exact Required String)
                    Text(
                        text = "Star Artist Studio, Talwara Khurd, Tehsil Ellenabad, District Sirsa, Haryana, India - 125102.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E2E2),
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Visual Map Representation Card (Clickable to open device map app)
                    StudioAddressMapCard(language = language)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mobile: 9812890081",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "WhatsApp: 9541024098",
                                style = MaterialTheme.typography.bodySmall,
                                color = WhatsAppGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "Hours: 10:00 AM - 8:00 PM (Everyday)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAAAAAA),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 6. SERVICES OFFERED SECTION (LazyVerticalGrid with Photography, Modeling, Production & Pricing)
        // =====================================================================
        item {
            ServicesOfferedGridSection(
                language = language,
                favoritePackageIds = favoritePackageIds,
                onBookPackage = { pkg ->
                    onBookService(pkg.recommendedServiceType)
                },
                onToggleFavorite = onToggleFavoritePackage,
                onOpenFullCatalog = {
                    onNavigate(StudioScreen.ServiceCatalog)
                }
            )
        }

        // =====================================================================
        // 6B. VERIFIED STUDIO CLIENT RATINGS & REVIEWS
        // =====================================================================
        item {
            StudioReviewsShowcaseCard(
                reviews = studioReviews,
                language = language,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // =====================================================================
        // 7. VERIFIED STUDIO CAPABILITIES & FACILITIES
        // =====================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(1.dp, StudioOutline, RoundedCornerShape(16.dp))
                    .testTag("verified_capabilities_card"),
                colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatBadge(
                        number = "4K UHD",
                        label = if (language == AppLanguage.HINDI) "सिनेमा प्रोडक्शन" else "Cinema Video"
                    )
                    StatBadge(
                        number = "PRO",
                        label = if (language == AppLanguage.HINDI) "लाइटिंग स्टेज" else "Lighting Bay"
                    )
                    StatBadge(
                        number = "18+",
                        label = if (language == AppLanguage.HINDI) "मॉडल नेटवर्क" else "Model Roster"
                    )
                    StatBadge(
                        number = "DAILY",
                        label = if (language == AppLanguage.HINDI) "10 AM - 8 PM" else "10 AM - 8 PM"
                    )
                }
            }
        }

        // =====================================================================
        // 7B. CLIENT & MODEL TESTIMONIAL CAROUSEL
        // =====================================================================
        item(key = "home_testimonial_carousel") {
            HomeTestimonialCarousel(
                reviews = studioReviews,
                language = language,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // =====================================================================
        // 8. CONNECT SECTION (Instagram, Facebook, LinkedIn)
        // =====================================================================
        item(key = "home_connect_section") {
            val context = LocalContext.current
            HomeConnectSection(
                language = language,
                onOpenUrl = { url -> StudioContactDetails.openExternalUrl(context, url) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }

    // Floating Action Button scrolling back to top on long scroll
    ScrollToTopFab(
        listState = listState,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = 24.dp)
            .testTag("home_scroll_to_top_fab")
    )
}
}

/**
 * Grid Tile Button for Instant Feature Navigation
 */
@Composable
fun StudioGridButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .border(
                BorderStroke(1.dp, Brush.verticalGradient(listOf(GoldPrimary.copy(alpha = 0.5f), StudioOutline))),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF282212))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF332912))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = GoldLight,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA5A5A5),
                    fontSize = 10.sp,
                    maxLines = 2,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
fun ServiceCard(
    icon: ImageVector,
    title: String,
    desc: String,
    badge: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioOutline, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("service_card_$title"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF262010))
                    .border(1.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = GoldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF332912))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = GoldLight,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0A0A0)
                )
            }

            // Share Service Button
            val context = LocalContext.current
            IconButton(
                onClick = {
                    ShareHelper.shareService(
                        context = context,
                        title = title,
                        description = desc,
                        badge = badge
                    )
                },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("share_service_${title}")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share $title",
                    tint = GoldLight.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun StatBadge(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = number,
            color = GoldPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp
        )
        Text(
            text = label,
            color = Color(0xFF909090),
            fontSize = 10.sp
        )
    }
}

@Composable
fun HomeConnectSection(
    language: AppLanguage,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHindi = language == AppLanguage.HINDI
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .testTag("home_connect_section"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHindi) "Connect (हमसे जुड़ें)" else "Connect",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isHindi)
                            "कास्टिंग, शूट्स व रील्स के लिए Star Artist Studio से जुड़ें"
                        else
                            "Follow Star Artist Studio on Instagram, Facebook & LinkedIn",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAAAAA)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GoldContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "@starartiststudio",
                        color = GoldPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SocialConnectButton(
                    label = "Instagram",
                    platform = SocialPlatform.INSTAGRAM,
                    badgeColors = listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF)),
                    contentDescription = "Star Artist Studio on Instagram",
                    testTag = "connect_instagram_btn",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenUrl(StudioContactDetails.INSTAGRAM_URL) }
                )
                SocialConnectButton(
                    label = "Facebook",
                    platform = SocialPlatform.FACEBOOK,
                    badgeColors = listOf(Color(0xFF1877F2), Color(0xFF1256B5)),
                    contentDescription = "Star Artist Studio on Facebook",
                    testTag = "connect_facebook_btn",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenUrl(StudioContactDetails.FACEBOOK_URL) }
                )
                SocialConnectButton(
                    label = "LinkedIn",
                    platform = SocialPlatform.LINKEDIN,
                    badgeColors = listOf(Color(0xFF0A66C2), Color(0xFF084B91)),
                    contentDescription = "Star Artist Studio on LinkedIn",
                    testTag = "connect_linkedin_btn",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenUrl(StudioContactDetails.LINKEDIN_URL) }
                )
            }
        }
    }
}

private enum class SocialPlatform {
    INSTAGRAM, FACEBOOK, LINKEDIN
}

@Composable
private fun SocialConnectButton(
    label: String,
    platform: SocialPlatform,
    badgeColors: List<Color>,
    contentDescription: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 76.dp)
            .border(1.dp, StudioOutline, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .semantics { this.contentDescription = contentDescription }
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(badgeColors)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val s = size.minDimension / 24f
                    when (platform) {
                        SocialPlatform.INSTAGRAM -> {
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(2.5f * s, 2.5f * s),
                                size = Size(19f * s, 19f * s),
                                cornerRadius = CornerRadius(5f * s, 5f * s),
                                style = Stroke(width = 2.1f * s)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4.2f * s,
                                center = Offset(12f * s, 12f * s),
                                style = Stroke(width = 2.1f * s)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 1.3f * s,
                                center = Offset(17.2f * s, 6.8f * s)
                            )
                        }
                        SocialPlatform.FACEBOOK -> {
                            val path = PathParser()
                                .parsePathString("M14,13.5h2.5l1,-4H14v-2c0,-1.03 0,-2 2,-2h1.5V2.14c-0.326,-0.043 -1.557,-0.14 -2.857,-0.14C11.928,2 10,3.657 10,6.7v2.8H7v4h3V22h4v-8.5z")
                                .toPath()
                            drawContext.canvas.save()
                            drawContext.canvas.scale(s, s)
                            drawPath(path = path, color = Color.White)
                            drawContext.canvas.restore()
                        }
                        SocialPlatform.LINKEDIN -> {
                            val path = PathParser()
                                .parsePathString("M6.94,8.5H3.56V19.5H6.94V8.5zM5.25,3.5c-1.11,0 -2,0.89 -2,2s0.89,2 2,2c1.1,0 2,-0.89 2,-2s-0.9,-2 -2,-2zM20.44,13.02c0,-3.14 -1.68,-4.8 -4.28,-4.8c-1.79,0 -2.68,0.99 -3.14,1.71V8.5H9.65c0.05,0.98 0,11 0,11h3.37v-6.14c0,-0.34 0.03,-0.67 0.13,-0.91c0.27,-0.67 0.88,-1.36 1.92,-1.36c1.34,0 1.99,1.02 1.99,2.51V19.5h3.38V13.02z")
                                .toPath()
                            drawContext.canvas.save()
                            drawContext.canvas.scale(s, s)
                            drawPath(path = path, color = Color.White)
                            drawContext.canvas.restore()
                        }
                    }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = GoldLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
