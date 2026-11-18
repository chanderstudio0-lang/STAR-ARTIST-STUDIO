package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MoodBoardConceptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodBoardConceptDao {

    @Query("SELECT * FROM mood_board_concepts ORDER BY createdAt DESC, id DESC")
    fun observeAllConcepts(): Flow<List<MoodBoardConceptEntity>>

    @Query("SELECT * FROM mood_board_concepts WHERE isFavorite = 1 ORDER BY createdAt DESC, id DESC")
    fun observeFavoriteConcepts(): Flow<List<MoodBoardConceptEntity>>

    @Query("SELECT * FROM mood_board_concepts WHERE projectType = :projectType ORDER BY createdAt DESC, id DESC")
    fun observeConceptsByType(projectType: String): Flow<List<MoodBoardConceptEntity>>

    @Query("SELECT * FROM mood_board_concepts WHERE sessionProjectId = :projectId ORDER BY isFavorite DESC, sortOrder ASC, createdAt DESC")
    fun observeConceptsForProject(projectId: Long): Flow<List<MoodBoardConceptEntity>>

    @Query("SELECT * FROM mood_board_concepts WHERE id = :id LIMIT 1")
    suspend fun getConceptById(id: Long): MoodBoardConceptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcept(concept: MoodBoardConceptEntity): Long

    @Update
    suspend fun updateConcept(concept: MoodBoardConceptEntity)

    @Query("UPDATE mood_board_concepts SET sessionProjectId = :projectId WHERE id = :conceptId")
    suspend fun assignConceptToProject(conceptId: Long, projectId: Long?)

    @Query("UPDATE mood_board_concepts SET boardSectionTag = :sectionTag WHERE id = :conceptId")
    suspend fun updateBoardSectionTag(conceptId: Long, sectionTag: String)

    @Delete
    suspend fun deleteConcept(concept: MoodBoardConceptEntity)

    @Query("DELETE FROM mood_board_concepts WHERE id = :id")
    suspend fun deleteConceptById(id: Long)
}
