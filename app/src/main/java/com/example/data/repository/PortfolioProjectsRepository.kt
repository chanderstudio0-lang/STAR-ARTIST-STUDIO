package com.example.data.repository

import com.example.data.local.dao.PortfolioProjectDao
import com.example.data.local.entity.PortfolioProjectEntity
import com.example.data.local.entity.PortfolioProjectMediaType
import kotlinx.coroutines.flow.Flow

/**
 * Repository mediating Room persistence for photography and video portfolio projects.
 */
class PortfolioProjectsRepository(
    private val portfolioProjectDao: PortfolioProjectDao
) {

    val allProjects: Flow<List<PortfolioProjectEntity>> = portfolioProjectDao.observeAllProjects()

    fun observeByMediaType(mediaType: String): Flow<List<PortfolioProjectEntity>> =
        portfolioProjectDao.observeProjectsByMediaType(mediaType)

    fun observeFavorites(): Flow<List<PortfolioProjectEntity>> =
        portfolioProjectDao.observeFavoriteProjects()

    suspend fun getProjectById(id: Long): PortfolioProjectEntity? =
        portfolioProjectDao.getProjectById(id)

    suspend fun saveProject(project: PortfolioProjectEntity): Long {
        val now = System.currentTimeMillis()
        val clean = if (project.id == 0L) {
            project.copy(
                title = project.title.trim(),
                category = project.category.trim().ifEmpty { "Editorial" },
                thumbnailUrl = project.thumbnailUrl.trim(),
                mediaUrl = project.mediaUrl.trim().ifEmpty { project.thumbnailUrl.trim() },
                createdAt = now,
                updatedAt = now
            )
        } else {
            project.copy(
                title = project.title.trim(),
                category = project.category.trim().ifEmpty { "Editorial" },
                thumbnailUrl = project.thumbnailUrl.trim(),
                mediaUrl = project.mediaUrl.trim().ifEmpty { project.thumbnailUrl.trim() },
                updatedAt = now
            )
        }
        return portfolioProjectDao.insertProject(clean)
    }

    suspend fun toggleFavorite(project: PortfolioProjectEntity) {
        portfolioProjectDao.updateFavorite(
            id = project.id,
            isFavorite = !project.isFavorite,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteProject(project: PortfolioProjectEntity) {
        portfolioProjectDao.deleteProject(project)
    }

    suspend fun deleteProjectById(id: Long) {
        portfolioProjectDao.deleteProjectById(id)
    }

    suspend fun seedShowcaseProjectsIfEmpty() {
        if (portfolioProjectDao.getProjectCount() == 0) {
            portfolioProjectDao.insertProjects(defaultShowcaseProjects())
        }
    }

    companion object {

        fun formatDuration(durationSeconds: Int): String {
            if (durationSeconds <= 0) return "00:00"
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            return "%02d:%02d".format(mins, secs)
        }

        fun defaultShowcaseProjects(nowMillis: Long = System.currentTimeMillis()): List<PortfolioProjectEntity> {
            return listOf(
                PortfolioProjectEntity(
                    projectCode = "proj_vogue_runway_photo",
                    title = "Vogue Runway Couture Editorial",
                    mediaType = PortfolioProjectMediaType.PHOTO,
                    category = "Fashion",
                    description = "High-contrast studio fashion editorial illuminated with dual Profoto octabox key lighting and sculpted rim highlights.",
                    thumbnailUrl = "drawable://fashion_editorial",
                    mediaUrl = "drawable://fashion_editorial",
                    clientName = "Noor Couture House",
                    location = "Star Artist Lighting Bay, Talwara Khurd",
                    cameraGear = "Sony A7R V • 85mm f/1.4 GM",
                    durationSeconds = 0,
                    shotCount = 36,
                    resolutionTag = "61MP RAW",
                    shootYear = 2026,
                    isFeatured = true,
                    isFavorite = true,
                    createdAt = nowMillis - 10_000L,
                    updatedAt = nowMillis - 10_000L
                ),
                PortfolioProjectEntity(
                    projectCode = "proj_cinema_music_video",
                    title = "4K Anamorphic Music Video Direction",
                    mediaType = PortfolioProjectMediaType.VIDEO,
                    category = "Music Video",
                    description = "Full-frame anamorphic Punjabi single production featuring motorized gimbal tracking, haze atmosphere, and golden backlight.",
                    thumbnailUrl = "drawable://studio_hero",
                    mediaUrl = "drawable://studio_hero",
                    clientName = "Crown Beats Records",
                    location = "Stage A • Sirsa Film Set",
                    cameraGear = "RED V-Raptor 8K • 40mm Anamorphic",
                    durationSeconds = 204,
                    shotCount = 1,
                    resolutionTag = "4K 60fps",
                    shootYear = 2026,
                    isFeatured = true,
                    isFavorite = false,
                    createdAt = nowMillis - 20_000L,
                    updatedAt = nowMillis - 20_000L
                ),
                PortfolioProjectEntity(
                    projectCode = "proj_royal_bridal_photo",
                    title = "Royal Heritage Bridal Lookbook",
                    mediaType = PortfolioProjectMediaType.PHOTO,
                    category = "Bridal",
                    description = "Regal wedding trousseau and heirloom gold jewelry portrait series styled with warm tungsten fill.",
                    thumbnailUrl = "drawable://bridal_heritage",
                    mediaUrl = "drawable://bridal_heritage",
                    clientName = "Maharani Jewellers Sirsa",
                    location = "Heritage Courtyard, Ellenabad",
                    cameraGear = "Canon EOS R5 • 50mm f/1.2L",
                    durationSeconds = 0,
                    shotCount = 48,
                    resolutionTag = "45MP RAW",
                    shootYear = 2026,
                    isFeatured = false,
                    isFavorite = true,
                    createdAt = nowMillis - 30_000L,
                    updatedAt = nowMillis - 30_000L
                ),
                PortfolioProjectEntity(
                    projectCode = "proj_runway_slowmo_video",
                    title = "Slow-Mo Couture Runway Film",
                    mediaType = PortfolioProjectMediaType.VIDEO,
                    category = "Fashion",
                    description = "120fps slow-motion lookbook film highlighting fabric movement under programmable RGBW LED tubes.",
                    thumbnailUrl = "drawable://fashion_editorial",
                    mediaUrl = "drawable://fashion_editorial",
                    clientName = "Zari Atelier",
                    location = "Star Artist Studio Stage B",
                    cameraGear = "Sony FX3 • Ronin RS4 Pro",
                    durationSeconds = 95,
                    shotCount = 1,
                    resolutionTag = "4K 120fps",
                    shootYear = 2026,
                    isFeatured = false,
                    isFavorite = false,
                    createdAt = nowMillis - 40_000L,
                    updatedAt = nowMillis - 40_000L
                ),
                PortfolioProjectEntity(
                    projectCode = "proj_dramatic_portrait_photo",
                    title = "Sculpted Rim-Light Character Portraits",
                    mediaType = PortfolioProjectMediaType.PHOTO,
                    category = "Portrait",
                    description = "Minimalist obsidian backdrop portraits engineered for casting directors and editorial cover features.",
                    thumbnailUrl = "drawable://dramatic_portrait",
                    mediaUrl = "drawable://dramatic_portrait",
                    clientName = "North India Talent Guild",
                    location = "Portrait Cyclorama Bay",
                    cameraGear = "Sony A7 IV • 135mm f/1.8 GM",
                    durationSeconds = 0,
                    shotCount = 22,
                    resolutionTag = "33MP RAW",
                    shootYear = 2026,
                    isFeatured = false,
                    isFavorite = false,
                    createdAt = nowMillis - 50_000L,
                    updatedAt = nowMillis - 50_000L
                ),
                PortfolioProjectEntity(
                    projectCode = "proj_luxury_tvc_video",
                    title = "Aurum Luxury Brand Commercial Spot",
                    mediaType = PortfolioProjectMediaType.VIDEO,
                    category = "Commercial",
                    description = "Macro tabletop cinematography with synchronized motion-control sliders and gold specular reflections.",
                    thumbnailUrl = "drawable://commercial_luxury",
                    mediaUrl = "drawable://commercial_luxury",
                    clientName = "Aurum Parfums India",
                    location = "Tabletop Macro Bay, Talwara Khurd",
                    cameraGear = "FX6 Cinema Line • Laowa 24mm Probe",
                    durationSeconds = 45,
                    shotCount = 1,
                    resolutionTag = "4K HDR",
                    shootYear = 2026,
                    isFeatured = false,
                    isFavorite = true,
                    createdAt = nowMillis - 60_000L,
                    updatedAt = nowMillis - 60_000L
                ),
                PortfolioProjectEntity(
                    projectCode = "proj_commercial_product_photo",
                    title = "Gold Edition Luxury Product Stills",
                    mediaType = PortfolioProjectMediaType.PHOTO,
                    category = "Commercial",
                    description = "Focus-stacked e-commerce and billboard product photography with crisp gradient glass highlights.",
                    thumbnailUrl = "drawable://commercial_luxury",
                    mediaUrl = "drawable://commercial_luxury",
                    clientName = "Aurum Parfums India",
                    location = "Product Table Bay",
                    cameraGear = "Sony A7R V • 90mm f/2.8 Macro",
                    durationSeconds = 0,
                    shotCount = 18,
                    resolutionTag = "61MP Studio",
                    shootYear = 2025,
                    isFeatured = false,
                    isFavorite = false,
                    createdAt = nowMillis - 70_000L,
                    updatedAt = nowMillis - 70_000L
                ),
                PortfolioProjectEntity(
                    projectCode = "proj_model_compcard_photo",
                    title = "18+ Talent Comp-Card & Audition Series",
                    mediaType = PortfolioProjectMediaType.PHOTO,
                    category = "Editorial",
                    description = "Clean daylight-balanced agency portfolio shoot capturing natural expression, silhouette, and movement.",
                    thumbnailUrl = "drawable://studio_model",
                    mediaUrl = "drawable://studio_model",
                    clientName = "Star Artist Talent Roster",
                    location = "Cyclorama Daylight Studio",
                    cameraGear = "Canon EOS R5 • 85mm f/1.2L",
                    durationSeconds = 0,
                    shotCount = 30,
                    resolutionTag = "45MP Pro",
                    shootYear = 2026,
                    isFeatured = false,
                    isFavorite = false,
                    createdAt = nowMillis - 80_000L,
                    updatedAt = nowMillis - 80_000L
                ),
                PortfolioProjectEntity(
                    projectCode = "proj_bridal_teaser_video",
                    title = "Royal Pre-Wedding Cinema Highlight Reel",
                    mediaType = PortfolioProjectMediaType.VIDEO,
                    category = "Bridal",
                    description = "Golden-hour architectural storytelling blended with ambient live audio and 10-bit film color grading.",
                    thumbnailUrl = "drawable://bridal_heritage",
                    mediaUrl = "drawable://bridal_heritage",
                    clientName = "Sidhu & Sandhu Wedding",
                    location = "Sirsa Fort & Heritage Palace",
                    cameraGear = "FX3 Dual Rig • 24-70mm GM II",
                    durationSeconds = 148,
                    shotCount = 1,
                    resolutionTag = "4K 10-bit",
                    shootYear = 2025,
                    isFeatured = false,
                    isFavorite = false,
                    createdAt = nowMillis - 90_000L,
                    updatedAt = nowMillis - 90_000L
                )
            )
        }
    }
}
