package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.local.dao.ProjectDraftDao
import com.example.data.local.dao.SessionHistoryDao
import com.example.data.local.entity.ProjectDraftEntity
import com.example.data.local.entity.SessionHistoryEntity
import com.example.data.repository.ArtistDraftsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ArtistProjectDraftsAndHistoryTest {

    private lateinit var db: AppDatabase
    private lateinit var draftDao: ProjectDraftDao
    private lateinit var sessionDao: SessionHistoryDao
    private lateinit var repository: ArtistDraftsRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        draftDao = db.projectDraftDao()
        sessionDao = db.sessionHistoryDao()
        repository = ArtistDraftsRepository(draftDao, sessionDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun projectDraft_insertAndRetrieve_persistsCorrectly() = runBlocking {
        val draft = ProjectDraftEntity(
            draftTitle = "Royal Velvet Bridal",
            projectType = "Bridal",
            clientOrModelName = "Simran Kaur",
            conceptNotes = "Regal traditional lehenga in rich velvet with candle & warm rim highlights",
            lightingSetupNotes = "Key softbox 45 deg, warm backlight rim, fill card",
            cameraGearNotes = "50mm f/1.2 & 85mm portrait prime",
            targetShootDate = "2026-11-28",
            estimatedBudgetInr = 25000L,
            isFavorite = true
        )

        val id = repository.saveDraft(draft)
        assertTrue("Generated ID should be positive", id > 0)

        val retrieved = repository.getDraftById(id)
        assertNotNull(retrieved)
        assertEquals("Royal Velvet Bridal", retrieved?.draftTitle)
        assertEquals("Bridal", retrieved?.projectType)
        assertEquals("Simran Kaur", retrieved?.clientOrModelName)
        assertEquals(25000L, retrieved?.estimatedBudgetInr)
        assertTrue(retrieved?.isFavorite == true)

        // Verify automatic session history logging
        val sessions = repository.allSessions.first()
        assertTrue("Session history should record draft creation", sessions.isNotEmpty())
        assertEquals("DRAFT_CREATION", sessions[0].sessionType)
        assertTrue(sessions[0].sessionTitle.contains("Royal Velvet Bridal"))
    }

    @Test
    fun projectDraft_updateAndToggleFavorite_worksCorrectly() = runBlocking {
        val draft = ProjectDraftEntity(
            draftTitle = "Cyberpunk Fashion Reel",
            projectType = "Fashion",
            conceptNotes = "Neon and smoke mood"
        )
        val id = repository.saveDraft(draft)
        val inserted = repository.getDraftById(id)!!
        assertFalse(inserted.isFavorite)

        repository.toggleFavorite(inserted)
        val afterToggle = repository.getDraftById(id)!!
        assertTrue("Draft should now be marked favorite", afterToggle.isFavorite)

        val favorites = repository.favoriteDrafts.first()
        assertEquals(1, favorites.size)
        assertEquals(id, favorites[0].id)
    }

    @Test
    fun projectDraft_deletion_clearsFromRoom() = runBlocking {
        val draft = ProjectDraftEntity(
            draftTitle = "Commercial Catalog Shoot",
            projectType = "Commercial"
        )
        val id = repository.saveDraft(draft)
        assertNotNull(repository.getDraftById(id))

        repository.deleteDraftById(id)
        assertNull(repository.getDraftById(id))

        val allDrafts = repository.allDrafts.first()
        assertTrue(allDrafts.isEmpty())
    }

    @Test
    fun sessionHistory_manualLogAndClear_succeeds() = runBlocking {
        repository.logSession(
            sessionType = "AI_CONCEPT",
            sessionTitle = "Royal Concept Generation",
            summaryDetails = "Generated 4-scene lighting & moodboard guide",
            durationSeconds = 12L
        )

        val sessions = repository.allSessions.first()
        assertEquals(1, sessions.size)
        assertEquals("AI_CONCEPT", sessions[0].sessionType)
        assertEquals(12L, sessions[0].sessionDurationSeconds)

        repository.clearAllSessions()
        val emptySessions = repository.allSessions.first()
        assertTrue(emptySessions.isEmpty())
    }
}
