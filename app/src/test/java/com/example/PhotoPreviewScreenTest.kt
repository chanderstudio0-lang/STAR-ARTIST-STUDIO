package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ServiceCatalogReferenceEntity
import com.example.data.model.AppLanguage
import com.example.data.repository.ServiceCatalogReferenceRepository
import com.example.ui.components.EncodedPortfolioPhoto
import com.example.ui.screens.CapturedReferenceShot
import com.example.ui.screens.FramingAspectGuide
import com.example.ui.screens.PhotoPreviewScreen
import com.example.ui.screens.PortfolioProjectsScreen
import com.example.ui.screens.ServiceCatalogScreen
import com.example.ui.theme.StarArtistTheme
import com.example.ui.viewmodel.PhotoPreviewDestination
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class PhotoPreviewScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var refRepo: ServiceCatalogReferenceRepository

    private val sampleDataUrl =
        "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAALCAABAAEBAREA/8QAFAABAAAAAAAAAAAAAAAAAAAACf/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAD8AVN//2Q=="

    private fun sampleShot(id: Long = 101L): CapturedReferenceShot = CapturedReferenceShot(
        id = id,
        encodedPhoto = EncodedPortfolioPhoto(
            dataUrl = sampleDataUrl,
            width = 1080,
            height = 1350,
            byteLength = 42_500
        ),
        aspectGuide = FramingAspectGuide.PORTRAIT_4_5,
        lensLabel = "Rear Cam",
        flashModeLabel = "Flash Auto",
        zoomLabel = "1.0x"
    )

    @Before
    fun setupDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        refRepo = ServiceCatalogReferenceRepository(db.serviceCatalogReferenceDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun roomRepository_savesGroupsAndDeletesCatalogReferenceShots() = runBlocking {
        val rowId = refRepo.saveReference(
            packageId = "pkg_photo_fashion",
            title = "Rim Light Makeup Check",
            notes = "Key strobe 45deg",
            imageDataUrl = sampleDataUrl,
            aspectGuide = "4:5 Portrait",
            resolutionTag = "1080×1350"
        )
        assertTrue(rowId > 0L)

        val grouped = refRepo.referencesByPackage.first()
        val fashionRefs = grouped["pkg_photo_fashion"].orEmpty()
        assertEquals(1, fashionRefs.size)
        assertEquals("Rim Light Makeup Check", fashionRefs.first().title)

        refRepo.deleteReference(fashionRefs.first())
        val afterDelete = refRepo.referencesByPackage.first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun photoPreview_withoutCameraPermission_showsRationaleAndFallbackOptions() {
        composeTestRule.setContent {
            StarArtistTheme {
                PhotoPreviewScreen(
                    language = AppLanguage.ENGLISH,
                    initialPermissionGrantedOverride = false
                )
            }
        }

        composeTestRule.onNodeWithTag("photo_preview_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("photo_preview_permission_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("photo_preview_grant_permission_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("photo_preview_system_camera_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("photo_preview_gallery_fallback_btn").assertIsDisplayed()
    }

    @Test
    fun photoPreview_capturedShot_savesToPortfolioArchive() {
        var savedTitle = ""
        var savedCategory = ""
        var savedUrl = ""
        var savedNotes = ""
        var openedDestination: PhotoPreviewDestination? = null

        composeTestRule.setContent {
            StarArtistTheme {
                PhotoPreviewScreen(
                    language = AppLanguage.ENGLISH,
                    initialDestination = PhotoPreviewDestination.PORTFOLIO_ARCHIVE,
                    initialPermissionGrantedOverride = false,
                    initialPreviewShots = listOf(sampleShot()),
                    onSaveToPortfolioArchive = { title, cat, url, _, _, desc, onSaved ->
                        savedTitle = title
                        savedCategory = cat
                        savedUrl = url
                        savedNotes = desc
                        onSaved()
                    },
                    onOpenDestinationScreen = { dest -> openedDestination = dest }
                )
            }
        }

        composeTestRule.onNodeWithTag("photo_preview_captured_image")
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("photo_preview_category_chip_bridal")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("photo_preview_title_input")
            .performScrollTo()
            .performTextReplacement("Royal Heritage Lighting Test")

        composeTestRule.onNodeWithTag("photo_preview_notes_input")
            .performScrollTo()
            .performTextReplacement("85mm f/1.4 • Soft octabox")

        composeTestRule.onNodeWithTag("photo_preview_save_btn")
            .performScrollTo()
            .performClick()

        assertEquals("Royal Heritage Lighting Test", savedTitle)
        assertEquals("Bridal", savedCategory)
        assertEquals(sampleDataUrl, savedUrl)
        assertEquals("85mm f/1.4 • Soft octabox", savedNotes)

        composeTestRule.onNodeWithTag("photo_preview_saved_banner")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("photo_preview_open_saved_target_btn")
            .performScrollTo()
            .performClick()

        assertEquals(PhotoPreviewDestination.PORTFOLIO_ARCHIVE, openedDestination)
    }

    @Test
    fun photoPreview_switchDestinationToServiceCatalog_attachesToSelectedPackage() {
        var attachedPackageId = ""
        var attachedTitle = ""
        var attachedAspect = ""

        composeTestRule.setContent {
            StarArtistTheme {
                PhotoPreviewScreen(
                    language = AppLanguage.ENGLISH,
                    initialDestination = PhotoPreviewDestination.PORTFOLIO_ARCHIVE,
                    initialPermissionGrantedOverride = false,
                    initialPreviewShots = listOf(sampleShot(202L)),
                    onSaveToServiceCatalog = { pkgId, title, _, _, aspectGuide, _, onSaved ->
                        attachedPackageId = pkgId
                        attachedTitle = title
                        attachedAspect = aspectGuide
                        onSaved()
                    }
                )
            }
        }

        // Change framing guide to 1:1 Catalog
        composeTestRule.onNodeWithTag("photo_preview_aspect_1_1").performClick()

        // Switch save destination to Service Catalog
        composeTestRule.onNodeWithTag("photo_preview_dest_catalog")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("photo_preview_package_chip_pkg_photo_bridal")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("photo_preview_title_input")
            .performScrollTo()
            .performTextReplacement("Bridal Pose Framing Ref")

        composeTestRule.onNodeWithTag("photo_preview_save_btn")
            .performScrollTo()
            .performClick()

        assertEquals("pkg_photo_bridal", attachedPackageId)
        assertEquals("Bridal Pose Framing Ref", attachedTitle)
        assertEquals("1:1 Catalog", attachedAspect)
        composeTestRule.onNodeWithTag("photo_preview_saved_banner")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun serviceCatalogScreen_displaysReferenceCamTriggerAndAttachedThumbnails() {
        var requestedPackageId: String? = "UNSET"
        val mockRefs = mapOf(
            "pkg_photo_fashion" to listOf(
                ServiceCatalogReferenceEntity(
                    id = 77L,
                    packageId = "pkg_photo_fashion",
                    title = "Backlight Vogue Framing",
                    imageDataUrl = sampleDataUrl
                )
            )
        )

        composeTestRule.setContent {
            StarArtistTheme {
                ServiceCatalogScreen(
                    language = AppLanguage.ENGLISH,
                    catalogReferenceShots = mockRefs,
                    onOpenReferenceCamera = { pkgId -> requestedPackageId = pkgId },
                    onBack = {},
                    onBookService = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("catalog_open_photo_preview_btn").performClick()
        assertEquals(null, requestedPackageId)

        composeTestRule.onNodeWithTag("service_catalog_list")
            .performScrollToNode(hasTestTag("catalog_card_pkg_photo_fashion"))
        composeTestRule.onNodeWithTag("catalog_card_ref_strip_pkg_photo_fashion")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("catalog_card_ref_cam_pkg_photo_fashion")
            .performClick()
        assertEquals("pkg_photo_fashion", requestedPackageId)
    }

    @Test
    fun portfolioProjectsScreen_exposesCameraTriggerButton() {
        var openedCam = false

        composeTestRule.setContent {
            StarArtistTheme {
                PortfolioProjectsScreen(
                    language = AppLanguage.ENGLISH,
                    projects = emptyList(),
                    onOpenPhotoPreview = { openedCam = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("portfolio_projects_camera_btn")
            .assertIsDisplayed()
            .performClick()
        assertTrue(openedCam)
    }
}
