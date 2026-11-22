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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import com.example.ui.components.StudioVisualAnalyticsDashboard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.model.AppLanguage
import com.example.data.model.BlogPost
import com.example.data.model.GalleryItem
import com.example.data.model.ModelRegistration
import com.example.data.model.ShootBooking
import com.example.data.model.StudioAnnouncement
import com.example.ui.auth.GoogleLogoBadge
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.auth.signOutUser
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.util.StudioConstants
import com.example.util.StudioContactDetails
import com.example.util.StudioStrings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.components.AdminBookingRequestsComponent
import com.example.ui.components.AdminDashboardMetrics
import com.example.ui.components.AdminEmailPreviewSection
import com.example.ui.components.AdminItemStatus
import com.example.ui.components.AdminModelVerificationCard
import com.example.ui.components.AdminModelVerificationSection
import com.example.ui.components.AdminRealtimeBookingTrackerCard
import com.example.ui.components.AdminSkeletonList
import com.example.ui.components.AdminStatusBadge
import com.example.ui.components.AdminSummaryDashboard
import com.example.ui.components.GoldLoadingSpinner
import com.example.ui.components.RealtimeBookingTrackerUiState
import com.example.ui.components.StatusFilterChip
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun AdminDashboardScreen(
    language: AppLanguage,
    isAdmin: Boolean,
    allModels: List<ModelRegistration>,
    allBookings: List<ShootBooking>,
    allGalleryItems: List<GalleryItem>,
    allBlogPosts: List<BlogPost> = emptyList(),
    allAnnouncements: List<StudioAnnouncement> = emptyList(),
    isLoading: Boolean = false,
    onUpdateModelStatus: (modelId: String, status: String, adminNotes: String) -> Unit,
    onUpdateBookingStatus: (bookingId: String, status: String) -> Unit,
    onDeleteBooking: (bookingId: String) -> Unit = {},
    onUpdateGalleryStatus: (itemId: String, status: String) -> Unit,
    onDeleteGalleryItem: (itemId: String) -> Unit,
    onPublishPost: (title: String, category: String, content: String, summary: String, imageUrl: String, tags: String, status: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onUpdatePost: (id: String, title: String, category: String, content: String, summary: String, imageUrl: String, tags: String, status: String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeletePost: (id: String) -> Unit = {},
    onPublishAnnouncement: (title: String, message: String, category: String, priority: String, actionRoute: String, onSuccess: () -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteAnnouncement: (announcementId: String) -> Unit = {},
    onAdminAuthSuccess: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onOpenFullAnalytics: () -> Unit = {},
    onSignOut: () -> Unit = {},
    bookingTrackerState: RealtimeBookingTrackerUiState = RealtimeBookingTrackerUiState(),
    onToggleBookingTracker: () -> Unit = {},
    onCheckBookingStatusesNow: () -> Unit = {}
) {
    val s = StudioStrings(language)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // If not authenticated as admin, render the dedicated Secure Admin Login Page
    if (!isAdmin) {
        AdminLoginPage(
            language = language,
            onSignInClick = { onFinished, onError ->
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        onAdminAuthSuccess()
                        onFinished()
                    },
                    onAuthError = { err ->
                        onFinished()
                        onError(err)
                    },
                    scope = coroutineScope,
                    onAuthCancelled = {
                        onFinished()
                    }
                )
            },
            onSwitchAccountClick = onSignOut,
            onBackToHomeClick = onNavigateToHome
        )
        return
    }

    // Authenticated Admin Dashboard Console
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("admin_dashboard_screen")
    ) {
        // Admin Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .border(
                    BorderStroke(1.dp, Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.6f), StudioOutline))),
                    RoundedCornerShape(14.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GoldContainer)
                            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Verified",
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = s.adminTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                        val currentAdminEmail = remember {
                            runCatching { Firebase.auth.currentUser?.email }.getOrNull()
                        }
                        Text(
                            text = "Admin: ${currentAdminEmail ?: StudioContactDetails.ADMIN_EMAIL}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF282010))
                        .border(1.dp, GoldPrimary, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "VERIFIED",
                        color = GoldPrimary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Real-Time Studio Operational Summary Metrics Dashboard
        val summaryMetrics = remember(allModels, allBookings, allGalleryItems, allBlogPosts) {
            AdminDashboardMetrics.compute(
                models = allModels,
                bookings = allBookings,
                galleryItems = allGalleryItems,
                blogPosts = allBlogPosts
            )
        }

        AdminSummaryDashboard(
            metrics = summaryMetrics,
            language = language,
            onSelectTab = { targetTab ->
                selectedTab = targetTab
            }
        )

        // Tabs: Models, Bookings, Gallery Review, Blog Posts, Visual Analytics
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp,
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
                    Text(
                        text = "${s.tabModels} (${allModels.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 0) GoldPrimary else Color.Gray
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "${s.tabBookings} (${allBookings.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 1) GoldPrimary else Color.Gray
                    )
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Text(
                        text = "${s.tabGalleryMgmt} (${allGalleryItems.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 2) GoldPrimary else Color.Gray
                    )
                }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = {
                    Text(
                        text = "${s.tabBlogMgmt} (${allBlogPosts.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 3) GoldPrimary else Color.Gray
                    )
                }
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = {
                    Text(
                        text = "📊 ${s.tabAnalytics}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 4) GoldPrimary else Color.Gray
                    )
                }
            )
            Tab(
                selected = selectedTab == 5,
                onClick = { selectedTab = 5 },
                modifier = Modifier.testTag("admin_tab_announcements"),
                text = {
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "📣 अलर्ट (${allAnnouncements.size})"
                        else
                            "📣 Alerts (${allAnnouncements.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 5) GoldPrimary else Color.Gray
                    )
                }
            )
            Tab(
                selected = selectedTab == 6,
                onClick = { selectedTab = 6 },
                modifier = Modifier.testTag("admin_tab_email_preview"),
                text = {
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "✉️ ईमेल प्रीव्यू"
                        else
                            "✉️ Email Preview",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 6) GoldPrimary else Color.Gray
                    )
                }
            )
        }

        when (selectedTab) {
            0 -> {
                // Dedicated Manual Model Verification & Official Studio Roster Section
                AdminModelVerificationSection(
                    models = allModels,
                    isLoading = isLoading,
                    language = language,
                    onUpdateModelStatus = onUpdateModelStatus
                )
            }
            1 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GoldContainer.copy(alpha = 0.25f))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("admin_fcm_booking_notice"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "स्थिति बदलने पर ग्राहक को रीयल-टाइम FCM अपॉइंटमेंट अलर्ट स्वतः भेजा जाता है।"
                            else
                                "Updating a booking status dispatches a real-time FCM appointment alert to the client.",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldLight
                        )
                    }
                    AdminRealtimeBookingTrackerCard(
                        bookings = allBookings,
                        trackerState = bookingTrackerState,
                        language = language,
                        onToggleAutoComplete = onToggleBookingTracker,
                        onCheckNow = onCheckBookingStatusesNow
                    )
                    AdminBookingRequestsComponent(
                        bookings = allBookings,
                        isLoading = isLoading,
                        language = language,
                        onUpdateStatus = onUpdateBookingStatus,
                        onDeleteBooking = onDeleteBooking
                    )
                }
            }
            2 -> {
                // Gallery Review
                if (isLoading) {
                    AdminSkeletonList()
                } else if (allGalleryItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "कोई गैलरी मीडिया समीक्षा हेतु नहीं है।" else "No gallery media items awaiting review.",
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 14.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allGalleryItems) { item ->
                            AdminGalleryCard(
                                item = item,
                                language = language,
                                onUpdateStatus = onUpdateGalleryStatus,
                                onDelete = onDeleteGalleryItem
                            )
                        }
                    }
                }
            }
            3 -> {
                // Studio Blog Posts Management
                var blogFilter by remember { mutableStateOf("ALL") }
                var showCreateDialog by remember { mutableStateOf(false) }
                var postToEdit by remember { mutableStateOf<BlogPost?>(null) }

                val filteredBlogPosts = remember(allBlogPosts, blogFilter) {
                    when (blogFilter) {
                        "PUBLISHED" -> allBlogPosts.filter { it.status.equals("published", ignoreCase = true) }
                        "DRAFT" -> allBlogPosts.filter { it.status.equals("draft", ignoreCase = true) }
                        else -> allBlogPosts
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Actions & Filter row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusFilterChip(
                                label = if (language == AppLanguage.HINDI) "सभी" else "All",
                                count = allBlogPosts.size,
                                isSelected = blogFilter == "ALL",
                                indicatorColor = null,
                                onClick = { blogFilter = "ALL" },
                                testTag = "admin_blog_filter_all"
                            )
                            StatusFilterChip(
                                label = if (language == AppLanguage.HINDI) "प्रकाशित" else "Published",
                                count = allBlogPosts.count { it.status.equals("published", ignoreCase = true) },
                                isSelected = blogFilter == "PUBLISHED",
                                indicatorColor = Color(0xFF10B981),
                                onClick = { blogFilter = "PUBLISHED" },
                                testTag = "admin_blog_filter_published"
                            )
                            StatusFilterChip(
                                label = if (language == AppLanguage.HINDI) "ड्राफ्ट" else "Drafts",
                                count = allBlogPosts.count { it.status.equals("draft", ignoreCase = true) },
                                isSelected = blogFilter == "DRAFT",
                                indicatorColor = Color(0xFFFBBF24),
                                onClick = { blogFilter = "DRAFT" },
                                testTag = "admin_blog_filter_draft"
                            )
                        }

                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("admin_new_post_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "नया लेख" else "+ New Post",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (filteredBlogPosts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "कोई ब्लॉग लेख उपलब्ध नहीं है।" else "No blog articles found.",
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredBlogPosts, key = { it.id.ifBlank { it.title } }) { post ->
                                BlogPostCard(
                                    post = post,
                                    language = language,
                                    isAdmin = true,
                                    onClick = { postToEdit = post },
                                    onEditClick = { postToEdit = post },
                                    onDeleteClick = { onDeletePost(post.id) }
                                )
                            }
                        }
                    }
                }

                if (showCreateDialog) {
                    BlogPostFormDialog(
                        language = language,
                        isSubmitting = false,
                        existingPost = null,
                        onDismiss = { showCreateDialog = false },
                        onSubmit = { title, cat, content, summary, imageUrl, tags, status ->
                            onPublishPost(title, cat, content, summary, imageUrl, tags, status)
                            showCreateDialog = false
                        }
                    )
                }

                postToEdit?.let { post ->
                    BlogPostFormDialog(
                        language = language,
                        isSubmitting = false,
                        existingPost = post,
                        onDismiss = { postToEdit = null },
                        onSubmit = { title, cat, content, summary, imageUrl, tags, status ->
                            onUpdatePost(post.id, title, cat, content, summary, imageUrl, tags, status)
                            postToEdit = null
                        }
                    )
                }
            }
            4 -> {
                // Visual Analytics Dashboard (Recharts-inspired)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("admin_analytics_tab_content")
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenFullAnalytics,
                                border = BorderStroke(1.dp, GoldPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_analytics_open_fullscreen_button")
                            ) {
                                Text(
                                    text = if (language == AppLanguage.HINDI)
                                        "पूर्ण-स्क्रीन Recharts एनालिटिक्स खोलें"
                                    else
                                        "Open Dedicated Recharts Analytics Screen",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    item {
                        StudioVisualAnalyticsDashboard(
                            bookings = allBookings,
                            language = language,
                            includeStudioBaseline = false
                        )
                    }
                }
            }
            5 -> {
                // Studio Announcements Broadcast & History (FCM Topic: studio_announcements)
                var showAnnouncementComposer by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("admin_announcements_tab_content")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "स्टूडियो FCM प्रसारण" else "FCM Topic Broadcasts",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                            Text(
                                text = "Topic: studio_announcements",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary
                            )
                        }

                        Button(
                            onClick = { showAnnouncementComposer = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color(0xFF141000)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("admin_broadcast_announcement_btn")
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "नई घोषणा भेजें" else "+ Broadcast",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (allAnnouncements.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (language == AppLanguage.HINDI)
                                    "अभी तक कोई स्टूडियो घोषणा प्रसारित नहीं की गई है।"
                                else
                                    "No studio announcements broadcast yet. Tap + Broadcast to notify all subscribed users.",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(allAnnouncements, key = { it.id }) { announcement ->
                                StudioAnnouncementCard(
                                    announcement = announcement,
                                    language = language,
                                    isAdmin = true,
                                    onDelete = { onDeleteAnnouncement(announcement.id) },
                                    onOpenAction = {}
                                )
                            }
                        }
                    }
                }

                if (showAnnouncementComposer) {
                    StudioAnnouncementComposerDialog(
                        language = language,
                        isSubmitting = false,
                        onDismiss = { showAnnouncementComposer = false },
                        onPublish = { title, message, category, priority, actionRoute ->
                            onPublishAnnouncement(title, message, category, priority, actionRoute) {
                                showAnnouncementComposer = false
                            }
                        }
                    )
                }
            }
            6 -> {
                AdminEmailPreviewSection(
                    bookings = allBookings,
                    models = allModels,
                    language = language
                )
            }
        }
    }
}

/**
 * Dedicated Secure Admin Login Page
 */
@Composable
fun AdminLoginPage(
    language: AppLanguage,
    onSignInClick: (onFinished: () -> Unit, onError: (String) -> Unit) -> Unit,
    onSwitchAccountClick: () -> Unit,
    onBackToHomeClick: () -> Unit
) {
    val s = StudioStrings(language)
    val currentUser = remember { runCatching { Firebase.auth.currentUser }.getOrNull() }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .padding(horizontal = 20.dp)
            .testTag("admin_login_screen"),
        contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Security Lock & Shield Icon
        item {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(GoldPrimary, GoldDark))
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(StudioBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Admin Security Shield",
                        tint = GoldPrimary,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        // 2. Title & Subtitle
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = s.adminLoginTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = s.adminLoginSub,
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldLight.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 3. Authorized Admin Invariant Restriction Card
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
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RESTRICTED ACCESS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2C2210))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "RBAC SECURED",
                                color = GoldPrimary,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = s.adminAuthorizedNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE0E0E0),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Authorized Admin: ${StudioContactDetails.ADMIN_EMAIL}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Current Session State Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF201313)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s.adminAccessDeniedLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${s.adminCurrentAccountLabel}: ${currentUser?.email ?: "Unauthenticated"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC0C0C0)
                    )
                }
            }
        }

        // Error Feedback Message
        if (authError != null) {
            item {
                Text(
                    text = authError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 5. Primary Admin Sign-In & Account Switch Buttons
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sign In / Re-authenticate Button
                Button(
                    onClick = {
                        isAuthenticating = true
                        authError = null
                        onSignInClick(
                            { isAuthenticating = false },
                            { err ->
                                isAuthenticating = false
                                authError = err
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("admin_google_signin_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color(0xFF141000)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isAuthenticating
                ) {
                    if (isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = StudioBlack,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GoogleLogoBadge()
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = s.adminSignInWithGoogle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Switch Account Button
                OutlinedButton(
                    onClick = onSwitchAccountClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_switch_account_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GoldLight
                    ),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = GoldPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.adminSwitchAccount,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Return to Studio Home Button
                Button(
                    onClick = onBackToHomeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_back_home_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioElevatedSurface,
                        contentColor = Color(0xFFD0D0D0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.adminBackToHome,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // 6. Zero-Trust Security Protocol Info
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SECURITY PROTOCOL & AUDIT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = s.adminSecurityPolicy,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAAAAA),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AdminModelCard(
    model: ModelRegistration,
    language: AppLanguage = AppLanguage.ENGLISH,
    onUpdate: (String, String, String) -> Unit
) {
    AdminModelVerificationCard(
        model = model,
        language = language,
        onUpdate = onUpdate
    )
}

@Composable
fun AdminGalleryCard(
    item: GalleryItem,
    language: AppLanguage = AppLanguage.ENGLISH,
    onUpdateStatus: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    val itemStatus = AdminItemStatus.from(item.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            itemStatus.primaryColor.copy(alpha = 0.6f),
                            itemStatus.borderColor.copy(alpha = 0.3f),
                            StudioOutline
                        )
                    )
                ),
                RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                    Text(
                        text = "Category: ${item.category} • Format: ${item.mediaType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFAAAAAA)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (item.isDemo) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFBF360C))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("DEMO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Visual Status Indicator Badge
                    AdminStatusBadge(
                        statusStr = item.status,
                        language = language
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.status != "approved") {
                    Button(
                        onClick = { onUpdateStatus(item.id, "approved") },
                        modifier = Modifier.weight(1f).testTag("gallery_approve_${item.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (item.status != "rejected") {
                    Button(
                        onClick = { onUpdateStatus(item.id, "rejected") },
                        modifier = Modifier.weight(1f).testTag("gallery_reject_${item.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = { onDelete(item.id) },
                    modifier = Modifier.testTag("gallery_delete_${item.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
