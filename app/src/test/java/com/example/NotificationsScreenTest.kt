package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.model.AppLanguage
import com.example.data.model.AppointmentNotification
import com.example.data.model.StudioAnnouncement
import com.example.ui.components.StudioTopAppBar
import com.example.ui.screens.NotificationsScreen
import com.example.ui.theme.StarArtistTheme
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class NotificationsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleAnnouncements = listOf(
        StudioAnnouncement(
            id = "ann_1",
            authorId = "admin_1",
            authorName = "Chander Studio",
            title = "Monsoon Fashion Casting Call",
            message = "Models 18+ invited for editorial lookbook shoot at 4K Cinema Bay.",
            category = "Casting Call",
            priority = "urgent",
            actionRoute = "models",
            createdAt = Timestamp(Date(System.currentTimeMillis() - 60_000))
        )
    )

    private val sampleAppointmentNotifications = listOf(
        AppointmentNotification(
            id = "appt_1",
            userId = "user_1",
            bookingId = "bk_1",
            title = "Shoot Booking Confirmed!",
            message = "Your High-Fashion Photography session is confirmed for tomorrow.",
            status = "Confirmed",
            serviceType = "High-Fashion Photography",
            isRead = false,
            createdAt = Timestamp(Date())
        )
    )

    @Test
    fun notificationsScreen_displaysFcmPreferencesAndFiltersTabs() {
        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                NotificationsScreen(
                    language = AppLanguage.ENGLISH,
                    isAdmin = false,
                    fcmToken = "sample_token_12345678",
                    announcementsOptIn = true,
                    appointmentPushOptIn = true,
                    announcements = sampleAnnouncements,
                    appointmentNotifications = sampleAppointmentNotifications,
                    onBack = {},
                    onSyncFcm = {},
                    onUpdatePushPreferences = { _, _ -> },
                    onMarkNotificationRead = {},
                    onMarkAllNotificationsRead = {},
                    onNavigateToScreen = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("fcm_settings_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Token Ready ••••12345678", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("appointment_notif_card_appt_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notifications_list")
            .performScrollToNode(hasTestTag("announcement_card_ann_1"))
        composeTestRule.onNodeWithTag("announcement_card_ann_1").assertIsDisplayed()

        // Filter to Appointments tab only
        composeTestRule.onNodeWithTag("notif_tab_appointments").performClick()
        composeTestRule.onNodeWithTag("appointment_notif_card_appt_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("announcement_card_ann_1").assertDoesNotExist()

        // Filter to Announcements tab only
        composeTestRule.onNodeWithTag("notif_tab_announcements").performClick()
        composeTestRule.onNodeWithTag("announcement_card_ann_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("appointment_notif_card_appt_1").assertDoesNotExist()
    }

    @Test
    fun notificationsScreen_markReadAndPushToggles_invokeCallbacks() {
        var markedReadId: String? = null
        var markedAll = false
        var updatedAnnouncementsOpt: Boolean? = null
        var updatedAppointmentOpt: Boolean? = null
        var synced = false

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                NotificationsScreen(
                    language = AppLanguage.ENGLISH,
                    isAdmin = false,
                    fcmToken = "fcm_tok_98765432",
                    announcementsOptIn = true,
                    appointmentPushOptIn = true,
                    announcements = sampleAnnouncements,
                    appointmentNotifications = sampleAppointmentNotifications,
                    onBack = {},
                    onSyncFcm = { synced = true },
                    onUpdatePushPreferences = { ann, appt ->
                        updatedAnnouncementsOpt = ann
                        updatedAppointmentOpt = appt
                    },
                    onMarkNotificationRead = { markedReadId = it },
                    onMarkAllNotificationsRead = { markedAll = true },
                    onNavigateToScreen = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("sync_fcm_button").performClick()
        assertTrue(synced)

        composeTestRule.onNodeWithTag("toggle_announcements_push").performClick()
        assertFalse(updatedAnnouncementsOpt ?: true)
        assertTrue(updatedAppointmentOpt ?: false)

        composeTestRule.onNodeWithTag("mark_read_btn_appt_1").performClick()
        assertEquals("appt_1", markedReadId)

        composeTestRule.onNodeWithTag("mark_all_read_button").performClick()
        assertTrue(markedAll)
    }

    @Test
    fun notificationsScreen_adminBroadcastComposer_publishesAnnouncement() {
        var publishedTitle = ""
        var publishedMessage = ""

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                NotificationsScreen(
                    language = AppLanguage.ENGLISH,
                    isAdmin = true,
                    fcmToken = "admin_tok_11223344",
                    announcementsOptIn = true,
                    appointmentPushOptIn = true,
                    announcements = emptyList(),
                    appointmentNotifications = emptyList(),
                    onBack = {},
                    onSyncFcm = {},
                    onUpdatePushPreferences = { _, _ -> },
                    onMarkNotificationRead = {},
                    onMarkAllNotificationsRead = {},
                    onPublishAnnouncement = { title, message, _, _, _, onSuccess ->
                        publishedTitle = title
                        publishedMessage = message
                        onSuccess()
                    },
                    onNavigateToScreen = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("open_admin_broadcast_dialog_button").performClick()
        composeTestRule.onNodeWithTag("announcement_composer_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("announcement_title_input").performTextInput("Winter Bridal Offer")
        composeTestRule.onNodeWithTag("announcement_message_input").performTextInput("Book your 4K bridal shoot before Dec 15 for complimentary reel highlights.")
        composeTestRule.onNodeWithTag("publish_announcement_confirm_button").performClick()

        assertEquals("Winter Bridal Offer", publishedTitle)
        assertTrue(publishedMessage.contains("4K bridal shoot"))
    }

    @Test
    fun topAppBar_displaysUnreadNotificationBadge_andOpensInbox() {
        var opened = false
        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                StudioTopAppBar(
                    language = AppLanguage.ENGLISH,
                    isAdmin = false,
                    unreadNotificationsCount = 3,
                    onToggleLanguage = {},
                    onOpenNotifications = { opened = true },
                    onOpenPrivacy = {},
                    onSignOut = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("notifications_bell_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notifications_unread_badge", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("3", useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag("notifications_bell_button").performClick()
        assertTrue(opened)
    }
}
