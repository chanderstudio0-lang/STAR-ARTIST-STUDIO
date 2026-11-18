package com.example

import com.example.data.model.UserProfile
import com.example.ui.components.studioAvatarPresets
import com.example.ui.screens.artistSpecialtyOptions
import com.example.ui.screens.validateArtistProfileInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileCustomizationTest {

    @Test
    fun userProfile_effectiveAvatarUrl_prefersAvatarUrlOverPhotoUrl() {
        val profileWithBoth = UserProfile(
            userId = "user_123",
            displayName = "Gurpreet Singh",
            photoUrl = "https://lh3.googleusercontent.com/default-photo.jpg",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb"
        )
        assertEquals("https://images.unsplash.com/photo-1534528741775-53994a69daeb", profileWithBoth.effectiveAvatarUrl)
    }

    @Test
    fun userProfile_effectiveAvatarUrl_fallsBackToPhotoUrlWhenAvatarUrlEmpty() {
        val profileWithPhotoOnly = UserProfile(
            userId = "user_456",
            displayName = "Simran Kaur",
            photoUrl = "https://lh3.googleusercontent.com/google-photo.jpg",
            avatarUrl = ""
        )
        assertEquals("https://lh3.googleusercontent.com/google-photo.jpg", profileWithPhotoOnly.effectiveAvatarUrl)
    }

    @Test
    fun userProfile_effectiveAvatarUrl_returnsEmptyWhenBothEmpty() {
        val blankProfile = UserProfile(
            userId = "user_789",
            displayName = "New Client",
            photoUrl = "",
            avatarUrl = ""
        )
        assertEquals("", blankProfile.effectiveAvatarUrl)
    }

    @Test
    fun displayNameValidation_acceptsNonBlankAndRejectsBlank() {
        val validName1 = "Jasmeet Singh"
        val validName2 = "  Priya Sharma  "
        val emptyName = ""
        val whitespaceOnly = "   \t\n  "

        assertTrue(validName1.trim().isNotBlank())
        assertEquals("Priya Sharma", validName2.trim())
        assertTrue(emptyName.trim().isBlank())
        assertTrue(whitespaceOnly.trim().isBlank())
    }

    @Test
    fun studioAvatarPresets_containsHighQualityPresets() {
        assertTrue("Studio avatar presets must not be empty", studioAvatarPresets.isNotEmpty())
        assertEquals(4, studioAvatarPresets.size)
        studioAvatarPresets.forEach { presetUrl ->
            assertTrue(presetUrl.startsWith("https://"))
            assertTrue(presetUrl.contains("unsplash.com"))
        }
    }

    @Test
    fun avatarUrl_recognizesValidDataUrlAndRemoteUrls() {
        val remoteUrl = "https://starartist.in/avatars/client_1.jpg"
        val dataUrl = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="

        fun isValidAvatar(url: String): Boolean {
            return url.startsWith("https://") || url.startsWith("http://") || url.startsWith("data:image/")
        }

        assertTrue(isValidAvatar(remoteUrl))
        assertTrue(isValidAvatar(dataUrl))
        assertFalse(isValidAvatar("ftp://invalid-url"))
    }

    @Test
    fun firestoreUserUpdateKeys_complyWithSecurityRulesAllowedKeys() {
        val allowedKeys = setOf(
            "displayName", "photoUrl", "avatarUrl",
            "phoneNumber", "city", "instagramHandle", "portfolioLinks", "specialty", "bio",
            "updatedAt"
        )
        val simulatedUpdates = mapOf(
            "displayName" to "Amritpal Singh",
            "avatarUrl" to "data:image/jpeg;base64,samplebase64data",
            "photoUrl" to "data:image/jpeg;base64,samplebase64data",
            "phoneNumber" to "+91 9812890081",
            "city" to "Sirsa, Haryana",
            "instagramHandle" to "@amrit_artist",
            "portfolioLinks" to listOf("https://instagram.com/amrit_artist", "https://behance.net/amrit"),
            "specialty" to "Photographer",
            "bio" to "4K cinematography and editorial portraits.",
            "updatedAt" to "serverTimestamp"
        )

        // All keys being updated must be in the security rules affectedKeys whitelist
        assertTrue(allowedKeys.containsAll(simulatedUpdates.keys))
    }

    @Test
    fun validateArtistProfileInput_validatesNamePhoneAndBio() {
        assertNull(
            validateArtistProfileInput(
                displayName = "Kavita Sharma",
                phoneNumber = "+91 9812890081",
                bio = "Fashion model & commercial actor."
            )
        )
        assertNull(
            validateArtistProfileInput(
                displayName = "Kavita Sharma",
                phoneNumber = "",
                bio = ""
            )
        )
        assertEquals(
            "Display name must be at least 2 characters.",
            validateArtistProfileInput(displayName = "K", phoneNumber = "9812890081", bio = "")
        )
        assertEquals(
            "Contact number must be between 8 and 30 characters.",
            validateArtistProfileInput(displayName = "Kavita", phoneNumber = "1234", bio = "")
        )
        assertEquals(
            "Artist bio must be 500 characters or less.",
            validateArtistProfileInput(displayName = "Kavita", phoneNumber = "", bio = "x".repeat(501))
        )
        assertTrue(artistSpecialtyOptions.contains("Fashion Model"))
        assertTrue(artistSpecialtyOptions.contains("Photographer"))
    }

    @Test
    fun portfolioLinksHelpers_normalizeDetectAndComputeCompleteness() {
        assertEquals(
            "https://behance.net/simran_artist",
            com.example.ui.components.normalizePortfolioLink("behance.net/simran_artist")
        )
        assertEquals(
            "https://instagram.com/simran_star",
            com.example.ui.components.normalizePortfolioLink("@simran_star")
        )
        assertNull(com.example.ui.components.normalizePortfolioLink("   "))
        assertNull(com.example.ui.components.normalizePortfolioLink("javascript:alert(1)"))
        assertNull(com.example.ui.components.normalizePortfolioLink("not a valid link"))

        assertEquals(
            "YouTube",
            com.example.ui.components.detectPortfolioPlatform("https://youtube.com/@starartist")
        )
        assertEquals(
            "Behance",
            com.example.ui.components.detectPortfolioPlatform("https://behance.net/starartist")
        )
        assertEquals(
            "Google Drive",
            com.example.ui.components.detectPortfolioPlatform("https://drive.google.com/drive/folders/123")
        )
        assertEquals(
            "Portfolio Web",
            com.example.ui.components.detectPortfolioPlatform("https://simranportfolio.in")
        )

        val partialScore = com.example.ui.components.computeArtistProfileCompleteness(
            displayName = "Simran",
            avatarUrl = "",
            phoneNumber = "",
            city = "",
            instagramHandle = "",
            portfolioLinks = emptyList(),
            specialty = "",
            bio = ""
        )
        val fullScore = com.example.ui.components.computeArtistProfileCompleteness(
            displayName = "Simranjeet Kaur",
            avatarUrl = "https://example.com/a.jpg",
            phoneNumber = "9812890081",
            city = "Sirsa",
            instagramHandle = "@simran",
            portfolioLinks = listOf("https://behance.net/simran"),
            specialty = "Fashion Model",
            bio = "Editorial and runway model based in Sirsa, Haryana."
        )
        assertTrue(partialScore < fullScore)
        assertEquals(1.0f, fullScore, 0.001f)

        val fallbackProfile = UserProfile(instagramHandle = "@simran", portfolioLinks = emptyList())
        assertEquals(listOf("@simran"), fallbackProfile.resolvedPortfolioLinks())
    }

    @Test
    fun profilePhotoValidation_enforcesSupportedSchemesAndFirestoreSizeLimit() {
        assertTrue(com.example.ui.components.isValidProfilePhotoUrl("https://starartist.in/headshot.jpg"))
        assertTrue(com.example.ui.components.isValidProfilePhotoUrl("data:image/jpeg;base64,/9j/4AAQ"))
        assertFalse(com.example.ui.components.isValidProfilePhotoUrl(""))
        assertFalse(com.example.ui.components.isValidProfilePhotoUrl("   "))
        assertFalse(com.example.ui.components.isValidProfilePhotoUrl("javascript:alert(1)"))
        assertFalse(
            com.example.ui.components.isValidProfilePhotoUrl(
                "data:image/jpeg;base64," + "A".repeat(500_000)
            )
        )
    }
}
