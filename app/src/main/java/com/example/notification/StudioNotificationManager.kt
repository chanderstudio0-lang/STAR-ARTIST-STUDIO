package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.ShootBooking
import com.example.data.model.StudioAnnouncement
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Handles incoming Firebase Cloud Messaging push notifications
 * (both targeted appointment status updates and topic-based studio announcements)
 * and FCM registration token refreshes for STAR ARTIST STUDIO clients.
 */
class StudioFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token generated: $token")
        serviceScope.launch {
            StudioNotificationManager.syncFcmTokenToFirestore(applicationContext, token)
            StudioNotificationManager.subscribeToTopic(StudioNotificationManager.TOPIC_STUDIO_ANNOUNCEMENTS)
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (!uid.isNullOrBlank()) {
                StudioNotificationManager.subscribeToTopic(StudioNotificationManager.userAppointmentsTopic(uid))
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val parsed = StudioNotificationManager.parseFcmPayload(
            data = remoteMessage.data,
            notificationTitle = remoteMessage.notification?.title,
            notificationBody = remoteMessage.notification?.body,
            from = remoteMessage.from
        )

        when (parsed.type) {
            StudioNotificationManager.FcmPayloadType.STUDIO_ANNOUNCEMENT -> {
                StudioNotificationManager.showStudioAnnouncementNotification(
                    context = applicationContext,
                    title = parsed.title,
                    message = parsed.message,
                    announcementId = parsed.announcementId,
                    category = parsed.category,
                    priority = parsed.priority,
                    actionRoute = parsed.actionRoute
                )
            }
            StudioNotificationManager.FcmPayloadType.APPOINTMENT_STATUS -> {
                StudioNotificationManager.showBookingStatusNotification(
                    context = applicationContext,
                    title = parsed.title,
                    message = parsed.message,
                    bookingId = parsed.bookingId,
                    status = parsed.status
                )
            }
        }
    }

    companion object {
        private const val TAG = "StudioFCM"
    }
}

/**
 * Manager handling notification channels, FCM token retrieval, topic subscriptions,
 * Firestore sync, and local/push dispatch for appointment status updates & studio announcements.
 */
object StudioNotificationManager {

    const val CHANNEL_ID_BOOKING_UPDATES = "star_artist_booking_updates"
    const val CHANNEL_NAME_BOOKING_UPDATES = "Booking Status Updates"
    const val CHANNEL_DESC_BOOKING_UPDATES = "Real-time notifications when your studio shoot booking status changes."

    const val CHANNEL_ID_SESSION_REMINDERS = "star_artist_session_reminders"
    const val CHANNEL_NAME_SESSION_REMINDERS = "24-Hour Session Reminders"
    const val CHANNEL_DESC_SESSION_REMINDERS = "Local reminders triggered 24 hours before your confirmed studio shoot."

    const val CHANNEL_ID_STUDIO_ANNOUNCEMENTS = "star_artist_studio_announcements"
    const val CHANNEL_NAME_STUDIO_ANNOUNCEMENTS = "Studio Announcements"
    const val CHANNEL_DESC_STUDIO_ANNOUNCEMENTS = "Casting calls, studio offers, workshops, and broadcast announcements."

    const val TOPIC_STUDIO_ANNOUNCEMENTS = "studio_announcements"

    const val EXTRA_BOOKING_ID = "EXTRA_BOOKING_ID"
    const val EXTRA_BOOKING_STATUS = "EXTRA_BOOKING_STATUS"
    const val EXTRA_OPEN_MY_BOOKINGS = "EXTRA_OPEN_MY_BOOKINGS"
    const val EXTRA_OPEN_ANNOUNCEMENTS = "EXTRA_OPEN_ANNOUNCEMENTS"
    const val EXTRA_ANNOUNCEMENT_ID = "EXTRA_ANNOUNCEMENT_ID"
    const val EXTRA_ANNOUNCEMENT_ROUTE = "EXTRA_ANNOUNCEMENT_ROUTE"

    private const val TAG = "StudioNotificationMgr"

    enum class FcmPayloadType {
        APPOINTMENT_STATUS,
        STUDIO_ANNOUNCEMENT
    }

    data class ParsedFcmMessage(
        val type: FcmPayloadType,
        val title: String,
        val message: String,
        val bookingId: String? = null,
        val status: String = "Updated",
        val announcementId: String? = null,
        val category: String = "Studio Update",
        val priority: String = "normal",
        val actionRoute: String = "home"
    )

    fun userAppointmentsTopic(userId: String): String {
        val sanitized = userId.trim().replace(Regex("[^a-zA-Z0-9_\\-.~%]"), "_")
        return "user_appointments_${sanitized.ifEmpty { "guest" }}"
    }

    /**
     * Parses incoming FCM data & notification payloads into a structured message model,
     * routing between targeted appointment status updates and studio-wide announcements.
     */
    fun parseFcmPayload(
        data: Map<String, String>,
        notificationTitle: String? = null,
        notificationBody: String? = null,
        from: String? = null
    ): ParsedFcmMessage {
        val rawType = data["type"]?.trim()?.lowercase().orEmpty()
        val isAnnouncement = rawType in setOf("studio_announcement", "announcement", "broadcast") ||
            from?.contains(TOPIC_STUDIO_ANNOUNCEMENTS) == true ||
            (!data["announcementId"].isNullOrBlank() && data["bookingId"].isNullOrBlank())

        return if (isAnnouncement) {
            val category = data["category"]?.takeIf { it.isNotBlank() } ?: "Studio Update"
            val priority = data["priority"]?.takeIf { it.isNotBlank() } ?: "normal"
            val rawTitle = notificationTitle ?: data["title"] ?: "Star Artist Studio Announcement"
            val rawBody = notificationBody ?: data["body"] ?: data["message"] ?: "New update from Star Artist Studio."
            val actionRoute = data["actionRoute"]?.takeIf { it.isNotBlank() } ?: "home"
            val (formattedTitle, formattedBody) = formatAnnouncementNotificationPayload(
                title = rawTitle,
                message = rawBody,
                category = category,
                priority = priority
            )
            ParsedFcmMessage(
                type = FcmPayloadType.STUDIO_ANNOUNCEMENT,
                title = formattedTitle,
                message = formattedBody,
                announcementId = data["announcementId"]?.takeIf { it.isNotBlank() },
                category = category,
                priority = priority,
                actionRoute = actionRoute
            )
        } else {
            val title = notificationTitle ?: data["title"] ?: "Star Artist Studio"
            val body = notificationBody ?: data["body"] ?: data["message"]
                ?: "Your shoot booking status has been updated."
            ParsedFcmMessage(
                type = FcmPayloadType.APPOINTMENT_STATUS,
                title = title,
                message = body,
                bookingId = data["bookingId"]?.takeIf { it.isNotBlank() },
                status = data["status"]?.takeIf { it.isNotBlank() } ?: "Updated"
            )
        }
    }

    /**
     * Creates the Android Notification Channels required for Android 8.0+ (API 26+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bookingUpdatesChannel = NotificationChannel(
                CHANNEL_ID_BOOKING_UPDATES,
                CHANNEL_NAME_BOOKING_UPDATES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_BOOKING_UPDATES
                enableLights(true)
                lightColor = Color.parseColor("#D4AF37")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }

            val sessionRemindersChannel = NotificationChannel(
                CHANNEL_ID_SESSION_REMINDERS,
                CHANNEL_NAME_SESSION_REMINDERS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_SESSION_REMINDERS
                enableLights(true)
                lightColor = Color.parseColor("#D4AF37")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
            }

            val announcementsChannel = NotificationChannel(
                CHANNEL_ID_STUDIO_ANNOUNCEMENTS,
                CHANNEL_NAME_STUDIO_ANNOUNCEMENTS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_STUDIO_ANNOUNCEMENTS
                enableLights(true)
                lightColor = Color.parseColor("#D4AF37")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(bookingUpdatesChannel)
            manager?.createNotificationChannel(sessionRemindersChannel)
            manager?.createNotificationChannel(announcementsChannel)
        }
    }

    /**
     * Checks whether Google Play Services is available on the device for FCM push registration.
     */
    fun isPushMessagingAvailable(context: Context): Boolean {
        return runCatching {
            val status = com.google.android.gms.common.GoogleApiAvailability
                .getInstance()
                .isGooglePlayServicesAvailable(context)
            status == com.google.android.gms.common.ConnectionResult.SUCCESS
        }.getOrDefault(false)
    }

    /**
     * Identifies non-fatal FCM/FIS registration errors (such as missing Play Store / GMS push
     * support on emulators or temporary network outages) that should degrade gracefully.
     */
    internal fun isExpectedUnavailableFcmError(throwable: Throwable): Boolean {
        val msg = throwable.message.orEmpty()
        return msg.contains("FCM Registration failed", ignoreCase = true) ||
            msg.contains("SERVICE_NOT_AVAILABLE", ignoreCase = true) ||
            msg.contains("FIS_AUTH_ERROR", ignoreCase = true) ||
            msg.contains("MISSING_INSTANCEID_SERVICE", ignoreCase = true) ||
            msg.contains("AUTHENTICATION_FAILED", ignoreCase = true) ||
            msg.contains("INTERNAL_SERVER_ERROR", ignoreCase = true)
    }

    /**
     * Retrieves the device FCM registration token asynchronously without blocking the UI thread
     * or emitting fatal errors on emulators/devices without GMS push registration.
     */
    suspend fun getOrFetchFcmToken(context: Context? = null): String? =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            if (context != null && !isPushMessagingAvailable(context)) {
                Log.w(TAG, "Google Play Services unavailable on this device/emulator; skipping FCM token registration.")
                return@withContext null
            }

            var lastError: Throwable? = null
            repeat(2) { attempt ->
                try {
                    val token = FirebaseMessaging.getInstance().token.await()
                    if (!token.isNullOrBlank()) {
                        return@withContext token
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                    val transient = isExpectedUnavailableFcmError(e)
                    if (attempt == 0 && transient) {
                        kotlinx.coroutines.delay(500L)
                    } else {
                        return@repeat
                    }
                }
            }

            val err = lastError
            if (err != null) {
                if (isExpectedUnavailableFcmError(err)) {
                    Log.w(TAG, "FCM push registration unavailable (${err.message}); continuing with local/in-app notifications.")
                } else {
                    Log.w(TAG, "Unable to retrieve FCM token (${err.message}); push notifications disabled for this session.")
                }
            }
            null
        }

    suspend fun subscribeToTopic(topic: String): Boolean {
        if (topic.isBlank()) return false
        return try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            Log.d(TAG, "Subscribed to FCM topic: $topic")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to subscribe to FCM topic $topic: ${e.message}")
            false
        }
    }

    suspend fun unsubscribeFromTopic(topic: String): Boolean {
        if (topic.isBlank()) return false
        return try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            Log.d(TAG, "Unsubscribed from FCM topic: $topic")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unsubscribe from FCM topic $topic: ${e.message}")
            false
        }
    }

    /**
     * Synchronizes FCM device token and topic subscriptions for announcements and per-user appointment updates.
     */
    suspend fun syncFcmTopicsAndToken(
        context: Context,
        userId: String?,
        announcementsOptIn: Boolean,
        appointmentPushOptIn: Boolean
    ): String? {
        createNotificationChannels(context)
        val token = getOrFetchFcmToken(context = context)
        syncFcmTokenToFirestore(
            context = context,
            token = token,
            announcementsOptIn = announcementsOptIn,
            appointmentPushOptIn = appointmentPushOptIn
        )
        if (token.isNullOrBlank()) {
            // Skip topic RPCs when no FCM registration token is available on this device/emulator
            return null
        }
        if (announcementsOptIn) {
            subscribeToTopic(TOPIC_STUDIO_ANNOUNCEMENTS)
        } else {
            unsubscribeFromTopic(TOPIC_STUDIO_ANNOUNCEMENTS)
        }
        if (!userId.isNullOrBlank()) {
            val userTopic = userAppointmentsTopic(userId)
            if (appointmentPushOptIn) {
                subscribeToTopic(userTopic)
            } else {
                unsubscribeFromTopic(userTopic)
            }
        }
        return token
    }

    /**
     * Saves the FCM registration token (and optional opt-in toggles) to the user's Firestore profile.
     */
    suspend fun syncFcmTokenToFirestore(
        context: Context,
        token: String?,
        announcementsOptIn: Boolean? = null,
        appointmentPushOptIn: Boolean? = null
    ) {
        if (token.isNullOrBlank() && announcementsOptIn == null && appointmentPushOptIn == null) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val dbId = context.applicationContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!token.isNullOrBlank()) {
                updates["fcmToken"] = token
                updates["fcmTokenUpdatedAt"] = FieldValue.serverTimestamp()
            }
            if (announcementsOptIn != null) {
                updates["announcementsOptIn"] = announcementsOptIn
            }
            if (appointmentPushOptIn != null) {
                updates["appointmentPushOptIn"] = appointmentPushOptIn
            }
            db.collection("users").document(uid).update(updates).await()
            Log.d(TAG, "Successfully synced FCM settings for user $uid")
        } catch (e: Exception) {
            Log.w(TAG, "Could not update user FCM settings in Firestore: ${e.message}")
        }
    }

    /**
     * Displays a rich system notification to the client regarding their booking status change.
     */
    fun showBookingStatusNotification(
        context: Context,
        title: String,
        message: String,
        bookingId: String? = null,
        status: String = ""
    ) {
        createNotificationChannels(context)

        if (!hasPostNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Skipping notification display.")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_BOOKING_ID, bookingId)
            putExtra(EXTRA_BOOKING_STATUS, status)
            putExtra(EXTRA_OPEN_MY_BOOKINGS, true)
        }

        val requestCode = ((bookingId?.hashCode() ?: System.currentTimeMillis().toInt()) and 0x0FFFFFFF).coerceAtLeast(1001)
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BOOKING_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(Color.parseColor("#D4AF37"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
            Log.d(TAG, "Notification displayed successfully for bookingId=$bookingId status=$status")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException displaying notification: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Exception displaying notification: ${e.message}")
        }
    }

    /**
     * Displays a system notification for a studio-wide announcement broadcast.
     */
    fun showStudioAnnouncementNotification(
        context: Context,
        title: String,
        message: String,
        announcementId: String? = null,
        category: String = "Studio Update",
        priority: String = "normal",
        actionRoute: String = "home"
    ): Boolean {
        createNotificationChannels(context)

        if (!hasPostNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Skipping studio announcement notification.")
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ANNOUNCEMENTS, true)
            putExtra(EXTRA_ANNOUNCEMENT_ID, announcementId)
            putExtra(EXTRA_ANNOUNCEMENT_ROUTE, actionRoute)
        }

        val requestCode = (((announcementId ?: title).hashCode() and 0x0FFFFFFF) + 80_000)
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifPriority = if (priority.equals("urgent", ignoreCase = true)) {
            NotificationCompat.PRIORITY_MAX
        } else {
            NotificationCompat.PRIORITY_HIGH
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_STUDIO_ANNOUNCEMENTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setSubText(category)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(notifPriority)
            .setColor(Color.parseColor("#D4AF37"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
            Log.d(TAG, "Studio announcement notification displayed id=$announcementId")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException displaying announcement notification: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception displaying announcement notification: ${e.message}")
            false
        }
    }

    fun showStudioAnnouncementNotification(
        context: Context,
        announcement: StudioAnnouncement
    ): Boolean {
        val (title, body) = formatAnnouncementNotificationPayload(
            title = announcement.title,
            message = announcement.message,
            category = announcement.category,
            priority = announcement.priority
        )
        return showStudioAnnouncementNotification(
            context = context,
            title = title,
            message = body,
            announcementId = announcement.id.takeIf { it.isNotBlank() },
            category = announcement.category,
            priority = announcement.priority,
            actionRoute = announcement.actionRoute
        )
    }

    /**
     * Displays a local 24-hour advance reminder notification triggered by AlarmManager
     * for a client's Confirmed studio shoot.
     */
    fun showSessionReminderNotification(
        context: Context,
        bookingId: String,
        serviceType: String,
        clientName: String,
        preferredDate: String,
        preferredTime: String,
        location: String
    ): Boolean {
        createNotificationChannels(context)

        if (!hasPostNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Skipping 24h session reminder notification.")
            return false
        }

        val safeClient = clientName.ifBlank { "Artist" }
        val safeService = serviceType.ifBlank { "Studio Session" }
        val safeTime = preferredTime.ifBlank { "Scheduled Time" }
        val safeLocation = location.ifBlank { "Star Artist Studio (Talwara Khurd)" }

        val title = "⏰ Studio Shoot in 24 Hours!"
        val message = "Hi $safeClient, your confirmed $safeService session is tomorrow ($preferredDate at $safeTime) at $safeLocation. Prepare your outfits & props!"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_BOOKING_ID, bookingId)
            putExtra(EXTRA_BOOKING_STATUS, "Confirmed")
            putExtra(EXTRA_OPEN_MY_BOOKINGS, true)
        }

        val requestCode = ((bookingId.hashCode() and 0x0FFFFFFF) + 50_000)
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SESSION_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(Color.parseColor("#D4AF37"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
            Log.d(TAG, "24h session reminder notification displayed for bookingId=$bookingId")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException displaying session reminder: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception displaying session reminder: ${e.message}")
            false
        }
    }

    fun hasPostNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Generates user-friendly notification copy based on status transition
     */
    fun formatNotificationPayload(
        clientName: String,
        serviceType: String,
        preferredDate: String,
        newStatus: String
    ): Pair<String, String> {
        val title = when (newStatus.lowercase()) {
            "confirmed" -> "✨ Shoot Booking Confirmed!"
            "completed" -> "🏆 Production Completed & Ready!"
            "cancelled" -> "Booking Cancelled"
            else -> "Booking Status Updated"
        }

        val body = when (newStatus.lowercase()) {
            "confirmed" -> "Dear $clientName, your $serviceType shoot on $preferredDate has been officially Confirmed by Star Artist Studio director. 4K lighting bays are reserved!"
            "completed" -> "Dear $clientName, your $serviceType shoot deliverables have been finalized and completed. Thank you for creating with Star Artist Studio!"
            "cancelled" -> "Dear $clientName, your booking for $serviceType on $preferredDate was cancelled. Please contact the studio for rescheduling."
            else -> "Dear $clientName, your booking for $serviceType has been updated to $newStatus."
        }

        return Pair(title, body)
    }

    fun formatAnnouncementNotificationPayload(
        title: String,
        message: String,
        category: String,
        priority: String
    ): Pair<String, String> {
        val prefix = if (priority.equals("urgent", ignoreCase = true)) "🚨 " else "📣 "
        val cleanTitle = title.trim().ifEmpty { "Star Artist Studio Announcement" }
        val finalTitle = if (cleanTitle.startsWith("🚨") || cleanTitle.startsWith("📣")) {
            cleanTitle
        } else {
            "$prefix$category: $cleanTitle"
        }
        val cleanBody = message.trim().ifEmpty { "Tap to view the latest studio update." }
        return Pair(finalTitle, cleanBody)
    }

    /**
     * Inspects previous booking status vs new status and triggers client notification
     * if the status moved from 'Pending' to 'Confirmed' or 'Completed'.
     */
    fun checkAndNotifyStatusTransition(
        context: Context,
        booking: ShootBooking,
        oldStatus: String,
        newStatus: String
    ) {
        val shouldNotify = (oldStatus.equals("Pending", ignoreCase = true) &&
                (newStatus.equals("Confirmed", ignoreCase = true) || newStatus.equals("Completed", ignoreCase = true))) ||
                (oldStatus.equals("Confirmed", ignoreCase = true) && newStatus.equals("Completed", ignoreCase = true))

        if (shouldNotify) {
            val (title, body) = formatNotificationPayload(
                clientName = booking.clientName.ifBlank { "Client" },
                serviceType = booking.serviceType,
                preferredDate = booking.preferredDate,
                newStatus = newStatus
            )

            showBookingStatusNotification(
                context = context,
                title = title,
                message = body,
                bookingId = booking.id,
                status = newStatus
            )
        }
    }
}
