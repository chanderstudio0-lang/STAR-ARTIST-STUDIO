package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity storing artist-captured reference photos attached to studio [ServicePackage] items.
 * Enables visual moodboards, lighting setup previews, and quick shot references directly inside
 * the Service Catalog without altering Firestore security rules or requiring broad storage permissions.
 */
@Entity(
    tableName = "service_catalog_references",
    indices = [Index(value = ["packageId"])]
)
data class ServiceCatalogReferenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageId: String,
    val title: String,
    val notes: String = "",
    val imageDataUrl: String,
    val aspectGuide: String = "4:5 Portrait",
    val resolutionTag: String = "HD Reference",
    val createdAt: Long = System.currentTimeMillis()
)
