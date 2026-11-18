package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SessionHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for Local Session History.
 */
@Dao
interface SessionHistoryDao {

    @Query("SELECT * FROM session_history ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<SessionHistoryEntity>>

    @Query("SELECT * FROM session_history WHERE sessionType = :sessionType ORDER BY timestamp DESC")
    fun getSessionsByType(sessionType: String): Flow<List<SessionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionHistoryEntity): Long

    @Delete
    suspend fun deleteSession(session: SessionHistoryEntity)

    @Query("DELETE FROM session_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM session_history")
    suspend fun clearAllSessions()
}
