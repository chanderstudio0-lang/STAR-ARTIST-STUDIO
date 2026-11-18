package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.StudioInfoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudioInfoDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveStudioInfo_succeeds() = runBlocking {
        val dao = db.studioInfoDao()
        val defaultInfo = StudioInfoRepository.defaultStudioInfo()
        dao.insertOrUpdate(defaultInfo)

        val retrieved = dao.getStudioInfoOnce()
        assertNotNull(retrieved)
        assertEquals("STAR ARTIST STUDIO", retrieved?.studioName)
        assertEquals("9812890081", retrieved?.mobileNumber)
        assertEquals("9541024098", retrieved?.whatsappNumber)
        assertEquals("Talwara Khurd", retrieved?.village)
        assertTrue(retrieved?.isOfflineAvailable == true)

        val flowEmission = dao.getStudioInfo().first()
        assertNotNull(flowEmission)
        assertEquals(retrieved?.fullAddress, flowEmission?.fullAddress)
    }

    @Test
    fun repository_seedsDefaultCache_whenEmpty() = runBlocking {
        val dao = db.studioInfoDao()
        val repository = StudioInfoRepository(dao)
        repository.seedCacheIfEmpty()

        val emission = repository.studioInfo.first()
        assertEquals("STAR ARTIST STUDIO", emission.studioName)
        assertEquals("chanderstudio0@gmail.com", emission.adminEmail)
        assertTrue(emission.isOfflineAvailable)
    }

    @Test
    fun repository_updatesStaleAdminEmail_whenReseeded() = runBlocking {
        val dao = db.studioInfoDao()
        val staleInfo = StudioInfoRepository.defaultStudioInfo().copy(adminEmail = "old@example.com")
        dao.insertOrUpdate(staleInfo)

        val repository = StudioInfoRepository(dao)
        repository.seedCacheIfEmpty()

        val emission = repository.studioInfo.first()
        assertEquals("chanderstudio0@gmail.com", emission.adminEmail)
    }
}
