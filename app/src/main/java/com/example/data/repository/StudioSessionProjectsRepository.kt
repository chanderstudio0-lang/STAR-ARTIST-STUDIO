package com.example.data.repository

import com.example.data.local.dao.StudioSessionProjectDao
import com.example.data.local.entity.StudioProjectStatus
import com.example.data.local.entity.StudioSessionProjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository mediating Room persistence for tracked studio session projects and status transitions.
 */
class StudioSessionProjectsRepository(
    private val dao: StudioSessionProjectDao,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    val allProjects: Flow<List<StudioSessionProjectEntity>> = dao.observeAllProjects()

    val activeProjects: Flow<List<StudioSessionProjectEntity>> = dao.observeActiveProjects()

    val archivedProjects: Flow<List<StudioSessionProjectEntity>> = dao.observeArchivedProjects()

    fun observeByStatus(status: StudioProjectStatus): Flow<List<StudioSessionProjectEntity>> =
        dao.observeProjectsByStatus(status)

    fun observeCountByStatus(status: StudioProjectStatus): Flow<Int> =
        dao.observeCountByStatus(status)

    suspend fun getProjectById(id: Long): StudioSessionProjectEntity? =
        dao.getProjectById(id)

    suspend fun createProject(project: StudioSessionProjectEntity): Long {
        val now = System.currentTimeMillis()
        val resolvedProgress = project.progressPercent.coerceIn(0, 100)
        val started = if (project.status != StudioProjectStatus.PLANNING) {
            project.startedAt ?: now
        } else {
            project.startedAt
        }
        val completed = if (project.status == StudioProjectStatus.COMPLETED) {
            project.completedAt ?: now
        } else {
            null
        }
        val code = project.sessionCode.trim().ifEmpty {
            "SAS-${(now % 10000).toString().padStart(4, '0')}"
        }
        return dao.insertProject(
            project.copy(
                sessionCode = code,
                projectTitle = project.projectTitle.trim(),
                clientName = project.clientName.trim(),
                serviceCategory = project.serviceCategory.trim().ifEmpty { "Photography" },
                scheduledDate = project.scheduledDate.trim(),
                deadlineDate = project.deadlineDate.trim(),
                studioStage = project.studioStage.trim().ifEmpty { "Stage A • Lighting Bay" },
                leadDirector = project.leadDirector.trim().ifEmpty { "Star Artist Team" },
                progressPercent = if (project.status == StudioProjectStatus.COMPLETED) 100 else resolvedProgress,
                startedAt = started,
                completedAt = completed,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateProject(project: StudioSessionProjectEntity) {
        dao.updateProject(
            project.copy(
                progressPercent = if (project.status == StudioProjectStatus.COMPLETED) {
                    100
                } else {
                    project.progressPercent.coerceIn(0, 100)
                },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateProjectStatus(
        project: StudioSessionProjectEntity,
        newStatus: StudioProjectStatus
    ) {
        val now = clockMillis()
        val targetProgress = when (newStatus) {
            StudioProjectStatus.PLANNING ->
                project.progressPercent.coerceAtMost(35).coerceAtLeast(15)
            StudioProjectStatus.IN_PRODUCTION ->
                maxOf(project.progressPercent, newStatus.defaultProgress).coerceAtMost(80)
            StudioProjectStatus.POST_PROCESSING ->
                maxOf(project.progressPercent, newStatus.defaultProgress).coerceAtMost(95)
            StudioProjectStatus.COMPLETED -> 100
        }
        val startedTimestamp = if (newStatus != StudioProjectStatus.PLANNING) {
            project.startedAt ?: now
        } else {
            null
        }
        val completedTimestamp = if (newStatus == StudioProjectStatus.COMPLETED) {
            now
        } else {
            null
        }
        // Settle running stopwatch duration whenever leaving IN_PRODUCTION
        val settledTrackedMillis = project.computeTotalTrackedMillis(now)
        val settledActiveStartedAt = if (newStatus == StudioProjectStatus.IN_PRODUCTION) {
            project.activeTimerStartedAt
        } else {
            null
        }
        dao.updateProjectStatus(
            id = project.id,
            status = newStatus,
            progressPercent = targetProgress,
            startedAt = startedTimestamp,
            completedAt = completedTimestamp,
            trackedProductionMillis = settledTrackedMillis,
            activeTimerStartedAt = settledActiveStartedAt,
            updatedAt = now
        )
    }

    suspend fun startProductionTimer(project: StudioSessionProjectEntity): StudioSessionProjectEntity {
        if (project.status != StudioProjectStatus.IN_PRODUCTION || project.activeTimerStartedAt != null) {
            return project
        }
        val now = clockMillis()
        dao.updateProductionStopwatch(
            id = project.id,
            trackedProductionMillis = project.trackedProductionMillis,
            activeTimerStartedAt = now,
            updatedAt = now
        )
        return project.copy(activeTimerStartedAt = now, updatedAt = now)
    }

    suspend fun pauseProductionTimer(project: StudioSessionProjectEntity): StudioSessionProjectEntity {
        val activeStart = project.activeTimerStartedAt ?: return project
        val now = clockMillis()
        val accumulated = (project.trackedProductionMillis + (now - activeStart).coerceAtLeast(0L))
            .coerceAtLeast(0L)
        dao.updateProductionStopwatch(
            id = project.id,
            trackedProductionMillis = accumulated,
            activeTimerStartedAt = null,
            updatedAt = now
        )
        return project.copy(
            trackedProductionMillis = accumulated,
            activeTimerStartedAt = null,
            updatedAt = now
        )
    }

    suspend fun resetProductionTimer(project: StudioSessionProjectEntity): StudioSessionProjectEntity {
        val now = clockMillis()
        dao.updateProductionStopwatch(
            id = project.id,
            trackedProductionMillis = 0L,
            activeTimerStartedAt = null,
            updatedAt = now
        )
        return project.copy(
            trackedProductionMillis = 0L,
            activeTimerStartedAt = null,
            updatedAt = now
        )
    }

    suspend fun archiveProject(project: StudioSessionProjectEntity): Boolean {
        if (project.status != StudioProjectStatus.COMPLETED) return false
        val now = clockMillis()
        dao.setProjectArchived(
            id = project.id,
            isArchived = true,
            archivedAt = project.archivedAt ?: now,
            updatedAt = now
        )
        return true
    }

    suspend fun restoreProject(project: StudioSessionProjectEntity) {
        val now = clockMillis()
        dao.setProjectArchived(
            id = project.id,
            isArchived = false,
            archivedAt = null,
            updatedAt = now
        )
    }

    suspend fun archiveAllCompleted(): Int {
        val now = clockMillis()
        return dao.archiveAllCompletedProjects(
            archivedAt = now,
            updatedAt = now
        )
    }

    suspend fun saveInProductionTaskNotes(
        project: StudioSessionProjectEntity,
        notes: String
    ): StudioSessionProjectEntity {
        val now = clockMillis()
        val trimmed = notes.trim()
        dao.updateInProductionTaskNotes(
            id = project.id,
            notes = trimmed,
            updatedAt = now
        )
        return project.copy(
            inProductionTaskNotes = trimmed,
            updatedAt = now
        )
    }

    suspend fun deleteProject(project: StudioSessionProjectEntity) {
        dao.deleteProject(project)
    }

    suspend fun seedDefaultProjectsIfEmpty() {
        if (dao.getTotalCount() == 0) {
            dao.insertProjects(defaultSessionProjects())
        }
    }

    companion object {
        fun defaultSessionProjects(nowMillis: Long = System.currentTimeMillis()): List<StudioSessionProjectEntity> {
            return listOf(
                StudioSessionProjectEntity(
                    sessionCode = "SAS-2601",
                    projectTitle = "Royal Couture Winter Lookbook",
                    clientName = "Noor Couture House",
                    serviceCategory = "Photography",
                    status = StudioProjectStatus.IN_PRODUCTION,
                    scheduledDate = "2026-11-16",
                    deadlineDate = "2026-11-20",
                    studioStage = "Stage A • Cyclorama & Profoto Rig",
                    leadDirector = "Chander Shekhar • Lead DOP",
                    progressPercent = 65,
                    deliverableCount = 40,
                    budgetInr = 24999L,
                    productionNotes = "4 wardrobe looks active; softbox key + rim strobe calibrated.",
                    startedAt = nowMillis - 7_200_000L,
                    trackedProductionMillis = 2_720_000L,
                    createdAt = nowMillis - 86_400_000L,
                    updatedAt = nowMillis - 1_800_000L
                ),
                StudioSessionProjectEntity(
                    sessionCode = "SAS-2602",
                    projectTitle = "Punjabi Single 4K Anamorphic Shoot",
                    clientName = "Crown Beats Records",
                    serviceCategory = "Video Production",
                    status = StudioProjectStatus.PLANNING,
                    scheduledDate = "2026-11-22",
                    deadlineDate = "2026-11-27",
                    studioStage = "Stage B • Hazer & Neon RGBW Stage",
                    leadDirector = "Star Artist Cinema Unit",
                    progressPercent = 25,
                    deliverableCount = 3,
                    budgetInr = 45000L,
                    productionNotes = "Storyboard locked; RED V-Raptor lens test & dolly track rehearsal scheduled.",
                    createdAt = nowMillis - 172_800_000L,
                    updatedAt = nowMillis - 3_600_000L
                ),
                StudioSessionProjectEntity(
                    sessionCode = "SAS-2603",
                    projectTitle = "18+ Talent Comp-Card & Audition Reel",
                    clientName = "Aarav Verma • Talent Roster",
                    serviceCategory = "Modeling",
                    status = StudioProjectStatus.POST_PROCESSING,
                    scheduledDate = "2026-11-13",
                    deadlineDate = "2026-11-18",
                    studioStage = "Color Grading & Retouch Suite",
                    leadDirector = "Senior Portrait Retoucher",
                    progressPercent = 88,
                    deliverableCount = 24,
                    budgetInr = 12999L,
                    productionNotes = "Primary selects approved; skin tone retention & comp-card PDF layout in progress.",
                    startedAt = nowMillis - 259_200_000L,
                    trackedProductionMillis = 5_400_000L,
                    createdAt = nowMillis - 345_600_000L,
                    updatedAt = nowMillis - 5_400_000L
                ),
                StudioSessionProjectEntity(
                    sessionCode = "SAS-2604",
                    projectTitle = "Maharani Jewellers Bridal Campaign",
                    clientName = "Maharani Jewellers Sirsa",
                    serviceCategory = "Bridal",
                    status = StudioProjectStatus.COMPLETED,
                    scheduledDate = "2026-11-08",
                    deadlineDate = "2026-11-12",
                    studioStage = "Heritage Courtyard Set • Completed",
                    leadDirector = "Chander Shekhar • Lead DOP",
                    progressPercent = 100,
                    deliverableCount = 55,
                    budgetInr = 32000L,
                    productionNotes = "Master 45MP print stills & teaser reel delivered and signed off by client.",
                    startedAt = nowMillis - 604_800_000L,
                    completedAt = nowMillis - 172_800_000L,
                    trackedProductionMillis = 14_400_000L,
                    createdAt = nowMillis - 691_200_000L,
                    updatedAt = nowMillis - 9_000_000L
                ),
                StudioSessionProjectEntity(
                    sessionCode = "SAS-2605",
                    projectTitle = "Aurum Luxury Perfume Macro Ad",
                    clientName = "Aurum Parfums India",
                    serviceCategory = "Commercial",
                    status = StudioProjectStatus.PLANNING,
                    scheduledDate = "2026-11-26",
                    deadlineDate = "2026-11-30",
                    studioStage = "Macro Tabletop Bay",
                    leadDirector = "Commercial Lighting Unit",
                    progressPercent = 18,
                    deliverableCount = 15,
                    budgetInr = 28500L,
                    productionNotes = "Acrylic reflection risers & probe lens macro test pending client sign-off.",
                    createdAt = nowMillis - 43_200_000L,
                    updatedAt = nowMillis - 12_000_000L
                )
            )
        }
    }
}
