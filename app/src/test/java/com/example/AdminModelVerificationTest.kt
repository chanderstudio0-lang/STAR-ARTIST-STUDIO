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
import com.example.data.model.ModelRegistration
import com.example.ui.components.AdminModelVerificationSection
import com.example.ui.components.filterAdminModelApplications
import com.example.ui.components.isPendingModelStatus
import com.example.ui.components.isRosterApprovedModelStatus
import com.example.ui.components.validateModelVerificationDecision
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.theme.StarArtistTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class AdminModelVerificationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleModels(): List<ModelRegistration> = listOf(
        ModelRegistration(
            id = "mdl_pending_1",
            userId = "u_anya",
            fullName = "Anya Kapoor",
            age = 22L,
            gender = "Female",
            city = "Sirsa",
            phoneNumber = "9812890081",
            skills = "Runway, Editorial",
            portfolioUrls = "https://instagram.com/anya_model",
            consentGiven = true,
            status = "pending",
            adminNotes = ""
        ),
        ModelRegistration(
            id = "mdl_pending_2",
            userId = "u_rohan",
            fullName = "Rohan Das",
            age = 20L,
            gender = "Male",
            city = "Hisar",
            phoneNumber = "9541024098",
            skills = "Commercial",
            portfolioUrls = "",
            consentGiven = true,
            status = "",
            adminNotes = ""
        ),
        ModelRegistration(
            id = "mdl_roster_1",
            userId = "u_meera",
            fullName = "Meera Sethi",
            age = 24L,
            gender = "Female",
            city = "Chandigarh",
            phoneNumber = "9988776655",
            skills = "Bridal, Couture",
            portfolioUrls = "https://instagram.com/meera_couture",
            consentGiven = true,
            status = "approved",
            adminNotes = "Official Roster - Bridal & Couture"
        ),
        ModelRegistration(
            id = "mdl_rejected_1",
            userId = "u_vikram",
            fullName = "Vikram Gill",
            age = 19L,
            gender = "Male",
            city = "Delhi",
            phoneNumber = "9001122334",
            skills = "Print",
            consentGiven = true,
            status = "rejected",
            adminNotes = "Portfolio link missing"
        )
    )

    @Test
    fun helperFunctions_filterStatusesAndEnforceManualVerificationRules() {
        assertTrue(isPendingModelStatus("pending"))
        assertTrue(isPendingModelStatus(""))
        assertFalse(isPendingModelStatus("approved"))
        assertTrue(isRosterApprovedModelStatus("approved"))

        val models = sampleModels()
        assertEquals(listOf("mdl_pending_1", "mdl_pending_2"), filterAdminModelApplications(models, "PENDING").map { it.id })
        assertEquals(listOf("mdl_roster_1"), filterAdminModelApplications(models, "APPROVED").map { it.id })
        assertEquals(listOf("mdl_rejected_1"), filterAdminModelApplications(models, "REJECTED").map { it.id })
        assertEquals(listOf("mdl_roster_1"), filterAdminModelApplications(models, "ALL", "Chandigarh").map { it.id })

        // Cannot add to roster unless manual check is confirmed
        assertNotNull(
            validateModelVerificationDecision(
                newStatus = "approved",
                age = 22L,
                consentGiven = true,
                manualChecksConfirmed = false,
                adminNotes = "",
                isHindi = false
            )
        )
        // Cannot add under-18 to roster even if checklist checked
        assertNotNull(
            validateModelVerificationDecision(
                newStatus = "approved",
                age = 17L,
                consentGiven = true,
                manualChecksConfirmed = true,
                adminNotes = "",
                isHindi = false
            )
        )
        // Succeeds when 18+, consent given, and manual verification confirmed
        assertNull(
            validateModelVerificationDecision(
                newStatus = "approved",
                age = 22L,
                consentGiven = true,
                manualChecksConfirmed = true,
                adminNotes = "Approved for commercial shoots",
                isHindi = false
            )
        )
        // Rejection requires brief admin reason
        assertNotNull(
            validateModelVerificationDecision(
                newStatus = "rejected",
                age = 20L,
                consentGiven = true,
                manualChecksConfirmed = false,
                adminNotes = "",
                isHindi = false
            )
        )
        assertNull(
            validateModelVerificationDecision(
                newStatus = "rejected",
                age = 20L,
                consentGiven = true,
                manualChecksConfirmed = false,
                adminNotes = "Incomplete portfolio",
                isHindi = false
            )
        )
    }

    @Test
    fun adminModelVerificationSection_enforcesManualCheckBeforeAddingToRoster_andConfirmsReject() {
        val updates = mutableListOf<Triple<String, String, String>>()

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                AdminModelVerificationSection(
                    models = sampleModels(),
                    isLoading = false,
                    language = AppLanguage.ENGLISH,
                    onUpdateModelStatus = { id, status, notes ->
                        updates.add(Triple(id, status, notes))
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("admin_model_verification_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_model_card_mdl_pending_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_model_card_mdl_roster_1").assertDoesNotExist()

        // Tapping Verify & Roster without checking manual check box shows inline validation error and blocks update
        composeTestRule.onNodeWithTag("admin_models_lazy_list")
            .performScrollToNode(hasTestTag("model_approve_button_mdl_pending_1"))
        composeTestRule.onNodeWithTag("model_approve_button_mdl_pending_1").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("model_verify_error_mdl_pending_1").assertIsDisplayed()
        assertTrue(updates.isEmpty())

        // Tick manual verification check, add admin note, and verify into Official Studio Roster
        composeTestRule.onNodeWithTag("admin_models_lazy_list")
            .performScrollToNode(hasTestTag("model_verify_check_mdl_pending_1"))
        composeTestRule.onNodeWithTag("model_verify_check_mdl_pending_1").performClick()
        composeTestRule.onNodeWithTag("model_admin_notes_input_mdl_pending_1")
            .performTextInput("Runway roster verified")
        composeTestRule.onNodeWithTag("model_approve_button_mdl_pending_1").performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(Triple("mdl_pending_1", "approved", "Runway roster verified")),
            updates
        )

        // Scroll to second pending applicant, add rejection reason, and confirm rejection dialog
        composeTestRule.onNodeWithTag("admin_models_lazy_list")
            .performScrollToNode(hasTestTag("admin_model_card_mdl_pending_2"))
        composeTestRule.onNodeWithTag("admin_models_lazy_list")
            .performScrollToNode(hasTestTag("model_quick_note_mdl_pending_2_1"))
        composeTestRule.onNodeWithTag("model_quick_note_mdl_pending_2_1").performClick()
        composeTestRule.onNodeWithTag("model_reject_button_mdl_pending_2").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("model_reject_dialog_mdl_pending_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("model_reject_confirm_mdl_pending_2").performClick()
        composeTestRule.waitForIdle()

        assertEquals(2, updates.size)
        assertEquals("mdl_pending_2", updates[1].first)
        assertEquals("rejected", updates[1].second)
        assertTrue(updates[1].third.contains("Incomplete portfolio"))

        // Switch to Official Roster chip to inspect verified roster members
        composeTestRule.onNodeWithTag("admin_model_filter_approved").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_model_card_mdl_roster_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("model_official_roster_badge_mdl_roster_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Meera Sethi").assertIsDisplayed()
    }

    @Test
    fun adminDashboardScreen_rendersModelVerificationSectionByDefaultWhenAuthenticated() {
        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                AdminDashboardScreen(
                    language = AppLanguage.ENGLISH,
                    isAdmin = true,
                    allModels = sampleModels(),
                    allBookings = emptyList(),
                    allGalleryItems = emptyList(),
                    onUpdateModelStatus = { _, _, _ -> },
                    onUpdateBookingStatus = { _, _ -> },
                    onUpdateGalleryStatus = { _, _ -> },
                    onDeleteGalleryItem = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("admin_dashboard_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_model_verification_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_roster_summary_badge").assertIsDisplayed()
    }
}
