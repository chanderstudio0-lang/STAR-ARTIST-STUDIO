package com.example

import android.app.Application
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.SuccessResult
import com.example.data.local.entity.ProjectDraftEntity
import com.example.data.model.AppLanguage
import com.example.data.model.ShootBooking
import com.example.data.model.UserProfile
import com.example.ui.screens.UserProfileScreen
import com.example.ui.screens.buildArtistProfileSummaryText
import com.example.ui.theme.StarArtistTheme
import com.google.firebase.Timestamp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class UserProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUpCoil() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fakeLoader = ImageLoader.Builder(context)
            .components {
                add(coil.intercept.Interceptor { chain ->
                    SuccessResult(
                        drawable = ColorDrawable(Color.DKGRAY),
                        request = chain.request,
                        dataSource = DataSource.MEMORY_CACHE
                    )
                })
            }
            .build()
        Coil.setImageLoader(fakeLoader)
    }

    @After
    fun tearDownCoil() {
        Coil.reset()
    }

    private fun sampleProfile(): UserProfile = UserProfile(
        id = "artist_uid_1",
        userId = "artist_uid_1",
        email = "simran@starartist.in",
        displayName = "Simranjeet Kaur",
        avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=256",
        phoneNumber = "+91 9812890081",
        city = "Sirsa, Haryana",
        instagramHandle = "@simran_star",
        portfolioLinks = listOf(
            "https://instagram.com/simran_star",
            "https://behance.net/simran_star"
        ),
        specialty = "Fashion Model",
        bio = "Editorial and bridal catalog artist based in Sirsa & NCR.",
        updatedAt = Timestamp(Date())
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun userProfileScreen_editsContactLinksAndBio_andSavesToFirestoreCallback() {
        var savedName = ""
        var savedPhone = ""
        var savedCity = ""
        var savedInstagram = ""
        var savedSpecialty = ""
        var savedBio = ""
        var savedLinks = emptyList<String>()

        composeTestRule.setContent {
            StarArtistTheme {
                UserProfileScreen(
                    userProfile = sampleProfile(),
                    userEmail = "simran@starartist.in",
                    currentUserId = "artist_uid_1",
                    modelsCount = 2,
                    bookingsCount = 3,
                    myBookings = emptyList(),
                    savedProjects = emptyList(),
                    isEmailVerified = true,
                    isSubmitting = false,
                    isStudioLight = false,
                    language = AppLanguage.ENGLISH,
                    onBack = {},
                    onUpdateArtistProfile = { name, _, phone, city, ig, specialty, bio, links, onComplete ->
                        savedName = name
                        savedPhone = phone
                        savedCity = city
                        savedInstagram = ig
                        savedSpecialty = specialty
                        savedBio = bio
                        savedLinks = links
                        onComplete(true, "Artist profile, contact & portfolio saved!")
                    },
                    onToggleTheme = {},
                    onSelectLanguage = {},
                    onOpenPrivacy = {},
                    onDeleteMyData = {},
                    onSignOut = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify initial Hero Identity & Bio preview
        composeTestRule.onNodeWithTag("artist_identity_hero_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_bio_preview")
            .assertTextContains("Editorial and bridal", substring = true)

        // Scroll to Contact Info card & update fields
        composeTestRule.onNodeWithTag("user_profile_list")
            .performScrollToNode(hasTestTag("profile_contact_info_card"))
        composeTestRule.onNodeWithTag("profile_display_name_input").performTextClearance()
        composeTestRule.onNodeWithTag("profile_display_name_input").performTextInput("Simran Kapoor")
        composeTestRule.onNodeWithTag("profile_phone_input").performTextClearance()
        composeTestRule.onNodeWithTag("profile_phone_input").performTextInput("+91 9541024098")
        composeTestRule.onNodeWithTag("profile_city_input").performTextClearance()
        composeTestRule.onNodeWithTag("profile_city_input").performTextInput("Ellenabad, Sirsa")

        // Scroll to bottom actions so Reset button is composed & visible
        composeTestRule.onNodeWithTag("user_profile_list")
            .performScrollToNode(hasTestTag("profile_save_button"))
        composeTestRule.onNodeWithTag("profile_reset_button").assertIsDisplayed()

        // Scroll to Portfolio Links card, remove second link, add a new normalized link
        composeTestRule.onNodeWithTag("user_profile_list")
            .performScrollToNode(hasTestTag("profile_portfolio_links_card"))
        composeTestRule.onNodeWithTag("profile_portfolio_links_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_remove_portfolio_link_1").performClick()
        composeTestRule.onNodeWithTag("profile_portfolio_url_input").performTextInput("youtube.com/@simran_reels")
        composeTestRule.onNodeWithTag("profile_add_portfolio_link_button").performClick()

        // Scroll to Bio card, switch specialty to Photographer & update bio
        composeTestRule.onNodeWithTag("user_profile_list")
            .performScrollToNode(hasTestTag("profile_bio_card"))
        composeTestRule.onNodeWithText("Photographer").performClick()
        composeTestRule.onNodeWithTag("profile_bio_input").performTextClearance()
        composeTestRule.onNodeWithTag("profile_bio_input").performTextInput("4K portrait photographer & runway artist.")

        // Scroll to Save button & Save to Firestore
        composeTestRule.onNodeWithTag("user_profile_list")
            .performScrollToNode(hasTestTag("profile_save_button"))
        composeTestRule.onNodeWithTag("profile_save_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Simran Kapoor", savedName)
        assertEquals("+91 9541024098", savedPhone)
        assertEquals("Ellenabad, Sirsa", savedCity)
        assertEquals("@simran_star", savedInstagram)
        assertEquals("Photographer", savedSpecialty)
        assertEquals("4K portrait photographer & runway artist.", savedBio)
        assertEquals(
            listOf("https://instagram.com/simran_star", "https://youtube.com/@simran_reels"),
            savedLinks
        )

        // Success banner appears and unsaved Reset button disappears once baseline syncs
        composeTestRule.onNodeWithTag("profile_save_success_banner").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("profile_reset_button").assertCountEquals(0)
    }

    @Test
    fun userProfileScreen_validatesInvalidInputAndSupportsReset() {
        var saveCalled = false

        composeTestRule.setContent {
            StarArtistTheme {
                UserProfileScreen(
                    userProfile = sampleProfile(),
                    userEmail = "simran@starartist.in",
                    currentUserId = "artist_uid_1",
                    modelsCount = 1,
                    bookingsCount = 1,
                    myBookings = emptyList(),
                    savedProjects = emptyList(),
                    isEmailVerified = true,
                    isSubmitting = false,
                    isStudioLight = false,
                    language = AppLanguage.ENGLISH,
                    onBack = {},
                    onUpdateArtistProfile = { _, _, _, _, _, _, _, _, _ ->
                        saveCalled = true
                    },
                    onToggleTheme = {},
                    onSelectLanguage = {},
                    onOpenPrivacy = {},
                    onDeleteMyData = {},
                    onSignOut = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Short display name should fail validation
        composeTestRule.onNodeWithTag("user_profile_list")
            .performScrollToNode(hasTestTag("profile_contact_info_card"))
        composeTestRule.onNodeWithTag("profile_display_name_input").performTextClearance()
        composeTestRule.onNodeWithTag("profile_display_name_input").performTextInput("S")
        composeTestRule.onNodeWithTag("user_profile_list")
            .performScrollToNode(hasTestTag("profile_save_button"))
        composeTestRule.onNodeWithTag("profile_save_button").performClick()
        composeTestRule.waitForIdle()

        assertFalse(saveCalled)
        composeTestRule.onNodeWithTag("profile_error_banner").assertIsDisplayed()

        // Clicking Reset restores synced displayName cleanly
        composeTestRule.onNodeWithTag("profile_reset_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("user_profile_list")
            .performScrollToNode(hasTestTag("profile_contact_info_card"))
        composeTestRule.onNodeWithTag("profile_display_name_input")
            .assertTextContains("Simranjeet Kaur", substring = true)
        composeTestRule.onAllNodesWithTag("profile_reset_button").assertCountEquals(0)
    }

    @Test
    fun userProfileScreen_tabsAndCopySummaryWorkInLightAndHindiModes() {
        val sampleBookings = listOf(
            ShootBooking(
                id = "bk_1",
                clientName = "Simranjeet Kaur",
                contactNumber = "9812890081",
                serviceType = "Bridal Portrait Shoot",
                preferredDate = "2026-12-01",
                preferredTime = "11:00 AM",
                status = "Confirmed"
            )
        )
        val sampleDrafts = listOf(
            ProjectDraftEntity(
                id = 1L,
                draftTitle = "Gold Rim Light Series",
                projectType = "Photography",
                conceptNotes = "Amber backlight with dark velvet backdrop"
            )
        )

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = true) {
                UserProfileScreen(
                    userProfile = sampleProfile(),
                    userEmail = "simran@starartist.in",
                    currentUserId = "artist_uid_1",
                    modelsCount = 1,
                    bookingsCount = 1,
                    myBookings = sampleBookings,
                    savedProjects = sampleDrafts,
                    isEmailVerified = true,
                    isSubmitting = false,
                    isStudioLight = true,
                    language = AppLanguage.HINDI,
                    onBack = {},
                    onUpdateArtistProfile = { _, _, _, _, _, _, _, _, onComplete ->
                        onComplete(true, "Saved")
                    },
                    onToggleTheme = {},
                    onSelectLanguage = {},
                    onOpenPrivacy = {},
                    onDeleteMyData = {},
                    onSignOut = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Copy summary card action in Hindi
        composeTestRule.onNodeWithTag("user_profile_list")
            .performScrollToNode(hasTestTag("profile_firestore_sync_card"))
        composeTestRule.onNodeWithTag("profile_copy_summary_button").performClick()
        composeTestRule.onNodeWithTag("profile_save_success_banner").assertIsDisplayed()

        // Switch to Activity tab & verify booking + draft are visible
        composeTestRule.onNodeWithTag("user_profile_list")
            .performScrollToNode(hasTestTag("user_profile_tabs"))
        composeTestRule.onNodeWithTag("profile_tab_activity").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Bridal Portrait Shoot").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Gold Rim Light Series").performScrollTo().assertIsDisplayed()

        // Switch to Account tab & verify Sign Out / Delete Data controls exist
        composeTestRule.onNodeWithTag("user_profile_list")
            .performScrollToNode(hasTestTag("user_profile_tabs"))
        composeTestRule.onNodeWithTag("profile_tab_account").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_screen_sign_out_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_screen_delete_data_button").assertIsDisplayed()
    }

    @Test
    fun buildArtistProfileSummaryText_formatsContactLinksAndBioCleanly() {
        val summary = buildArtistProfileSummaryText(
            displayName = "Simranjeet Kaur",
            specialty = "Fashion Model",
            city = "Sirsa, Haryana",
            phoneNumber = "+91 9812890081",
            email = "simran@starartist.in",
            instagramHandle = "@simran_star",
            portfolioLinks = listOf("https://behance.net/simran_star"),
            bio = "Runway & editorial talent."
        )
        assertTrue(summary.contains("Simranjeet Kaur • Fashion Model"))
        assertTrue(summary.contains("Sirsa, Haryana"))
        assertTrue(summary.contains("+91 9812890081"))
        assertTrue(summary.contains("https://behance.net/simran_star"))
        assertTrue(summary.contains("Runway & editorial talent."))
    }

    @Test
    fun userProfileScreen_activityTab_separatesUpcomingAndPastAppointments_andTriggersActions() {
        var openBookingsCalled = false
        var bookedServiceType = ""

        val mixedBookings = listOf(
            ShootBooking(
                id = "bk_upcoming_1",
                clientName = "Simranjeet Kaur",
                contactNumber = "9812890081",
                serviceType = "Photography (High-Fashion Editorial)",
                preferredDate = "2099-12-20",
                preferredTime = "10:00 AM",
                status = "Confirmed"
            ),
            ShootBooking(
                id = "bk_past_1",
                clientName = "Simranjeet Kaur",
                contactNumber = "9812890081",
                serviceType = "Modeling (Portfolio & Comp-Card Shoot)",
                preferredDate = "2024-02-14",
                preferredTime = "02:00 PM",
                status = "Completed"
            )
        )

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                UserProfileScreen(
                    userProfile = sampleProfile(),
                    userEmail = "simran@starartist.in",
                    currentUserId = "artist_uid_1",
                    modelsCount = 1,
                    bookingsCount = 2,
                    myBookings = mixedBookings,
                    savedProjects = emptyList(),
                    isEmailVerified = true,
                    isSubmitting = false,
                    isStudioLight = false,
                    language = AppLanguage.ENGLISH,
                    onBack = {},
                    onUpdateArtistProfile = { _, _, _, _, _, _, _, _, onComplete ->
                        onComplete(true, "Saved")
                    },
                    onToggleTheme = {},
                    onSelectLanguage = {},
                    onOpenPrivacy = {},
                    onDeleteMyData = {},
                    onSignOut = {},
                    onBookService = { bookedServiceType = it },
                    onOpenMyBookings = { openBookingsCalled = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("user_profile_list")
            .performScrollToNode(hasTestTag("user_profile_tabs"))
        composeTestRule.onNodeWithTag("profile_tab_activity").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_appointments_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_upcoming_appointments_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_upcoming_booking_bk_upcoming_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_past_appointments_section")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_past_booking_bk_past_1")
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("profile_open_my_bookings_button")
            .performScrollTo()
            .performClick()
        assertTrue(openBookingsCalled)

        composeTestRule.onNodeWithTag("profile_book_new_shoot_button")
            .performScrollTo()
            .performClick()
        assertTrue(bookedServiceType.isNotBlank())
    }

    @Test
    fun userProfileScreen_favoritesTab_managesCategoryFilterBookingAndToggle() {
        val toggledIds = mutableListOf<String>()
        var bookedServiceType = ""
        var openCatalogCalled = false

        composeTestRule.setContent {
            var currentFavorites by remember {
                mutableStateOf(setOf("pkg_photo_fashion"))
            }
            StarArtistTheme(isStudioLight = false) {
                UserProfileScreen(
                    userProfile = sampleProfile(),
                    userEmail = "simran@starartist.in",
                    currentUserId = "artist_uid_1",
                    modelsCount = 1,
                    bookingsCount = 0,
                    myBookings = emptyList(),
                    savedProjects = emptyList(),
                    favoritePackageIds = currentFavorites,
                    isEmailVerified = true,
                    isSubmitting = false,
                    isStudioLight = false,
                    language = AppLanguage.ENGLISH,
                    onBack = {},
                    onUpdateArtistProfile = { _, _, _, _, _, _, _, _, onComplete ->
                        onComplete(true, "Saved")
                    },
                    onToggleFavoritePackage = { pkg ->
                        toggledIds.add(pkg.id)
                        currentFavorites = if (pkg.id in currentFavorites) {
                            currentFavorites - pkg.id
                        } else {
                            currentFavorites + pkg.id
                        }
                    },
                    onToggleTheme = {},
                    onSelectLanguage = {},
                    onOpenPrivacy = {},
                    onDeleteMyData = {},
                    onSignOut = {},
                    onBookService = { bookedServiceType = it },
                    onOpenServiceCatalog = { openCatalogCalled = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("user_profile_list")
            .performScrollToNode(hasTestTag("user_profile_tabs"))
        composeTestRule.onNodeWithTag("profile_tab_favorites").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_favorites_tab_content").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_favorite_card_pkg_photo_fashion").assertIsDisplayed()

        // Filter to Video Production -> empty in saved until added, category pill reads "Video Production"
        composeTestRule.onNodeWithTag("profile_favorites_category_production")
            .assertTextContains("Video Production")
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_favorites_empty_card").assertIsDisplayed()

        // Save Video Production package via quick bookmark button
        composeTestRule.onNodeWithTag("profile_quick_fav_pkg_video_music")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertTrue("pkg_video_music" in toggledIds)

        composeTestRule.onNodeWithTag("profile_favorite_card_pkg_video_music")
            .performScrollTo()
            .assertIsDisplayed()

        // Book from saved favorite
        composeTestRule.onNodeWithTag("profile_favorite_book_pkg_video_music")
            .performScrollTo()
            .performClick()
        assertEquals("Video (4K Cinema Music Video Direction)", bookedServiceType)

        // Remove saved favorite
        composeTestRule.onNodeWithTag("profile_remove_fav_pkg_video_music")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_favorites_empty_card")
            .performScrollTo()
            .assertIsDisplayed()

        // Full Catalog shortcut callback works
        composeTestRule.onNodeWithTag("profile_open_catalog_button")
            .performScrollTo()
            .performClick()
        assertTrue(openCatalogCalled)
    }
}
