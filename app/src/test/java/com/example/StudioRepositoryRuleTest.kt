package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ModelRegistration
import com.example.data.model.ShootBooking
import com.example.data.repository.StudioRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createBooking_authenticatedUser_succeedsAndPersists() = runBlocking {
        signInTestUser(TEST_USER_EMAIL)
        val repository = StudioRepository(firestore)

        val booking = ShootBooking(
            clientName = "Kavita Sharma",
            contactNumber = "9812890081",
            serviceType = "High-Fashion Photography",
            preferredDate = "2026-12-10",
            preferredTime = "12:00 PM",
            location = "Studio (Talwara Khurd)"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBooking(booking)
        }
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun createBooking_allServiceTypes_modelingPhotographyVideoProduction_persistsAndObserves() = runBlocking {
        val uid = signInTestUser(TEST_USER_EMAIL)
        val repository = StudioRepository(firestore)

        val services = listOf("Modeling", "Photography", "Video Production")
        val createdIds = mutableListOf<String>()

        services.forEachIndexed { index, service ->
            val booking = ShootBooking(
                clientName = "Simranjeet Kaur",
                contactNumber = "9812890081",
                serviceType = service,
                preferredDate = "2026-12-${15 + index}",
                preferredTime = "10:00 AM (Morning Key Light Bay)",
                location = "Studio (Talwara Khurd - 4K Lighting Bay)",
                notes = "Session request for $service"
            )
            val res = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createBooking(booking) }
            assertTrue("Expected $service booking to succeed: ${res.exceptionOrNull()}", res.isSuccess)
            val docId = res.getOrNull()
            assertNotNull(docId)
            assertTrue(docId!!.isNotBlank())
            createdIds.add(docId)
        }

        val myBookings = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeMyBookings(uid).first { list ->
                services.all { svc -> list.any { it.serviceType == svc } }
            }
        }
        assertEquals(3, createdIds.distinct().size)
        services.forEach { svc ->
            val saved = myBookings.first { it.serviceType == svc }
            assertEquals("Pending", saved.status)
            assertEquals(uid, saved.userId)
        }

        // Invalid booking fails cleanly
        val invalidRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBooking(
                ShootBooking(
                    clientName = "A",
                    contactNumber = "123",
                    serviceType = "Video Production",
                    preferredDate = "2026-12-20"
                )
            )
        }
        assertTrue(invalidRes.isFailure)
    }

    @Test
    fun submitModelRegistration_adultUser_succeeds() = runBlocking {
        signInTestUser(TEST_USER_EMAIL)
        val repository = StudioRepository(firestore)

        val model = ModelRegistration(
            fullName = "Ananya Singh",
            age = 20,
            gender = "Female",
            city = "Sirsa",
            phoneNumber = "9812890081",
            skills = "Ramp, Commercial",
            consentGiven = true,
            status = "pending"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitModelRegistration(model)
        }
        assertTrue(result.isSuccess)
    }

    @Test
    fun observeMyBookings_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = StudioRepository(firestore)

        var caughtException: Exception? = null
        try {
            withTimeout(3000L) {
                repository.observeMyBookings("unauth_user_id").first()
            }
        } catch (e: Exception) {
            caughtException = e
        }
        assertNotNull(caughtException)
        val firestoreEx = caughtException?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun updateUserProfile_authenticatedArtist_persistsContactDisplayNameAndAvatar() = runBlocking {
        val uid = signInTestUser(TEST_USER_EMAIL)
        val repository = StudioRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateUserProfile(
                userId = uid,
                email = TEST_USER_EMAIL,
                displayName = "Artist Initial"
            )
        }

        val expectedLinks = listOf(
            "https://instagram.com/simran_star",
            "https://behance.net/simran_star",
            "https://youtube.com/@simran_star"
        )
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateUserProfile(
                userId = uid,
                displayName = "Simranjeet Kaur",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=256",
                phoneNumber = "+91 9812890081",
                city = "Sirsa, Haryana",
                instagramHandle = "@simran_star",
                specialty = "Fashion Model",
                bio = "Runway and bridal portfolio artist.",
                portfolioLinks = expectedLinks
            )
        }

        assertTrue(updateResult.isSuccess)
        val updated = updateResult.getOrNull()
        assertNotNull(updated)
        assertEquals("Simranjeet Kaur", updated?.displayName)
        assertEquals("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=256", updated?.effectiveAvatarUrl)
        assertEquals("+91 9812890081", updated?.phoneNumber)
        assertEquals("Sirsa, Haryana", updated?.city)
        assertEquals("@simran_star", updated?.instagramHandle)
        assertEquals(expectedLinks, updated?.portfolioLinks)
        assertEquals("Fashion Model", updated?.specialty)
        assertEquals("Runway and bridal portfolio artist.", updated?.bio)

        val observed = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it?.displayName == "Simranjeet Kaur" }
        }
        assertNotNull(observed)
        assertEquals("+91 9812890081", observed?.phoneNumber)
        assertEquals(expectedLinks, observed?.portfolioLinks)
    }

    @Test
    fun updateProfilePhoto_authenticatedArtist_updatesHeadshotPreservingBioAndLinks() = runBlocking {
        val uid = signInTestUser(TEST_USER_EMAIL)
        val repository = StudioRepository(firestore)

        val expectedLinks = listOf("https://behance.net/headshot_artist")
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateUserProfile(
                userId = uid,
                displayName = "Mehak Kapoor",
                phoneNumber = "+91 9812890081",
                bio = "Studio fashion & bridal portrait model.",
                portfolioLinks = expectedLinks
            )
        }

        val capturedHeadshotDataUrl = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAA=="
        val photoResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateProfilePhoto(userId = uid, avatarUrl = capturedHeadshotDataUrl)
        }

        assertTrue("Expected updateProfilePhoto to succeed: ${photoResult.exceptionOrNull()}", photoResult.isSuccess)
        val updated = photoResult.getOrNull()
        assertNotNull(updated)
        assertEquals(capturedHeadshotDataUrl, updated?.avatarUrl)
        assertEquals(capturedHeadshotDataUrl, updated?.photoUrl)
        assertEquals(capturedHeadshotDataUrl, updated?.effectiveAvatarUrl)
        assertEquals("Studio fashion & bridal portrait model.", updated?.bio)
        assertEquals(expectedLinks, updated?.portfolioLinks)

        // Oversized payload is rejected before Firestore write
        val oversizedResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateProfilePhoto(
                userId = uid,
                avatarUrl = "data:image/jpeg;base64," + "A".repeat(500_005)
            )
        }
        assertTrue(oversizedResult.isFailure)
    }

    @Test
    fun submitServiceReview_authenticatedUser_persistsAndUpdatesInReviewsCollection() = runBlocking {
        val uid = signInTestUser(TEST_USER_EMAIL)
        val repository = StudioRepository(firestore)

        val submitResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitServiceReview(
                bookingId = "completed_booking_01",
                serviceType = "Photography (High-Fashion Editorial)",
                rating = 5,
                comment = "Outstanding 4K lighting bay and very supportive studio crew!",
                reviewerName = "Simranjeet Kaur",
                reviewerAvatarUrl = "https://example.com/avatar.jpg"
            )
        }

        assertTrue("Expected submitServiceReview to succeed, but failed: ${submitResult.exceptionOrNull()}", submitResult.isSuccess)
        val reviewId = submitResult.getOrNull()
        assertEquals("${uid}_completed_booking_01", reviewId)

        val myReviews = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeMyReviews(uid).first { list -> list.any { it.bookingId == "completed_booking_01" } }
        }
        val saved = myReviews.first { it.bookingId == "completed_booking_01" }
        assertEquals(5L, saved.rating)
        assertEquals("Simranjeet Kaur", saved.reviewerName)
        assertEquals("Outstanding 4K lighting bay and very supportive studio crew!", saved.comment)

        // Verify updating existing review preserves createdAt and updates rating/comment
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitServiceReview(
                bookingId = "completed_booking_01",
                serviceType = "Photography (High-Fashion Editorial)",
                rating = 4,
                comment = "Updated: Great lighting and quick turnaround on edited stills.",
                reviewerName = "Simranjeet Kaur",
                reviewerAvatarUrl = "https://example.com/avatar.jpg"
            )
        }
        assertTrue("Expected review update to succeed, but failed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)

        val allReviews = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeReviews().first { list ->
                list.any { it.bookingId == "completed_booking_01" && it.rating == 4L }
            }
        }
        val updated = allReviews.first { it.bookingId == "completed_booking_01" }
        assertEquals(4L, updated.rating)
        assertEquals("Updated: Great lighting and quick turnaround on edited stills.", updated.comment)
    }

    @Test
    fun submitServiceReview_invalidRating_rejectedBySecurityRules() = runBlocking {
        signInTestUser(TEST_USER_EMAIL)
        val repository = StudioRepository(firestore)

        val invalidResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitServiceReview(
                bookingId = "completed_booking_02",
                serviceType = "Video Production",
                rating = 6,
                comment = "Attempting invalid 6-star rating.",
                reviewerName = "Simranjeet Kaur"
            )
        }

        assertTrue(invalidResult.isFailure)
        val firestoreEx = invalidResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun isUserAdmin_regularUserAndUnverifiedAdminEmail_returnsFalseAndDeniesAdminCollections() = runBlocking {
        val uid = signInTestUser(TEST_USER_EMAIL)
        val repository = StudioRepository(firestore)

        // Regular user must NOT be treated as admin
        val isRegularAdmin = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.isUserAdmin(uid)
        }
        org.junit.Assert.assertFalse(isRegularAdmin)

        // Regular user observing allBookings must fail with PERMISSION_DENIED
        var caughtException: Exception? = null
        try {
            withTimeout(3000L) {
                repository.observeAllBookings().first()
            }
        } catch (e: Exception) {
            caughtException = e
        }
        assertNotNull(caughtException)
        val firestoreEx = caughtException?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)

        // Even if someone registers in emulator with chanderstudio0@gmail.com without verifying email,
        // isUserAdmin() and getOrCreateUserProfile() must NOT grant admin privileges until isEmailVerified == true
        val unverifiedAdminUid = signInTestUser("chanderstudio0@gmail.com")
        val unverifiedProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateUserProfile(
                userId = unverifiedAdminUid,
                email = "chanderstudio0@gmail.com",
                displayName = "Unverified Admin"
            )
        }
        assertEquals("user", unverifiedProfile.role)
        val isUnverifiedAdmin = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.isUserAdmin(unverifiedAdminUid)
        }
        org.junit.Assert.assertFalse(isUnverifiedAdmin)
    }

    @Test
    fun addGalleryItem_cameraCapturedDataUri_succeedsAndOmitsDuplicateThumbnail() = runBlocking {
        val uid = signInTestUser(TEST_USER_EMAIL)
        val repository = StudioRepository(firestore)

        val simulatedHighQualityDataUri = "data:image/jpeg;base64," + "Q".repeat(65_000)
        val item = com.example.data.model.GalleryItem(
            title = "Studio Gold Rim Portrait",
            category = "Portrait",
            mediaType = "photo",
            mediaUrl = simulatedHighQualityDataUri,
            thumbnailUrl = simulatedHighQualityDataUri,
            isDemo = false
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addGalleryItem(item, autoApprove = false)
        }
        assertTrue("Expected camera portfolio upload to succeed: ${result.exceptionOrNull()}", result.isSuccess)
        val docId = result.getOrNull()!!
        val savedSnap = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("gallery").document(docId).get().await()
        }
        val saved = savedSnap.toObject(com.example.data.model.GalleryItem::class.java)!!
        assertEquals(uid, saved.userId)
        assertEquals("pending", saved.status)
        assertEquals("", saved.thumbnailUrl)
        assertEquals(simulatedHighQualityDataUri, saved.mediaUrl)

        val ownerQuerySnap = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("gallery").whereEqualTo("userId", uid).get().await()
        }
        assertTrue(ownerQuerySnap.documents.any { it.id == docId })
    }

    @Test
    fun toggleAndUpdateFavoriteServicePackages_authenticatedUser_persistsAndRejectsUnauthenticated() = runBlocking {
        val uid = signInTestUser(TEST_USER_EMAIL)
        val repository = StudioRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateUserProfile(
                userId = uid,
                email = TEST_USER_EMAIL,
                displayName = "Simranjeet Client"
            )
            // Ensure deterministic clean state even if emulator retains prior runs
            repository.updateFavoriteServicePackages(uid, emptyList())
        }

        val addFirst = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleFavoriteServicePackage(uid, "photo_editorial")
        }
        assertTrue("Expected first favorite toggle to succeed: ${addFirst.exceptionOrNull()}", addFirst.isSuccess)
        assertEquals(listOf("photo_editorial"), addFirst.getOrNull())

        val batchUpdate = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateFavoriteServicePackages(
                userId = uid,
                favoritePackageIds = listOf("photo_editorial", "modeling_portfolio", "prod_music_video", "modeling_portfolio")
            )
        }
        assertTrue("Expected batch favorite update to succeed: ${batchUpdate.exceptionOrNull()}", batchUpdate.isSuccess)
        assertEquals(listOf("photo_editorial", "modeling_portfolio", "prod_music_video"), batchUpdate.getOrNull())

        val observed = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first {
                it?.favoritePackageIds?.contains("prod_music_video") == true
            }
        }
        assertEquals(
            listOf("photo_editorial", "modeling_portfolio", "prod_music_video"),
            observed?.favoritePackageIds
        )

        val removeOne = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleFavoriteServicePackage(uid, "modeling_portfolio")
        }
        assertTrue(removeOne.isSuccess)
        assertEquals(listOf("photo_editorial", "prod_music_video"), removeOne.getOrNull())

        auth.signOut()
        val unauthUpdate = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateFavoriteServicePackages(uid, listOf("photo_editorial"))
        }
        assertTrue(unauthUpdate.isFailure)
    }

    private companion object {
        const val TEST_USER_EMAIL = "artist_user@starartist.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
    }
}
