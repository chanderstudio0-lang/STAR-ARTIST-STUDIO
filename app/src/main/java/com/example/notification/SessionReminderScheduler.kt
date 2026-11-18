package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.data.model.ShootBooking
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ScheduledReminderRecord(
    val bookingId: String,
    val serviceType: String,
    val clientName: String,
    val preferredDate: String,
    val preferredTime: String,
    val location: String,
    val triggerAtMillis: Long,
    val isExact: Boolean
)

sealed class ReminderScheduleResult {
    data class Scheduled(val triggerAtMillis: Long, val isExact: Boolean) : ReminderScheduleResult()
    data object SkippedOrCancelled : ReminderScheduleResult()
}

data class ReminderSyncSummary(
    val scheduledCount: Int,
    val cancelledCount: Int,
    val exactCount: Int,
    val inexactFallbackCount: Int
)

sealed class SessionReminderUiState {
    data class Scheduled(val triggerAtMillis: Long, val isExact: Boolean) : SessionReminderUiState()
    data class Within24Hours(val sessionStartMillis: Long) : SessionReminderUiState()
    data object NotApplicable : SessionReminderUiState()
}

/**
 * Schedules, updates, persists, and cancels local 24-hour advance reminders
 * for Confirmed studio sessions using the Android AlarmManager API.
 */
object SessionReminderScheduler {

    private const val TAG = "SessionReminderSched"
    private const val PREFS_NAME = "star_artist_session_reminders"
    private const val KEY_RECORDS_JSON = "scheduled_reminders_json"

    val REMINDER_LEAD_TIME_MILLIS: Long = TimeUnit.HOURS.toMillis(24)

    const val ACTION_SESSION_REMINDER = "com.example.action.SESSION_REMINDER_24H"
    const val EXTRA_BOOKING_ID = "extra_reminder_booking_id"
    const val EXTRA_SERVICE_TYPE = "extra_reminder_service_type"
    const val EXTRA_CLIENT_NAME = "extra_reminder_client_name"
    const val EXTRA_PREFERRED_DATE = "extra_reminder_preferred_date"
    const val EXTRA_PREFERRED_TIME = "extra_reminder_preferred_time"
    const val EXTRA_LOCATION = "extra_reminder_location"
    const val EXTRA_TRIGGER_AT_MILLIS = "extra_reminder_trigger_at_millis"

    private val datePatterns = listOf("yyyy-MM-dd", "yyyy/MM/dd", "dd-MM-yyyy", "dd/MM/yyyy")
    private val timePatterns = listOf("h:mm a", "hh:mm a", "H:mm", "HH:mm")

    /**
     * Parses [preferredDate] and [preferredTime] into the epoch milliseconds of session start.
     * Descriptive suffixes inside parentheses (e.g. "10:00 AM (Morning Key Light)") are stripped.
     * If [preferredDate] is valid but [preferredTime] is empty or "Flexible", defaults to 11:00 AM.
     */
    fun parseSessionStartMillis(
        preferredDate: String,
        preferredTime: String
    ): Long? {
        val rawDate = preferredDate.trim()
        if (rawDate.isEmpty()) return null

        var parsedDateMillis: Long? = null
        for (pattern in datePatterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val date = parser.parse(rawDate)
                if (date != null) {
                    parsedDateMillis = date.time
                    break
                }
            } catch (_: Exception) {
                // Try next date format
            }
        }
        val dateMillis = parsedDateMillis ?: return null

        val dateCal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
        }

        val (hourOfDay, minute) = parseTimeHourMinute(preferredTime)

        return Calendar.getInstance().apply {
            set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
            set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    internal fun parseTimeHourMinute(preferredTime: String): Pair<Int, Int> {
        val stripped = preferredTime
            .substringBefore("(")
            .trim()
            .uppercase(Locale.US)
            .replace(".", "")
            .replace(Regex("\\s+"), " ")

        if (stripped.isEmpty() || stripped == "FLEXIBLE" || stripped == "TBD") {
            return 11 to 0 // Default 11:00 AM matching ShootBooking default
        }

        for (pattern in timePatterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = parser.parse(stripped) ?: continue
                val cal = Calendar.getInstance().apply { time = parsed }
                return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
            } catch (_: Exception) {
                // Try next time format
            }
        }

        // Regex fallback for embedded times like "Morning 9:30 AM"
        val match = Regex("(\\d{1,2}):(\\d{2})\\s*(AM|PM)?").find(stripped)
        if (match != null) {
            var hour = match.groupValues[1].toIntOrNull() ?: 11
            val minute = (match.groupValues[2].toIntOrNull() ?: 0).coerceIn(0, 59)
            val amPm = match.groupValues[3]
            if (amPm == "PM" && hour in 1..11) hour += 12
            if (amPm == "AM" && hour == 12) hour = 0
            return hour.coerceIn(0, 23) to minute
        }

        return 11 to 0
    }

    /**
     * Computes the exact trigger timestamp (24 hours before session start) for a Confirmed booking.
     * Returns null if booking ID is blank, status is not "Confirmed", date cannot be parsed,
     * or the 24-hour reminder time is already in the past relative to [nowMillis].
     */
    fun computeReminderTriggerAtMillis(
        booking: ShootBooking,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (booking.id.isBlank()) return null
        if (!booking.status.trim().equals("Confirmed", ignoreCase = true)) return null

        val sessionStartMillis = parseSessionStartMillis(
            preferredDate = booking.preferredDate,
            preferredTime = booking.preferredTime
        ) ?: return null

        val triggerAtMillis = sessionStartMillis - REMINDER_LEAD_TIME_MILLIS
        return if (triggerAtMillis > nowMillis) triggerAtMillis else null
    }

    /**
     * Evaluates UI badge state for a single booking in MyBookingsScreen.
     */
    fun evaluateUiState(
        context: Context,
        booking: ShootBooking,
        nowMillis: Long = System.currentTimeMillis()
    ): SessionReminderUiState {
        if (!booking.status.trim().equals("Confirmed", ignoreCase = true)) {
            return SessionReminderUiState.NotApplicable
        }
        val sessionStartMillis = parseSessionStartMillis(
            preferredDate = booking.preferredDate,
            preferredTime = booking.preferredTime
        ) ?: return SessionReminderUiState.NotApplicable

        if (sessionStartMillis <= nowMillis) {
            return SessionReminderUiState.NotApplicable
        }

        val triggerAtMillis = sessionStartMillis - REMINDER_LEAD_TIME_MILLIS
        return if (triggerAtMillis > nowMillis) {
            val persisted = readPersistedRecords(context)[booking.id]
            val isExact = persisted?.isExact ?: canScheduleExactAlarms(context)
            SessionReminderUiState.Scheduled(
                triggerAtMillis = triggerAtMillis,
                isExact = isExact
            )
        } else {
            SessionReminderUiState.Within24Hours(sessionStartMillis = sessionStartMillis)
        }
    }

    /**
     * Returns true when exact alarms can be scheduled without SecurityException.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    /**
     * Builds the Settings Intent to let users grant Exact Alarm permission on Android 12+ (API 31+).
     */
    fun buildExactAlarmSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Schedules (or cancels if ineligible) a 24-hour advance local notification alarm
     * for [booking] using AlarmManager.
     */
    fun scheduleReminder(
        context: Context,
        booking: ShootBooking,
        nowMillis: Long = System.currentTimeMillis()
    ): ReminderScheduleResult {
        val appContext = context.applicationContext
        val triggerAtMillis = computeReminderTriggerAtMillis(booking, nowMillis)
        if (triggerAtMillis == null) {
            if (booking.id.isNotBlank()) {
                cancelReminder(appContext, booking.id)
            }
            return ReminderScheduleResult.SkippedOrCancelled
        }

        val record = ScheduledReminderRecord(
            bookingId = booking.id,
            serviceType = booking.serviceType,
            clientName = booking.clientName,
            preferredDate = booking.preferredDate,
            preferredTime = booking.preferredTime,
            location = booking.location,
            triggerAtMillis = triggerAtMillis,
            isExact = false
        )

        return scheduleRecordInternal(appContext, record)
    }

    private fun scheduleRecordInternal(
        appContext: Context,
        record: ScheduledReminderRecord
    ): ReminderScheduleResult {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return ReminderScheduleResult.SkippedOrCancelled

        val pendingIntent = buildReminderPendingIntent(
            context = appContext,
            record = record,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return ReminderScheduleResult.SkippedOrCancelled

        val scheduledExact = applyAlarm(alarmManager, record.triggerAtMillis, pendingIntent)

        val records = readPersistedRecords(appContext).toMutableMap()
        records[record.bookingId] = record.copy(isExact = scheduledExact)
        writePersistedRecords(appContext, records)
        updateBootReceiverState(appContext, enabled = records.isNotEmpty())

        Log.d(
            TAG,
            "Scheduled 24h reminder for bookingId=${record.bookingId} at ${record.triggerAtMillis} (exact=$scheduledExact)"
        )
        return ReminderScheduleResult.Scheduled(
            triggerAtMillis = record.triggerAtMillis,
            isExact = scheduledExact
        )
    }

    private fun applyAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ): Boolean {
        val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        if (canUseExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                return true
            } catch (e: SecurityException) {
                Log.w(TAG, "Exact alarm permission revoked; falling back to inexact alarm", e)
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
        return false
    }

    /**
     * Cancels any scheduled AlarmManager reminder and removes persisted record for [bookingId].
     */
    fun cancelReminder(context: Context, bookingId: String) {
        if (bookingId.isBlank()) return
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        val cancelIntent = buildReminderIntent(
            context = appContext,
            bookingId = bookingId,
            serviceType = "",
            clientName = "",
            preferredDate = "",
            preferredTime = "",
            location = "",
            triggerAtMillis = 0L
        )

        val existingPendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCodeFor(bookingId),
            cancelIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existingPendingIntent != null) {
            alarmManager?.cancel(existingPendingIntent)
            existingPendingIntent.cancel()
        }

        val records = readPersistedRecords(appContext).toMutableMap()
        if (records.remove(bookingId) != null) {
            writePersistedRecords(appContext, records)
        }
        updateBootReceiverState(appContext, enabled = records.isNotEmpty())
    }

    /**
     * Reconciles all user bookings with AlarmManager:
     * - Cancels alarms for removed, cancelled, completed, pending, or past bookings
     * - Schedules/updates 24h alarms for every upcoming Confirmed booking (>24h away)
     */
    fun syncConfirmedSessionReminders(
        context: Context,
        bookings: List<ShootBooking>,
        nowMillis: Long = System.currentTimeMillis()
    ): ReminderSyncSummary {
        val appContext = context.applicationContext
        val existingIds = readPersistedRecords(appContext).keys.toSet()

        val eligibleBookings = bookings.mapNotNull { booking ->
            val triggerAtMillis = computeReminderTriggerAtMillis(booking, nowMillis)
            if (triggerAtMillis != null) booking to triggerAtMillis else null
        }
        val eligibleIds = eligibleBookings.map { it.first.id }.toSet()

        var cancelledCount = 0
        for (staleId in existingIds) {
            if (staleId !in eligibleIds) {
                cancelReminder(appContext, staleId)
                cancelledCount++
            }
        }

        var scheduledCount = 0
        var exactCount = 0
        var inexactCount = 0
        for ((booking, _) in eligibleBookings) {
            when (val result = scheduleReminder(appContext, booking, nowMillis)) {
                is ReminderScheduleResult.Scheduled -> {
                    scheduledCount++
                    if (result.isExact) exactCount++ else inexactCount++
                }
                ReminderScheduleResult.SkippedOrCancelled -> Unit
            }
        }

        return ReminderSyncSummary(
            scheduledCount = scheduledCount,
            cancelledCount = cancelledCount,
            exactCount = exactCount,
            inexactFallbackCount = inexactCount
        )
    }

    /**
     * Clears every persisted reminder (e.g., on explicit sign-out or account deletion).
     */
    fun clearAllReminders(context: Context) {
        val appContext = context.applicationContext
        val existingIds = readPersistedRecords(appContext).keys.toList()
        for (id in existingIds) {
            cancelReminder(appContext, id)
        }
    }

    /**
     * Re-arms persisted active reminders after device reboot or exact-alarm permission change.
     */
    fun reschedulePersistedReminders(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        val appContext = context.applicationContext
        val stored = readPersistedRecords(appContext)
        if (stored.isEmpty()) {
            updateBootReceiverState(appContext, enabled = false)
            return 0
        }

        val activeRecords = mutableMapOf<String, ScheduledReminderRecord>()
        var rearmedCount = 0
        for ((bookingId, record) in stored) {
            if (record.triggerAtMillis > nowMillis) {
                val result = scheduleRecordInternal(appContext, record)
                if (result is ReminderScheduleResult.Scheduled) {
                    activeRecords[bookingId] = record.copy(isExact = result.isExact)
                    rearmedCount++
                }
            }
        }

        writePersistedRecords(appContext, activeRecords)
        updateBootReceiverState(appContext, enabled = activeRecords.isNotEmpty())
        return rearmedCount
    }

    fun getScheduledRecord(context: Context, bookingId: String): ScheduledReminderRecord? {
        return readPersistedRecords(context.applicationContext)[bookingId]
    }

    fun getAllScheduledRecords(context: Context): List<ScheduledReminderRecord> {
        return readPersistedRecords(context.applicationContext).values.sortedBy { it.triggerAtMillis }
    }

    internal fun markReminderFired(context: Context, bookingId: String) {
        val appContext = context.applicationContext
        val records = readPersistedRecords(appContext).toMutableMap()
        if (records.remove(bookingId) != null) {
            writePersistedRecords(appContext, records)
        }
        updateBootReceiverState(appContext, enabled = records.isNotEmpty())
    }

    internal fun requestCodeFor(bookingId: String): Int {
        return (bookingId.hashCode() and 0x0FFFFFFF) + 10_000
    }

    private fun buildReminderPendingIntent(
        context: Context,
        record: ScheduledReminderRecord,
        flags: Int
    ): PendingIntent? {
        val intent = buildReminderIntent(
            context = context,
            bookingId = record.bookingId,
            serviceType = record.serviceType,
            clientName = record.clientName,
            preferredDate = record.preferredDate,
            preferredTime = record.preferredTime,
            location = record.location,
            triggerAtMillis = record.triggerAtMillis
        )
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(record.bookingId),
            intent,
            flags
        )
    }

    private fun buildReminderIntent(
        context: Context,
        bookingId: String,
        serviceType: String,
        clientName: String,
        preferredDate: String,
        preferredTime: String,
        location: String,
        triggerAtMillis: Long
    ): Intent {
        return Intent(context, SessionReminderReceiver::class.java).apply {
            action = ACTION_SESSION_REMINDER
            putExtra(EXTRA_BOOKING_ID, bookingId)
            putExtra(EXTRA_SERVICE_TYPE, serviceType)
            putExtra(EXTRA_CLIENT_NAME, clientName)
            putExtra(EXTRA_PREFERRED_DATE, preferredDate)
            putExtra(EXTRA_PREFERRED_TIME, preferredTime)
            putExtra(EXTRA_LOCATION, location)
            putExtra(EXTRA_TRIGGER_AT_MILLIS, triggerAtMillis)
        }
    }

    private fun updateBootReceiverState(context: Context, enabled: Boolean) {
        try {
            val receiver = ComponentName(context, SessionReminderBootReceiver::class.java)
            val newState = if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            context.packageManager.setComponentEnabledSetting(
                receiver,
                newState,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            Log.w(TAG, "Unable to update boot receiver component state: ${e.message}")
        }
    }

    private fun readPersistedRecords(context: Context): Map<String, ScheduledReminderRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_RECORDS_JSON, null) ?: return emptyMap()
        val map = mutableMapOf<String, ScheduledReminderRecord>()
        try {
            val array = JSONArray(rawJson)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val bookingId = obj.optString("bookingId")
                if (bookingId.isBlank()) continue
                map[bookingId] = ScheduledReminderRecord(
                    bookingId = bookingId,
                    serviceType = obj.optString("serviceType"),
                    clientName = obj.optString("clientName"),
                    preferredDate = obj.optString("preferredDate"),
                    preferredTime = obj.optString("preferredTime"),
                    location = obj.optString("location"),
                    triggerAtMillis = obj.optLong("triggerAtMillis"),
                    isExact = obj.optBoolean("isExact", false)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse persisted reminders JSON: ${e.message}")
        }
        return map
    }

    private fun writePersistedRecords(
        context: Context,
        records: Map<String, ScheduledReminderRecord>
    ) {
        val array = JSONArray()
        records.values.forEach { rec ->
            val obj = JSONObject().apply {
                put("bookingId", rec.bookingId)
                put("serviceType", rec.serviceType)
                put("clientName", rec.clientName)
                put("preferredDate", rec.preferredDate)
                put("preferredTime", rec.preferredTime)
                put("location", rec.location)
                put("triggerAtMillis", rec.triggerAtMillis)
                put("isExact", rec.isExact)
            }
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECORDS_JSON, array.toString())
            .apply()
    }
}

/**
 * BroadcastReceiver invoked by AlarmManager 24 hours prior to a Confirmed studio session.
 */
class SessionReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != SessionReminderScheduler.ACTION_SESSION_REMINDER) return
        val bookingId = intent.getStringExtra(SessionReminderScheduler.EXTRA_BOOKING_ID).orEmpty()
        if (bookingId.isBlank()) return

        val serviceType = intent.getStringExtra(SessionReminderScheduler.EXTRA_SERVICE_TYPE).orEmpty()
        val clientName = intent.getStringExtra(SessionReminderScheduler.EXTRA_CLIENT_NAME).orEmpty()
        val preferredDate = intent.getStringExtra(SessionReminderScheduler.EXTRA_PREFERRED_DATE).orEmpty()
        val preferredTime = intent.getStringExtra(SessionReminderScheduler.EXTRA_PREFERRED_TIME).orEmpty()
        val location = intent.getStringExtra(SessionReminderScheduler.EXTRA_LOCATION).orEmpty()

        StudioNotificationManager.showSessionReminderNotification(
            context = context,
            bookingId = bookingId,
            serviceType = serviceType,
            clientName = clientName,
            preferredDate = preferredDate,
            preferredTime = preferredTime,
            location = location
        )

        SessionReminderScheduler.markReminderFired(context, bookingId)
    }
}

/**
 * BroadcastReceiver that re-arms upcoming 24-hour session reminders after device reboot
 * or when exact-alarm permission is granted by the user.
 */
class SessionReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val isBootCompleted = action == Intent.ACTION_BOOT_COMPLETED
        val isExactAlarmPermChanged = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED

        if (isBootCompleted || isExactAlarmPermChanged) {
            SessionReminderScheduler.reschedulePersistedReminders(context)
        }
    }
}
