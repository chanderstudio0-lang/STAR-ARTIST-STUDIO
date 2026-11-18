package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.StudioProjectStatus
import com.example.data.local.entity.StudioSessionProjectEntity
import com.example.data.model.AppLanguage
import com.example.data.repository.StudioSessionProjectsRepository
import com.example.ui.screens.ProjectStatusTrackerScreen
import com.example.ui.screens.ScheduleEventKind
import com.example.ui.screens.StudioScheduleCalendarHelper
import com.example.ui.theme.StarArtistTheme
import com.example.ui.viewmodel.ProjectStatusViewModel
import com.example.ui.viewmodel.ProjectTrackerUiState
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ProjectStatusViewModelAndRoomTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: StudioSessionProjectsRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StudioSessionProjectsRepository(db.studioSessionProjectDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun awaitCondition(timeoutMs: Long = 5000L, predicate: suspend () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!predicate()) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                delay(25)
            }
        }
    }

    @Test
    fun roomSchemaAndMigration6To7_persistAndTransitionStatusesAccurately() = runBlocking {
        // 1. Verify MIGRATION_6_7 creates studio_session_projects table & indexes cleanly
        val context = ApplicationProvider.getApplicationContext<Application>()
        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory SQLite helper
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Simulate v6 schema before running migration 6 -> 7
                    db.execSQL("CREATE TABLE IF NOT EXISTS `studio_info` (`id` INTEGER PRIMARY KEY NOT NULL)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        val rawDb = sqliteHelper.writableDatabase
        AppDatabase.MIGRATION_6_7.migrate(rawDb)
        rawDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='studio_session_projects'").use { cursor ->
            assertTrue("Expected studio_session_projects table after MIGRATION_6_7", cursor.moveToFirst())
        }
        sqliteHelper.close()

        // 2. Verify DAO & Repository CRUD + TypeConverter workflow states
        val planningId = repository.createProject(
            StudioSessionProjectEntity(
                sessionCode = "SAS-9001",
                projectTitle = "Golden Hour Bridal Test",
                clientName = "Ananya Bridal House",
                serviceCategory = "Photography",
                status = StudioProjectStatus.PLANNING,
                scheduledDate = "2026-12-01"
            )
        )
        assertTrue(planningId > 0L)

        var entity = repository.getProjectById(planningId)!!
        assertEquals(StudioProjectStatus.PLANNING, entity.status)
        assertNull(entity.startedAt)
        assertNull(entity.completedAt)
        assertEquals(1, repository.observeCountByStatus(StudioProjectStatus.PLANNING).first())

        // Transition Planning -> In Production
        repository.updateProjectStatus(entity, StudioProjectStatus.IN_PRODUCTION)
        entity = repository.getProjectById(planningId)!!
        assertEquals(StudioProjectStatus.IN_PRODUCTION, entity.status)
        assertNotNull(entity.startedAt)
        assertNull(entity.completedAt)
        assertEquals(StudioProjectStatus.IN_PRODUCTION.defaultProgress, entity.progressPercent)

        // Transition In Production -> Completed
        repository.updateProjectStatus(entity, StudioProjectStatus.COMPLETED)
        entity = repository.getProjectById(planningId)!!
        assertEquals(StudioProjectStatus.COMPLETED, entity.status)
        assertNotNull(entity.completedAt)
        assertEquals(100, entity.progressPercent)
        assertEquals(1, repository.observeByStatus(StudioProjectStatus.COMPLETED).first().size)

        // Reverting back to Planning resets timestamps cleanly
        repository.updateProjectStatus(entity, StudioProjectStatus.PLANNING)
        entity = repository.getProjectById(planningId)!!
        assertEquals(StudioProjectStatus.PLANNING, entity.status)
        assertNull(entity.startedAt)
        assertNull(entity.completedAt)

        // Delete record
        repository.deleteProject(entity)
        assertNull(repository.getProjectById(planningId))
    }

    @Test
    fun roomMigration9To10_andStopwatchTimer_trackPauseSettleAndResetAccurately() {
        runBlocking {
            // 1. Verify MIGRATION_9_10 adds stopwatch columns on existing v9 studio_session_projects table
            val context = ApplicationProvider.getApplicationContext<Application>()
            val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `studio_session_projects` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `sessionCode` TEXT NOT NULL,
                                `projectTitle` TEXT NOT NULL,
                                `clientName` TEXT NOT NULL,
                                `serviceCategory` TEXT NOT NULL,
                                `status` TEXT NOT NULL,
                                `scheduledDate` TEXT NOT NULL,
                                `studioStage` TEXT NOT NULL,
                                `leadDirector` TEXT NOT NULL,
                                `progressPercent` INTEGER NOT NULL,
                                `deliverableCount` INTEGER NOT NULL,
                                `budgetInr` INTEGER NOT NULL,
                                `productionNotes` TEXT NOT NULL,
                                `startedAt` INTEGER,
                                `completedAt` INTEGER,
                                `createdAt` INTEGER NOT NULL,
                                `updatedAt` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
            val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
            val rawDb = sqliteHelper.writableDatabase
            AppDatabase.MIGRATION_9_10.migrate(rawDb)
            rawDb.query("PRAGMA table_info(`studio_session_projects`)").use { cursor ->
                val cols = mutableSetOf<String>()
                val nameIdx = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    cols.add(cursor.getString(nameIdx))
                }
                assertTrue(cols.contains("trackedProductionMillis"))
                assertTrue(cols.contains("activeTimerStartedAt"))
            }
            sqliteHelper.close()

            // 2. Verify controllable clock stopwatch Start -> Tick -> Pause -> Auto-Settle -> Reset
            var fakeNow = 1_000_000L
            val clockedRepo = StudioSessionProjectsRepository(
                dao = db.studioSessionProjectDao(),
                clockMillis = { fakeNow }
            )
            val viewModel = ProjectStatusViewModel(
                repository = clockedRepo,
                seedDefaultsOnInit = false,
                clockMillis = { fakeNow }
            )

            val projectId = clockedRepo.createProject(
                StudioSessionProjectEntity(
                    sessionCode = "SAS-STOP1",
                    projectTitle = "Chrono Lighting Calibration",
                    clientName = "Aura Couture",
                    serviceCategory = "Photography",
                    status = StudioProjectStatus.IN_PRODUCTION
                )
            )
            var current = clockedRepo.getProjectById(projectId)!!
            assertEquals("00:00:00", StudioSessionProjectEntity.formatDurationHhMmSs(current.computeTotalTrackedMillis(fakeNow)))

            // Start stopwatch
            viewModel.toggleProductionTimer(current)
            awaitCondition { clockedRepo.getProjectById(projectId)?.isTimerRunning == true }
            current = clockedRepo.getProjectById(projectId)!!
            assertEquals(1_000_000L, current.activeTimerStartedAt)

            // Advance 1 min 5 sec
            fakeNow += 65_000L
            assertEquals(65_000L, current.computeTotalTrackedMillis(fakeNow))
            assertEquals("00:01:05", StudioSessionProjectEntity.formatDurationHhMmSs(current.computeTotalTrackedMillis(fakeNow)))

            // Pause stopwatch
            viewModel.toggleProductionTimer(current)
            awaitCondition { clockedRepo.getProjectById(projectId)?.isTimerRunning == false }
            current = clockedRepo.getProjectById(projectId)!!
            assertEquals(65_000L, current.trackedProductionMillis)
            assertNull(current.activeTimerStartedAt)

            // Resume at fakeNow = 2_000_000L, advance +10s, then transition status out of IN_PRODUCTION
            fakeNow = 2_000_000L
            viewModel.toggleProductionTimer(current)
            awaitCondition { clockedRepo.getProjectById(projectId)?.isTimerRunning == true }
            current = clockedRepo.getProjectById(projectId)!!

            fakeNow += 10_000L
            viewModel.updateProjectStatus(current, StudioProjectStatus.POST_PROCESSING)
            awaitCondition {
                clockedRepo.getProjectById(projectId)?.status == StudioProjectStatus.POST_PROCESSING
            }
            current = clockedRepo.getProjectById(projectId)!!
            assertNull("Expected active timer to settle/pause on leaving IN_PRODUCTION", current.activeTimerStartedAt)
            assertEquals(75_000L, current.trackedProductionMillis)

            // Reset stopwatch
            viewModel.resetProductionTimer(current)
            awaitCondition { clockedRepo.getProjectById(projectId)?.trackedProductionMillis == 0L }
            assertEquals(0L, clockedRepo.getProjectById(projectId)!!.trackedProductionMillis)
        }
    }

    @Test
    fun roomMigration10To11_andArchiveCompletedProjects_hideFromDashboardAndRestoreCleanly() {
        runBlocking {
            // 1. Verify MIGRATION_10_11 adds isArchived, archivedAt, and index
            val context = ApplicationProvider.getApplicationContext<Application>()
            val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `studio_session_projects` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `sessionCode` TEXT NOT NULL,
                                `projectTitle` TEXT NOT NULL,
                                `clientName` TEXT NOT NULL,
                                `serviceCategory` TEXT NOT NULL,
                                `status` TEXT NOT NULL,
                                `scheduledDate` TEXT NOT NULL,
                                `studioStage` TEXT NOT NULL,
                                `leadDirector` TEXT NOT NULL,
                                `progressPercent` INTEGER NOT NULL,
                                `deliverableCount` INTEGER NOT NULL,
                                `budgetInr` INTEGER NOT NULL,
                                `productionNotes` TEXT NOT NULL,
                                `startedAt` INTEGER,
                                `completedAt` INTEGER,
                                `trackedProductionMillis` INTEGER NOT NULL DEFAULT 0,
                                `activeTimerStartedAt` INTEGER,
                                `createdAt` INTEGER NOT NULL,
                                `updatedAt` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
            val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
            val rawDb = sqliteHelper.writableDatabase
            AppDatabase.MIGRATION_10_11.migrate(rawDb)
            rawDb.query("PRAGMA table_info(`studio_session_projects`)").use { cursor ->
                val cols = mutableSetOf<String>()
                val nameIdx = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    cols.add(cursor.getString(nameIdx))
                }
                assertTrue(cols.contains("isArchived"))
                assertTrue(cols.contains("archivedAt"))
            }
            sqliteHelper.close()

            // 2. Verify repository/ViewModel only archives COMPLETED projects and preserves historical data
            var fakeNow = 9_000_000L
            val clockedRepo = StudioSessionProjectsRepository(
                dao = db.studioSessionProjectDao(),
                clockMillis = { fakeNow }
            )
            val planningId = clockedRepo.createProject(
                StudioSessionProjectEntity(
                    sessionCode = "SAS-PLAN",
                    projectTitle = "Active Pre-Shoot",
                    clientName = "Client A",
                    serviceCategory = "Photography",
                    status = StudioProjectStatus.PLANNING
                )
            )
            val completedId = clockedRepo.createProject(
                StudioSessionProjectEntity(
                    sessionCode = "SAS-DONE",
                    projectTitle = "Wrapped Commercial Shoot",
                    clientName = "Client B",
                    serviceCategory = "Commercial",
                    status = StudioProjectStatus.COMPLETED,
                    trackedProductionMillis = 7_200_000L,
                    progressPercent = 100
                )
            )

            val viewModel = ProjectStatusViewModel(
                repository = clockedRepo,
                seedDefaultsOnInit = false,
                clockMillis = { fakeNow }
            )

            val planningEntity = clockedRepo.getProjectById(planningId)!!
            val completedEntity = clockedRepo.getProjectById(completedId)!!

            // Non-completed project cannot be archived
            viewModel.archiveProject(planningEntity)
            awaitCondition {
                viewModel.statusFeedback.value.orEmpty().contains("Only completed")
            }
            assertEquals(2, clockedRepo.activeProjects.first().size)

            // Archive completed project -> hidden from activeProjects, preserved in archivedProjects & allProjects
            viewModel.archiveProject(completedEntity)
            awaitCondition {
                clockedRepo.activeProjects.first().size == 1 &&
                    clockedRepo.archivedProjects.first().size == 1
            }
            val archivedRecord = clockedRepo.archivedProjects.first().first()
            assertTrue(archivedRecord.isArchived)
            assertEquals(fakeNow, archivedRecord.archivedAt)
            assertEquals(7_200_000L, archivedRecord.trackedProductionMillis)
            assertEquals(2, clockedRepo.allProjects.first().size)

            // Restore returns project back to active dashboard
            viewModel.restoreArchivedProject(archivedRecord)
            awaitCondition {
                clockedRepo.activeProjects.first().size == 2 &&
                    clockedRepo.archivedProjects.first().isEmpty()
            }

            // Bulk archiveAllCompleted archives only completed sessions
            viewModel.archiveAllCompleted()
            awaitCondition {
                clockedRepo.activeProjects.first().size == 1 &&
                    clockedRepo.archivedProjects.first().size == 1
            }
            assertEquals(planningId, clockedRepo.activeProjects.first().first().id)
        }
    }

    @Test
    fun roomMigration11To12_andInProductionTaskNotes_persistAcrossDetailSaves() {
        runBlocking {
            // 1. Verify MIGRATION_11_12 adds inProductionTaskNotes column on existing v11 schema
            val context = ApplicationProvider.getApplicationContext<Application>()
            val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(11) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `studio_session_projects` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `sessionCode` TEXT NOT NULL,
                                `projectTitle` TEXT NOT NULL,
                                `clientName` TEXT NOT NULL,
                                `serviceCategory` TEXT NOT NULL,
                                `status` TEXT NOT NULL,
                                `scheduledDate` TEXT NOT NULL,
                                `studioStage` TEXT NOT NULL,
                                `leadDirector` TEXT NOT NULL,
                                `progressPercent` INTEGER NOT NULL,
                                `deliverableCount` INTEGER NOT NULL,
                                `budgetInr` INTEGER NOT NULL,
                                `productionNotes` TEXT NOT NULL,
                                `startedAt` INTEGER,
                                `completedAt` INTEGER,
                                `trackedProductionMillis` INTEGER NOT NULL DEFAULT 0,
                                `activeTimerStartedAt` INTEGER,
                                `isArchived` INTEGER NOT NULL DEFAULT 0,
                                `archivedAt` INTEGER,
                                `createdAt` INTEGER NOT NULL,
                                `updatedAt` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
            val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
            val rawDb = sqliteHelper.writableDatabase
            AppDatabase.MIGRATION_11_12.migrate(rawDb)
            rawDb.query("PRAGMA table_info(`studio_session_projects`)").use { cursor ->
                val cols = mutableSetOf<String>()
                val nameIdx = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    cols.add(cursor.getString(nameIdx))
                }
                assertTrue(cols.contains("inProductionTaskNotes"))
            }
            sqliteHelper.close()

            // 2. Save custom 'In Production' task notes in Room and verify persistence across status transitions
            val projId = repository.createProject(
                StudioSessionProjectEntity(
                    sessionCode = "SAS-NOTES",
                    projectTitle = "Editorial Strobe Shoot",
                    clientName = "Maya Couture",
                    serviceCategory = "Photography",
                    status = StudioProjectStatus.IN_PRODUCTION
                )
            )
            val viewModel = ProjectStatusViewModel(repository, seedDefaultsOnInit = false)
            val created = repository.getProjectById(projId)!!

            viewModel.saveInProductionTaskNotes(
                created,
                "85mm f/1.4 key light at 45 deg, rim 1/4 CTO warm gel"
            )
            awaitCondition {
                repository.getProjectById(projId)?.inProductionTaskNotes ==
                    "85mm f/1.4 key light at 45 deg, rim 1/4 CTO warm gel"
            }

            // Transition status -> POST_PROCESSING preserves the saved task notes
            val updated = repository.getProjectById(projId)!!
            viewModel.updateProjectStatus(updated, StudioProjectStatus.POST_PROCESSING)
            awaitCondition {
                repository.getProjectById(projId)?.status == StudioProjectStatus.POST_PROCESSING
            }
            assertEquals(
                "85mm f/1.4 key light at 45 deg, rim 1/4 CTO warm gel",
                repository.getProjectById(projId)!!.inProductionTaskNotes
            )
        }
    }

    @Test
    fun projectStatusViewModel_tracksTransitionsFilteringAndSummary() = runBlocking {
        val viewModel = ProjectStatusViewModel(repository, seedDefaultsOnInit = false)

        // Start collecting stateIn flows via empty no-op job wait helper
        var created = false
        viewModel.createSessionProject(
            projectTitle = "Editorial Runway Lookbook",
            clientName = "Kaur Couture",
            serviceCategory = "Modeling",
            status = StudioProjectStatus.PLANNING,
            scheduledDate = "2026-12-05",
            productionNotes = "Casting & cyclorama setup"
        ) { ok -> created = ok }

        awaitCondition { created && repository.allProjects.first().isNotEmpty() }

        viewModel.createSessionProject(
            projectTitle = "4K Music Video Stage Shoot",
            clientName = "Sukh Records",
            serviceCategory = "Video Production",
            status = StudioProjectStatus.IN_PRODUCTION,
            scheduledDate = "2026-12-07"
        )

        awaitCondition { repository.allProjects.first().size == 2 }

        val all = repository.allProjects.first()
        assertEquals(2, all.size)

        val planningProj = all.first { it.status == StudioProjectStatus.PLANNING }
        viewModel.updateProjectStatus(planningProj, StudioProjectStatus.COMPLETED)
        awaitCondition {
            repository.getProjectById(planningProj.id)?.status == StudioProjectStatus.COMPLETED
        }

        val completedEntity = repository.getProjectById(planningProj.id)!!
        assertEquals(100, completedEntity.progressPercent)

        val inProdProj = all.first { it.status == StudioProjectStatus.IN_PRODUCTION }
        viewModel.advanceProjectStage(inProdProj)
        awaitCondition {
            repository.getProjectById(inProdProj.id)?.status == StudioProjectStatus.POST_PROCESSING
        }
        viewModel.advanceProjectStage(repository.getProjectById(inProdProj.id)!!)
        awaitCondition {
            repository.getProjectById(inProdProj.id)?.status == StudioProjectStatus.COMPLETED
        }

        // Verify filter and search state flows reactively filter projects
        viewModel.setStatusFilter(StudioProjectStatus.COMPLETED)
        viewModel.setSearchQuery("Runway")
        awaitCondition {
            viewModel.selectedStatusFilter.value == StudioProjectStatus.COMPLETED &&
                viewModel.searchQuery.value == "Runway"
        }

        // Verify delete removes item and emits feedback
        viewModel.deleteSessionProject(repository.getProjectById(planningProj.id)!!)
        awaitCondition {
            repository.allProjects.first().size == 1 &&
                viewModel.statusFeedback.value.orEmpty().contains("Removed")
        }
        assertTrue(viewModel.statusFeedback.value.orEmpty().contains("Removed"))
    }

    private fun waitUntilWithMainIdle(
        timeoutMillis: Long = 5000L,
        failureDetails: () -> String = { "" },
        condition: () -> Boolean
    ) {
        try {
            composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                condition()
            }
        } catch (t: AssertionError) {
            val extra = failureDetails()
            if (extra.isNotBlank()) {
                throw AssertionError("$extra | ${t.message}", t)
            } else {
                throw t
            }
        }
    }

    @Test
    fun projectStatusTrackerScreen_rendersSeededStatusesFiltersTransitionsAndCreatesSession() {
        val viewModel = ProjectStatusViewModel(repository, seedDefaultsOnInit = true)

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                ProjectStatusTrackerScreen(
                    language = AppLanguage.ENGLISH,
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }

        waitUntilWithMainIdle(
            failureDetails = {
                "Seeded state mismatch: uiState=${viewModel.uiState.value}, summary=${viewModel.statusSummary.value}"
            }
        ) {
            viewModel.uiState.value is ProjectTrackerUiState.Success &&
                viewModel.statusSummary.value.total == 5
        }
        composeTestRule.waitForIdle()

        // Verify header & KPI pills are shown
        composeTestRule.onNodeWithTag("project_tracker_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("project_tracker_kpi_planning").assertIsDisplayed()
        composeTestRule.onNodeWithTag("project_tracker_kpi_in_production").assertIsDisplayed()
        composeTestRule.onNodeWithTag("project_tracker_kpi_completed").assertIsDisplayed()

        // Filter to 'Planning' only (2 seeded items: SAS-2602, SAS-2605)
        composeTestRule.onNodeWithTag("project_tracker_filter_planning").performClick()
        waitUntilWithMainIdle {
            val projects = (viewModel.uiState.value as? ProjectTrackerUiState.Success)?.projects.orEmpty()
            projects.size == 2 && projects.all { it.status == StudioProjectStatus.PLANNING }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Punjabi Single 4K Anamorphic Shoot").assertIsDisplayed()

        // Advance one Planning project -> In Production via card action button
        val targetPlanning = (viewModel.uiState.value as ProjectTrackerUiState.Success).projects.first {
            it.projectTitle == "Punjabi Single 4K Anamorphic Shoot"
        }
        composeTestRule.onNodeWithTag("project_tracker_screen")
            .performScrollToNode(hasTestTag("project_tracker_advance_btn_${targetPlanning.id}"))
        composeTestRule.onNodeWithTag("project_tracker_advance_btn_${targetPlanning.id}")
            .performClick()

        waitUntilWithMainIdle(
            failureDetails = {
                "Transition mismatch: summary=${viewModel.statusSummary.value}, uiState=${viewModel.uiState.value}"
            }
        ) {
            viewModel.statusSummary.value.planningCount == 1 &&
                viewModel.statusSummary.value.inProductionCount == 2
        }

        // Clear filter back to All
        composeTestRule.onNodeWithTag("project_tracker_screen")
            .performScrollToNode(hasTestTag("project_tracker_filter_all"))
        composeTestRule.onNodeWithTag("project_tracker_filter_all").performClick()
        waitUntilWithMainIdle {
            val projects = (viewModel.uiState.value as? ProjectTrackerUiState.Success)?.projects.orEmpty()
            projects.size == 5
        }

        // Add a brand new Studio Session Project via modal dialog
        composeTestRule.onNodeWithTag("project_tracker_screen")
            .performScrollToNode(hasTestTag("project_tracker_add_btn"))
        composeTestRule.onNodeWithTag("project_tracker_add_btn").performClick()

        waitUntilWithMainIdle {
            composeTestRule.onAllNodes(hasTestTag("project_tracker_dialog_title_input"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("project_tracker_dialog_title_input")
            .performScrollTo()
            .performTextReplacement("Winter Bridal Reel Shoot")
        composeTestRule.onNodeWithTag("project_tracker_dialog_client_input")
            .performScrollTo()
            .performTextReplacement("Mehak Brides")
        composeTestRule.onNodeWithTag("project_tracker_dialog_status_in_production")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("project_tracker_dialog_save_btn")
            .performClick()

        waitUntilWithMainIdle {
            viewModel.statusSummary.value.total == 6 &&
                composeTestRule.onAllNodesWithText("Winter Bridal Reel Shoot")
                    .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Winter Bridal Reel Shoot").assertIsDisplayed()
    }

    @Test
    fun projectStatusTrackerScreen_stopwatchStartsTicksPausesAndResetsOnInProductionCards() {
        runBlocking {
            var fakeNow = 5_000_000L
            val clockedRepo = StudioSessionProjectsRepository(
                dao = db.studioSessionProjectDao(),
                clockMillis = { fakeNow }
            )
            val projectId = clockedRepo.createProject(
                StudioSessionProjectEntity(
                    sessionCode = "SAS-LIVE1",
                    projectTitle = "Neon Runway Production",
                    clientName = "Star Talent",
                    serviceCategory = "Modeling",
                    status = StudioProjectStatus.IN_PRODUCTION
                )
            )
            val viewModel = ProjectStatusViewModel(
                repository = clockedRepo,
                seedDefaultsOnInit = false,
                clockMillis = { fakeNow }
            )

            composeTestRule.setContent {
                StarArtistTheme(isStudioLight = false) {
                    ProjectStatusTrackerScreen(
                        language = AppLanguage.ENGLISH,
                        viewModel = viewModel,
                        onBack = {}
                    )
                }
            }

            val stopwatchTag = "project_tracker_stopwatch_$projectId"
            val timeTag = "project_tracker_stopwatch_time_$projectId"
            val toggleTag = "project_tracker_stopwatch_toggle_$projectId"
            val resetTag = "project_tracker_stopwatch_reset_$projectId"

            waitUntilWithMainIdle {
                (viewModel.uiState.value as? ProjectTrackerUiState.Success)?.projects?.isNotEmpty() == true
            }

            composeTestRule.onNodeWithTag("project_tracker_screen")
                .performScrollToNode(hasTestTag(stopwatchTag))
            composeTestRule.onNodeWithTag("project_tracker_screen")
                .performScrollToNode(hasTestTag(toggleTag))
            composeTestRule.onNodeWithTag(timeTag).assertTextEquals("00:00:00")

            // Start the stopwatch and advance mock clock by 1h 01m 05s
            composeTestRule.onNodeWithTag(toggleTag).performClick()
            waitUntilWithMainIdle {
                (viewModel.uiState.value as? ProjectTrackerUiState.Success)
                    ?.projects?.firstOrNull { it.id == projectId }?.isTimerRunning == true
            }

            fakeNow += 3_665_000L
            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                composeTestRule.onAllNodesWithText("01:01:05").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag(timeTag).assertTextEquals("01:01:05")

            // Pause stopwatch & verify Resume label appears with preserved time
            composeTestRule.onNodeWithTag(toggleTag).performClick()
            waitUntilWithMainIdle {
                (viewModel.uiState.value as? ProjectTrackerUiState.Success)
                    ?.projects?.firstOrNull { it.id == projectId }?.isTimerRunning == false
            }
            composeTestRule.onNodeWithText("Resume").assertIsDisplayed()
            composeTestRule.onNodeWithTag(timeTag).assertTextEquals("01:01:05")

            // Reset stopwatch & verify readout resets to 00:00:00
            composeTestRule.onNodeWithTag(resetTag).performClick()
            composeTestRule.waitUntil(timeoutMillis = 5000L) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                (viewModel.uiState.value as? ProjectTrackerUiState.Success)
                    ?.projects?.firstOrNull { it.id == projectId }?.trackedProductionMillis == 0L
            }
            composeTestRule.onNodeWithTag(timeTag).assertTextEquals("00:00:00")
        }
    }

    @Test
    fun projectStatusTrackerScreen_archiveHidesCompletedFromMainDashboardAndRestoresFromHistory() {
        runBlocking {
            val viewModel = ProjectStatusViewModel(repository, seedDefaultsOnInit = true)

            composeTestRule.setContent {
                StarArtistTheme(isStudioLight = false) {
                    ProjectStatusTrackerScreen(
                        language = AppLanguage.ENGLISH,
                        viewModel = viewModel,
                        onBack = {}
                    )
                }
            }

            waitUntilWithMainIdle {
                viewModel.uiState.value is ProjectTrackerUiState.Success &&
                    viewModel.statusSummary.value.total == 5
            }

            val completedProject = (viewModel.uiState.value as ProjectTrackerUiState.Success)
                .projects.first { it.status == StudioProjectStatus.COMPLETED }

            // Scroll to completed card's Archive button and click it
            composeTestRule.onNodeWithTag("project_tracker_screen")
                .performScrollToNode(hasTestTag("project_tracker_archive_btn_${completedProject.id}"))
            composeTestRule.onNodeWithTag("project_tracker_archive_btn_${completedProject.id}")
                .performClick()

            // Main dashboard count drops 5 -> 4, archivedCount increments 0 -> 1, and card is hidden
            waitUntilWithMainIdle {
                viewModel.statusSummary.value.total == 4 &&
                    viewModel.statusSummary.value.completedCount == 0 &&
                    viewModel.statusSummary.value.archivedCount == 1 &&
                    composeTestRule.onAllNodesWithText(completedProject.projectTitle)
                        .fetchSemanticsNodes().isEmpty()
            }

            // Switch to Archived historical reference tab & confirm project + logged time badge appear
            composeTestRule.onNodeWithTag("project_tracker_screen")
                .performScrollToNode(hasTestTag("project_tracker_view_archived_tab"))
            composeTestRule.onNodeWithTag("project_tracker_view_archived_tab").performClick()

            waitUntilWithMainIdle {
                composeTestRule.onAllNodesWithText(completedProject.projectTitle)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag("project_tracker_archived_badge_${completedProject.id}")
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag("project_tracker_logged_time_${completedProject.id}")
                .assertIsDisplayed()

            // Restore project back to main dashboard
            composeTestRule.onNodeWithTag("project_tracker_screen")
                .performScrollToNode(hasTestTag("project_tracker_restore_btn_${completedProject.id}"))
            composeTestRule.onNodeWithTag("project_tracker_restore_btn_${completedProject.id}")
                .performClick()

            waitUntilWithMainIdle {
                viewModel.statusSummary.value.total == 5 &&
                    viewModel.statusSummary.value.archivedCount == 0
            }

            composeTestRule.onNodeWithTag("project_tracker_view_active_tab").performClick()
            waitUntilWithMainIdle {
                composeTestRule.onAllNodesWithText(completedProject.projectTitle)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @Test
    fun projectDetailDialog_savesPersistentInProductionTaskNotesToRoom() {
        runBlocking {
            val projId = repository.createProject(
                StudioSessionProjectEntity(
                    sessionCode = "SAS-DETAIL1",
                    projectTitle = "Golden Couture Stage A",
                    clientName = "Noor Couture",
                    serviceCategory = "Photography",
                    status = StudioProjectStatus.IN_PRODUCTION
                )
            )
            val viewModel = ProjectStatusViewModel(repository, seedDefaultsOnInit = false)

            composeTestRule.setContent {
                StarArtistTheme(isStudioLight = false) {
                    ProjectStatusTrackerScreen(
                        language = AppLanguage.ENGLISH,
                        viewModel = viewModel,
                        onBack = {}
                    )
                }
            }

            val detailTriggerTag = "project_tracker_open_detail_$projId"
            waitUntilWithMainIdle {
                (viewModel.uiState.value as? ProjectTrackerUiState.Success)?.projects?.isNotEmpty() == true
            }

            // Scroll to and tap the project overview to open the Project Detail View
            composeTestRule.onNodeWithTag("project_tracker_screen")
                .performScrollToNode(hasTestTag(detailTriggerTag))
            composeTestRule.onNodeWithTag(detailTriggerTag).performClick()
            waitUntilWithMainIdle {
                composeTestRule.onAllNodes(hasTestTag("project_detail_dialog")).fetchSemanticsNodes().isNotEmpty()
            }

            // Enter custom 'In Production' task notes and save to Room
            val customNotes = "Look 2 shot at ISO 200; gold reflector camera right approved."
            composeTestRule.onNodeWithTag("project_detail_in_production_notes_input")
                .performScrollTo()
                .performTextReplacement(customNotes)
            composeTestRule.onNodeWithTag("project_detail_save_notes_btn")
                .performScrollTo()
                .performClick()

            waitUntilWithMainIdle {
                viewModel.allProjects.value.firstOrNull { it.id == projId }?.inProductionTaskNotes == customNotes &&
                    composeTestRule.onAllNodesWithText("Saved in Room").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(customNotes, repository.getProjectById(projId)?.inProductionTaskNotes)

            // Close Project Detail View & verify preview appears on main card
            composeTestRule.onNodeWithTag("project_detail_close_btn").performClick()
            waitUntilWithMainIdle {
                composeTestRule.onAllNodes(hasTestTag("project_detail_dialog")).fetchSemanticsNodes().isEmpty() &&
                    composeTestRule.onAllNodes(hasTestTag("project_tracker_card_task_notes_$projId"))
                        .fetchSemanticsNodes().isNotEmpty()
            }

            // Re-open Project Detail View and confirm notes persisted cleanly in the input field
            composeTestRule.onNodeWithTag(detailTriggerTag).performClick()
            waitUntilWithMainIdle {
                composeTestRule.onAllNodes(hasTestTag("project_detail_dialog")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag("project_detail_in_production_notes_input")
                .assertTextContains(customNotes)
        }
    }

    @Test
    fun roomMigration12To13_andScheduleCalendarHelper_syncSessionDatesDeadlinesAndOverdue() = runBlocking {
        // 1. Verify MIGRATION_12_13 adds deadlineDate without losing existing v12 rows
        val context = ApplicationProvider.getApplicationContext<Application>()
        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(12) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `studio_session_projects` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `sessionCode` TEXT NOT NULL,
                            `projectTitle` TEXT NOT NULL,
                            `clientName` TEXT NOT NULL,
                            `serviceCategory` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `scheduledDate` TEXT NOT NULL,
                            `studioStage` TEXT NOT NULL,
                            `leadDirector` TEXT NOT NULL,
                            `progressPercent` INTEGER NOT NULL,
                            `deliverableCount` INTEGER NOT NULL,
                            `budgetInr` INTEGER NOT NULL,
                            `productionNotes` TEXT NOT NULL,
                            `inProductionTaskNotes` TEXT NOT NULL DEFAULT '',
                            `startedAt` INTEGER,
                            `completedAt` INTEGER,
                            `trackedProductionMillis` INTEGER NOT NULL DEFAULT 0,
                            `activeTimerStartedAt` INTEGER,
                            `isArchived` INTEGER NOT NULL DEFAULT 0,
                            `archivedAt` INTEGER,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO `studio_session_projects` (
                            `sessionCode`, `projectTitle`, `clientName`, `serviceCategory`, `status`,
                            `scheduledDate`, `studioStage`, `leadDirector`, `progressPercent`, `deliverableCount`,
                            `budgetInr`, `productionNotes`, `createdAt`, `updatedAt`
                        ) VALUES (
                            'SAS-V12', 'Legacy Shoot', 'Studio Client', 'Photography', 'PLANNING',
                            '2026-11-18', 'Stage A', 'Lead DOP', 20, 25, 10000, '', 1000, 1000
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        val rawDb = sqliteHelper.writableDatabase
        AppDatabase.MIGRATION_12_13.migrate(rawDb)
        rawDb.query("PRAGMA table_info(`studio_session_projects`)").use { cursor ->
            val cols = mutableSetOf<String>()
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) cols.add(cursor.getString(nameIdx))
            assertTrue(cols.contains("deadlineDate"))
        }
        rawDb.query("SELECT projectTitle, deadlineDate FROM `studio_session_projects` WHERE sessionCode='SAS-V12'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Legacy Shoot", cursor.getString(0))
            assertEquals("", cursor.getString(1))
        }
        sqliteHelper.close()

        // 2. Verify date parsing & monthly calendar overview calculations on fixed "2026-11-15"
        assertEquals("2026-11-05", StudioScheduleCalendarHelper.normalizeIsoDate("2026-11-5"))
        assertEquals("2026-11-18", StudioScheduleCalendarHelper.normalizeIsoDate("2026/11/18"))
        assertNull(StudioScheduleCalendarHelper.normalizeIsoDate("TBD"))

        val nov15Millis = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 15, 10, 0, 0)
        }.timeInMillis

        val projects = listOf(
            StudioSessionProjectEntity(
                id = 1L,
                projectTitle = "Overdue Lookbook",
                clientName = "Aura",
                serviceCategory = "Photography",
                status = StudioProjectStatus.IN_PRODUCTION,
                scheduledDate = "2026-11-10",
                deadlineDate = "2026-11-14" // strictly before Nov 15 -> overdue
            ),
            StudioSessionProjectEntity(
                id = 2L,
                projectTitle = "Completed Bridal",
                clientName = "Maharani",
                serviceCategory = "Bridal",
                status = StudioProjectStatus.COMPLETED,
                scheduledDate = "2026-11-08",
                deadlineDate = "2026-11-12" // completed -> NOT overdue
            ),
            StudioSessionProjectEntity(
                id = 3L,
                projectTitle = "December Music Video",
                clientName = "Crown Beats",
                serviceCategory = "Video Production",
                status = StudioProjectStatus.PLANNING,
                scheduledDate = "2026-12-04",
                deadlineDate = "2026-12-12"
            )
        )

        val novOverview = StudioScheduleCalendarHelper.buildMonthlyScheduleOverview(
            projects = projects,
            year = 2026,
            monthZeroBased = Calendar.NOVEMBER,
            todayMillis = nov15Millis
        )
        assertEquals("November 2026", novOverview.monthLabel)
        assertEquals(2, novOverview.totalSessionsInMonth)
        assertEquals(2, novOverview.totalDeadlinesInMonth)
        assertEquals(1, novOverview.overdueDeadlinesInMonth)
        assertEquals(ScheduleEventKind.SESSION_DATE, novOverview.eventsByDate["2026-11-10"]?.single()?.kind)
        assertTrue(novOverview.eventsByDate["2026-11-14"]?.single()?.isOverdue == true)
        assertTrue(novOverview.eventsByDate["2026-11-12"]?.single()?.isOverdue == false)

        // Month shifting across year boundary
        val (decYear, decMonth) = StudioScheduleCalendarHelper.shiftYearMonth(2026, Calendar.NOVEMBER, 1)
        assertEquals(2026 to Calendar.DECEMBER, decYear to decMonth)
        val (janYear, janMonth) = StudioScheduleCalendarHelper.shiftYearMonth(2026, Calendar.DECEMBER, 1)
        assertEquals(2027 to Calendar.JANUARY, janYear to janMonth)

        val decOverview = StudioScheduleCalendarHelper.buildMonthlyScheduleOverview(
            projects = projects,
            year = decYear,
            monthZeroBased = decMonth,
            todayMillis = nov15Millis
        )
        assertEquals(1, decOverview.totalSessionsInMonth)
        assertEquals(1, decOverview.totalDeadlinesInMonth)
        assertEquals(0, decOverview.overdueDeadlinesInMonth)
    }

    @Test
    fun projectScheduleCalendarView_syncsWithRoomProjectsDisplaysDeadlinesAndOpensDetailDialog() {
        runBlocking {
            val fixedNov15Millis = Calendar.getInstance().apply {
                clear()
                set(2026, Calendar.NOVEMBER, 15, 9, 30, 0)
            }.timeInMillis

            val seededLookbookId = repository.createProject(
                StudioSessionProjectEntity(
                    sessionCode = "SAS-CAL1",
                    projectTitle = "Royal Couture Winter Lookbook",
                    clientName = "Noor Couture House",
                    serviceCategory = "Photography",
                    status = StudioProjectStatus.IN_PRODUCTION,
                    scheduledDate = "2026-11-16",
                    deadlineDate = "2026-11-20"
                )
            )
            repository.createProject(
                StudioSessionProjectEntity(
                    sessionCode = "SAS-CAL2",
                    projectTitle = "Desert Vogue Editorial",
                    clientName = "Rhea Kapoor",
                    serviceCategory = "Modeling",
                    status = StudioProjectStatus.PLANNING,
                    scheduledDate = "2026-12-05",
                    deadlineDate = "2026-12-10"
                )
            )

            val viewModel = ProjectStatusViewModel(
                repository = repository,
                seedDefaultsOnInit = false,
                clockMillis = { fixedNov15Millis }
            )

            composeTestRule.setContent {
                StarArtistTheme(isStudioLight = false) {
                    ProjectStatusTrackerScreen(
                        language = AppLanguage.ENGLISH,
                        viewModel = viewModel,
                        onBack = {}
                    )
                }
            }

            waitUntilWithMainIdle {
                (viewModel.uiState.value as? ProjectTrackerUiState.Success)?.projects?.size == 2
            }

            // Switch to Calendar View tab
            composeTestRule.onNodeWithTag("project_tracker_view_calendar_tab").performClick()

            waitUntilWithMainIdle {
                composeTestRule.onAllNodes(hasTestTag("project_schedule_calendar_view"))
                    .fetchSemanticsNodes().isNotEmpty()
            }

            // Verify November 2026 monthly overview & metrics from Room data
            composeTestRule.onNodeWithTag("project_calendar_month_label").assertTextEquals("November 2026")
            composeTestRule.onNodeWithTag("project_tracker_screen")
                .performScrollToNode(hasTestTag("project_calendar_day_2026-11-20"))

            // Verify session dot on Nov 16 and deadline dot on Nov 20 (unmerged tree inside clickable day tile)
            composeTestRule.onNodeWithTag("project_calendar_dot_session_2026-11-16", useUnmergedTree = true)
                .assertExists()
            composeTestRule.onNodeWithTag("project_calendar_dot_deadline_2026-11-20", useUnmergedTree = true)
                .assertExists()

            // Tap Nov 20 on the calendar grid -> verify synced deadline event renders in agenda
            composeTestRule.onNodeWithTag("project_calendar_day_2026-11-20").performClick()
            waitUntilWithMainIdle {
                composeTestRule.onAllNodes(hasTestTag("project_calendar_event_deadline_$seededLookbookId"))
                    .fetchSemanticsNodes().isNotEmpty()
            }

            composeTestRule.onNodeWithTag("project_tracker_screen")
                .performScrollToNode(hasTestTag("project_calendar_event_deadline_$seededLookbookId"))
            composeTestRule.onNodeWithTag("project_calendar_event_deadline_$seededLookbookId")
                .performClick()

            // Verify ProjectDetailDialog opens with synced deadline label
            waitUntilWithMainIdle {
                composeTestRule.onAllNodes(hasTestTag("project_detail_dialog"))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag("project_detail_deadline_label").assertIsDisplayed()
            composeTestRule.onNodeWithTag("project_detail_close_btn").performClick()

            // Navigate forward to December 2026 and verify synced Dec session & deadline
            composeTestRule.onNodeWithTag("project_tracker_screen")
                .performScrollToNode(hasTestTag("project_calendar_next_month_btn"))
            composeTestRule.onNodeWithTag("project_calendar_next_month_btn").performClick()

            waitUntilWithMainIdle {
                composeTestRule.onAllNodesWithText("December 2026").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag("project_calendar_day_2026-12-05").performClick()
            composeTestRule.onNodeWithTag("project_calendar_agenda_month_tab").performClick()
            waitUntilWithMainIdle {
                composeTestRule.onAllNodesWithText("Desert Vogue Editorial").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }
}
