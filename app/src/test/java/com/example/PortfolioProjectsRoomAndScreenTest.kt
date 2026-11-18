package com.example

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.SuccessResult
import com.example.data.local.AppDatabase
import com.example.data.local.entity.PortfolioProjectEntity
import com.example.data.local.entity.PortfolioProjectMediaType
import com.example.data.model.AppLanguage
import com.example.data.repository.PortfolioProjectsRepository
import com.example.ui.screens.PortfolioProjectsScreen
import com.example.ui.screens.ProjectMediaFilter
import com.example.ui.screens.PortfolioProjectFilterUtils
import com.example.ui.theme.StarArtistTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class PortfolioProjectsRoomAndScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: PortfolioProjectsRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val fakeLoader = ImageLoader.Builder(context)
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

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PortfolioProjectsRepository(db.portfolioProjectDao())
    }

    @After
    fun tearDown() {
        db.close()
        Coil.reset()
    }

    @Test
    fun roomSchema_seedsObservesFavoritesAndDeletesPortfolioProjects() = runBlocking {
        repository.seedShowcaseProjectsIfEmpty()
        // Calling seed twice should not duplicate showcase entries
        repository.seedShowcaseProjectsIfEmpty()

        val all = repository.allProjects.first()
        assertEquals(9, all.size)

        val photos = repository.observeByMediaType(PortfolioProjectMediaType.PHOTO).first()
        val videos = repository.observeByMediaType(PortfolioProjectMediaType.VIDEO).first()
        assertEquals(5, photos.size)
        assertEquals(4, videos.size)

        // Insert a new custom video project
        val insertedId = repository.saveProject(
            PortfolioProjectEntity(
                title = "  Autumn Commercial Film  ",
                mediaType = PortfolioProjectMediaType.VIDEO,
                category = "Commercial",
                thumbnailUrl = "drawable://commercial_luxury",
                mediaUrl = "drawable://commercial_luxury",
                clientName = "Sirsa Gold House",
                cameraGear = "Sony FX3 • 50mm GM",
                durationSeconds = 125,
                resolutionTag = "4K 60fps"
            )
        )
        assertTrue(insertedId > 0L)

        val fetched = repository.getProjectById(insertedId)
        assertNotNull(fetched)
        assertEquals("Autumn Commercial Film", fetched!!.title)
        assertFalse(fetched.isFavorite)

        // Toggle favorite and verify favorite Flow updates
        repository.toggleFavorite(fetched)
        val favoritesAfterToggle = repository.observeFavorites().first()
        assertTrue(favoritesAfterToggle.any { it.id == insertedId && it.isFavorite })

        // Delete inserted project
        repository.deleteProjectById(insertedId)
        assertEquals(9, repository.allProjects.first().size)
    }

    @Test
    fun repositoryAndFilterUtils_formatDurationsAndFilterAccurately() {
        assertEquals("03:24", PortfolioProjectsRepository.formatDuration(204))
        assertEquals("00:45", PortfolioProjectsRepository.formatDuration(45))
        assertEquals("00:00", PortfolioProjectsRepository.formatDuration(0))

        val sample = PortfolioProjectsRepository.defaultShowcaseProjects()
            .mapIndexed { index, entity -> entity.copy(id = index.toLong() + 1L) }

        val videoOnly = PortfolioProjectFilterUtils.filterProjects(
            projects = sample,
            mediaFilter = ProjectMediaFilter.VIDEO,
            selectedCategory = "All",
            searchQuery = ""
        )
        assertEquals(4, videoOnly.size)
        assertTrue(videoOnly.all { it.mediaType == PortfolioProjectMediaType.VIDEO })

        val searchBridal = PortfolioProjectFilterUtils.filterProjects(
            projects = sample,
            mediaFilter = ProjectMediaFilter.ALL,
            selectedCategory = "Bridal",
            searchQuery = "Maharani"
        )
        assertEquals(1, searchBridal.size)
        assertEquals("Royal Heritage Bridal Lookbook", searchBridal.first().title)
    }

    @Test
    fun portfolioProjectsScreen_filtersThumbnailsFavoritesAndDetailPreview() {
        val sampleProjects = listOf(
            PortfolioProjectEntity(
                id = 101L,
                title = "Vogue Couture Editorial",
                mediaType = PortfolioProjectMediaType.PHOTO,
                category = "Fashion",
                description = "Editorial fashion lookbook series.",
                thumbnailUrl = "drawable://fashion_editorial",
                mediaUrl = "drawable://fashion_editorial",
                clientName = "Noor Couture",
                cameraGear = "Sony A7R V • 85mm GM",
                shotCount = 32,
                resolutionTag = "61MP RAW",
                isFeatured = false,
                isFavorite = false
            ),
            PortfolioProjectEntity(
                id = 102L,
                title = "4K Music Video Reel",
                mediaType = PortfolioProjectMediaType.VIDEO,
                category = "Music Video",
                description = "Anamorphic 4K cinema direction reel.",
                thumbnailUrl = "drawable://studio_hero",
                mediaUrl = "drawable://studio_hero",
                clientName = "Crown Beats",
                cameraGear = "RED V-Raptor • 40mm",
                durationSeconds = 195,
                resolutionTag = "4K 60fps",
                isFeatured = false,
                isFavorite = true
            )
        )

        var toggledProjectId: Long? = null
        var bookedService = ""

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = false) {
                PortfolioProjectsScreen(
                    language = AppLanguage.ENGLISH,
                    projects = sampleProjects,
                    onToggleFavorite = { toggledProjectId = it.id },
                    onBookSimilarShoot = { bookedService = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("portfolio_projects_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("project_card_101").assertIsDisplayed()
        composeTestRule.onNodeWithTag("project_photo_badge_101", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("project_card_102").assertIsDisplayed()
        composeTestRule.onNodeWithTag("project_video_badge_102", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("project_video_duration_102", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("03:15", useUnmergedTree = true).assertIsDisplayed()

        // Filter to Video Projects only -> card 101 hidden, card 102 displayed
        composeTestRule.onNodeWithTag("projects_filter_video").performScrollTo().performClick()
        assertEquals(0, composeTestRule.onAllNodesWithTag("project_card_101").fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag("project_card_102").assertIsDisplayed()

        // Switch back to All and search something empty -> empty state -> reset filters
        composeTestRule.onNodeWithTag("projects_search_input").performTextReplacement("NonExistentGear999")
        composeTestRule.onNodeWithTag("projects_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("projects_reset_filters_button").performClick()
        composeTestRule.onNodeWithTag("project_card_101").assertIsDisplayed()

        // Favorite toggle triggers callback
        composeTestRule.onNodeWithTag("project_favorite_btn_101").performClick()
        assertEquals(101L, toggledProjectId)

        // Open Project Detail Dialog for video project & tap Book Similar Shoot
        composeTestRule.onNodeWithTag("portfolio_projects_grid")
            .performScrollToNode(hasTestTag("project_card_102"))
        composeTestRule.onNodeWithTag("project_card_102").performClick()
        composeTestRule.onNodeWithTag("project_detail_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("project_detail_book_btn").performScrollTo().performClick()
        assertEquals("Video Production", bookedService)
    }

    @Test
    fun portfolioProjectsScreen_lightTheme_addProjectDialogSavesToCallback() {
        var addedTitle = ""
        var addedMediaType = ""
        var addedDuration = 0

        composeTestRule.setContent {
            StarArtistTheme(isStudioLight = true) {
                PortfolioProjectsScreen(
                    language = AppLanguage.ENGLISH,
                    projects = emptyList(),
                    onAddProject = { title, mediaType, _, _, _, _, _, durationSecs, _, _, _ ->
                        addedTitle = title
                        addedMediaType = mediaType
                        addedDuration = durationSecs
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("add_portfolio_project_button").performClick()
        composeTestRule.onNodeWithTag("add_project_dialog").assertIsDisplayed()

        composeTestRule.onNodeWithTag("add_project_media_video").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("add_project_title_input").performScrollTo().performTextReplacement("Luxury Jewellery Film")
        composeTestRule.onNodeWithTag("add_project_client_input").performScrollTo().performTextReplacement("Sirsa Jewellers")
        composeTestRule.onNodeWithTag("add_project_gear_input").performScrollTo().performTextReplacement("Sony FX3")
        composeTestRule.onNodeWithTag("add_project_duration_input").performScrollTo().performTextReplacement("120")

        composeTestRule.onNodeWithTag("add_project_save_btn").performScrollTo().performClick()

        assertEquals("Luxury Jewellery Film", addedTitle)
        assertEquals(PortfolioProjectMediaType.VIDEO, addedMediaType)
        assertEquals(120, addedDuration)
    }
}
