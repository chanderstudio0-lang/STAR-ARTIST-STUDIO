package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an artist's local project draft in STAR ARTIST STUDIO.
 * Stores unpublished concepts, photoshoot plans, model moodboards, and cinematography drafts.
 */
@Entity(tableName = "project_drafts")
data class ProjectDraftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val draftTitle: String,
    val projectType: String, // "Photography", "Modeling", "Video Reel", "Bridal"
    val clientOrModelName: String = "",
    val conceptNotes: String = "",
    val lightingSetupNotes: String = "",
    val cameraGearNotes: String = "",
    val targetShootDate: String = "",
    val estimatedBudgetInr: Long = 0L,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
