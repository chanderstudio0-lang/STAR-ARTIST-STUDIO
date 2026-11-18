package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Media classification stored in [PortfolioProjectEntity.mediaType].
 */
object PortfolioProjectMediaType {
    const val PHOTO = "PHOTO"
    const val VIDEO = "VIDEO"
}

/**
 * Room entity representing a photography or video production project in STAR ARTIST STUDIO.
 * Persists local showcase projects with image/video thumbnail references, technical camera metadata,
 * duration/shot count, and client/favorite state.
 */
@Entity(
    tableName = "portfolio_projects",
    indices = [
        Index(value = ["mediaType"]),
        Index(value = ["category"])
    ]
)
data class PortfolioProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectCode: String = "",
    val title: String,
    val mediaType: String, // PortfolioProjectMediaType.PHOTO or PortfolioProjectMediaType.VIDEO
    val category: String,
    val description: String = "",
    val thumbnailUrl: String,
    val mediaUrl: String,
    val clientName: String = "",
    val location: String = "Star Artist Studio, Talwara Khurd",
    val cameraGear: String = "",
    val durationSeconds: Int = 0,
    val shotCount: Int = 1,
    val resolutionTag: String = "4K HDR",
    val shootYear: Int = 2026,
    val isFeatured: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
