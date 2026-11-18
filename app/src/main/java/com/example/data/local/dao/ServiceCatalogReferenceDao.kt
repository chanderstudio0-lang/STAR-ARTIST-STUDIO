package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ServiceCatalogReferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceCatalogReferenceDao {

    @Query("SELECT * FROM service_catalog_references ORDER BY createdAt DESC, id DESC")
    fun observeAllReferences(): Flow<List<ServiceCatalogReferenceEntity>>

    @Query("SELECT * FROM service_catalog_references WHERE packageId = :packageId ORDER BY createdAt DESC, id DESC")
    fun observeReferencesForPackage(packageId: String): Flow<List<ServiceCatalogReferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReference(reference: ServiceCatalogReferenceEntity): Long

    @Delete
    suspend fun deleteReference(reference: ServiceCatalogReferenceEntity)

    @Query("DELETE FROM service_catalog_references WHERE id = :id")
    suspend fun deleteReferenceById(id: Long)

    @Query("DELETE FROM service_catalog_references WHERE packageId = :packageId")
    suspend fun deleteReferencesForPackage(packageId: String)
}
