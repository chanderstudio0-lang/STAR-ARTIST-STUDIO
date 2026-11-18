package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.data.model.AppointmentNotification
import com.example.data.model.StudioAnnouncement
import com.example.notification.StudioNotificationManager
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.StudioScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed class UnifiedFeedEntry(val sortMillis: Long) {
    class AppointmentEntry(val item: AppointmentNotification) :
        UnifiedFeedEntry(item.createdAt?.toDate()?.time ?: 0L)

    class AnnouncementEntry(val item: StudioAnnouncement) :
        UnifiedFeedEntry(item.createdAt?.toDate()?.time ?: 0L)
}

@Composable
fun NotificationsScreen(
    language: AppLanguage,
    isAdmin: Boolean,
    fcmToken: String,
    announcementsOptIn: Boolean,
    appointmentPushOptIn: Boolean,
    announcements: List<StudioAnnouncement>,
    appointmentNotifications: List<AppointmentNotification>,
    isSubmitting: Boolean = false,
    onBack: () -> Unit,
    onSyncFcm: () -> Unit,
    onUpdatePushPreferences: (announcementsOptIn: Boolean, appointmentPushOptIn: Boolean) -> Unit,
    onMarkNotificationRead: (notificationId: String) -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    onPublishAnnouncement: (title: String, message: String, category: String, priority: String, actionRoute: String, onSuccess: () -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteAnnouncement: (announcementId: String) -> Unit = {},
    onNavigateToScreen: (StudioScreen) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var hasPostPermission by remember {
        mutableStateOf(StudioNotificationManager.hasPostNotificationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPostPermission = granted
        if (granted) {
            onSyncFcm()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = Appointments, 2 = Announcements
    var showBroadcastDialog by remember { mutableStateOf(false) }

    val unreadCount = remember(appointmentNotifications) {
        appointmentNotifications.count { !it.isRead }
    }

    val unifiedFeed = remember(announcements, appointmentNotifications) {
        (appointmentNotifications.map { UnifiedFeedEntry.AppointmentEntry(it) } +
            announcements.map { UnifiedFeedEntry.AnnouncementEntry(it) })
            .sortedByDescending { it.sortMillis }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("notifications_screen")
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("notifications_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (language == AppLanguage.HINDI) "वापस" else "Back",
                        tint = GoldPrimary
                    )
                }
                Column {
                    Text(
                        text = if (language == AppLanguage.HINDI) "अलर्ट व स्टूडियो सूचनाएं" else "Alerts & Studio Feed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "Firebase Cloud Messaging द्वारा रीयल-टाइम अपडेट"
                        else
                            "Real-time FCM appointment & studio updates",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            if (unreadCount > 0) {
                TextButton(
                    onClick = onMarkAllNotificationsRead,
                    modifier = Modifier.testTag("mark_all_read_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "सभी पढ़ें" else "Mark Read ($unreadCount)",
                        color = GoldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Filter Tabs
        TabRow(
            selectedTabIndex = selectedTab,
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
                modifier = Modifier.testTag("notif_tab_all"),
                text = {
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "सभी (${unifiedFeed.size})"
                        else
                            "All (${unifiedFeed.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 0) GoldPrimary else TextSecondary
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.testTag("notif_tab_appointments"),
                text = {
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "अपॉइंटमेंट (${appointmentNotifications.size})"
                        else
                            "Appointments (${appointmentNotifications.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 1) GoldPrimary else TextSecondary
                    )
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                modifier = Modifier.testTag("notif_tab_announcements"),
                text = {
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "घोषणाएं (${announcements.size})"
                        else
                            "Announcements (${announcements.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == 2) GoldPrimary else TextSecondary
                    )
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.CenterHorizontally)
                .testTag("notifications_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Permission Banner on Android 13+ if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostPermission) {
                item(key = "push_permission_banner") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .testTag("notification_permission_card"),
                        colors = CardDefaults.cardColors(containerColor = GoldContainer.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = GoldPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI)
                                        "पुश नोटिफिकेशन अनुमति आवश्यक है"
                                    else
                                        "Enable Device Push Notifications",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI)
                                    "शूट बुकिंग पुष्टि और स्टूडियो घोषणाएं तुरंत पाने के लिए नोटिफिकेशन की अनुमति दें।"
                                else
                                    "Allow notification permission so you receive instant alerts when your shoot is Confirmed or a new Casting Call is announced.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = Color(0xFF141000)
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("enable_push_permission_button")
                            ) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "अनुमति चालू करें" else "Allow Push Alerts",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // FCM Device Token & Topic Subscription Preferences Card
            item(key = "fcm_preferences_card") {
                FcmSubscriptionPreferencesCard(
                    language = language,
                    fcmToken = fcmToken,
                    announcementsOptIn = announcementsOptIn,
                    appointmentPushOptIn = appointmentPushOptIn,
                    onSyncFcm = onSyncFcm,
                    onUpdatePushPreferences = onUpdatePushPreferences
                )
            }

            // Admin Quick Broadcast CTA
            if (isAdmin) {
                item(key = "admin_quick_broadcast") {
                    Button(
                        onClick = { showBroadcastDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF141000)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_admin_broadcast_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI)
                                "नई स्टूडियो घोषणा प्रसारित करें (FCM टॉपिक)"
                            else
                                "Broadcast Studio Announcement (FCM Topic)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Filtered Feed Items
            when (selectedTab) {
                0 -> {
                    if (unifiedFeed.isEmpty()) {
                        item(key = "empty_all") {
                            EmptyNotificationsCard(language = language, tabIndex = 0)
                        }
                    } else {
                        items(
                            items = unifiedFeed,
                            key = { entry ->
                                when (entry) {
                                    is UnifiedFeedEntry.AppointmentEntry -> "appt_${entry.item.id}"
                                    is UnifiedFeedEntry.AnnouncementEntry -> "ann_${entry.item.id}"
                                }
                            }
                        ) { entry ->
                            when (entry) {
                                is UnifiedFeedEntry.AppointmentEntry -> {
                                    AppointmentNotificationCard(
                                        notification = entry.item,
                                        language = language,
                                        onMarkRead = { onMarkNotificationRead(entry.item.id) },
                                        onViewBookings = {
                                            if (!entry.item.isRead) {
                                                onMarkNotificationRead(entry.item.id)
                                            }
                                            onNavigateToScreen(StudioScreen.MyBookings)
                                        }
                                    )
                                }
                                is UnifiedFeedEntry.AnnouncementEntry -> {
                                    StudioAnnouncementCard(
                                        announcement = entry.item,
                                        language = language,
                                        isAdmin = isAdmin,
                                        onDelete = { onDeleteAnnouncement(entry.item.id) },
                                        onOpenAction = {
                                            onNavigateToScreen(resolveAnnouncementDestination(entry.item.actionRoute))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (appointmentNotifications.isEmpty()) {
                        item(key = "empty_appointments") {
                            EmptyNotificationsCard(language = language, tabIndex = 1)
                        }
                    } else {
                        items(
                            items = appointmentNotifications,
                            key = { "appt_${it.id}" }
                        ) { notif ->
                            AppointmentNotificationCard(
                                notification = notif,
                                language = language,
                                onMarkRead = { onMarkNotificationRead(notif.id) },
                                onViewBookings = {
                                    if (!notif.isRead) {
                                        onMarkNotificationRead(notif.id)
                                    }
                                    onNavigateToScreen(StudioScreen.MyBookings)
                                }
                            )
                        }
                    }
                }
                else -> {
                    if (announcements.isEmpty()) {
                        item(key = "empty_announcements") {
                            EmptyNotificationsCard(language = language, tabIndex = 2)
                        }
                    } else {
                        items(
                            items = announcements,
                            key = { "ann_${it.id}" }
                        ) { announcement ->
                            StudioAnnouncementCard(
                                announcement = announcement,
                                language = language,
                                isAdmin = isAdmin,
                                onDelete = { onDeleteAnnouncement(announcement.id) },
                                onOpenAction = {
                                    onNavigateToScreen(resolveAnnouncementDestination(announcement.actionRoute))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBroadcastDialog) {
        StudioAnnouncementComposerDialog(
            language = language,
            isSubmitting = isSubmitting,
            onDismiss = { showBroadcastDialog = false },
            onPublish = { title, message, category, priority, actionRoute ->
                onPublishAnnouncement(title, message, category, priority, actionRoute) {
                    showBroadcastDialog = false
                }
            }
        )
    }
}

@Composable
private fun FcmSubscriptionPreferencesCard(
    language: AppLanguage,
    fcmToken: String,
    announcementsOptIn: Boolean,
    appointmentPushOptIn: Boolean,
    onSyncFcm: () -> Unit,
    onUpdatePushPreferences: (Boolean, Boolean) -> Unit
) {
    val isLight = StudioTheme.isLight
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, StudioOutline),
                RoundedCornerShape(14.dp)
            )
            .testTag("fcm_settings_card"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(GoldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (language == AppLanguage.HINDI) "पुश अलर्ट व FCM स्थिति" else "FCM Push Channels & Topics",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (fcmToken.isNotBlank())
                                "Token Ready ••••${fcmToken.takeLast(8)}"
                            else if (language == AppLanguage.HINDI)
                                "सिंक करने के लिए टैप करें"
                            else
                                "Tap Sync to register device token",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (fcmToken.isNotBlank()) SuccessGreen else TextSecondary
                        )
                    }
                }

                OutlinedButton(
                    onClick = onSyncFcm,
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("sync_fcm_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == AppLanguage.HINDI) "सिंक" else "Sync",
                        color = GoldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Toggle 1: Appointment Status Updates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "अपॉइंटमेंट स्थिति अपडेट" else "Appointment Status Updates",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "बुकिंग पुष्टि, पूर्ण होने या बदलाव पर तुरंत पुश अलर्ट"
                        else
                            "Instant push when bookings are Confirmed or Completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = appointmentPushOptIn,
                    onCheckedChange = { enabled ->
                        onUpdatePushPreferences(announcementsOptIn, enabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (isLight) Color.White else Color(0xFF141000),
                        checkedTrackColor = GoldPrimary
                    ),
                    modifier = Modifier.testTag("toggle_appointment_push")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Toggle 2: Studio Announcements Topic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "स्टूडियो घोषणाएं (टॉपिक)" else "Studio Announcements Topic",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (language == AppLanguage.HINDI)
                            "कास्टिंग कॉल, ऑफ़र और 4K स्टेज न्यूज़ ('studio_announcements')"
                        else
                            "Casting calls, promo offers & stage news ('studio_announcements')",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = announcementsOptIn,
                    onCheckedChange = { enabled ->
                        onUpdatePushPreferences(enabled, appointmentPushOptIn)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (isLight) Color.White else Color(0xFF141000),
                        checkedTrackColor = GoldPrimary
                    ),
                    modifier = Modifier.testTag("toggle_announcements_push")
                )
            }
        }
    }
}

@Composable
fun AppointmentNotificationCard(
    notification: AppointmentNotification,
    language: AppLanguage,
    onMarkRead: () -> Unit,
    onViewBookings: () -> Unit
) {
    val statusColor = when (notification.status.lowercase()) {
        "confirmed" -> SuccessGreen
        "completed" -> GoldPrimary
        "cancelled" -> Color(0xFFEF4444)
        else -> Color(0xFFF59E0B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    width = if (notification.isRead) 1.dp else 1.5.dp,
                    color = if (notification.isRead) StudioOutline else GoldPrimary.copy(alpha = 0.8f)
                ),
                RoundedCornerShape(14.dp)
            )
            .clickable {
                if (!notification.isRead) onMarkRead()
            }
            .testTag("appointment_notif_card_${notification.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) StudioCardSurface else StudioElevatedSurface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = notification.status.uppercase(),
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    if (notification.serviceType.isNotBlank()) {
                        Text(
                            text = notification.serviceType,
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary)
                            .testTag("unread_dot_${notification.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestampLabel(notification.createdAt?.toDate(), language),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.8f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!notification.isRead) {
                        TextButton(
                            onClick = onMarkRead,
                            modifier = Modifier.testTag("mark_read_btn_${notification.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "पढ़ा गया" else "Mark Read",
                                color = GoldPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onViewBookings,
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("view_booking_from_notif_${notification.id}")
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "बुकिंग देखें" else "View Booking",
                            color = GoldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudioAnnouncementCard(
    announcement: StudioAnnouncement,
    language: AppLanguage,
    isAdmin: Boolean,
    onDelete: () -> Unit = {},
    onOpenAction: () -> Unit
) {
    val isUrgent = announcement.priority.equals("urgent", ignoreCase = true)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    width = if (isUrgent) 1.5.dp else 1.dp,
                    brush = Brush.horizontalGradient(
                        if (isUrgent) listOf(Color(0xFFEF4444), GoldPrimary)
                        else listOf(GoldPrimary.copy(alpha = 0.6f), StudioOutline)
                    )
                ),
                RoundedCornerShape(14.dp)
            )
            .testTag("announcement_card_${announcement.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = if (isUrgent) Color(0xFFEF4444) else GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = announcement.category.uppercase(),
                            color = GoldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isUrgent) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "URGENT",
                                color = Color(0xFFEF4444),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                if (isAdmin) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_announcement_${announcement.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Announcement",
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = announcement.message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${announcement.authorName} • ${formatTimestampLabel(announcement.createdAt?.toDate(), language)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                TextButton(
                    onClick = onOpenAction,
                    modifier = Modifier.testTag("announcement_action_${announcement.id}")
                ) {
                    Text(
                        text = announcementActionLabel(announcement.actionRoute, language),
                        color = GoldPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsCard(language: AppLanguage, tabIndex: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioOutline, RoundedCornerShape(14.dp))
            .testTag("notifications_empty_state"),
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (tabIndex == 2) Icons.Default.Campaign else Icons.Default.Notifications,
                contentDescription = null,
                tint = GoldPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = when {
                    tabIndex == 1 && language == AppLanguage.HINDI -> "अभी कोई अपॉइंटमेंट अपडेट नहीं है।"
                    tabIndex == 1 -> "No appointment status alerts yet."
                    tabIndex == 2 && language == AppLanguage.HINDI -> "अभी कोई स्टूडियो घोषणा नहीं है।"
                    tabIndex == 2 -> "No studio announcements posted yet."
                    language == AppLanguage.HINDI -> "आपका सूचना केंद्र अभी खाली है।"
                    else -> "You're all caught up! No notifications yet."
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (language == AppLanguage.HINDI)
                    "जब आपकी शूट बुकिंग की स्थिति बदलेगी या स्टूडियो नई घोषणा करेगा, वह यहाँ तुरंत दिखेगी।"
                else
                    "Booking confirmations, status transitions, and studio casting broadcasts appear here in real time.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun StudioAnnouncementComposerDialog(
    language: AppLanguage,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onPublish: (title: String, message: String, category: String, priority: String, actionRoute: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Studio Update") }
    var isUrgent by remember { mutableStateOf(false) }
    var selectedRoute by remember { mutableStateOf("home") }

    val categories = listOf(
        "Studio Update",
        "Casting Call",
        "Offer & Discount",
        "Schedule Alert",
        "Workshop & Gear"
    )
    val routes = listOf(
        "home" to (if (language == AppLanguage.HINDI) "होम" else "Home"),
        "bookings" to (if (language == AppLanguage.HINDI) "बुकिंग" else "Book Shoot"),
        "catalog" to (if (language == AppLanguage.HINDI) "पैकेज" else "Catalog"),
        "portfolio" to (if (language == AppLanguage.HINDI) "पोर्टफोलियो" else "Portfolio"),
        "models" to (if (language == AppLanguage.HINDI) "मॉडल" else "Apply Model")
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .border(1.2.dp, GoldPrimary.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                .testTag("announcement_composer_dialog"),
            colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.HINDI) "स्टूडियो घोषणा प्रसारित करें" else "Broadcast Studio Announcement",
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (language == AppLanguage.HINDI) "बंद करें" else "Close dialog",
                            tint = TextSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (language == AppLanguage.HINDI) "शीर्षक *" else "Headline *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("announcement_title_input"),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(if (language == AppLanguage.HINDI) "संदेश विवरण *" else "Announcement Message *") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("announcement_message_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    text = if (language == AppLanguage.HINDI) "श्रेणी:" else "Category:",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color(0xFF141000)
                            )
                        )
                    }
                }

                Text(
                    text = if (language == AppLanguage.HINDI) "डीप-लिंक गंतव्य:" else "Tap Destination Route:",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    routes.forEach { (routeKey, label) ->
                        FilterChip(
                            selected = selectedRoute == routeKey,
                            onClick = { selectedRoute = routeKey },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color(0xFF141000)
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.HINDI) "अत्यावश्यक (Urgent) प्राथमिकता" else "Mark as Urgent Alert",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                    Switch(
                        checked = isUrgent,
                        onCheckedChange = { isUrgent = it },
                        modifier = Modifier.testTag("announcement_urgent_switch")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel",
                            color = GoldLight
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onPublish(
                                title,
                                message,
                                selectedCategory,
                                if (isUrgent) "urgent" else "normal",
                                selectedRoute
                            )
                        },
                        enabled = !isSubmitting && title.trim().length >= 3 && message.trim().length >= 5,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF141000)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("publish_announcement_confirm_button")
                    ) {
                        Text(
                            text = if (language == AppLanguage.HINDI) "प्रसारित करें" else "Publish Now",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun resolveAnnouncementDestination(actionRoute: String): StudioScreen {
    return when (actionRoute.trim().lowercase()) {
        "bookings" -> StudioScreen.ShootBooking
        "catalog" -> StudioScreen.ServiceCatalog
        "portfolio" -> StudioScreen.Portfolio
        "models" -> StudioScreen.ModelRegistration
        else -> StudioScreen.Home
    }
}

private fun announcementActionLabel(actionRoute: String, language: AppLanguage): String {
    return when (actionRoute.trim().lowercase()) {
        "bookings" -> if (language == AppLanguage.HINDI) "शूट बुक करें" else "Book Shoot"
        "catalog" -> if (language == AppLanguage.HINDI) "पैकेज देखें" else "Explore Catalog"
        "portfolio" -> if (language == AppLanguage.HINDI) "पोर्टफोलियो देखें" else "View Portfolio"
        "models" -> if (language == AppLanguage.HINDI) "मॉडल आवेदन" else "Apply Model"
        else -> if (language == AppLanguage.HINDI) "स्टूडियो होम" else "Studio Home"
    }
}

private fun formatTimestampLabel(date: Date?, language: AppLanguage): String {
    if (date == null) return if (language == AppLanguage.HINDI) "अभी" else "Just now"
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return formatter.format(date)
}
