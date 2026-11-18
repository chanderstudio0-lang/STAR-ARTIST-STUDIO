package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ArtistBioEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for managing local artist bio persistence.
 */
@Dao
interface ArtistBioDao {

    @Query("SELECT * FROM artist_bios WHERE userId = :userId LIMIT 1")
    fun observeArtistBio(userId: String): Flow<ArtistBioEntity?>

    @Query("SELECT * FROM artist_bios WHERE userId = :userId LIMIT 1")
    suspend fun getArtistBioOnce(userId: String): ArtistBioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtistBio(entity: ArtistBioEntity)

    @Query("DELETE FROM artist_bios WHERE userId = :userId")
    suspend fun deleteArtistBio(userId: String)

    @Query("DELETE FROM artist_bios")
    suspend fun clearAll()
}
