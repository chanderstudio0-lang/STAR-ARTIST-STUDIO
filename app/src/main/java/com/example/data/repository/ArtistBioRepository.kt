package com.example.data.repository

import com.example.data.local.dao.ArtistBioDao
import com.example.data.local.entity.ArtistBioEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository abstracting local Room persistence for an artist's professional bio and specialty.
 */
class ArtistBioRepository(private val artistBioDao: ArtistBioDao) {

    fun observeBio(userId: String): Flow<ArtistBioEntity?> = artistBioDao.observeArtistBio(userId)

    suspend fun getBioOnce(userId: String): ArtistBioEntity? = artistBioDao.getArtistBioOnce(userId)

    suspend fun saveBio(
        userId: String,
        specialty: String,
        bioText: String,
        syncedToCloud: Boolean = false
    ): ArtistBioEntity {
        val sanitizedBio = bioText.trim().take(MAX_BIO_LENGTH)
        val sanitizedSpecialty = specialty.trim().take(100)
        val entity = ArtistBioEntity(
            userId = userId,
            specialty = sanitizedSpecialty,
            bioText = sanitizedBio,
            syncedToCloud = syncedToCloud,
            updatedAtMillis = System.currentTimeMillis()
        )
        artistBioDao.upsertArtistBio(entity)
        return entity
    }

    suspend fun deleteBio(userId: String) {
        artistBioDao.deleteArtistBio(userId)
    }

    companion object {
        const val MAX_BIO_LENGTH = 500
    }
}
