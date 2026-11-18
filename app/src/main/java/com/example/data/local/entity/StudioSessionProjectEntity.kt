package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * Lifecycle workflow statuses for tracking studio sessions & production projects in Room.
 */
enum class StudioProjectStatus(
    val labelEn: String,
    val labelHi: String,
    val defaultProgress: Int
) {
    PLANNING("Planning", "योजना चरण", 20),
    IN_PRODUCTION("In Production", "शूटिंग प्रगति पर", 60),
    POST_PROCESSING("Post-Processing", "एडिटिंग व ग्रेडिंग", 85),
    COMPLETED("Completed", "पूर्ण", 100);

    fun nextStage(): StudioProjectStatus? = when (this) {
        PLANNING -> IN_PRODUCTION
        IN_PRODUCTION -> POST_PROCESSING
        POST_PROCESSING -> COMPLETED
        COMPLETED -> null
    }

    companion object {
        fun fromStorageValue(raw: String?): StudioProjectStatus {
            val normalized = raw?.trim().orEmpty()
            return entries.firstOrNull {
                it.name.equals(normalized, ignoreCase = true) ||
                    it.labelEn.equals(normalized, ignoreCase = true)
            } ?: PLANNING
        }
    }
}

/**
 * Room TypeConverters for [StudioProjectStatus].
 */
class StudioProjectStatusConverters {
    @TypeConverter
    fun fromStatus(status: StudioProjectStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String?): StudioProjectStatus =
        StudioProjectStatus.fromStorageValue(value)
}

/**
 * Room entity representing a tracked studio session project across pre-production planning,
 * active studio/on-location production, post-processing, and completion.
 */
@Entity(
    tableName = "studio_session_projects",
    indices = [
        Index(value = ["status"]),
        Index(value = ["serviceCategory"]),
        Index(value = ["isArchived"])
    ]
)
data class StudioSessionProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sessionCode: String = "",
    val projectTitle: String,
    val clientName: String,
    val serviceCategory: String, // "Photography", "Modeling", "Video Production", "Bridal", "Commercial"
    val status: StudioProjectStatus = StudioProjectStatus.PLANNING,
    val scheduledDate: String = "",
    val deadlineDate: String = "",
    val studioStage: String = "Stage A • Lighting Bay",
    val leadDirector: String = "Star Artist Team",
    val progressPercent: Int = status.defaultProgress,
    val deliverableCount: Int = 25,
    val budgetInr: Long = 0L,
    val productionNotes: String = "",
    val inProductionTaskNotes: String = "",
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val trackedProductionMillis: Long = 0L,
    val activeTimerStartedAt: Long? = null,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isTimerRunning: Boolean
        get() = activeTimerStartedAt != null && status == StudioProjectStatus.IN_PRODUCTION

    fun computeTotalTrackedMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val activeDelta = if (status == StudioProjectStatus.IN_PRODUCTION) {
            activeTimerStartedAt?.let { (nowMillis - it).coerceAtLeast(0L) } ?: 0L
        } else {
            0L
        }
        return (trackedProductionMillis + activeDelta).coerceAtLeast(0L)
    }

    companion object {
        fun formatDurationHhMmSs(totalMillis: Long): String {
            val totalSeconds = (totalMillis.coerceAtLeast(0L) / 1000L)
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            return "%02d:%02d:%02d".format(hours, minutes, seconds)
        }
    }
}
