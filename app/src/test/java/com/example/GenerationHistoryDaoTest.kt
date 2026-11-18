package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.GenerationHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GenerationHistoryDaoTest {

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
    fun insertAndRetrieveHistory_succeeds() = runBlocking {
        val dao = db.generationHistoryDao()
        val item = GenerationHistoryEntity(
            category = "photo",
            title = "Royal Heritage Bridal Stills",
            parametersSummary = "Bridal • Studio Talwara Khurd",
            content = "Full photoshoot production guide..."
        )
        val id = dao.insertHistory(item)
        assertTrue(id > 0)

        val all = dao.getAllHistory().first()
        assertEquals(1, all.size)
        assertEquals("Royal Heritage Bridal Stills", all[0].title)
        assertEquals("photo", all[0].category)

        dao.deleteHistory(all[0])
        val empty = dao.getAllHistory().first()
        assertTrue(empty.isEmpty())
    }
}
