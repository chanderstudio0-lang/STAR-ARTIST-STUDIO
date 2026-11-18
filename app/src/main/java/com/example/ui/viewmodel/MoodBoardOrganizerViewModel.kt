package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.entity.MoodBoardConceptEntity
import com.example.data.local.entity.MoodBoardReferenceEntity
import com.example.data.local.entity.PortfolioProjectMediaType
import com.example.data.local.entity.StudioProjectStatus
import com.example.data.local.entity.StudioSessionProjectEntity
import com.example.data.repository.MoodBoardRepository
import com.example.data.repository.PortfolioProjectsRepository
import com.example.data.repository.ServiceCatalogReferenceRepository
import com.example.data.repository.StudioSessionProjectsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "MoodBoardOrgVM"

enum class MoodBoardMediaFilter {
    ALL,
    CONCEPT_ART,
    REFERENCE_PHOTOS,
    PINNED
}

data class MoodBoardWorkspaceStats(
    val totalItems: Int = 0,
    val conceptArtCount: Int = 0,
    val referencePhotoCount: Int = 0,
    val pinnedCount: Int = 0
)

data class StudioImportCandidate(
    val id: String,
    val title: String,
    val category: String,
    val imageUrl: String,
    val sourceLabel: String
)

/**
 * Unified visual tile uniting AI-generated concept art and real reference photos
 * inside an ongoing project's Mood Board workspace.
 */
sealed interface MoodBoardTile {
    val stableKey: String
    val title: String
    val sectionTag: String
    val sessionProjectId: Long?
    val imageModelPathOrUrl: String
    val colorPaletteTags: String
    val isPinned: Boolean
    val createdAt: Long
    val subtitleOrNotes: String

    data class ConceptArtTile(val entity: MoodBoardConceptEntity) : MoodBoardTile {
        override val stableKey: String = "concept_${entity.id}"
        override val title: String = entity.boardTitle
        override val sectionTag: String = entity.boardSectionTag.ifBlank { "Concept Art" }
        override val sessionProjectId: Long? = entity.sessionProjectId
        override val imageModelPathOrUrl: String = entity.localImagePath
        override val colorPaletteTags: String = entity.colorPaletteTags
        override val isPinned: Boolean = entity.isFavorite
        override val createdAt: Long = entity.createdAt
        override val subtitleOrNotes: String = entity.prompt
    }

    data class ReferencePhotoTile(val entity: MoodBoardReferenceEntity) : MoodBoardTile {
        override val stableKey: String = "ref_${entity.id}"
        override val title: String = entity.title
        override val sectionTag: String = entity.sectionTag
        override val sessionProjectId: Long? = entity.sessionProjectId
        override val imageModelPathOrUrl: String = entity.imagePathOrUrl
        override val colorPaletteTags: String = entity.colorSwatches
        override val isPinned: Boolean = entity.isPinned
        override val createdAt: Long = entity.createdAt
        override val subtitleOrNotes: String = entity.notes
    }
}

/**
 * ViewModel backing the dedicated Mood Board organizer section where users curate
 * AI-generated concept art alongside reference photos for ongoing studio projects.
 */
class MoodBoardOrganizerViewModel(
    private val moodBoardRepository: MoodBoardRepository,
    private val sessionProjectsRepository: StudioSessionProjectsRepository,
    serviceCatalogReferenceRepository: ServiceCatalogReferenceRepository? = null,
    portfolioProjectsRepository: PortfolioProjectsRepository? = null,
    seedProjectsOnInit: Boolean = true
) : ViewModel() {

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    private val _mediaFilter = MutableStateFlow(MoodBoardMediaFilter.ALL)
    val mediaFilter: StateFlow<MoodBoardMediaFilter> = _mediaFilter.asStateFlow()

    private val _selectedSectionTag = MutableStateFlow(ALL_SECTIONS)
    val selectedSectionTag: StateFlow<String> = _selectedSectionTag.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    val ongoingProjects: StateFlow<List<StudioSessionProjectEntity>> = sessionProjectsRepository.allProjects
        .catch { emit(emptyList()) }
        .combine(MutableStateFlow(Unit)) { list, _ ->
            // Show active ongoing studio shoots (Planning, In Production, Post-Processing) first, Completed after
            list.sortedBy {
                when (it.status) {
                    StudioProjectStatus.IN_PRODUCTION -> 0
                    StudioProjectStatus.PLANNING -> 1
                    StudioProjectStatus.POST_PROCESSING -> 2
                    StudioProjectStatus.COMPLETED -> 3
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val allConcepts: StateFlow<List<MoodBoardConceptEntity>> = moodBoardRepository.allConcepts
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val allReferences: StateFlow<List<MoodBoardReferenceEntity>> = moodBoardRepository.allReferences
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val selectedProject: StateFlow<StudioSessionProjectEntity?> = combine(
        ongoingProjects,
        _selectedProjectId
    ) { projects, id ->
        if (id == null) null else projects.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val projectItemCounts: StateFlow<Map<Long, Int>> = combine(
        allConcepts,
        allReferences
    ) { concepts, refs ->
        val counts = mutableMapOf<Long, Int>()
        concepts.forEach { c ->
            c.sessionProjectId?.let { pid -> counts[pid] = (counts[pid] ?: 0) + 1 }
        }
        refs.forEach { r ->
            r.sessionProjectId?.let { pid -> counts[pid] = (counts[pid] ?: 0) + 1 }
        }
        counts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    private val projectScopedTiles: StateFlow<List<MoodBoardTile>> = combine(
        allConcepts,
        allReferences,
        _selectedProjectId
    ) { concepts, refs, projectId ->
        val scopedConcepts = if (projectId == null) {
            concepts
        } else {
            concepts.filter { it.sessionProjectId == projectId }
        }.map { MoodBoardTile.ConceptArtTile(it) }

        val scopedRefs = if (projectId == null) {
            refs
        } else {
            refs.filter { it.sessionProjectId == projectId }
        }.map { MoodBoardTile.ReferencePhotoTile(it) }

        (scopedConcepts + scopedRefs).sortedWith(
            compareByDescending<MoodBoardTile> { it.isPinned }
                .thenByDescending { it.createdAt }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val workspaceStats: StateFlow<MoodBoardWorkspaceStats> = projectScopedTiles
        .combine(MutableStateFlow(Unit)) { tiles, _ ->
            MoodBoardWorkspaceStats(
                totalItems = tiles.size,
                conceptArtCount = tiles.count { it is MoodBoardTile.ConceptArtTile },
                referencePhotoCount = tiles.count { it is MoodBoardTile.ReferencePhotoTile },
                pinnedCount = tiles.count { it.isPinned }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = MoodBoardWorkspaceStats()
        )

    val filteredTiles: StateFlow<List<MoodBoardTile>> = combine(
        projectScopedTiles,
        _mediaFilter,
        _selectedSectionTag,
        _searchQuery
    ) { tiles, filter, sectionTag, query ->
        val q = query.trim().lowercase()
        tiles.filter { tile ->
            val matchesMedia = when (filter) {
                MoodBoardMediaFilter.ALL -> true
                MoodBoardMediaFilter.CONCEPT_ART -> tile is MoodBoardTile.ConceptArtTile
                MoodBoardMediaFilter.REFERENCE_PHOTOS -> tile is MoodBoardTile.ReferencePhotoTile
                MoodBoardMediaFilter.PINNED -> tile.isPinned
            }
            val matchesSection = sectionTag == ALL_SECTIONS ||
                tile.sectionTag.equals(sectionTag, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                tile.title.lowercase().contains(q) ||
                tile.subtitleOrNotes.lowercase().contains(q) ||
                tile.sectionTag.lowercase().contains(q) ||
                tile.colorPaletteTags.lowercase().contains(q)
            matchesMedia && matchesSection && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val importCandidates: StateFlow<List<StudioImportCandidate>> = combine(
        serviceCatalogReferenceRepository?.allReferences ?: flowOf(emptyList()),
        portfolioProjectsRepository?.allProjects ?: flowOf(emptyList())
    ) { catalogRefs, portfolioItems ->
        val fromCatalog = catalogRefs.map { ref ->
            StudioImportCandidate(
                id = "cat_${ref.id}",
                title = ref.title,
                category = ref.aspectGuide,
                imageUrl = ref.imageDataUrl,
                sourceLabel = "Catalog Reference"
            )
        }
        val fromPortfolio = portfolioItems
            .filter { it.mediaType.equals(PortfolioProjectMediaType.PHOTO, ignoreCase = true) }
            .map { proj ->
                StudioImportCandidate(
                    id = "port_${proj.id}",
                    title = proj.title,
                    category = proj.category,
                    imageUrl = proj.thumbnailUrl.ifBlank { proj.mediaUrl },
                    sourceLabel = "Studio Portfolio"
                )
            }
        (fromCatalog + fromPortfolio).filter { it.imageUrl.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        if (seedProjectsOnInit) {
            viewModelScope.launch {
                runCatching { sessionProjectsRepository.seedDefaultProjectsIfEmpty() }
            }
        }
    }

    fun selectProject(projectId: Long?) {
        _selectedProjectId.value = projectId
    }

    fun setMediaFilter(filter: MoodBoardMediaFilter) {
        _mediaFilter.value = filter
    }

    fun setSectionFilter(sectionTag: String) {
        _selectedSectionTag.value = sectionTag
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addReferenceFromGalleryUri(
        uri: Uri,
        title: String,
        sectionTag: String,
        notes: String = "",
        colorSwatches: String = "Warm Gold, Deep Shadow",
        targetProjectId: Long? = _selectedProjectId.value
    ) {
        viewModelScope.launch {
            runCatching {
                moodBoardRepository.addReferencePhotoFromUri(
                    uri = uri,
                    title = title,
                    sectionTag = sectionTag,
                    notes = notes,
                    sessionProjectId = targetProjectId,
                    colorSwatches = colorSwatches
                )
            }.onSuccess { saved ->
                _feedbackMessage.value = "Added reference \"${saved.title}\" to Mood Board."
            }.onFailure { err ->
                Log.e(TAG, "Failed adding gallery reference photo", err)
                _feedbackMessage.value = "Could not import photo: ${err.localizedMessage ?: "File error"}"
            }
        }
    }

    fun addReferenceFromCameraShot(
        dataUrl: String,
        title: String,
        sectionTag: String,
        notes: String = "",
        targetProjectId: Long? = _selectedProjectId.value
    ) {
        viewModelScope.launch {
            runCatching {
                moodBoardRepository.addReferenceFromUrlOrDataUrl(
                    imagePathOrUrl = dataUrl,
                    title = title,
                    sectionTag = sectionTag,
                    sourceType = MoodBoardReferenceEntity.SOURCE_CAMERA_SHOT,
                    notes = notes,
                    sessionProjectId = targetProjectId
                )
            }.onSuccess { saved ->
                _feedbackMessage.value = "Camera reference \"${saved.title}\" added to Mood Board."
            }.onFailure { err ->
                Log.e(TAG, "Failed adding camera reference photo", err)
                _feedbackMessage.value = "Could not save camera reference: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun importStudioCandidate(
        candidate: StudioImportCandidate,
        sectionTag: String = "Framing Guide",
        targetProjectId: Long? = _selectedProjectId.value
    ) {
        viewModelScope.launch {
            runCatching {
                moodBoardRepository.addReferenceFromUrlOrDataUrl(
                    imagePathOrUrl = candidate.imageUrl,
                    title = candidate.title,
                    sectionTag = sectionTag,
                    sourceType = MoodBoardReferenceEntity.SOURCE_STUDIO_PORTFOLIO,
                    notes = "Imported from ${candidate.sourceLabel} (${candidate.category})",
                    sessionProjectId = targetProjectId
                )
            }.onSuccess { saved ->
                _feedbackMessage.value = "Imported \"${saved.title}\" onto Mood Board."
            }.onFailure { err ->
                Log.e(TAG, "Failed importing studio candidate", err)
                _feedbackMessage.value = "Import failed: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun toggleTilePin(tile: MoodBoardTile) {
        viewModelScope.launch {
            runCatching {
                when (tile) {
                    is MoodBoardTile.ConceptArtTile -> moodBoardRepository.toggleFavorite(tile.entity)
                    is MoodBoardTile.ReferencePhotoTile -> moodBoardRepository.toggleReferencePin(tile.entity)
                }
            }
        }
    }

    fun updateTileOrganization(
        tile: MoodBoardTile,
        targetProjectId: Long?,
        sectionTag: String,
        notes: String
    ) {
        viewModelScope.launch {
            runCatching {
                when (tile) {
                    is MoodBoardTile.ConceptArtTile -> {
                        moodBoardRepository.updateConceptOrganization(
                            concept = tile.entity,
                            sectionTag = sectionTag,
                            sessionProjectId = targetProjectId
                        )
                    }
                    is MoodBoardTile.ReferencePhotoTile -> {
                        moodBoardRepository.updateReferenceMetadata(
                            reference = tile.entity,
                            title = tile.entity.title,
                            sectionTag = sectionTag,
                            notes = notes,
                            colorSwatches = tile.entity.colorSwatches,
                            sessionProjectId = targetProjectId
                        )
                    }
                }
            }.onSuccess {
                _feedbackMessage.value = "Updated Mood Board organization for \"${tile.title}\"."
            }.onFailure { err ->
                _feedbackMessage.value = "Update failed: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun deleteTile(tile: MoodBoardTile) {
        viewModelScope.launch {
            runCatching {
                when (tile) {
                    is MoodBoardTile.ConceptArtTile -> moodBoardRepository.deleteConcept(tile.entity)
                    is MoodBoardTile.ReferencePhotoTile -> moodBoardRepository.deleteReference(tile.entity)
                }
            }.onSuccess {
                _feedbackMessage.value = "Removed \"${tile.title}\" from Mood Board."
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    companion object {
        const val ALL_SECTIONS = "All Sections"
        val SECTION_TAGS = listOf(
            "Concept Art",
            "Lighting Ref",
            "Wardrobe & Styling",
            "Set & Props",
            "Framing Guide",
            "Color Reference"
        )

        fun factory(
            moodBoardRepository: MoodBoardRepository,
            sessionProjectsRepository: StudioSessionProjectsRepository,
            serviceCatalogReferenceRepository: ServiceCatalogReferenceRepository? = null,
            portfolioProjectsRepository: PortfolioProjectsRepository? = null
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MoodBoardOrganizerViewModel(
                    moodBoardRepository = moodBoardRepository,
                    sessionProjectsRepository = sessionProjectsRepository,
                    serviceCatalogReferenceRepository = serviceCatalogReferenceRepository,
                    portfolioProjectsRepository = portfolioProjectsRepository
                )
            }
        }
    }
}
