package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing local session history of artist studio workflows:
 * e.g., AI concept sessions, lighting stage setup logs, storyboard exports, booking inquiry sessions.
 */
@Entity(tableName = "session_history")
data class SessionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionType: String, // "AI_CONCEPT", "STORYBOARD", "DRAFT_CREATION", "BOOKING_INQUIRY", "GALLERY_PREVIEW"
    val sessionTitle: String,
    val summaryDetails: String,
    val sessionDurationSeconds: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)
