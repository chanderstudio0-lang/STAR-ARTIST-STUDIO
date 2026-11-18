package com.example.data.repository

import com.example.data.local.dao.ProjectDraftDao
import com.example.data.local.dao.SessionHistoryDao
import com.example.data.local.entity.ProjectDraftEntity
import com.example.data.local.entity.SessionHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository mediating access to local artist project drafts and session history.
 */
class ArtistDraftsRepository(
    private val projectDraftDao: ProjectDraftDao,
    private val sessionHistoryDao: SessionHistoryDao
) {

    val allDrafts: Flow<List<ProjectDraftEntity>> = projectDraftDao.getAllDrafts()
    val allSessions: Flow<List<SessionHistoryEntity>> = sessionHistoryDao.getAllSessions()
    val favoriteDrafts: Flow<List<ProjectDraftEntity>> = projectDraftDao.getFavoriteDrafts()

    fun getDraftsByType(type: String): Flow<List<ProjectDraftEntity>> =
        projectDraftDao.getDraftsByType(type)

    fun getSessionsByType(type: String): Flow<List<SessionHistoryEntity>> =
        sessionHistoryDao.getSessionsByType(type)

    suspend fun getDraftById(id: Long): ProjectDraftEntity? =
        projectDraftDao.getDraftById(id)

    suspend fun saveDraft(draft: ProjectDraftEntity): Long {
        val now = System.currentTimeMillis()
        val toSave = if (draft.id == 0L) {
            draft.copy(createdAt = now, updatedAt = now)
        } else {
            draft.copy(updatedAt = now)
        }
        val id = projectDraftDao.insertDraft(toSave)

        // Automatically log to session history
        sessionHistoryDao.insertSession(
            SessionHistoryEntity(
                sessionType = "DRAFT_CREATION",
                sessionTitle = "Draft: ${toSave.draftTitle}",
                summaryDetails = "Saved ${toSave.projectType} draft for ${toSave.clientOrModelName.ifBlank { "Unassigned" }}",
                timestamp = now
            )
        )
        return id
    }

    suspend fun updateDraft(draft: ProjectDraftEntity) {
        val updated = draft.copy(updatedAt = System.currentTimeMillis())
        projectDraftDao.updateDraft(updated)
    }

    suspend fun toggleFavorite(draft: ProjectDraftEntity) {
        val updated = draft.copy(
            isFavorite = !draft.isFavorite,
            updatedAt = System.currentTimeMillis()
        )
        projectDraftDao.updateDraft(updated)
    }

    suspend fun deleteDraft(draft: ProjectDraftEntity) {
        projectDraftDao.deleteDraft(draft)
    }

    suspend fun deleteDraftById(id: Long) {
        projectDraftDao.deleteById(id)
    }

    suspend fun logSession(
        sessionType: String,
        sessionTitle: String,
        summaryDetails: String,
        durationSeconds: Long = 0L
    ): Long {
        return sessionHistoryDao.insertSession(
            SessionHistoryEntity(
                sessionType = sessionType,
                sessionTitle = sessionTitle,
                summaryDetails = summaryDetails,
                sessionDurationSeconds = durationSeconds,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearAllDrafts() {
        projectDraftDao.clearAllDrafts()
    }

    suspend fun clearAllSessions() {
        sessionHistoryDao.clearAllSessions()
    }
}
