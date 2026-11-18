package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.ArtistBioRepository
import com.example.ui.components.validateArtistBioForRoom
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
class ArtistBioRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ArtistBioRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ArtistBioRepository(db.artistBioDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveBio_persistsAndObservesViaFlow() = runBlocking {
        val userId = "artist_uid_001"
        val bio = "Editorial runway model and commercial talent with 4 years of studio experience."

        repository.saveBio(
            userId = userId,
            specialty = "Fashion Model",
            bioText = "  $bio  ",
            syncedToCloud = false
        )

        val observed = repository.observeBio(userId).first()
        assertNotNull(observed)
        assertEquals(userId, observed?.userId)
        assertEquals("Fashion Model", observed?.specialty)
        assertEquals(bio, observed?.bioText)
        assertFalse(observed?.syncedToCloud ?: true)
    }

    @Test
    fun saveBio_replacesExistingEntryForSameUser() = runBlocking {
        val userId = "artist_uid_002"
        repository.saveBio(
            userId = userId,
            specialty = "Photographer",
            bioText = "Initial draft bio.",
            syncedToCloud = false
        )

        repository.saveBio(
            userId = userId,
            specialty = "Videographer",
            bioText = "Updated 4K music video director & commercial cinematographer.",
            syncedToCloud = true
        )

        val loaded = repository.getBioOnce(userId)
        assertNotNull(loaded)
        assertEquals("Videographer", loaded?.specialty)
        assertEquals("Updated 4K music video director & commercial cinematographer.", loaded?.bioText)
        assertTrue(loaded?.syncedToCloud ?: false)
    }

    @Test
    fun saveBio_truncatesBeyondMaxLengthAndIsolatesUsers() = runBlocking {
        val oversizedBio = "A".repeat(620)
        repository.saveBio(
            userId = "artist_a",
            specialty = "Makeup Artist",
            bioText = oversizedBio
        )
        repository.saveBio(
            userId = "artist_b",
            specialty = "Music Artist",
            bioText = "Vocalist & stage performer."
        )

        val loadedA = repository.getBioOnce("artist_a")
        val loadedB = repository.getBioOnce("artist_b")
        assertEquals(ArtistBioRepository.MAX_BIO_LENGTH, loadedA?.bioText?.length)
        assertEquals("Vocalist & stage performer.", loadedB?.bioText)

        repository.deleteBio("artist_a")
        assertNull(repository.getBioOnce("artist_a"))
        assertNotNull(repository.getBioOnce("artist_b"))
    }

    @Test
    fun validateArtistBioForRoom_enforcesNonBlankAndMaxLength() {
        assertEquals(
            "Please enter your professional bio before saving.",
            validateArtistBioForRoom("")
        )
        assertEquals(
            "Please enter your professional bio before saving.",
            validateArtistBioForRoom("   \n\t  ")
        )
        assertNull(validateArtistBioForRoom("4K portrait photographer based in Sirsa."))
        assertEquals(
            "Artist bio must be 500 characters or less.",
            validateArtistBioForRoom("B".repeat(501))
        )
    }
}
