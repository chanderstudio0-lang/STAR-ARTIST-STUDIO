package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.GenerationHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for STAR ARTIST STUDIO AI-generated creative concepts & storyboards.
 */
@Dao
interface GenerationHistoryDao {

    @Query("SELECT * FROM generation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<GenerationHistoryEntity>>

    @Query("SELECT * FROM generation_history WHERE category = :category ORDER BY timestamp DESC")
    fun getHistoryByCategory(category: String): Flow<List<GenerationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: GenerationHistoryEntity): Long

    @Delete
    suspend fun deleteHistory(item: GenerationHistoryEntity)

    @Query("DELETE FROM generation_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM generation_history")
    suspend fun clearAll()
}
