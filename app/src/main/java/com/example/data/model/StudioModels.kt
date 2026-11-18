package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val avatarUrl: String = "",
    val phoneNumber: String = "",
    val city: String = "",
    val instagramHandle: String = "",
    val portfolioLinks: List<String> = emptyList(),
    val specialty: String = "",
    val bio: String = "",
    val role: String = "user", // "user" or "admin"
    val fcmToken: String = "",
    val announcementsOptIn: Boolean = true,
    val appointmentPushOptIn: Boolean = true,
    val favoritePackageIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val effectiveAvatarUrl: String
        get() = avatarUrl.ifEmpty { photoUrl }

    fun resolvedPortfolioLinks(): List<String> {
        val normalized = portfolioLinks.map { it.trim() }.filter { it.isNotBlank() }
        return if (normalized.isNotEmpty()) {
            normalized
        } else {
            val ig = instagramHandle.trim()
            if (ig.isNotBlank()) listOf(ig) else emptyList()
        }
    }
}

@IgnoreExtraProperties
data class ModelRegistration(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val age: Long = 18L,
    val gender: String = "Female",
    val city: String = "",
    val phoneNumber: String = "",
    val skills: String = "",
    val portfolioUrls: String = "",
    val consentGiven: Boolean = true,
    val status: String = "pending", // "pending", "approved", "rejected"
    val adminNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ShootBooking(
    @DocumentId val id: String = "",
    val userId: String = "",
    val clientName: String = "",
    val contactNumber: String = "",
    val serviceType: String = "Photography",
    val preferredDate: String = "",
    val preferredTime: String = "11:00 AM",
    val location: String = "Studio (Talwara Khurd)",
    val notes: String = "",
    val status: String = "Pending", // "Pending", "Confirmed", "Completed", "Cancelled"
    val fcmToken: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GalleryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "Fashion", // "Fashion", "Portrait", "Commercial", "Portfolio", "Video Reel", "Bridal", "Editorial"
    val mediaType: String = "photo", // "photo" or "video"
    val mediaUrl: String = "",
    val thumbnailUrl: String = "",
    val isDemo: Boolean = true,
    val status: String = "approved", // "pending", "approved", "rejected"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BlogPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "Star Artist Studio",
    val title: String = "",
    val category: String = "Studio News", // "Studio News", "Photography Tips", "Behind The Scenes", "Gear & Lighting", "Model Spotlight"
    val content: String = "",
    val summary: String = "",
    val imageUrl: String = "",
    val tags: String = "",
    val status: String = "published", // "published", "draft"
    val readTimeMinutes: Long = 3L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ServiceReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookingId: String = "",
    val reviewerName: String = "",
    val reviewerAvatarUrl: String = "",
    val serviceType: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudioAnnouncement(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "Star Artist Studio",
    val title: String = "",
    val message: String = "",
    val category: String = "Studio Update", // "Studio Update", "Casting Call", "Offer & Discount", "Schedule Alert", "Workshop & Gear"
    val priority: String = "normal", // "normal", "urgent"
    val targetTopic: String = "studio_announcements",
    val actionRoute: String = "home", // "home", "bookings", "catalog", "portfolio", "models"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppointmentNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookingId: String = "",
    val title: String = "",
    val message: String = "",
    val status: String = "Confirmed", // "Pending", "Confirmed", "Completed", "Cancelled", "Rescheduled"
    val serviceType: String = "",
    val targetFcmToken: String = "",
    @get:PropertyName("isRead")
    @field:PropertyName("isRead")
    val isRead: Boolean = false,
    val channelType: String = "appointment_status",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class AppLanguage {
    ENGLISH,
    HINDI
}
