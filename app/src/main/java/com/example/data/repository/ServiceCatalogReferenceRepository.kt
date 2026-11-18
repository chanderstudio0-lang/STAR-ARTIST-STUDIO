package com.example.data.repository

import com.example.data.local.dao.ServiceCatalogReferenceDao
import com.example.data.local.entity.ServiceCatalogReferenceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository mediating Room persistence for visual reference shots attached to Service Catalog packages.
 */
class ServiceCatalogReferenceRepository(
    private val dao: ServiceCatalogReferenceDao
) {
    val allReferences: Flow<List<ServiceCatalogReferenceEntity>> = dao.observeAllReferences()

    val referencesByPackage: Flow<Map<String, List<ServiceCatalogReferenceEntity>>> =
        dao.observeAllReferences().map { items ->
            items.groupBy { it.packageId }
        }

    suspend fun saveReference(
        packageId: String,
        title: String,
        notes: String,
        imageDataUrl: String,
        aspectGuide: String,
        resolutionTag: String
    ): Long {
        val entity = ServiceCatalogReferenceEntity(
            packageId = packageId.trim(),
            title = title.trim().ifEmpty { "Studio Reference Shot" },
            notes = notes.trim(),
            imageDataUrl = imageDataUrl.trim(),
            aspectGuide = aspectGuide.trim().ifEmpty { "4:5 Portrait" },
            resolutionTag = resolutionTag.trim().ifEmpty { "HD Reference" },
            createdAt = System.currentTimeMillis()
        )
        return dao.insertReference(entity)
    }

    suspend fun deleteReference(reference: ServiceCatalogReferenceEntity) {
        dao.deleteReference(reference)
    }

    suspend fun deleteReferencesForPackage(packageId: String) {
        dao.deleteReferencesForPackage(packageId)
    }
}
