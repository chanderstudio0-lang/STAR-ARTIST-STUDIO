package com.example.ui.screens

import com.example.data.model.AppLanguage
import com.example.data.model.StudioServicesData
import com.example.ui.components.EncodedPortfolioPhoto
import com.example.ui.viewmodel.PhotoPreviewDestination

/**
 * Aspect-ratio composition guides rendered over the CameraX viewfinder for portfolio & catalog framing.
 */
enum class FramingAspectGuide(
    val id: String,
    val labelEn: String,
    val labelHi: String,
    val targetRatio: Float?
) {
    FULL("full", "Full", "फुल", null),
    SQUARE_1_1("1_1", "1:1 Catalog", "1:1 कैटलॉग", 1.0f),
    PORTRAIT_4_5("4_5", "4:5 Portrait", "4:5 पोर्ट्रेट", 0.8f),
    CINEMA_16_9("16_9", "16:9 Cinema", "16:9 सिनेमा", 16f / 9f)
}

/**
 * Represents a single quick reference shot captured or imported during a Photo Preview session.
 */
data class CapturedReferenceShot(
    val id: Long,
    val encodedPhoto: EncodedPortfolioPhoto,
    val aspectGuide: FramingAspectGuide = FramingAspectGuide.PORTRAIT_4_5,
    val lensLabel: String = "Rear HD",
    val flashModeLabel: String = "Auto",
    val zoomLabel: String = "1.0x"
) {
    val sizeKb: Int
        get() = (encodedPhoto.byteLength / 1024).coerceAtLeast(1)

    val resolutionBadge: String
        get() = "${encodedPhoto.width}×${encodedPhoto.height} • ${aspectGuide.labelEn}"
}

object PhotoPreviewDefaults {
    val portfolioCategories = listOf(
        "Fashion",
        "Editorial",
        "Portrait",
        "Bridal",
        "Commercial",
        "Modeling"
    )

    fun defaultShotTitle(
        destination: PhotoPreviewDestination,
        category: String,
        selectedPackageId: String,
        shotNumber: Int,
        language: AppLanguage
    ): String {
        val isHindi = language == AppLanguage.HINDI
        return when (destination) {
            PhotoPreviewDestination.SERVICE_CATALOG -> {
                val pkg = StudioServicesData.findPackageById(selectedPackageId)
                val pkgName = pkg?.let { if (isHindi) it.titleHi else it.titleEn } ?: "Catalog Package"
                if (isHindi) "$pkgName • संदर्भ शॉट #$shotNumber" else "$pkgName • Ref #$shotNumber"
            }
            PhotoPreviewDestination.PORTFOLIO_ARCHIVE -> {
                if (isHindi) "$category पोर्टफोलियो संदर्भ #$shotNumber" else "$category Portfolio Ref #$shotNumber"
            }
            PhotoPreviewDestination.STUDIO_GALLERY -> {
                if (isHindi) "स्टूडियो $category प्रीव्यू #$shotNumber" else "Studio $category Preview #$shotNumber"
            }
            PhotoPreviewDestination.MOOD_BOARD -> {
                if (isHindi) "मूड बोर्ड संदर्भ शॉट #$shotNumber" else "$category Mood Board Ref #$shotNumber"
            }
        }
    }
}
