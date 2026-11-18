package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ProjectDraftEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for Artist Project Drafts.
 */
@Dao
interface ProjectDraftDao {

    @Query("SELECT * FROM project_drafts ORDER BY updatedAt DESC")
    fun getAllDrafts(): Flow<List<ProjectDraftEntity>>

    @Query("SELECT * FROM project_drafts WHERE projectType = :projectType ORDER BY updatedAt DESC")
    fun getDraftsByType(projectType: String): Flow<List<ProjectDraftEntity>>

    @Query("SELECT * FROM project_drafts WHERE id = :id LIMIT 1")
    suspend fun getDraftById(id: Long): ProjectDraftEntity?

    @Query("SELECT * FROM project_drafts WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteDrafts(): Flow<List<ProjectDraftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: ProjectDraftEntity): Long

    @Update
    suspend fun updateDraft(draft: ProjectDraftEntity)

    @Delete
    suspend fun deleteDraft(draft: ProjectDraftEntity)

    @Query("DELETE FROM project_drafts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM project_drafts")
    suspend fun clearAllDrafts()
}
