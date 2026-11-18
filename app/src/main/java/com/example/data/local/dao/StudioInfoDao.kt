package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.StudioInfoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for locally cached STAR ARTIST STUDIO essential information and contact details.
 */
@Dao
interface StudioInfoDao {

    @Query("SELECT * FROM studio_info WHERE id = 'primary' LIMIT 1")
    fun getStudioInfo(): Flow<StudioInfoEntity?>

    @Query("SELECT * FROM studio_info WHERE id = 'primary' LIMIT 1")
    suspend fun getStudioInfoOnce(): StudioInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(info: StudioInfoEntity)

    @Query("DELETE FROM studio_info")
    suspend fun clear()
}
