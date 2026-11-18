package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AppointmentNotification
import com.example.data.model.BlogPost
import com.example.data.model.GalleryItem
import com.example.data.model.ModelRegistration
import com.example.data.model.ServiceReview
import com.example.data.model.ShootBooking
import com.example.data.model.StudioAnnouncement
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.StudioConstants
import com.example.util.StudioContactDetails
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudioRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before performing this operation.")
    }

    // --- USER PROFILE & ADMIN CHECK ---

    suspend fun getOrCreateUserProfile(
        userId: String,
        email: String,
        displayName: String,
        photoUrl: String = ""
    ): UserProfile {
        val docRef = db.collection("users").document(userId)
        val snapshot = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
        val currentUser = auth.currentUser
        val isVerifiedAdminEmail = email.equals(StudioConstants.ADMIN_EMAIL, ignoreCase = true) &&
            (currentUser == null || currentUser.uid != userId || currentUser.isEmailVerified)
        val role = if (isVerifiedAdminEmail) "admin" else "user"
        val cleanPhotoUrl = photoUrl.trim().takeIf {
            it.isNotEmpty() && it.length <= 500_000 &&
                (it.startsWith("https://") || it.startsWith("http://"))
        }.orEmpty()

        if (!snapshot.exists()) {
            val profile = UserProfile(
                userId = userId,
                email = email,
                displayName = displayName,
                photoUrl = cleanPhotoUrl,
                role = role
            )
            val data = buildMap<String, Any> {
                put("userId", userId)
                put("email", email)
                put("displayName", displayName)
                if (cleanPhotoUrl.isNotEmpty()) {
                    put("photoUrl", cleanPhotoUrl)
                }
                put("role", role)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            try {
                docRef.set(data).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }

            // If verified designated studio admin, also record in /admins collection
            if (isVerifiedAdminEmail) {
                try {
                    db.collection("admins").document(userId).set(
                        mapOf(
                            "userId" to userId,
                            "email" to email,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("StudioRepository", "Admin doc creation skipped: ${e.message}")
                }
            }
            return profile
        } else {
            val existing = snapshot.toObject(UserProfile::class.java) ?: UserProfile(userId = userId, email = email)
            if (isVerifiedAdminEmail && existing.role != "admin") {
                try {
                    docRef.update("role", "admin", "updatedAt", FieldValue.serverTimestamp()).await()
                    db.collection("admins").document(userId).set(
                        mapOf("userId" to userId, "email" to email, "createdAt" to FieldValue.serverTimestamp())
                    ).await()
                    return existing.copy(role = "admin")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                    Log.w("StudioRepository", "Admin role sync skipped: ${e.message}")
                }
            }
            return existing
        }
    }

    suspend fun isUserAdmin(userId: String): Boolean {
        val user = auth.currentUser ?: return false
        if (user.uid != userId) return false
        if (user.email.equals(StudioConstants.ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified) {
            return true
        }
        return try {
            val adminDoc = db.collection("admins").document(userId).get().await()
            adminDoc.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("StudioRepository", "Admin check fallback to false: ${e.message}")
            false
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, path)
                }
                throw error
            }
    }

    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        avatarUrl: String = "",
        phoneNumber: String? = null,
        city: String? = null,
        instagramHandle: String? = null,
        specialty: String? = null,
        bio: String? = null,
        portfolioLinks: List<String>? = null
    ): Result<UserProfile> {
        val docRef = db.collection("users").document(userId)
        return try {
            val uid = requireUserId()
            val existingSnap = docRef.get().await()
            val trimmedName = displayName.trim()
            val trimmedAvatar = avatarUrl.trim()
            val sanitizedLinks = portfolioLinks
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() && it.length <= 300 }
                ?.distinct()
                ?.take(10)

            if (!existingSnap.exists()) {
                val currentUser = auth.currentUser
                val email = currentUser?.email.orEmpty()
                val isAdminEmail = email.equals(StudioConstants.ADMIN_EMAIL, ignoreCase = true) && currentUser?.isEmailVerified == true
                val role = if (isAdminEmail) "admin" else "user"
                val newDocData = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "email" to email,
                    "displayName" to trimmedName,
                    "role" to role,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (trimmedAvatar.isNotBlank()) {
                    newDocData["avatarUrl"] = trimmedAvatar
                    newDocData["photoUrl"] = trimmedAvatar
                }
                if (phoneNumber != null) newDocData["phoneNumber"] = phoneNumber.trim()
                if (city != null) newDocData["city"] = city.trim()
                if (instagramHandle != null) newDocData["instagramHandle"] = instagramHandle.trim()
                if (sanitizedLinks != null) newDocData["portfolioLinks"] = sanitizedLinks
                if (specialty != null) newDocData["specialty"] = specialty.trim()
                if (bio != null) newDocData["bio"] = bio.trim()
                docRef.set(newDocData).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to trimmedName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (trimmedAvatar.isNotBlank()) {
                    updates["avatarUrl"] = trimmedAvatar
                    updates["photoUrl"] = trimmedAvatar
                }
                if (phoneNumber != null) updates["phoneNumber"] = phoneNumber.trim()
                if (city != null) updates["city"] = city.trim()
                if (instagramHandle != null) updates["instagramHandle"] = instagramHandle.trim()
                if (sanitizedLinks != null) updates["portfolioLinks"] = sanitizedLinks
                if (specialty != null) updates["specialty"] = specialty.trim()
                if (bio != null) updates["bio"] = bio.trim()
                docRef.update(updates).await()
            }

            // Also update Firebase Auth profile so currentUser reflects new displayName and photoUri
            val user = auth.currentUser
            if (user != null && user.uid == userId) {
                try {
                    user.updateProfile(
                        userProfileChangeRequest {
                            this.displayName = trimmedName
                            if (trimmedAvatar.isNotBlank() && (trimmedAvatar.startsWith("http://") || trimmedAvatar.startsWith("https://"))) {
                                this.photoUri = android.net.Uri.parse(trimmedAvatar)
                            }
                        }
                    ).await()
                } catch (e: Exception) {
                    Log.w("StudioRepository", "Auth profile update skipped: ${e.message}")
                }
            }

            val updatedDoc = docRef.get().await()
            val profile = updatedDoc.toObject(UserProfile::class.java) ?: UserProfile(
                userId = userId,
                displayName = trimmedName,
                avatarUrl = trimmedAvatar,
                photoUrl = trimmedAvatar,
                phoneNumber = phoneNumber?.trim().orEmpty(),
                city = city?.trim().orEmpty(),
                instagramHandle = instagramHandle?.trim().orEmpty(),
                portfolioLinks = sanitizedLinks ?: emptyList(),
                specialty = specialty?.trim().orEmpty(),
                bio = bio?.trim().orEmpty()
            )
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePhoto(
        userId: String,
        avatarUrl: String
    ): Result<UserProfile> {
        val trimmedAvatar = avatarUrl.trim()
        if (trimmedAvatar.length > 500_000) {
            return Result.failure(IllegalArgumentException("Profile photo size exceeds Firestore 500 KB limit."))
        }
        val docRef = db.collection("users").document(userId)
        return try {
            val uid = requireUserId()
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val currentUser = auth.currentUser
                val email = currentUser?.email.orEmpty()
                val role = if (email.equals(StudioConstants.ADMIN_EMAIL, ignoreCase = true) && currentUser?.isEmailVerified == true) "admin" else "user"
                val displayName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Star Artist Member"
                val newDocData = mapOf(
                    "userId" to uid,
                    "email" to email,
                    "displayName" to displayName,
                    "avatarUrl" to trimmedAvatar,
                    "photoUrl" to trimmedAvatar,
                    "role" to role,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(newDocData).await()
            } else {
                docRef.update(
                    mapOf(
                        "avatarUrl" to trimmedAvatar,
                        "photoUrl" to trimmedAvatar,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val user = auth.currentUser
            if (user != null && user.uid == userId && (trimmedAvatar.startsWith("http://") || trimmedAvatar.startsWith("https://"))) {
                try {
                    user.updateProfile(
                        userProfileChangeRequest {
                            this.photoUri = android.net.Uri.parse(trimmedAvatar)
                        }
                    ).await()
                } catch (e: Exception) {
                    Log.w("StudioRepository", "Auth photo update skipped: ${e.message}")
                }
            }

            val updatedDoc = docRef.get().await()
            val profile = updatedDoc.toObject(UserProfile::class.java) ?: UserProfile(
                userId = userId,
                avatarUrl = trimmedAvatar,
                photoUrl = trimmedAvatar
            )
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- MODEL REGISTRATION ---

    fun observeMyModelRegistrations(userId: String): Flow<List<ModelRegistration>> {
        val path = "models"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { it.toObjects(ModelRegistration::class.java) }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeAllModelRegistrations(): Flow<List<ModelRegistration>> {
        val path = "models"
        return db.collection(path)
            .snapshots()
            .map { it.toObjects(ModelRegistration::class.java) }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun submitModelRegistration(model: ModelRegistration): Result<String> {
        val uid = requireUserId()
        val path = "models"
        return try {
            val docRef = db.collection(path).document()
            val payload = mapOf(
                "userId" to uid,
                "fullName" to model.fullName.trim(),
                "age" to model.age,
                "gender" to model.gender,
                "city" to model.city.trim(),
                "phoneNumber" to model.phoneNumber.trim(),
                "skills" to model.skills.trim(),
                "portfolioUrls" to model.portfolioUrls.trim(),
                "consentGiven" to true,
                "status" to "pending",
                "adminNotes" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateModelStatus(modelId: String, newStatus: String, adminNotes: String = ""): Result<Unit> {
        val docRef = db.collection("models").document(modelId)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "adminNotes" to adminNotes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteModelRegistration(modelId: String): Result<Unit> {
        val docRef = db.collection("models").document(modelId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- SHOOT BOOKINGS ---

    fun observeMyBookings(userId: String): Flow<List<ShootBooking>> {
        val path = "bookings"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ShootBooking::class.java)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeAllBookings(): Flow<List<ShootBooking>> {
        val path = "bookings"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ShootBooking::class.java)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createBooking(booking: ShootBooking): Result<String> {
        val uid = requireUserId()
        val path = "bookings"

        val cleanName = booking.clientName.trim()
        val cleanPhone = booking.contactNumber.trim()
        val cleanService = booking.serviceType.trim()
        val cleanDate = booking.preferredDate.trim()
        val cleanTime = booking.preferredTime.trim().take(50)
        val cleanLocation = booking.location.trim().ifEmpty { "Studio (Talwara Khurd)" }.take(200)
        val cleanNotes = booking.notes.trim().take(1000)

        if (cleanName.length !in 2..100) {
            return Result.failure(IllegalArgumentException("Client name must be between 2 and 100 characters."))
        }
        if (cleanPhone.length !in 8..20) {
            return Result.failure(IllegalArgumentException("Contact number must be between 8 and 20 characters."))
        }
        if (cleanService.isEmpty() || cleanService.length > 100) {
            return Result.failure(IllegalArgumentException("Please select a valid service type (Modeling, Photography, or Video Production)."))
        }
        if (cleanDate.isEmpty() || cleanDate.length > 50) {
            return Result.failure(IllegalArgumentException("Please provide a valid preferred shoot date."))
        }

        return try {
            val docRef = db.collection(path).document()
            val payload = mapOf(
                "userId" to uid,
                "clientName" to cleanName,
                "contactNumber" to cleanPhone,
                "serviceType" to cleanService,
                "preferredDate" to cleanDate,
                "preferredTime" to cleanTime,
                "location" to cleanLocation,
                "notes" to cleanNotes,
                "status" to "Pending",
                "fcmToken" to booking.fcmToken,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBookingStatus(bookingId: String, newStatus: String): Result<Unit> {
        val docRef = db.collection("bookings").document(bookingId)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelUserBooking(bookingId: String): Result<Unit> {
        requireUserId()
        val cleanId = bookingId.trim()
        if (cleanId.isEmpty()) {
            return Result.failure(IllegalArgumentException("Booking ID cannot be empty."))
        }
        val docRef = db.collection("bookings").document(cleanId)
        return try {
            docRef.update(
                mapOf(
                    "status" to "Cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteBooking(bookingId: String): Result<Unit> {
        val docRef = db.collection("bookings").document(bookingId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- GALLERY ITEMS ---

    fun observeApprovedGallery(): Flow<List<GalleryItem>> {
        val path = "gallery"
        return db.collection(path)
            .whereEqualTo("status", "approved")
            .snapshots()
            .map { it.toObjects(GalleryItem::class.java) }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMyGalleryItems(userId: String): Flow<List<GalleryItem>> {
        val path = "gallery"
        if (userId.isBlank()) return flowOf(emptyList())
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { it.toObjects(GalleryItem::class.java) }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeAllGalleryItems(): Flow<List<GalleryItem>> {
        val path = "gallery"
        return db.collection(path)
            .snapshots()
            .map { it.toObjects(GalleryItem::class.java) }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addGalleryItem(item: GalleryItem, autoApprove: Boolean = false): Result<String> {
        val uid = requireUserId()
        val path = "gallery"
        val cleanTitle = item.title.trim()
        val cleanMediaUrl = item.mediaUrl.trim()
        val cleanThumbnailUrl = if (
            cleanMediaUrl.startsWith("data:image/", ignoreCase = true) &&
            (item.thumbnailUrl.trim() == cleanMediaUrl || item.thumbnailUrl.trim().length > 200_000)
        ) {
            ""
        } else {
            item.thumbnailUrl.trim()
        }
        if (cleanTitle.isEmpty() || cleanTitle.length > 120) {
            return Result.failure(IllegalArgumentException("Portfolio title must be between 1 and 120 characters."))
        }
        if (cleanMediaUrl.isEmpty() || cleanMediaUrl.length > 850_000) {
            return Result.failure(IllegalArgumentException("Portfolio media exceeds the 850 KB upload limit."))
        }
        val status = if (autoApprove) "approved" else "pending"
        return try {
            val docRef = db.collection(path).document()
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "category" to item.category,
                "mediaType" to item.mediaType,
                "mediaUrl" to cleanMediaUrl,
                "thumbnailUrl" to cleanThumbnailUrl,
                "isDemo" to item.isDemo,
                "status" to status,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateGalleryStatus(itemId: String, status: String): Result<Unit> {
        val docRef = db.collection("gallery").document(itemId)
        return try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteGalleryItem(itemId: String): Result<Unit> {
        val docRef = db.collection("gallery").document(itemId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- DATA DELETION REQUEST COMPLIANCE ---

    suspend fun deleteUserData(userId: String): Result<Unit> {
        return try {
            // 1. Delete user's model registrations
            val models = db.collection("models").whereEqualTo("userId", userId).get().await()
            for (doc in models.documents) {
                doc.reference.delete().await()
            }
            // 2. Delete user's shoot bookings
            val bookings = db.collection("bookings").whereEqualTo("userId", userId).get().await()
            for (doc in bookings.documents) {
                doc.reference.delete().await()
            }
            // 3. Delete user's uploaded gallery media items
            val gallery = db.collection("gallery").whereEqualTo("userId", userId).get().await()
            for (doc in gallery.documents) {
                doc.reference.delete().await()
            }
            // 4. Delete user's service reviews
            val reviews = db.collection("reviews").whereEqualTo("userId", userId).get().await()
            for (doc in reviews.documents) {
                doc.reference.delete().await()
            }
            // 5. Delete user's appointment notifications subcollection
            val notifications = db.collection("users").document(userId).collection("notifications").get().await()
            for (doc in notifications.documents) {
                doc.reference.delete().await()
            }
            // 6. Delete user profile document from Firestore
            db.collection("users").document(userId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("StudioRepository", "Failed to delete user data", e)
            Result.failure(e)
        }
    }

    // Seed default DEMO gallery items if collection is empty
    suspend fun seedInitialDemoGalleryIfNeeded(adminUid: String) {
        try {
            val existing = db.collection("gallery").limit(1).get().await()
            if (existing.isEmpty) {
                val demoItems = listOf(
                    GalleryItem(
                        userId = adminUid,
                        title = "Royal Gold Bridal Shoot",
                        category = "Bridal",
                        mediaType = "photo",
                        mediaUrl = "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=800&q=80",
                        thumbnailUrl = "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?w=400&q=80",
                        isDemo = true,
                        status = "approved"
                    ),
                    GalleryItem(
                        userId = adminUid,
                        title = "Vogue Runway High Fashion",
                        category = "Fashion",
                        mediaType = "photo",
                        mediaUrl = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=800&q=80",
                        thumbnailUrl = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=400&q=80",
                        isDemo = true,
                        status = "approved"
                    ),
                    GalleryItem(
                        userId = adminUid,
                        title = "4K Cinema Ad Reel Production",
                        category = "Video Reel",
                        mediaType = "video",
                        mediaUrl = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=800&q=80",
                        thumbnailUrl = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=400&q=80",
                        isDemo = true,
                        status = "approved"
                    ),
                    GalleryItem(
                        userId = adminUid,
                        title = "Luxury Watch Commercial Campaign",
                        category = "Commercial",
                        mediaType = "photo",
                        mediaUrl = "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=800&q=80",
                        thumbnailUrl = "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=400&q=80",
                        isDemo = true,
                        status = "approved"
                    ),
                    GalleryItem(
                        userId = adminUid,
                        title = "Star Artist Studio Editorial Stills",
                        category = "Editorial",
                        mediaType = "photo",
                        mediaUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&q=80",
                        thumbnailUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80",
                        isDemo = true,
                        status = "approved"
                    ),
                    GalleryItem(
                        userId = adminUid,
                        title = "Classic Studio Monochromatic Portrait",
                        category = "Portrait",
                        mediaType = "photo",
                        mediaUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&q=80",
                        thumbnailUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&q=80",
                        isDemo = true,
                        status = "approved"
                    )
                )

                for (item in demoItems) {
                    addGalleryItem(item, autoApprove = true)
                }
            }
        } catch (e: Exception) {
            Log.w("StudioRepository", "Demo seed check completed or skipped: ${e.message}")
        }
    }

    // --- STUDIO BLOG, NEWS & PHOTOGRAPHY TIPS ---

    fun observeBlogPosts(): Flow<List<BlogPost>> {
        val path = "posts"
        return db.collection(path)
            .whereEqualTo("status", "published")
            .snapshots()
            .map { snapshot ->
                val posts = snapshot.toObjects(BlogPost::class.java)
                if (posts.isEmpty()) {
                    defaultSeedBlogPosts()
                } else {
                    posts.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                Log.w("StudioRepository", "Failed observing posts, falling back to seed: ${e.message}")
                emit(defaultSeedBlogPosts())
            }
    }

    fun observeAllBlogPostsForAdmin(): Flow<List<BlogPost>> {
        val path = "posts"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                val posts = snapshot.toObjects(BlogPost::class.java)
                if (posts.isEmpty()) {
                    defaultSeedBlogPosts()
                } else {
                    posts.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                Log.w("StudioRepository", "Admin posts listener error: ${e.message}")
                emit(defaultSeedBlogPosts())
            }
    }

    suspend fun createBlogPost(
        title: String,
        category: String,
        content: String,
        summary: String,
        imageUrl: String,
        tags: String,
        status: String = "published",
        authorName: String = "Star Artist Studio",
        readTimeMinutes: Long = 3L
    ): Result<String> {
        val uid = requireUserId()
        val path = "posts"
        return try {
            val docRef = db.collection(path).document()
            val data = hashMapOf<String, Any>(
                "authorId" to uid,
                "authorName" to authorName,
                "title" to title.trim(),
                "category" to category,
                "content" to content.trim(),
                "summary" to summary.trim(),
                "imageUrl" to imageUrl.trim(),
                "tags" to tags.trim(),
                "status" to status,
                "readTimeMinutes" to readTimeMinutes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBlogPost(
        postId: String,
        title: String,
        category: String,
        content: String,
        summary: String,
        imageUrl: String,
        tags: String,
        status: String
    ): Result<Unit> {
        val path = "posts"
        return try {
            val updates = mapOf(
                "title" to title.trim(),
                "category" to category,
                "content" to content.trim(),
                "summary" to summary.trim(),
                "imageUrl" to imageUrl.trim(),
                "tags" to tags.trim(),
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection(path).document(postId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBlogPost(postId: String): Result<Unit> {
        val path = "posts"
        return try {
            db.collection(path).document(postId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- SERVICE RATINGS & REVIEWS ---

    fun observeReviews(): Flow<List<ServiceReview>> {
        val path = "reviews"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ServiceReview::class.java)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMyReviews(userId: String): Flow<List<ServiceReview>> {
        val path = "reviews"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ServiceReview::class.java)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun submitServiceReview(
        bookingId: String,
        serviceType: String,
        rating: Int,
        comment: String,
        reviewerName: String,
        reviewerAvatarUrl: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val cleanBookingId = bookingId.trim()
        val reviewId = "${uid}_${cleanBookingId}"
        val docRef = db.collection("reviews").document(reviewId)
        val safeReviewerName = reviewerName.trim().ifEmpty { "Studio Member" }.take(100)
        val safeServiceType = serviceType.trim().ifEmpty { "Studio Session" }.take(100)
        val safeAvatar = reviewerAvatarUrl.trim()
        val cleanComment = comment.trim()

        return try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val payload = hashMapOf<String, Any>(
                    "userId" to uid,
                    "bookingId" to cleanBookingId,
                    "reviewerName" to safeReviewerName,
                    "reviewerAvatarUrl" to safeAvatar,
                    "serviceType" to safeServiceType,
                    "rating" to rating.toLong(),
                    "comment" to cleanComment,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = hashMapOf<String, Any>(
                    "rating" to rating.toLong(),
                    "comment" to cleanComment,
                    "reviewerName" to safeReviewerName,
                    "reviewerAvatarUrl" to safeAvatar,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(reviewId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- FIREBASE CLOUD MESSAGING: STUDIO ANNOUNCEMENTS & APPOINTMENT NOTIFICATIONS ---

    fun observeStudioAnnouncements(): Flow<List<StudioAnnouncement>> {
        val path = "announcements"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(StudioAnnouncement::class.java)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun publishStudioAnnouncement(
        title: String,
        message: String,
        category: String = "Studio Update",
        priority: String = "normal",
        actionRoute: String = "home",
        authorName: String = "Star Artist Studio"
    ): Result<String> {
        val uid = requireUserId()
        val path = "announcements"
        val cleanTitle = title.trim()
        val cleanMessage = message.trim()
        if (cleanTitle.length < 3 || cleanTitle.length > 140) {
            return Result.failure(IllegalArgumentException("Announcement title must be between 3 and 140 characters."))
        }
        if (cleanMessage.length < 5 || cleanMessage.length > 2000) {
            return Result.failure(IllegalArgumentException("Announcement message must be between 5 and 2000 characters."))
        }
        val validCategories = setOf("Studio Update", "Casting Call", "Offer & Discount", "Schedule Alert", "Workshop & Gear")
        val safeCategory = if (category in validCategories) category else "Studio Update"
        val safePriority = if (priority == "urgent") "urgent" else "normal"
        val validRoutes = setOf("home", "bookings", "catalog", "portfolio", "models")
        val safeRoute = if (actionRoute in validRoutes) actionRoute else "home"

        return try {
            val docRef = db.collection(path).document()
            val payload = hashMapOf<String, Any>(
                "authorId" to uid,
                "authorName" to authorName.trim().ifEmpty { "Star Artist Studio" }.take(100),
                "title" to cleanTitle,
                "message" to cleanMessage,
                "category" to safeCategory,
                "priority" to safePriority,
                "targetTopic" to "studio_announcements",
                "actionRoute" to safeRoute,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteStudioAnnouncement(announcementId: String): Result<Unit> {
        requireUserId()
        val cleanId = announcementId.trim()
        if (cleanId.isEmpty()) {
            return Result.failure(IllegalArgumentException("Announcement ID cannot be empty."))
        }
        val docRef = db.collection("announcements").document(cleanId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserNotifications(userId: String): Flow<List<AppointmentNotification>> {
        if (userId.isBlank()) return flowOf(emptyList())
        val path = "users/$userId/notifications"
        return db.collection("users")
            .document(userId)
            .collection("notifications")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AppointmentNotification::class.java)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createAppointmentNotification(
        targetUserId: String,
        bookingId: String,
        title: String,
        message: String,
        status: String,
        serviceType: String = "",
        targetFcmToken: String = ""
    ): Result<String> {
        requireUserId()
        val cleanTargetUserId = targetUserId.trim()
        val cleanBookingId = bookingId.trim()
        val cleanTitle = title.trim()
        val cleanMessage = message.trim()
        if (cleanTargetUserId.isEmpty() || cleanBookingId.isEmpty()) {
            return Result.failure(IllegalArgumentException("Target user and booking ID are required."))
        }
        if (cleanTitle.length < 2 || cleanMessage.length < 3) {
            return Result.failure(IllegalArgumentException("Notification title and message cannot be empty."))
        }
        val validStatuses = setOf("Pending", "Confirmed", "Completed", "Cancelled", "Rescheduled")
        val safeStatus = if (status in validStatuses) status else "Confirmed"
        val path = "users/$cleanTargetUserId/notifications"
        return try {
            val docRef = db.collection("users")
                .document(cleanTargetUserId)
                .collection("notifications")
                .document()
            val payload = hashMapOf<String, Any>(
                "userId" to cleanTargetUserId,
                "bookingId" to cleanBookingId,
                "title" to cleanTitle.take(160),
                "message" to cleanMessage.take(2000),
                "status" to safeStatus,
                "serviceType" to serviceType.trim().take(120),
                "targetFcmToken" to targetFcmToken.trim().take(512),
                "isRead" to false,
                "channelType" to "appointment_status",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> {
        requireUserId()
        val cleanUserId = userId.trim()
        val cleanNotificationId = notificationId.trim()
        if (cleanUserId.isEmpty() || cleanNotificationId.isEmpty()) {
            return Result.failure(IllegalArgumentException("User ID and notification ID cannot be empty."))
        }
        val docRef = db.collection("users")
            .document(cleanUserId)
            .collection("notifications")
            .document(cleanNotificationId)
        return try {
            docRef.update(
                mapOf(
                    "isRead" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(userId: String, notificationIds: List<String>): Result<Unit> {
        requireUserId()
        val cleanUserId = userId.trim()
        val distinctIds = notificationIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(400)
        if (cleanUserId.isEmpty() || distinctIds.isEmpty()) {
            return Result.success(Unit)
        }
        val path = "users/$cleanUserId/notifications"
        return try {
            val batch = db.batch()
            val colRef = db.collection("users").document(cleanUserId).collection("notifications")
            distinctIds.forEach { notifId ->
                batch.update(
                    colRef.document(notifId),
                    mapOf(
                        "isRead" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateNotificationPreferences(
        userId: String,
        announcementsOptIn: Boolean,
        appointmentPushOptIn: Boolean,
        fcmToken: String? = null
    ): Result<Unit> {
        requireUserId()
        val cleanUserId = userId.trim()
        if (cleanUserId.isEmpty()) {
            return Result.failure(IllegalArgumentException("User ID cannot be empty."))
        }
        val docRef = db.collection("users").document(cleanUserId)
        return try {
            val updates = mutableMapOf<String, Any>(
                "announcementsOptIn" to announcementsOptIn,
                "appointmentPushOptIn" to appointmentPushOptIn,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!fcmToken.isNullOrBlank()) {
                updates["fcmToken"] = fcmToken.trim().take(512)
                updates["fcmTokenUpdatedAt"] = FieldValue.serverTimestamp()
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- CLIENT FAVORITE SERVICE PACKAGES ---

    suspend fun updateFavoriteServicePackages(
        userId: String,
        favoritePackageIds: List<String>
    ): Result<List<String>> {
        val sanitized = favoritePackageIds
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length <= 64 }
            .distinct()
            .take(30)
        val docRef = db.collection("users").document(userId)
        return try {
            val uid = requireUserId()
            if (uid != userId) {
                return Result.failure(IllegalAccessException("Cannot update favorites for another user."))
            }
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val currentUser = auth.currentUser
                val email = currentUser?.email.orEmpty()
                val role = if (email.equals(StudioConstants.ADMIN_EMAIL, ignoreCase = true) && currentUser?.isEmailVerified == true) "admin" else "user"
                val displayName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Star Artist Member"
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "email" to email,
                        "displayName" to displayName,
                        "role" to role,
                        "favoritePackageIds" to sanitized,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "favoritePackageIds" to sanitized,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(sanitized)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleFavoriteServicePackage(
        userId: String,
        packageId: String
    ): Result<List<String>> {
        val cleanId = packageId.trim()
        if (cleanId.isEmpty()) {
            return Result.failure(IllegalArgumentException("Service package ID cannot be empty."))
        }
        val docRef = db.collection("users").document(userId)
        return try {
            val uid = requireUserId()
            if (uid != userId) {
                return Result.failure(IllegalAccessException("Cannot modify another user's favorite packages."))
            }
            val snap = docRef.get().await()
            val currentList = if (snap.exists()) {
                @Suppress("UNCHECKED_CAST")
                (snap.get("favoritePackageIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
            } else {
                emptyList()
            }
            val nextList = if (cleanId in currentList) {
                currentList.filterNot { it == cleanId }
            } else {
                (currentList + cleanId).distinct().take(30)
            }
            updateFavoriteServicePackages(userId, nextList)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        fun defaultSeedBlogPosts(): List<BlogPost> {
            return listOf(
                BlogPost(
                    id = "seed_post_1",
                    authorId = "studio_admin",
                    authorName = "Chander Studio",
                    title = "Cinematic 3-Point Lighting Guide for Fashion Portfolios",
                    category = "Photography Tips",
                    content = """Cinematic lighting elevates high-fashion photography from standard catalog shots to striking editorial imagery. At Star Artist Studio in Talwara Khurd, we utilize a specialized three-point lighting bay:

1. Key Light: A 600W daylight-balanced strobe or continuous LED diffused through a 120cm parabolic softbox, positioned 45 degrees relative to the model.
2. Fill Light: A secondary large 5-in-1 reflector or soft diffused strip box to soften harsh shadow transitions on the cheekbones without flattening dimension.
3. Hair & Rim Accent: A grid-focused backlight angled down from a 45-degree angle behind the talent to separate hair and shoulders cleanly against deep obsidian backdrops.

Pro Tip for Sirsa Sunlight Shoots: When taking models outdoor around Ellenabad during Golden Hour, always angle talent with backlighting and bounce warm specular reflections back into their eyes using our gold reflector foil.""",
                    summary = "Discover how to set up key, fill, and hair lights for high-fashion editorial portraits with deep obsidian separation.",
                    imageUrl = "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&q=80",
                    tags = "lighting,fashion,editorial,tips",
                    status = "published",
                    readTimeMinutes = 4L
                ),
                BlogPost(
                    id = "seed_post_2",
                    authorId = "studio_admin",
                    authorName = "Star Artist Studio",
                    title = "Studio Upgrade: In-House 4K Cinema Stage Launched in Talwara Khurd",
                    category = "Studio News",
                    content = """We are thrilled to announce the completion of our upgraded production facility at Star Artist Studio, Talwara Khurd, Tehsil Ellenabad, District Sirsa, Haryana - 125102!

Key Facilities Available Now:
• In-house 18ft acoustic soundstage with full overhead lighting grid
• High-speed motorized camera slider and 3-axis stabilization gimbal rigs
• 4K Cinema camera package tuned for Punjabi music videos and commercial brand advertisements
• Dedicated climate-controlled makeup, wardrobe, and green room suites for verified 18+ models
• Instant on-set preview calibrated monitors for client approval

Appointments for pre-wedding productions, fashion agency portfolios, and music video directing can now be booked online or via WhatsApp.""",
                    summary = "Tour our newly expanded 4K soundstage, acoustic bays, and professional model green rooms in Talwara Khurd, Sirsa.",
                    imageUrl = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=800&q=80",
                    tags = "news,soundstage,4k,studio",
                    status = "published",
                    readTimeMinutes = 3L
                ),
                BlogPost(
                    id = "seed_post_3",
                    authorId = "studio_admin",
                    authorName = "Star Artist Studio",
                    title = "Behind The Scenes: Directing a High-Energy 30-Second Music Video Reel",
                    category = "Behind The Scenes",
                    content = """Go behind the scenes of our latest commercial music video production directed at Star Artist Studio:

Scene Planning & Framing:
Before rolling cameras, our director and lighting team mapped each beat to camera movements. Using slow-motion 60fps gimbal tracking, we opened with an intense dynamic walk-in as haze diffused the rim lights.

Color Grading & Gold Contrast:
To achieve the signature Black & Gold aesthetic, we controlled wardrobe colors with custom black silk kurtas and gold-embroidered shawls. In post-production, we pushed warm amber skin tones while pinning pure obsidian blacks.

Director Note: Seamless transitions depend 90% on precise talent blocking and eye-line consistency across cut points.""",
                    summary = "Step behind the lens to see how our production crew directs, lights, and color grades high-energy music video reels.",
                    imageUrl = "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=800&q=80",
                    tags = "bts,musicvideo,cinematography,directing",
                    status = "published",
                    readTimeMinutes = 5L
                )
            )
        }
    }
}
