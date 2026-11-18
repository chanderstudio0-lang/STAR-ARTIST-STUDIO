package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.entity.StudioProjectStatus
import com.example.data.local.entity.StudioSessionProjectEntity
import com.example.data.repository.StudioSessionProjectsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProjectTrackerUiState {
    data object Loading : ProjectTrackerUiState
    data class Success(val projects: List<StudioSessionProjectEntity>) : ProjectTrackerUiState
    data class Error(val message: String) : ProjectTrackerUiState
}

data class ProjectStatusSummary(
    val total: Int = 0,
    val planningCount: Int = 0,
    val inProductionCount: Int = 0,
    val postProcessingCount: Int = 0,
    val completedCount: Int = 0,
    val averageCompletion: Int = 0,
    val activeTimersCount: Int = 0,
    val archivedCount: Int = 0
)

enum class ProjectTrackerViewMode {
    BOARD,
    CALENDAR,
    ARCHIVED
}

/**
 * ViewModel backed by Room [StudioSessionProjectsRepository] for tracking studio session
 * lifecycle workflows across Planning, In Production, Post-Processing, and Completed stages,
 * including a persistent stopwatch timer for 'In Production' tasks and monthly calendar sync.
 */
class ProjectStatusViewModel(
    private val repository: StudioSessionProjectsRepository,
    seedDefaultsOnInit: Boolean = true,
    val clockMillis: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _selectedStatusFilter = MutableStateFlow<StudioProjectStatus?>(null)
    val selectedStatusFilter: StateFlow<StudioProjectStatus?> = _selectedStatusFilter.asStateFlow()

    private val _viewMode = MutableStateFlow(ProjectTrackerViewMode.BOARD)
    val viewMode: StateFlow<ProjectTrackerViewMode> = _viewMode.asStateFlow()

    val showArchived: StateFlow<Boolean> = _viewMode
        .map { it == ProjectTrackerViewMode.ARCHIVED }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = false
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFeedback = MutableStateFlow<String?>(null)
    val statusFeedback: StateFlow<String?> = _statusFeedback.asStateFlow()

    private val _hasLoadedFromDb = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val allProjects: StateFlow<List<StudioSessionProjectEntity>> = repository.activeProjects
        .onEach { _hasLoadedFromDb.value = true }
        .catch { err ->
            _errorMessage.value = err.localizedMessage ?: "Failed to load session projects"
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val archivedProjects: StateFlow<List<StudioSessionProjectEntity>> = repository.archivedProjects
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val filteredProjects: StateFlow<List<StudioSessionProjectEntity>> = combine(
        allProjects,
        archivedProjects,
        _viewMode,
        _selectedStatusFilter,
        _searchQuery
    ) { active, archived, mode, statusFilter, query ->
        if (mode == ProjectTrackerViewMode.ARCHIVED) {
            applyFilters(archived, null, query)
        } else {
            applyFilters(active, statusFilter, query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val statusSummary: StateFlow<ProjectStatusSummary> = combine(
        allProjects,
        archivedProjects
    ) { projects, archived ->
        if (projects.isEmpty()) {
            ProjectStatusSummary(archivedCount = archived.size)
        } else {
            ProjectStatusSummary(
                total = projects.size,
                planningCount = projects.count { it.status == StudioProjectStatus.PLANNING },
                inProductionCount = projects.count { it.status == StudioProjectStatus.IN_PRODUCTION },
                postProcessingCount = projects.count { it.status == StudioProjectStatus.POST_PROCESSING },
                completedCount = projects.count { it.status == StudioProjectStatus.COMPLETED },
                averageCompletion = projects.sumOf { it.progressPercent } / projects.size,
                activeTimersCount = projects.count { it.isTimerRunning },
                archivedCount = archived.size
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ProjectStatusSummary()
    )

    val uiState: StateFlow<ProjectTrackerUiState> = combine(
        filteredProjects,
        _hasLoadedFromDb,
        _errorMessage
    ) { projects, hasLoaded, error ->
        when {
            error != null -> ProjectTrackerUiState.Error(error)
            !hasLoaded -> ProjectTrackerUiState.Loading
            else -> ProjectTrackerUiState.Success(projects)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ProjectTrackerUiState.Loading
    )

    init {
        if (seedDefaultsOnInit) {
            viewModelScope.launch {
                runCatching {
                    repository.seedDefaultProjectsIfEmpty()
                }.onFailure { err ->
                    _errorMessage.value = err.localizedMessage ?: "Unable to initialize session projects"
                }
            }
        }
    }

    fun setStatusFilter(status: StudioProjectStatus?) {
        _selectedStatusFilter.value = status
    }

    fun setViewMode(mode: ProjectTrackerViewMode) {
        _viewMode.value = mode
    }

    fun setShowArchived(showArchived: Boolean) {
        _viewMode.value = if (showArchived) {
            ProjectTrackerViewMode.ARCHIVED
        } else {
            ProjectTrackerViewMode.BOARD
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createSessionProject(
        projectTitle: String,
        clientName: String,
        serviceCategory: String,
        status: StudioProjectStatus = StudioProjectStatus.PLANNING,
        scheduledDate: String = "",
        deadlineDate: String = "",
        studioStage: String = "Stage A • Lighting Bay",
        leadDirector: String = "Star Artist Team",
        progressPercent: Int = status.defaultProgress,
        deliverableCount: Int = 25,
        budgetInr: Long = 0L,
        productionNotes: String = "",
        onResult: (Boolean) -> Unit = {}
    ) {
        val cleanTitle = projectTitle.trim()
        val cleanClient = clientName.trim()
        if (cleanTitle.isBlank() || cleanClient.isBlank()) {
            _statusFeedback.value = "Project title and client name are required."
            onResult(false)
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.createProject(
                    StudioSessionProjectEntity(
                        projectTitle = cleanTitle,
                        clientName = cleanClient,
                        serviceCategory = serviceCategory.trim().ifEmpty { "Photography" },
                        status = status,
                        scheduledDate = scheduledDate.trim().ifEmpty { "TBD" },
                        deadlineDate = deadlineDate.trim(),
                        studioStage = studioStage.trim().ifEmpty { "Stage A • Lighting Bay" },
                        leadDirector = leadDirector.trim().ifEmpty { "Star Artist Team" },
                        progressPercent = progressPercent.coerceIn(0, 100),
                        deliverableCount = deliverableCount.coerceAtLeast(1),
                        budgetInr = budgetInr.coerceAtLeast(0L),
                        productionNotes = productionNotes.trim()
                    )
                )
            }.onSuccess {
                _errorMessage.value = null
                _statusFeedback.value = "Added \"$cleanTitle\" (${status.labelEn}) to Room tracker."
                onResult(true)
            }.onFailure { err ->
                _statusFeedback.value = "Failed to save session: ${err.localizedMessage ?: "Unknown error"}"
                onResult(false)
            }
        }
    }

    fun updateProjectStatus(
        project: StudioSessionProjectEntity,
        newStatus: StudioProjectStatus
    ) {
        if (project.status == newStatus) return
        viewModelScope.launch {
            runCatching {
                repository.updateProjectStatus(project, newStatus)
            }.onSuccess {
                _statusFeedback.value = "${project.projectTitle} moved to ${newStatus.labelEn}."
            }.onFailure { err ->
                _statusFeedback.value = "Could not update status: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun advanceProjectStage(project: StudioSessionProjectEntity) {
        val next = project.status.nextStage() ?: return
        updateProjectStatus(project, next)
    }

    fun updateProjectProgress(
        project: StudioSessionProjectEntity,
        progressPercent: Int
    ) {
        val clamped = progressPercent.coerceIn(0, 100)
        val resolvedStatus = if (clamped >= 100) StudioProjectStatus.COMPLETED else project.status
        viewModelScope.launch {
            runCatching {
                repository.updateProject(
                    project.copy(
                        progressPercent = clamped,
                        status = resolvedStatus,
                        completedAt = if (resolvedStatus == StudioProjectStatus.COMPLETED) {
                            project.completedAt ?: System.currentTimeMillis()
                        } else {
                            null
                        }
                    )
                )
            }.onSuccess {
                _statusFeedback.value = "Updated progress for ${project.projectTitle} to $clamped%."
            }.onFailure { err ->
                _statusFeedback.value = "Progress update failed: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun updateProductionDetails(
        project: StudioSessionProjectEntity,
        productionNotes: String,
        progressPercent: Int,
        studioStage: String,
        scheduledDate: String = project.scheduledDate,
        deadlineDate: String = project.deadlineDate
    ) {
        val clamped = progressPercent.coerceIn(0, 100)
        val resolvedStatus = if (clamped >= 100) StudioProjectStatus.COMPLETED else project.status
        viewModelScope.launch {
            runCatching {
                repository.updateProject(
                    project.copy(
                        productionNotes = productionNotes.trim(),
                        progressPercent = clamped,
                        studioStage = studioStage.trim().ifEmpty { project.studioStage },
                        scheduledDate = scheduledDate.trim().ifEmpty { project.scheduledDate },
                        deadlineDate = deadlineDate.trim(),
                        status = resolvedStatus,
                        completedAt = if (resolvedStatus == StudioProjectStatus.COMPLETED) {
                            project.completedAt ?: System.currentTimeMillis()
                        } else {
                            null
                        }
                    )
                )
            }.onSuccess {
                _statusFeedback.value = "Saved schedule & production details for ${project.projectTitle}."
            }.onFailure { err ->
                _statusFeedback.value = "Update failed: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun toggleProductionTimer(project: StudioSessionProjectEntity) {
        if (project.status != StudioProjectStatus.IN_PRODUCTION) {
            _statusFeedback.value = "Move \"${project.projectTitle}\" to In Production to start its stopwatch."
            return
        }
        viewModelScope.launch {
            runCatching {
                if (project.isTimerRunning) {
                    repository.pauseProductionTimer(project)
                } else {
                    repository.startProductionTimer(project)
                }
            }.onSuccess { updated ->
                _statusFeedback.value = if (updated.isTimerRunning) {
                    "Stopwatch started for ${project.projectTitle}."
                } else {
                    "Stopwatch paused (${StudioSessionProjectEntity.formatDurationHhMmSs(updated.trackedProductionMillis)})."
                }
            }.onFailure { err ->
                _statusFeedback.value = "Timer update failed: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun resetProductionTimer(project: StudioSessionProjectEntity) {
        viewModelScope.launch {
            runCatching {
                repository.resetProductionTimer(project)
            }.onSuccess {
                _statusFeedback.value = "Reset stopwatch for ${project.projectTitle}."
            }.onFailure { err ->
                _statusFeedback.value = "Timer reset failed: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun archiveProject(project: StudioSessionProjectEntity) {
        if (project.status != StudioProjectStatus.COMPLETED) {
            _statusFeedback.value = "Only completed sessions can be archived."
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.archiveProject(project)
            }.onSuccess { archived ->
                if (archived) {
                    _statusFeedback.value = "Archived \"${project.projectTitle}\" for historical reference."
                }
            }.onFailure { err ->
                _statusFeedback.value = "Archive failed: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun restoreArchivedProject(project: StudioSessionProjectEntity) {
        viewModelScope.launch {
            runCatching {
                repository.restoreProject(project)
            }.onSuccess {
                _statusFeedback.value = "Restored \"${project.projectTitle}\" to main dashboard."
            }.onFailure { err ->
                _statusFeedback.value = "Restore failed: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun archiveAllCompleted() {
        viewModelScope.launch {
            runCatching {
                repository.archiveAllCompleted()
            }.onSuccess { count ->
                _statusFeedback.value = if (count > 0) {
                    "Archived $count completed session(s) to historical reference."
                } else {
                    "No completed sessions ready to archive."
                }
            }.onFailure { err ->
                _statusFeedback.value = "Archive failed: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun saveInProductionTaskNotes(
        project: StudioSessionProjectEntity,
        notes: String
    ) {
        viewModelScope.launch {
            runCatching {
                repository.saveInProductionTaskNotes(project, notes)
            }.onSuccess {
                _statusFeedback.value = "Saved 'In Production' task notes for ${project.projectTitle}."
            }.onFailure { err ->
                _statusFeedback.value = "Could not save task notes: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun deleteSessionProject(project: StudioSessionProjectEntity) {
        viewModelScope.launch {
            runCatching {
                repository.deleteProject(project)
            }.onSuccess {
                _statusFeedback.value = "Removed \"${project.projectTitle}\" from Room tracker."
            }.onFailure { err ->
                _statusFeedback.value = "Delete failed: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun clearStatusFeedback() {
        _statusFeedback.value = null
    }

    private fun applyFilters(
        projects: List<StudioSessionProjectEntity>,
        statusFilter: StudioProjectStatus?,
        query: String
    ): List<StudioSessionProjectEntity> {
        val normalizedQuery = query.trim().lowercase()
        return projects.filter { project ->
            val matchesStatus = statusFilter == null || project.status == statusFilter
            val matchesQuery = normalizedQuery.isEmpty() ||
                project.projectTitle.lowercase().contains(normalizedQuery) ||
                project.clientName.lowercase().contains(normalizedQuery) ||
                project.sessionCode.lowercase().contains(normalizedQuery) ||
                project.serviceCategory.lowercase().contains(normalizedQuery) ||
                project.studioStage.lowercase().contains(normalizedQuery) ||
                project.productionNotes.lowercase().contains(normalizedQuery)
            matchesStatus && matchesQuery
        }
    }

    companion object {
        fun factory(
            repository: StudioSessionProjectsRepository,
            seedDefaultsOnInit: Boolean = true
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProjectStatusViewModel(
                    repository = repository,
                    seedDefaultsOnInit = seedDefaultsOnInit
                )
            }
        }
    }
}
