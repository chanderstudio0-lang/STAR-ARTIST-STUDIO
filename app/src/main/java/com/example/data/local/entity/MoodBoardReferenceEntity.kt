package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a visual reference photo organized inside a project's Mood Board
 * alongside AI-generated concept art.
 */
@Entity(
    tableName = "mood_board_references",
    indices = [
        Index(value = ["sessionProjectId"]),
        Index(value = ["sectionTag"]),
        Index(value = ["isPinned"])
    ]
)
data class MoodBoardReferenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sessionProjectId: Long? = null,
    val title: String,
    val sectionTag: String = "Lighting Ref",
    val sourceType: String = SOURCE_GALLERY_PHOTO,
    val imagePathOrUrl: String,
    val notes: String = "",
    val colorSwatches: String = "Warm Gold, Deep Shadow",
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SOURCE_GALLERY_PHOTO = "GALLERY_PHOTO"
        const val SOURCE_CAMERA_SHOT = "CAMERA_SHOT"
        const val SOURCE_STUDIO_PORTFOLIO = "STUDIO_PORTFOLIO"
    }
}
