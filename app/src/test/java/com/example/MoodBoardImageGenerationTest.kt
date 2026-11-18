package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.SuccessResult
import com.example.data.ai.ConceptArtGenerator
import com.example.data.ai.FirebaseConceptArtGenerator
import com.example.data.local.AppDatabase
import com.example.data.model.AppLanguage
import com.example.data.repository.ArtistDraftsRepository
import com.example.data.repository.MoodBoardRepository
import com.example.ui.screens.MoodBoardArtScreen
import com.example.ui.theme.StarArtistTheme
import com.example.ui.viewmodel.MoodBoardArtUiState
import com.example.ui.viewmodel.MoodBoardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class MoodBoardImageGenerationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var appContext: Application
    private lateinit var db: AppDatabase
    private lateinit var moodBoardRepository: MoodBoardRepository
    private lateinit var draftsRepository: ArtistDraftsRepository

    @Before
    fun setup() {
        appContext = ApplicationProvider.getApplicationContext()
        val fakeLoader = ImageLoader.Builder(appContext)
            .components {
                add(coil.intercept.Interceptor { chain ->
                    SuccessResult(
                        drawable = ColorDrawable(Color.DKGRAY),
                        request = chain.request,
                        dataSource = DataSource.MEMORY_CACHE
                    )
                })
            }
            .build()
        Coil.setImageLoader(fakeLoader)

        db = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        moodBoardRepository = MoodBoardRepository(db.moodBoardConceptDao(), appContext)
        draftsRepository = ArtistDraftsRepository(db.projectDraftDao(), db.sessionHistoryDao())
    }

    @After
    fun tearDown() {
        File(appContext.filesDir, "moodboard_art").deleteRecursively()
        db.close()
    }

    private suspend fun awaitCondition(timeoutMs: Long = 5000L, predicate: suspend () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!predicate()) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                delay(20)
            }
        }
    }

    @Test
    fun roomMigration7To8_andMoodBoardRepository_persistFavoriteLinkAndDelete() {
        runBlocking {
        // 1. Verify MIGRATION_7_8 creates mood_board_concepts table & indices on an existing v7 schema
        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(appContext)
            .name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `studio_session_projects` (`id` INTEGER PRIMARY KEY NOT NULL)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        val rawDb = sqliteHelper.writableDatabase
        AppDatabase.MIGRATION_7_8.migrate(rawDb)
        rawDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='mood_board_concepts'").use { cursor ->
            assertTrue("Expected mood_board_concepts table after MIGRATION_7_8", cursor.moveToFirst())
        }
        sqliteHelper.close()

        // 2. Verify MoodBoardRepository saves JPEG on disk & row in Room
        val sampleBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        sampleBitmap.eraseColor(Color.rgb(212, 175, 55))

        val saved = moodBoardRepository.saveGeneratedConcept(
            bitmap = sampleBitmap,
            boardTitle = "Golden Couture Test",
            projectType = "Photography",
            prompt = "Moody gold couture silhouette",
            visualStyle = "Moody Editorial",
            lightingSetup = "Chiaroscuro Rim Light",
            aspectRatioLabel = "1:1 Lookbook",
            modelUsed = FirebaseConceptArtGenerator.MODEL_NANO_BANANA_FAST,
            colorPaletteTags = "Obsidian, Gold"
        )
        assertTrue(saved.id > 0L)
        val imageFile = File(saved.localImagePath)
        assertTrue("Expected persisted mood board JPEG on disk", imageFile.exists() && imageFile.length() > 0L)

        // 3. Toggle favorite & link to ProjectDraftEntity
        moodBoardRepository.toggleFavorite(saved)
        val favorited = moodBoardRepository.getConceptById(saved.id)!!
        assertTrue(favorited.isFavorite)
        assertEquals(1, moodBoardRepository.favoriteConcepts.first().size)

        moodBoardRepository.linkToDraft(favorited, 42L)
        val linked = moodBoardRepository.getConceptById(saved.id)!!
        assertEquals(42L, linked.linkedDraftId)

        // 4. Delete removes both DB row and JPEG file on disk
        moodBoardRepository.deleteConcept(linked)
        assertNull(moodBoardRepository.getConceptById(saved.id))
        assertFalse("Expected image file deleted with concept", imageFile.exists())
        }
    }

    @Test
    fun moodBoardViewModel_handlesGenerationRemixErrorsAndDraftPromotion() {
        runBlocking {
        var shouldFail = false
        var generatedCounter = 0

        val fakeGenerator = object : ConceptArtGenerator {
            override suspend fun generateConceptBitmap(prompt: String, modelName: String): Bitmap? {
                if (shouldFail) {
                    throw IllegalStateException("Firebase App Check attestation error (403) test failure")
                }
                generatedCounter++
                return Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.rgb(180, 140, 40))
                }
            }

            override suspend fun remixConceptBitmap(
                baseBitmap: Bitmap,
                editInstruction: String,
                modelName: String
            ): Bitmap? {
                generatedCounter++
                return Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.rgb(40, 190, 210))
                }
            }
        }

        val viewModel = MoodBoardViewModel(
            moodBoardRepository = moodBoardRepository,
            draftsRepository = draftsRepository,
            generator = fakeGenerator
        )

        // 1. Blank prompt rejects without fake art
        viewModel.generateConceptArt(
            boardTitle = "Empty Prompt",
            projectType = "Photography",
            prompt = "   ",
            visualStyle = "Moody Editorial",
            lightingSetup = "Rim Light",
            aspectRatioLabel = "1:1 Lookbook",
            colorPaletteTags = "Gold"
        )
        assertTrue(viewModel.uiState.value is MoodBoardArtUiState.Error)

        // 2. Real backend error propagates cleanly (no fallback Canvas image!) & recovers on retry
        shouldFail = true
        viewModel.generateConceptArt(
            boardTitle = "Royal Bridal Board",
            projectType = "Bridal",
            prompt = "Crimson silk veil in sunlit palace arches",
            visualStyle = "Royal Heritage",
            lightingSetup = "Golden Hour Warm",
            aspectRatioLabel = "3:4 Editorial",
            colorPaletteTags = "Crimson, Antique Gold"
        )
        awaitCondition { viewModel.uiState.value is MoodBoardArtUiState.Error }
        val errorState = viewModel.uiState.value as MoodBoardArtUiState.Error
        assertTrue(errorState.canRetry)
        assertTrue(errorState.message.contains("403") || errorState.message.contains("App Check"))

        shouldFail = false
        viewModel.retryLastAction()
        awaitCondition { viewModel.uiState.value is MoodBoardArtUiState.Success }
        val successState = viewModel.uiState.value as MoodBoardArtUiState.Success
        assertEquals("Royal Bridal Board", successState.boardTitle)
        assertEquals(1, generatedCounter)

        // 3. Remix updates bitmap via image-to-image call
        viewModel.remixCurrentConcept("Add warm golden floating marigold petals")
        awaitCondition {
            val s = viewModel.uiState.value as? MoodBoardArtUiState.Success
            s != null && s.rawPrompt.contains("Remix:")
        }
        assertEquals(2, generatedCounter)

        // 4. Promote to ProjectDraftEntity persists both MoodBoardConcept and ProjectDraft + session log
        viewModel.createProjectDraftFromCurrentArt()
        awaitCondition {
            val concepts = moodBoardRepository.allConcepts.first()
            val drafts = draftsRepository.allDrafts.first()
            concepts.firstOrNull()?.linkedDraftId != null && drafts.isNotEmpty()
        }
        val savedConcept = moodBoardRepository.allConcepts.first().first()
        val savedDraft = draftsRepository.allDrafts.first().first()
        assertNotNull(savedConcept.linkedDraftId)
        assertEquals(savedDraft.id, savedConcept.linkedDraftId)
        assertEquals("Royal Bridal Board", savedDraft.draftTitle)
        }
    }

    @Test
    fun moodBoardArtScreen_generatesSavesRemixesAndFiltersMoodBoards() {
        runBlocking {
        val fakeGenerator = object : ConceptArtGenerator {
            override suspend fun generateConceptBitmap(prompt: String, modelName: String): Bitmap {
                return Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.rgb(212, 175, 55))
                }
            }

            override suspend fun remixConceptBitmap(
                baseBitmap: Bitmap,
                editInstruction: String,
                modelName: String
            ): Bitmap {
                return Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.rgb(30, 160, 180))
                }
            }
        }

        val viewModel = MoodBoardViewModel(
            moodBoardRepository = moodBoardRepository,
            draftsRepository = draftsRepository,
            generator = fakeGenerator
        )

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                MoodBoardArtScreen(
                    language = AppLanguage.ENGLISH,
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }

        // Scroll to prompt field and trigger generation
        composeTestRule.onNodeWithTag("moodboard_title_input").performTextReplacement("Obsidian Runway Lookbook")
        composeTestRule.onNodeWithTag("moodboard_prompt_input")
            .performTextReplacement("Dramatic studio runway silhouette under gold spotlight beams")
        composeTestRule.onNodeWithTag("moodboard_art_screen")
            .performScrollToNode(hasTestTag("moodboard_generate_btn"))
        composeTestRule.onNodeWithTag("moodboard_generate_btn").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.onAllNodesWithTag("moodboard_generated_image").fetchSemanticsNodes().isNotEmpty()
        }

        // Save generated concept to Room mood boards
        composeTestRule.onNodeWithTag("moodboard_art_screen")
            .performScrollToNode(hasTestTag("moodboard_save_btn"))
        composeTestRule.onNodeWithTag("moodboard_save_btn").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.onAllNodesWithTag("moodboard_empty_archive").fetchSemanticsNodes().isEmpty()
        }

        // Test inline remix action on generated artwork
        composeTestRule.onNodeWithTag("moodboard_art_screen")
            .performScrollToNode(hasTestTag("moodboard_refine_input"))
        composeTestRule.onNodeWithTag("moodboard_refine_input")
            .performTextReplacement("Make shadows deeper")
        composeTestRule.onNodeWithTag("moodboard_refine_btn").performClick()

        awaitCondition {
            val state = viewModel.uiState.value as? MoodBoardArtUiState.Success
            state != null && state.rawPrompt.contains("Make shadows deeper")
        }

        // Verify filter chip hides non-matching category and restores on "All"
        composeTestRule.onNodeWithTag("moodboard_art_screen")
            .performScrollToNode(hasTestTag("moodboard_filter_video_production"))
        composeTestRule.onNodeWithTag("moodboard_filter_video_production").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.onAllNodesWithTag("moodboard_empty_archive").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("moodboard_filter_all").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.onAllNodesWithTag("moodboard_empty_archive").fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithTag("moodboard_art_screen")
            .performScrollToNode(hasTestTag("moodboard_saved_card_1"))
        composeTestRule.onNodeWithTag("moodboard_saved_card_1").assertIsDisplayed()
        }
    }
}
