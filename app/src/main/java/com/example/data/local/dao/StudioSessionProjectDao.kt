package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.StudioProjectStatus
import com.example.data.local.entity.StudioSessionProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioSessionProjectDao {

    @Query("SELECT * FROM studio_session_projects ORDER BY updatedAt DESC, id DESC")
    fun observeAllProjects(): Flow<List<StudioSessionProjectEntity>>

    @Query("SELECT * FROM studio_session_projects WHERE isArchived = 0 ORDER BY updatedAt DESC, id DESC")
    fun observeActiveProjects(): Flow<List<StudioSessionProjectEntity>>

    @Query("SELECT * FROM studio_session_projects WHERE isArchived = 1 ORDER BY archivedAt DESC, updatedAt DESC, id DESC")
    fun observeArchivedProjects(): Flow<List<StudioSessionProjectEntity>>

    @Query("SELECT * FROM studio_session_projects WHERE status = :status AND isArchived = 0 ORDER BY updatedAt DESC, id DESC")
    fun observeProjectsByStatus(status: StudioProjectStatus): Flow<List<StudioSessionProjectEntity>>

    @Query("SELECT COUNT(*) FROM studio_session_projects WHERE status = :status AND isArchived = 0")
    fun observeCountByStatus(status: StudioProjectStatus): Flow<Int>

    @Query("SELECT * FROM studio_session_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): StudioSessionProjectEntity?

    @Query("SELECT COUNT(*) FROM studio_session_projects")
    suspend fun getTotalCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: StudioSessionProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<StudioSessionProjectEntity>)

    @Update
    suspend fun updateProject(project: StudioSessionProjectEntity)

    @Query(
        """
        UPDATE studio_session_projects
        SET status = :status,
            progressPercent = :progressPercent,
            startedAt = :startedAt,
            completedAt = :completedAt,
            trackedProductionMillis = :trackedProductionMillis,
            activeTimerStartedAt = :activeTimerStartedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateProjectStatus(
        id: Long,
        status: StudioProjectStatus,
        progressPercent: Int,
        startedAt: Long?,
        completedAt: Long?,
        trackedProductionMillis: Long,
        activeTimerStartedAt: Long?,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE studio_session_projects
        SET trackedProductionMillis = :trackedProductionMillis,
            activeTimerStartedAt = :activeTimerStartedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateProductionStopwatch(
        id: Long,
        trackedProductionMillis: Long,
        activeTimerStartedAt: Long?,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE studio_session_projects
        SET isArchived = :isArchived,
            archivedAt = :archivedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun setProjectArchived(
        id: Long,
        isArchived: Boolean,
        archivedAt: Long?,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE studio_session_projects
        SET isArchived = 1,
            archivedAt = :archivedAt,
            updatedAt = :updatedAt
        WHERE status = 'COMPLETED' AND isArchived = 0
        """
    )
    suspend fun archiveAllCompletedProjects(
        archivedAt: Long,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE studio_session_projects
        SET inProductionTaskNotes = :notes,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateInProductionTaskNotes(
        id: Long,
        notes: String,
        updatedAt: Long
    )

    @Delete
    suspend fun deleteProject(project: StudioSessionProjectEntity)

    @Query("DELETE FROM studio_session_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}
