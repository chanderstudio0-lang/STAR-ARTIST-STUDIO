package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for locally cached STAR ARTIST STUDIO essential information and contact details.
 * Ensures contact numbers, address, and studio directions are 100% accessible offline without network connectivity.
 */
@Entity(tableName = "studio_info")
data class StudioInfoEntity(
    @PrimaryKey
    val id: String = "primary",
    val studioName: String,
    val tagline: String,
    val fullAddress: String,
    val village: String,
    val tehsil: String,
    val district: String,
    val state: String,
    val country: String,
    val pincode: String,
    val mobileNumber: String,
    val formattedMobile: String,
    val callUri: String,
    val whatsappNumber: String,
    val formattedWhatsapp: String,
    val whatsappUrl: String,
    val adminEmail: String,
    val openingHours: String,
    val establishedText: String,
    val isOfflineAvailable: Boolean = true,
    val lastCachedTimestamp: Long = System.currentTimeMillis()
)
