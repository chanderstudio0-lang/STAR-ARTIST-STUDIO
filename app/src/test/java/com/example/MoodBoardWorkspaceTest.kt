package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.util.Base64
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
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
import com.example.data.ai.FirebaseConceptArtGenerator
import com.example.data.local.AppDatabase
import com.example.data.local.entity.MoodBoardReferenceEntity
import com.example.data.local.entity.StudioProjectStatus
import com.example.data.local.entity.StudioSessionProjectEntity
import com.example.data.model.AppLanguage
import com.example.data.repository.MoodBoardRepository
import com.example.data.repository.StudioSessionProjectsRepository
import com.example.ui.screens.MoodBoardWorkspaceScreen
import com.example.ui.theme.StarArtistTheme
import com.example.ui.viewmodel.MoodBoardMediaFilter
import com.example.ui.viewmodel.MoodBoardOrganizerViewModel
import com.example.ui.viewmodel.MoodBoardTile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class MoodBoardWorkspaceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var appContext: Application
    private lateinit var db: AppDatabase
    private lateinit var moodBoardRepository: MoodBoardRepository
    private lateinit var sessionProjectsRepository: StudioSessionProjectsRepository

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
        moodBoardRepository = MoodBoardRepository(
            dao = db.moodBoardConceptDao(),
            appContext = appContext,
            referenceDao = db.moodBoardReferenceDao()
        )
        sessionProjectsRepository = StudioSessionProjectsRepository(db.studioSessionProjectDao())
    }

    @After
    fun tearDown() {
        File(appContext.filesDir, "moodboard_art").deleteRecursively()
        File(appContext.filesDir, "moodboard_refs").deleteRecursively()
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

    private fun createSampleJpegDataUrl(color: Int = Color.rgb(212, 175, 55)): String {
        val bmp = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        bmp.recycle()
        return "data:image/jpeg;base64,$encoded"
    }

    @Test
    fun roomMigration8To9_andMoodBoardReferences_persistProjectAssignmentPinAndDelete() {
        runBlocking {
            // 1. Verify MIGRATION_8_9 alters mood_board_concepts and creates mood_board_references
            val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(appContext)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(8) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `mood_board_concepts` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `boardTitle` TEXT NOT NULL,
                                `projectType` TEXT NOT NULL,
                                `prompt` TEXT NOT NULL,
                                `visualStyle` TEXT NOT NULL,
                                `lightingSetup` TEXT NOT NULL,
                                `aspectRatioLabel` TEXT NOT NULL,
                                `modelUsed` TEXT NOT NULL,
                                `localImagePath` TEXT NOT NULL,
                                `colorPaletteTags` TEXT NOT NULL,
                                `linkedDraftId` INTEGER,
                                `isFavorite` INTEGER NOT NULL,
                                `createdAt` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
            val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
            val rawDb = sqliteHelper.writableDatabase
            AppDatabase.MIGRATION_8_9.migrate(rawDb)

            rawDb.query("PRAGMA table_info(`mood_board_concepts`)").use { cursor ->
                val cols = mutableSetOf<String>()
                val nameIdx = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    cols.add(cursor.getString(nameIdx))
                }
                assertTrue(cols.contains("sessionProjectId"))
                assertTrue(cols.contains("boardSectionTag"))
                assertTrue(cols.contains("sortOrder"))
            }
            rawDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='mood_board_references'").use { cursor ->
                assertTrue("Expected mood_board_references table after MIGRATION_8_9", cursor.moveToFirst())
            }
            sqliteHelper.close()

            // 2. Verify reference photo persistence into app-private moodboard_refs directory
            val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.rgb(200, 150, 40))
            }
            val savedRef = moodBoardRepository.addReferencePhotoFromBitmap(
                bitmap = bitmap,
                title = "Backlit Rim Test",
                sectionTag = "Lighting Ref",
                sourceType = MoodBoardReferenceEntity.SOURCE_CAMERA_SHOT,
                notes = "1/4 CTO gel on hair light",
                sessionProjectId = 7L,
                colorSwatches = "Warm Amber, Deep Noir"
            )
            assertTrue(savedRef.id > 0L)
            val refFile = File(savedRef.imagePathOrUrl)
            assertTrue(refFile.exists() && refFile.absolutePath.contains("moodboard_refs"))

            // 3. Pin toggle, metadata update, and project assignment
            moodBoardRepository.toggleReferencePin(savedRef)
            val pinned = moodBoardRepository.getReferenceById(savedRef.id)!!
            assertTrue(pinned.isPinned)

            moodBoardRepository.updateReferenceMetadata(
                reference = pinned,
                title = "Updated Key Light Diagram",
                sectionTag = "Set & Props",
                notes = "8ft octabox camera left",
                colorSwatches = "Ivory, Gold",
                sessionProjectId = 12L
            )
            val updated = moodBoardRepository.getReferenceById(savedRef.id)!!
            assertEquals("Updated Key Light Diagram", updated.title)
            assertEquals("Set & Props", updated.sectionTag)
            assertEquals(12L, updated.sessionProjectId)

            // 4. Delete cleans up both DB row and stored image file
            moodBoardRepository.deleteReference(updated)
            assertNull(moodBoardRepository.getReferenceById(savedRef.id))
            assertFalse(refFile.exists())
        }
    }

    @Test
    fun moodBoardOrganizerViewModel_unifiesConceptArtAndReferencePhotosPerProject() {
        runBlocking {
            val proj1Id = sessionProjectsRepository.createProject(
                StudioSessionProjectEntity(
                    sessionCode = "SAS-9001",
                    projectTitle = "Royal Couture Shoot",
                    clientName = "Noor Couture",
                    serviceCategory = "Photography",
                    status = StudioProjectStatus.IN_PRODUCTION
                )
            )
            val proj2Id = sessionProjectsRepository.createProject(
                StudioSessionProjectEntity(
                    sessionCode = "SAS-9002",
                    projectTitle = "Neon Music Video",
                    clientName = "Crown Beats",
                    serviceCategory = "Video Production",
                    status = StudioProjectStatus.PLANNING
                )
            )

            val conceptBmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.rgb(212, 175, 55))
            }
            val concept = moodBoardRepository.saveGeneratedConcept(
                bitmap = conceptBmp,
                boardTitle = "Velvet Throne Concept",
                projectType = "Photography",
                prompt = "Regal velvet draping with gold rim lighting",
                visualStyle = "Moody Editorial",
                lightingSetup = "Chiaroscuro Rim Light",
                aspectRatioLabel = "3:4 Editorial",
                modelUsed = FirebaseConceptArtGenerator.MODEL_NANO_BANANA_PRO,
                colorPaletteTags = "Obsidian, Gold",
                sessionProjectId = proj1Id,
                boardSectionTag = "Concept Art"
            )

            val camRef = moodBoardRepository.addReferenceFromUrlOrDataUrl(
                imagePathOrUrl = createSampleJpegDataUrl(Color.rgb(180, 120, 40)),
                title = "Palace Archway Test Shot",
                sectionTag = "Lighting Ref",
                sourceType = MoodBoardReferenceEntity.SOURCE_CAMERA_SHOT,
                notes = "Natural 4pm window spill",
                sessionProjectId = proj1Id
            )

            val stageRef = moodBoardRepository.addReferenceFromUrlOrDataUrl(
                imagePathOrUrl = createSampleJpegDataUrl(Color.rgb(30, 140, 210)),
                title = "Cyan Hazer Gel Ref",
                sectionTag = "Color Reference",
                sourceType = MoodBoardReferenceEntity.SOURCE_GALLERY_PHOTO,
                notes = "Dual Astera tube backlight",
                sessionProjectId = proj2Id
            )

            val viewModel = MoodBoardOrganizerViewModel(
                moodBoardRepository = moodBoardRepository,
                sessionProjectsRepository = sessionProjectsRepository,
                seedProjectsOnInit = false
            )
            // Keep StateFlows active during test verification
            val collectJob = launch { viewModel.filteredTiles.collect {} }
            val statsJob = launch { viewModel.workspaceStats.collect {} }
            val projectsJob = launch { viewModel.ongoingProjects.collect {} }

            awaitCondition { viewModel.ongoingProjects.value.size == 2 && viewModel.filteredTiles.value.size == 3 }

            // Select Project 1 and verify mixed Concept Art + Reference Photo tiles side-by-side
            viewModel.selectProject(proj1Id)
            awaitCondition { viewModel.filteredTiles.value.size == 2 }
            assertEquals(2, viewModel.workspaceStats.value.totalItems)
            assertEquals(1, viewModel.workspaceStats.value.conceptArtCount)
            assertEquals(1, viewModel.workspaceStats.value.referencePhotoCount)

            // Media type filters
            viewModel.setMediaFilter(MoodBoardMediaFilter.CONCEPT_ART)
            awaitCondition {
                viewModel.filteredTiles.value.size == 1 &&
                    viewModel.filteredTiles.value.first() is MoodBoardTile.ConceptArtTile
            }
            assertTrue(viewModel.filteredTiles.value.first() is MoodBoardTile.ConceptArtTile)

            viewModel.setMediaFilter(MoodBoardMediaFilter.REFERENCE_PHOTOS)
            awaitCondition {
                viewModel.filteredTiles.value.size == 1 &&
                    viewModel.filteredTiles.value.first() is MoodBoardTile.ReferencePhotoTile
            }
            assertEquals(camRef.title, viewModel.filteredTiles.value.first().title)

            // Toggle pin & filter PINNED
            viewModel.toggleTilePin(MoodBoardTile.ReferencePhotoTile(camRef))
            viewModel.setMediaFilter(MoodBoardMediaFilter.PINNED)
            awaitCondition { viewModel.filteredTiles.value.size == 1 && viewModel.filteredTiles.value.first().isPinned }

            // Search query & reassign Project 2's reference into Project 1
            viewModel.setMediaFilter(MoodBoardMediaFilter.ALL)
            viewModel.updateTileOrganization(
                tile = MoodBoardTile.ReferencePhotoTile(stageRef),
                targetProjectId = proj1Id,
                sectionTag = "Set & Props",
                notes = "Reassigned to Royal Couture pre-light"
            )
            awaitCondition { viewModel.filteredTiles.value.size == 3 }

            viewModel.setSearchQuery("Hazer")
            awaitCondition { viewModel.filteredTiles.value.size == 1 }
            assertEquals("Set & Props", viewModel.filteredTiles.value.first().sectionTag)
            assertEquals(concept.sessionProjectId, proj1Id)

            collectJob.cancel()
            statsJob.cancel()
            projectsJob.cancel()
        }
    }

    @Test
    fun moodBoardWorkspaceScreen_rendersOrganizesInspectsAndReassignsTiles() {
        runBlocking {
            val proj1Id = sessionProjectsRepository.createProject(
                StudioSessionProjectEntity(
                    sessionCode = "SAS-701",
                    projectTitle = "Golden Bridal Suite",
                    clientName = "Riya Malhotra",
                    serviceCategory = "Bridal",
                    status = StudioProjectStatus.IN_PRODUCTION
                )
            )
            val proj2Id = sessionProjectsRepository.createProject(
                StudioSessionProjectEntity(
                    sessionCode = "SAS-702",
                    projectTitle = "Editorial Denim Look",
                    clientName = "Studio Roster",
                    serviceCategory = "Modeling",
                    status = StudioProjectStatus.PLANNING
                )
            )

            val concept = moodBoardRepository.saveGeneratedConcept(
                bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.DKGRAY) },
                boardTitle = "Saffron Drape Concept",
                projectType = "Bridal",
                prompt = "Saffron silk bridal drape under warm arch shadows",
                visualStyle = "Royal Heritage",
                lightingSetup = "Golden Hour Warm",
                aspectRatioLabel = "3:4 Editorial",
                modelUsed = FirebaseConceptArtGenerator.MODEL_NANO_BANANA_PRO,
                colorPaletteTags = "Saffron, Gold",
                sessionProjectId = proj1Id
            )

            val refPhoto = moodBoardRepository.addReferenceFromUrlOrDataUrl(
                imagePathOrUrl = createSampleJpegDataUrl(Color.rgb(90, 150, 190)),
                title = "Backlight Halo Check",
                sectionTag = "Lighting Ref",
                sourceType = MoodBoardReferenceEntity.SOURCE_CAMERA_SHOT,
                notes = "200W strobe behind veil",
                sessionProjectId = proj1Id
            )

            val viewModel = MoodBoardOrganizerViewModel(
                moodBoardRepository = moodBoardRepository,
                sessionProjectsRepository = sessionProjectsRepository,
                seedProjectsOnInit = false
            )

            composeTestRule.setContent {
                StarArtistTheme(isStudioLight = false) {
                    MoodBoardWorkspaceScreen(
                        language = AppLanguage.ENGLISH,
                        viewModel = viewModel,
                        onBack = {},
                        onOpenConceptGenerator = {},
                        onOpenCameraReference = {},
                        onRemixConceptInAiStudio = {}
                    )
                }
            }

            val conceptTag = "moodboard_tile_concept_${concept.id}"
            val refTag = "moodboard_tile_ref_${refPhoto.id}"

            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                composeTestRule.onAllNodesWithTag("moodboard_project_pill_$proj1Id").fetchSemanticsNodes().isNotEmpty() &&
                    composeTestRule.onAllNodesWithTag(conceptTag).fetchSemanticsNodes().isNotEmpty() &&
                    composeTestRule.onAllNodesWithTag(refTag).fetchSemanticsNodes().isNotEmpty()
            }

            // Select Project 1 pill & confirm both AI concept art + reference photo tiles appear side-by-side
            composeTestRule.onNodeWithTag("moodboard_project_pill_$proj1Id").performClick()
            composeTestRule.onNodeWithTag(conceptTag).assertIsDisplayed()
            composeTestRule.onNodeWithTag(refTag).assertIsDisplayed()

            // Filter by Concept Art only
            composeTestRule.onNodeWithTag("moodboard_ws_media_concept_art").performClick()
            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                composeTestRule.onAllNodesWithTag(conceptTag).fetchSemanticsNodes().isNotEmpty() &&
                    composeTestRule.onAllNodesWithTag(refTag).fetchSemanticsNodes().isEmpty()
            }

            // Filter by Reference Photos only
            composeTestRule.onNodeWithTag("moodboard_ws_media_reference_photos").performClick()
            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                composeTestRule.onAllNodesWithTag(refTag).fetchSemanticsNodes().isNotEmpty() &&
                    composeTestRule.onAllNodesWithTag(conceptTag).fetchSemanticsNodes().isEmpty()
            }

            // Restore All media filter and open Tile Inspector on refPhoto
            composeTestRule.onNodeWithTag("moodboard_ws_media_all").performClick()
            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                composeTestRule.onAllNodesWithTag(refTag).fetchSemanticsNodes().isNotEmpty()
            }

            composeTestRule.onNodeWithTag(refTag).performClick()
            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                composeTestRule.onAllNodesWithTag("moodboard_inspector_dialog").fetchSemanticsNodes().isNotEmpty()
            }

            // Reassign to Project 2 and tag as "Set & Props", then save
            composeTestRule.onNodeWithTag("inspector_assign_project_$proj2Id").performClick()
            composeTestRule.onNodeWithTag("inspector_tag_set_&_props").performClick()
            composeTestRule.onNodeWithTag("inspector_notes_input").performTextReplacement("Moved to Denim Look set")
            composeTestRule.onNodeWithTag("inspector_save_btn").performClick()

            // Since Project 1 pill is still selected, refPhoto should disappear from Project 1 board
            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                composeTestRule.onAllNodesWithTag("moodboard_inspector_dialog").fetchSemanticsNodes().isEmpty() &&
                    composeTestRule.onAllNodesWithTag(refTag).fetchSemanticsNodes().isEmpty()
            }

            // Switch to Project 2 pill and confirm reassigned refPhoto is now there
            composeTestRule.onNodeWithTag("moodboard_project_pill_$proj2Id").performClick()
            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                composeTestRule.onAllNodesWithTag(refTag).fetchSemanticsNodes().isNotEmpty()
            }

            // Open Inspector on Project 2's tile and delete it
            composeTestRule.onNodeWithTag(refTag).performClick()
            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                composeTestRule.onAllNodesWithTag("inspector_delete_btn").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag("inspector_delete_btn").performClick()

            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                composeTestRule.onAllNodesWithTag("moodboard_ws_empty_state").fetchSemanticsNodes().isNotEmpty()
            }
            assertNotNull(moodBoardRepository.getConceptById(concept.id))
            assertNull(moodBoardRepository.getReferenceById(refPhoto.id))
        }
    }
}
