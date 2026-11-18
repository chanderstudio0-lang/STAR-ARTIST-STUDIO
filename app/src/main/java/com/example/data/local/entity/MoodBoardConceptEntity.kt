package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an AI-generated creative concept art piece saved to a project mood board.
 * Image binaries are persisted in app-private storage ([localImagePath]) to avoid SQLite cursor size limits.
 */
@Entity(
    tableName = "mood_board_concepts",
    indices = [
        Index(value = ["projectType"]),
        Index(value = ["isFavorite"]),
        Index(value = ["sessionProjectId"])
    ]
)
data class MoodBoardConceptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val boardTitle: String,
    val projectType: String,
    val prompt: String,
    val visualStyle: String,
    val lightingSetup: String,
    val aspectRatioLabel: String,
    val modelUsed: String,
    val localImagePath: String,
    val colorPaletteTags: String = "Obsidian Black, Burnished Gold",
    val linkedDraftId: Long? = null,
    val sessionProjectId: Long? = null,
    val boardSectionTag: String = "Concept Art",
    val sortOrder: Int = 0,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
