package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity persisting an artist's professional bio and specialty locally on-device.
 * Supports instant offline editing and local draft retention scoped by authenticated user ID.
 */
@Entity(tableName = "artist_bios")
data class ArtistBioEntity(
    @PrimaryKey
    val userId: String,
    val specialty: String = "",
    val bioText: String = "",
    val syncedToCloud: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
