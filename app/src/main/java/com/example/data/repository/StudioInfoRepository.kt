package com.example.data.repository

import com.example.data.local.dao.StudioInfoDao
import com.example.data.local.entity.StudioInfoEntity
import com.example.util.StudioConstants
import com.example.util.StudioContactDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Repository mediating access to locally cached STAR ARTIST STUDIO essential information and contact details.
 * Ensures the app has instantaneous offline access to verified studio contact channels, addresses, and directions.
 */
class StudioInfoRepository(
    private val studioInfoDao: StudioInfoDao,
    externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        fun defaultStudioInfo(): StudioInfoEntity = StudioInfoEntity(
            id = "primary",
            studioName = StudioConstants.STUDIO_NAME,
            tagline = StudioContactDetails.TAGLINE,
            fullAddress = StudioContactDetails.ADDRESS,
            village = StudioContactDetails.VILLAGE,
            tehsil = StudioContactDetails.TEHSIL,
            district = StudioContactDetails.DISTRICT,
            state = StudioContactDetails.STATE,
            country = StudioContactDetails.COUNTRY,
            pincode = StudioContactDetails.PINCODE,
            mobileNumber = StudioContactDetails.MOBILE_NUMBER,
            formattedMobile = StudioContactDetails.FORMATTED_MOBILE,
            callUri = StudioContactDetails.CALL_URI,
            whatsappNumber = StudioContactDetails.WHATSAPP_NUMBER,
            formattedWhatsapp = StudioContactDetails.FORMATTED_WHATSAPP,
            whatsappUrl = StudioContactDetails.WHATSAPP_URL,
            adminEmail = StudioContactDetails.ADMIN_EMAIL,
            openingHours = StudioContactDetails.STUDIO_HOURS,
            establishedText = "EST. 125102 • SIRSA, HARYANA",
            isOfflineAvailable = true,
            lastCachedTimestamp = System.currentTimeMillis()
        )
    }

    init {
        // Pre-seed local Room cache if it does not yet exist
        externalScope.launch {
            seedCacheIfEmpty()
        }
    }

    suspend fun seedCacheIfEmpty() {
        val existing = studioInfoDao.getStudioInfoOnce()
        if (existing == null || existing.adminEmail != StudioContactDetails.ADMIN_EMAIL) {
            studioInfoDao.insertOrUpdate(defaultStudioInfo())
        }
    }

    /**
     * Reactive stream of cached studio info from Room, guaranteeing a non-null fallback.
     */
    val studioInfo: Flow<StudioInfoEntity> = studioInfoDao.getStudioInfo().map { cached ->
        cached ?: defaultStudioInfo()
    }

    suspend fun refreshCache(): StudioInfoEntity {
        val refreshed = defaultStudioInfo().copy(lastCachedTimestamp = System.currentTimeMillis())
        studioInfoDao.insertOrUpdate(refreshed)
        return refreshed
    }
}
