package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MoodBoardReferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodBoardReferenceDao {

    @Query("SELECT * FROM mood_board_references ORDER BY isPinned DESC, sortOrder ASC, createdAt DESC, id DESC")
    fun observeAllReferences(): Flow<List<MoodBoardReferenceEntity>>

    @Query("SELECT * FROM mood_board_references WHERE sessionProjectId = :projectId ORDER BY isPinned DESC, sortOrder ASC, createdAt DESC, id DESC")
    fun observeReferencesForProject(projectId: Long): Flow<List<MoodBoardReferenceEntity>>

    @Query("SELECT * FROM mood_board_references WHERE id = :id LIMIT 1")
    suspend fun getReferenceById(id: Long): MoodBoardReferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReference(reference: MoodBoardReferenceEntity): Long

    @Update
    suspend fun updateReference(reference: MoodBoardReferenceEntity)

    @Query("UPDATE mood_board_references SET sessionProjectId = :projectId WHERE id = :referenceId")
    suspend fun assignReferenceToProject(referenceId: Long, projectId: Long?)

    @Delete
    suspend fun deleteReference(reference: MoodBoardReferenceEntity)

    @Query("DELETE FROM mood_board_references WHERE id = :id")
    suspend fun deleteReferenceById(id: Long)
}
