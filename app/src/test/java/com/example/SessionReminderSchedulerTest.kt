package com.example

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppLanguage
import com.example.data.model.ShootBooking
import com.example.notification.ReminderScheduleResult
import com.example.notification.SessionReminderBootReceiver
import com.example.notification.SessionReminderReceiver
import com.example.notification.SessionReminderScheduler
import com.example.notification.StudioNotificationManager
import com.example.ui.screens.BookingScheduleHelper
import com.example.ui.screens.MyBookingsScreen
import com.example.ui.theme.StarArtistTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class SessionReminderSchedulerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager

    private val refNowMillis: Long = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 13, 9, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val refTodayMillis: Long =
        BookingScheduleHelper.parsePreferredDateMidnightMillis("2026-11-13")!!

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        SessionReminderScheduler.clearAllReminders(context)
    }

    @After
    fun tearDown() {
        SessionReminderScheduler.clearAllReminders(context)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @Test
    fun computeReminderTriggerAtMillis_subtracts24HoursOnlyForConfirmedFutureBookings() {
        val confirmedFuture = ShootBooking(
            id = "bk_conf_1",
            clientName = "Aarav",
            serviceType = "Editorial Fashion",
            preferredDate = "2026-11-20",
            preferredTime = "10:00 AM (Morning Key Light)",
            status = "Confirmed"
        )

        val expectedSessionStart = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val expectedTrigger = expectedSessionStart - TimeUnit.HOURS.toMillis(24)

        val actualTrigger = SessionReminderScheduler.computeReminderTriggerAtMillis(
            booking = confirmedFuture,
            nowMillis = refNowMillis
        )
        assertEquals(expectedTrigger, actualTrigger)

        // Pending, Cancelled, Completed bookings must not schedule a 24h reminder
        assertNull(
            SessionReminderScheduler.computeReminderTriggerAtMillis(
                confirmedFuture.copy(status = "Pending"),
                refNowMillis
            )
        )
        assertNull(
            SessionReminderScheduler.computeReminderTriggerAtMillis(
                confirmedFuture.copy(status = "Cancelled"),
                refNowMillis
            )
        )
        assertNull(
            SessionReminderScheduler.computeReminderTriggerAtMillis(
                confirmedFuture.copy(status = "Completed"),
                refNowMillis
            )
        )

        // Session starting in 12 hours (< 24 hours away) has a past reminder trigger -> returns null
        val within24h = confirmedFuture.copy(
            id = "bk_within_24",
            preferredDate = "2026-11-13",
            preferredTime = "09:00 PM"
        )
        assertNull(
            SessionReminderScheduler.computeReminderTriggerAtMillis(
                booking = within24h,
                nowMillis = refNowMillis
            )
        )
    }

    @Test
    fun scheduleReminder_armsExactAlarmManagerWakeupAndFallsBackWhenPermissionDenied() {
        val booking = ShootBooking(
            id = "bk_exact_1",
            clientName = "Meera",
            serviceType = "Bridal Portrait",
            preferredDate = "2026-11-20",
            preferredTime = "02:30 PM",
            location = "Studio Bay A",
            status = "Confirmed"
        )

        // Exact alarms permitted -> schedules exact wakeup alarm at 24h before
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val exactResult = SessionReminderScheduler.scheduleReminder(context, booking, refNowMillis)
        assertTrue(exactResult is ReminderScheduleResult.Scheduled)
        exactResult as ReminderScheduleResult.Scheduled
        assertTrue(exactResult.isExact)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals(1, scheduledAlarms.size)
        assertEquals(AlarmManager.RTC_WAKEUP, scheduledAlarms.first().type)
        assertEquals(exactResult.triggerAtMillis, scheduledAlarms.first().triggerAtTime)
        assertTrue(SessionReminderScheduler.getScheduledRecord(context, booking.id)?.isExact == true)

        // Exact alarms denied on Android 14+ -> degrades gracefully to inexact alarm without throwing
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val inexactResult = SessionReminderScheduler.scheduleReminder(context, booking, refNowMillis)
        assertTrue(inexactResult is ReminderScheduleResult.Scheduled)
        inexactResult as ReminderScheduleResult.Scheduled
        assertFalse(inexactResult.isExact)
        assertFalse(SessionReminderScheduler.getScheduledRecord(context, booking.id)?.isExact ?: true)
    }

    @Test
    fun syncConfirmedSessionReminders_cancelsAlarmsWhenBookingCancelledOrRemoved() {
        val booking1 = ShootBooking(
            id = "bk_sync_1",
            clientName = "Kabir",
            serviceType = "Portfolio Shoot",
            preferredDate = "2026-11-22",
            preferredTime = "11:00 AM",
            status = "Confirmed"
        )
        val booking2 = ShootBooking(
            id = "bk_sync_2",
            clientName = "Riya",
            serviceType = "Music Video",
            preferredDate = "2026-11-25",
            preferredTime = "04:00 PM",
            status = "Pending"
        )

        val firstSync = SessionReminderScheduler.syncConfirmedSessionReminders(
            context = context,
            bookings = listOf(booking1, booking2),
            nowMillis = refNowMillis
        )
        assertEquals(1, firstSync.scheduledCount)
        assertEquals(0, firstSync.cancelledCount)
        assertNotNull(SessionReminderScheduler.getScheduledRecord(context, "bk_sync_1"))
        assertNull(SessionReminderScheduler.getScheduledRecord(context, "bk_sync_2"))

        // Flip booking1 -> Cancelled, booking2 -> Confirmed
        val secondSync = SessionReminderScheduler.syncConfirmedSessionReminders(
            context = context,
            bookings = listOf(
                booking1.copy(status = "Cancelled"),
                booking2.copy(status = "Confirmed")
            ),
            nowMillis = refNowMillis
        )
        assertEquals(1, secondSync.scheduledCount)
        assertEquals(1, secondSync.cancelledCount)
        assertNull(SessionReminderScheduler.getScheduledRecord(context, "bk_sync_1"))
        assertNotNull(SessionReminderScheduler.getScheduledRecord(context, "bk_sync_2"))
    }

    @Test
    fun sessionReminderReceiver_postsNotificationAndBootReceiverRearmsPersistedAlarms() {
        val app = context.applicationContext as Application
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val booking = ShootBooking(
            id = "bk_rx_1",
            clientName = "Aisha",
            serviceType = "Editorial Portrait",
            preferredDate = "2026-11-21",
            preferredTime = "10:00 AM",
            location = "Talwara Khurd Studio",
            status = "Confirmed"
        )
        SessionReminderScheduler.scheduleReminder(context, booking, refNowMillis)
        assertEquals(1, SessionReminderScheduler.getAllScheduledRecords(context).size)

        // Simulate device reboot: re-arming persisted active alarms
        val bootReceiver = SessionReminderBootReceiver()
        bootReceiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertEquals(1, SessionReminderScheduler.getAllScheduledRecords(context).size)
        assertTrue(shadowAlarmManager.scheduledAlarms.isNotEmpty())

        // Simulate AlarmManager firing the BroadcastReceiver
        val receiver = SessionReminderReceiver()
        val fireIntent = Intent(context, SessionReminderReceiver::class.java).apply {
            action = SessionReminderScheduler.ACTION_SESSION_REMINDER
            putExtra(SessionReminderScheduler.EXTRA_BOOKING_ID, booking.id)
            putExtra(SessionReminderScheduler.EXTRA_SERVICE_TYPE, booking.serviceType)
            putExtra(SessionReminderScheduler.EXTRA_CLIENT_NAME, booking.clientName)
            putExtra(SessionReminderScheduler.EXTRA_PREFERRED_DATE, booking.preferredDate)
            putExtra(SessionReminderScheduler.EXTRA_PREFERRED_TIME, booking.preferredTime)
            putExtra(SessionReminderScheduler.EXTRA_LOCATION, booking.location)
        }
        receiver.onReceive(context, fireIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val posted = shadowOf(notificationManager).allNotifications
        assertEquals(1, posted.size)
        assertEquals(StudioNotificationManager.CHANNEL_ID_SESSION_REMINDERS, posted.first().channelId)

        // Fired record is cleared so it won't re-fire after next reboot
        assertNull(SessionReminderScheduler.getScheduledRecord(context, booking.id))
    }

    @Test
    fun myBookingsScreen_displaysReminderBadgeAndExactAlarmBannerAppropriately() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        val bookings = listOf(
            ShootBooking(
                id = "bk_ui_future",
                clientName = "Dev",
                serviceType = "Fashion Lookbook",
                preferredDate = "2026-11-20",
                preferredTime = "10:00 AM",
                status = "Confirmed"
            ),
            ShootBooking(
                id = "bk_ui_within24",
                clientName = "Dev",
                serviceType = "Quick Headshots",
                preferredDate = "2026-11-14",
                preferredTime = "08:00 AM",
                status = "Confirmed"
            ),
            ShootBooking(
                id = "bk_ui_pending",
                clientName = "Dev",
                serviceType = "Music Video",
                preferredDate = "2026-11-22",
                preferredTime = "11:00 AM",
                status = "Pending"
            )
        )

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                MyBookingsScreen(
                    language = AppLanguage.ENGLISH,
                    myBookings = bookings,
                    referenceTodayMillis = refTodayMillis,
                    referenceNowMillis = refNowMillis,
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Exact alarm settings banner appears when denied on API 31+ and future confirmed reminder exists
        composeTestRule.onNodeWithTag("my_bookings_exact_alarm_banner").assertIsDisplayed()

        // Confirmed <24h card shows within-24h status
        composeTestRule.onNodeWithTag("my_bookings_list")
            .performScrollToNode(hasTestTag("my_booking_reminder_bk_ui_within24"))
        composeTestRule.onNodeWithTag("my_booking_reminder_bk_ui_within24")
            .assertIsDisplayed()
            .assertTextContains("Session starts within 24 hours")

        // Confirmed >24h card shows 24h reminder scheduled timestamp
        composeTestRule.onNodeWithTag("my_bookings_list")
            .performScrollToNode(hasTestTag("my_booking_reminder_bk_ui_future"))
        composeTestRule.onNodeWithTag("my_booking_reminder_bk_ui_future")
            .assertIsDisplayed()
            .assertTextContains("24h reminder scheduled", substring = true)

        // Pending booking must not display a 24h reminder badge
        composeTestRule.onNodeWithTag("my_booking_reminder_bk_ui_pending").assertDoesNotExist()
    }
}
