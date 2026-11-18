package com.example

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.SuccessResult
import com.example.data.model.AppLanguage
import com.example.data.model.GalleryItem
import com.example.ui.components.createPortfolioCaptureDestination
import com.example.ui.components.encodePortfolioPhotoUriToDataUrl
import com.example.ui.components.isValidPortfolioMediaUrl
import com.example.ui.screens.PortfolioScreen
import com.example.ui.screens.UploadMediaDialog
import com.example.ui.theme.StarArtistTheme
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class PortfolioCameraCaptureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUpCoil() {
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
    }

    @After
    fun tearDownCoil() {
        Coil.reset()
    }

    @Test
    fun encodePortfolioPhotoUriToDataUrl_scalesToHdAndFitsFirestoreLimit() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val sourceFile = File(context.cacheDir, "test_hd_portfolio_src.jpg")
        val sourceBitmap = Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888)
        Canvas(sourceBitmap).apply {
            drawColor(Color.rgb(212, 175, 55))
        }
        FileOutputStream(sourceFile).use { out ->
            sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        sourceBitmap.recycle()

        val encoded = encodePortfolioPhotoUriToDataUrl(context, Uri.fromFile(sourceFile))
        sourceFile.delete()

        assertNotNull(encoded)
        assertEquals(1440, encoded!!.width)
        assertEquals(1080, encoded.height)
        assertTrue(encoded.dataUrl.startsWith("data:image/jpeg;base64,"))
        assertTrue(encoded.byteLength <= 780_000)
        assertTrue(isValidPortfolioMediaUrl(encoded.dataUrl))
    }

    @Test
    fun createPortfolioCaptureDestination_createsScopedPortfolioFileProviderUri() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val (file, uri) = createPortfolioCaptureDestination(context)
        assertTrue(file.absolutePath.contains("portfolio"))
        assertTrue(uri.toString().contains("fileprovider"))
        file.delete()
    }

    @Test
    fun isValidPortfolioMediaUrl_validatesSupportedSchemesAndSizeBounds() {
        assertTrue(isValidPortfolioMediaUrl("https://images.unsplash.com/photo-1.jpg"))
        assertTrue(isValidPortfolioMediaUrl("http://example.com/photo.jpg"))
        assertTrue(isValidPortfolioMediaUrl("data:image/jpeg;base64,abc123"))
        assertFalse(isValidPortfolioMediaUrl(""))
        assertFalse(isValidPortfolioMediaUrl("ftp://example.com/photo.jpg"))
        assertFalse(isValidPortfolioMediaUrl("data:image/jpeg;base64," + "A".repeat(860_000)))
    }

    @Test
    fun portfolioScreen_cameraButtonOpensPermissionPromptWhenCameraNotGranted() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.CAMERA)

        composeTestRule.setContent {
            StarArtistTheme {
                PortfolioScreen(
                    language = AppLanguage.ENGLISH,
                    portfolioItems = emptyList()
                )
            }
        }

        composeTestRule.onNodeWithTag("portfolio_camera_trigger_button").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("studio_camera_capture_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camera_permission_request_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("grant_camera_permission_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable Camera Hardware Permission").assertIsDisplayed()
    }

    @Test
    fun uploadMediaDialog_withCameraPermissionGranted_showsCaptureControlsAndSubmitsUrl() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.CAMERA)

        var uploadedTitle = ""
        var uploadedUrl = ""

        composeTestRule.setContent {
            StarArtistTheme {
                UploadMediaDialog(
                    language = AppLanguage.ENGLISH,
                    isSubmitting = false,
                    initialStartCamera = false,
                    onDismiss = {},
                    onUpload = { title, _, _, url, _ ->
                        uploadedTitle = title
                        uploadedUrl = url
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("upload_camera_capture_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("upload_pick_photo_btn").assertIsDisplayed()

        // Launch in-app camera dialog cleanly
        composeTestRule.onNodeWithTag("upload_camera_capture_btn").performClick()
        composeTestRule.onNodeWithTag("studio_camera_capture_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camerax_shutter_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camerax_system_camera_fallback_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camerax_close_button").performClick()

        // Verify direct URL submission inside the same dialog
        composeTestRule.onNodeWithTag("upload_title_input").performTextInput("Gold Studio Showcase")
        composeTestRule.onNodeWithTag("upload_url_input").performTextInput("https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=800")
        composeTestRule.onNodeWithTag("upload_submit_btn").performClick()

        assertEquals("Gold Studio Showcase", uploadedTitle)
        assertEquals("https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=800", uploadedUrl)
    }

    @Test
    fun cameraXCaptureDialog_showsViewfinderFlashZoomAndLensControlsWhenCameraHardwareAvailable() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        Shadows.shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_CAMERA_ANY, true)

        composeTestRule.setContent {
            StarArtistTheme {
                UploadMediaDialog(
                    language = AppLanguage.ENGLISH,
                    isSubmitting = false,
                    initialStartCamera = true,
                    onDismiss = {},
                    onUpload = { _, _, _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("studio_camera_capture_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camerax_viewfinder_preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camerax_flash_toggle_button").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("camerax_zoom_toggle_button").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("camerax_switch_lens_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camerax_shutter_button").assertIsDisplayed()
    }

    @Test
    fun portfolioScreen_displaysUserCapturedPendingAndApprovedStatusBadges() {
        val pendingCapture = GalleryItem(
            id = "cam_pending_1",
            userId = "artist_1",
            title = "Studio Camera Backstage Shot",
            category = "Portrait",
            mediaType = "photo",
            mediaUrl = "data:image/jpeg;base64,abc123",
            isDemo = false,
            status = "pending"
        )
        val savedCapture = GalleryItem(
            id = "cam_saved_1",
            userId = "artist_1",
            title = "Approved Bridal Run-through",
            category = "Bridal",
            mediaType = "photo",
            mediaUrl = "data:image/jpeg;base64,def456",
            isDemo = false,
            status = "approved"
        )

        composeTestRule.setContent {
            StarArtistTheme {
                PortfolioScreen(
                    language = AppLanguage.ENGLISH,
                    portfolioItems = listOf(pendingCapture, savedCapture)
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("portfolio_card_cam_pending_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("portfolio_pending_badge_cam_pending_1", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("portfolio_card_cam_saved_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("portfolio_saved_badge_cam_saved_1", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("portfolio_works_count").assertTextContains("10 Samples")
    }
}
