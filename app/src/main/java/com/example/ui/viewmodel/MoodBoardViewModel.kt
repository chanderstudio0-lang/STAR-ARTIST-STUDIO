package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.ConceptArtGenerator
import com.example.data.ai.FirebaseConceptArtGenerator
import com.example.data.local.entity.MoodBoardConceptEntity
import com.example.data.local.entity.ProjectDraftEntity
import com.example.data.repository.ArtistDraftsRepository
import com.example.data.repository.MoodBoardRepository
import com.google.firebase.ai.type.QuotaExceededException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "MoodBoardVM"
private const val GENERATION_TIMEOUT_MS = 60_000L

sealed interface MoodBoardArtUiState {
    data object Idle : MoodBoardArtUiState
    data class Loading(
        val statusMessage: String = "Synthesizing concept art (may take 30-60s)...",
        val isRemix: Boolean = false
    ) : MoodBoardArtUiState

    data class Success(
        val bitmap: Bitmap,
        val boardTitle: String,
        val projectType: String,
        val compiledPrompt: String,
        val rawPrompt: String,
        val visualStyle: String,
        val lightingSetup: String,
        val aspectRatioLabel: String,
        val modelUsed: String,
        val colorPaletteTags: String,
        val sessionProjectId: Long? = null,
        val isSavedToBoard: Boolean = false,
        val savedConceptId: Long? = null
    ) : MoodBoardArtUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : MoodBoardArtUiState
}

/**
 * ViewModel managing runtime Firebase AI Logic image generation and Room-persisted mood boards.
 */
class MoodBoardViewModel(
    private val moodBoardRepository: MoodBoardRepository,
    private val draftsRepository: ArtistDraftsRepository? = null,
    private val generator: ConceptArtGenerator = FirebaseConceptArtGenerator()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MoodBoardArtUiState>(MoodBoardArtUiState.Idle)
    val uiState: StateFlow<MoodBoardArtUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _targetSessionProjectId = MutableStateFlow<Long?>(null)
    val targetSessionProjectId: StateFlow<Long?> = _targetSessionProjectId.asStateFlow()

    private var lastRetryBlock: (() -> Unit)? = null

    val allConcepts: StateFlow<List<MoodBoardConceptEntity>> = moodBoardRepository.allConcepts
        .catch { err ->
            Log.e(TAG, "Failed observing mood board concepts", err)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val filteredConcepts: StateFlow<List<MoodBoardConceptEntity>> = combine(
        allConcepts,
        _selectedFilter
    ) { items, filter ->
        when (filter) {
            "All" -> items
            "Favorites" -> items.filter { it.isFavorite }
            else -> items.filter { it.projectType.equals(filter, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setTargetSessionProject(projectId: Long?) {
        _targetSessionProjectId.value = projectId
        val latest = _uiState.value
        if (latest is MoodBoardArtUiState.Success) {
            _uiState.value = latest.copy(sessionProjectId = projectId)
        }
    }

    fun generateConceptArt(
        boardTitle: String,
        projectType: String,
        prompt: String,
        visualStyle: String,
        lightingSetup: String,
        aspectRatioLabel: String,
        colorPaletteTags: String,
        modelName: String = FirebaseConceptArtGenerator.MODEL_NANO_BANANA_FAST,
        sessionProjectId: Long? = _targetSessionProjectId.value
    ) {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isBlank()) {
            _uiState.value = MoodBoardArtUiState.Error(
                message = "Please enter a creative visual prompt for your mood board concept.",
                canRetry = false
            )
            return
        }

        val resolvedTitle = boardTitle.trim().ifEmpty { "$projectType Concept Board" }
        val resolvedPalette = colorPaletteTags.trim().ifEmpty { "Obsidian Black, Burnished Gold" }
        val compiledPrompt = buildArtDirectionPrompt(
            projectType = projectType,
            userPrompt = trimmedPrompt,
            visualStyle = visualStyle,
            lightingSetup = lightingSetup,
            aspectRatioLabel = aspectRatioLabel,
            colorPaletteTags = resolvedPalette
        )

        lastRetryBlock = {
            generateConceptArt(
                boardTitle = resolvedTitle,
                projectType = projectType,
                prompt = trimmedPrompt,
                visualStyle = visualStyle,
                lightingSetup = lightingSetup,
                aspectRatioLabel = aspectRatioLabel,
                colorPaletteTags = resolvedPalette,
                modelName = modelName
            )
        }

        viewModelScope.launch {
            _uiState.value = MoodBoardArtUiState.Loading(
                statusMessage = "Generating mood board artwork with $modelName (30-60s)...",
                isRemix = false
            )
            try {
                val bitmap = withTimeout(GENERATION_TIMEOUT_MS) {
                    generator.generateConceptBitmap(compiledPrompt, modelName)
                }
                if (bitmap != null) {
                    _uiState.value = MoodBoardArtUiState.Success(
                        bitmap = bitmap,
                        boardTitle = resolvedTitle,
                        projectType = projectType,
                        compiledPrompt = compiledPrompt,
                        rawPrompt = trimmedPrompt,
                        visualStyle = visualStyle,
                        lightingSetup = lightingSetup,
                        aspectRatioLabel = aspectRatioLabel,
                        modelUsed = modelName,
                        colorPaletteTags = resolvedPalette,
                        sessionProjectId = sessionProjectId,
                        isSavedToBoard = false
                    )
                } else {
                    _uiState.value = MoodBoardArtUiState.Error(
                        message = "No image was returned by the model. Try adjusting the prompt details.",
                        canRetry = true
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Mood board generation timed out", e)
                _uiState.value = MoodBoardArtUiState.Error(
                    message = "Image generation timed out after 60 seconds. Please retry.",
                    canRetry = true
                )
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "Firebase AI image quota exceeded", e)
                _uiState.value = MoodBoardArtUiState.Error(
                    message = "Image generation quota or rate limit reached. Ensure Firebase Blaze billing and quota allow image generation, then try again.",
                    canRetry = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image generation failed", e)
                _uiState.value = MoodBoardArtUiState.Error(
                    message = formatImageError(e),
                    canRetry = true
                )
            }
        }
    }

    fun remixCurrentConcept(refinementInstruction: String) {
        val current = _uiState.value as? MoodBoardArtUiState.Success ?: return
        val cleanEdit = refinementInstruction.trim()
        if (cleanEdit.isBlank()) {
            _feedbackMessage.value = "Enter refinement instructions to remix this mood board frame."
            return
        }

        val fullEditInstruction = "Refine this studio mood board photograph concept while preserving core composition: $cleanEdit"
        lastRetryBlock = {
            remixCurrentConcept(cleanEdit)
        }

        viewModelScope.launch {
            _uiState.value = MoodBoardArtUiState.Loading(
                statusMessage = "Remixing mood board concept with ${current.modelUsed} (30-60s)...",
                isRemix = true
            )
            try {
                val updatedBitmap = withTimeout(GENERATION_TIMEOUT_MS) {
                    generator.remixConceptBitmap(
                        baseBitmap = current.bitmap,
                        editInstruction = fullEditInstruction,
                        modelName = current.modelUsed
                    )
                }
                if (updatedBitmap != null) {
                    _uiState.value = current.copy(
                        bitmap = updatedBitmap,
                        rawPrompt = "${current.rawPrompt} • Remix: $cleanEdit",
                        compiledPrompt = "${current.compiledPrompt}\nRefinement: $cleanEdit",
                        isSavedToBoard = false,
                        savedConceptId = null
                    )
                    _feedbackMessage.value = "Concept artwork refined."
                } else {
                    _uiState.value = MoodBoardArtUiState.Error(
                        message = "Model did not return a remixed image. Try a clearer visual edit instruction.",
                        canRetry = true
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Mood board remix timed out", e)
                _uiState.value = MoodBoardArtUiState.Error(
                    message = "Concept remix timed out after 60 seconds. Please retry.",
                    canRetry = true
                )
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "Firebase AI image remix quota exceeded", e)
                _uiState.value = MoodBoardArtUiState.Error(
                    message = "Image generation quota exceeded. Please try again later.",
                    canRetry = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Concept remix failed", e)
                _uiState.value = MoodBoardArtUiState.Error(
                    message = formatImageError(e),
                    canRetry = true
                )
            }
        }
    }

    fun saveCurrentArtworkToMoodBoard() {
        val current = _uiState.value as? MoodBoardArtUiState.Success ?: return
        if (current.isSavedToBoard) {
            _feedbackMessage.value = "Already saved to your Mood Board Archive."
            return
        }

        viewModelScope.launch {
            runCatching {
                val saved = moodBoardRepository.saveGeneratedConcept(
                    bitmap = current.bitmap,
                    boardTitle = current.boardTitle,
                    projectType = current.projectType,
                    prompt = current.rawPrompt,
                    visualStyle = current.visualStyle,
                    lightingSetup = current.lightingSetup,
                    aspectRatioLabel = current.aspectRatioLabel,
                    modelUsed = current.modelUsed,
                    colorPaletteTags = current.colorPaletteTags,
                    sessionProjectId = current.sessionProjectId ?: _targetSessionProjectId.value
                )
                draftsRepository?.logSession(
                    sessionType = "MOOD_BOARD_ART",
                    sessionTitle = "Mood Board Art: ${saved.boardTitle}",
                    summaryDetails = "${saved.projectType} • ${saved.visualStyle} • ${saved.aspectRatioLabel}"
                )
                saved
            }.onSuccess { saved ->
                val latest = _uiState.value
                if (latest is MoodBoardArtUiState.Success) {
                    _uiState.value = latest.copy(
                        isSavedToBoard = true,
                        savedConceptId = saved.id
                    )
                }
                _feedbackMessage.value = "Saved \"${saved.boardTitle}\" to Mood Board."
            }.onFailure { err ->
                Log.e(TAG, "Failed saving mood board concept", err)
                _feedbackMessage.value = "Save failed: ${err.localizedMessage ?: "Storage error"}"
            }
        }
    }

    fun createProjectDraftFromCurrentArt() {
        val current = _uiState.value as? MoodBoardArtUiState.Success ?: return
        val draftsRepo = draftsRepository ?: run {
            _feedbackMessage.value = "Project drafts repository unavailable."
            return
        }

        viewModelScope.launch {
            runCatching {
                val concept = if (current.savedConceptId != null) {
                    moodBoardRepository.getConceptById(current.savedConceptId)
                } else {
                    moodBoardRepository.saveGeneratedConcept(
                        bitmap = current.bitmap,
                        boardTitle = current.boardTitle,
                        projectType = current.projectType,
                        prompt = current.rawPrompt,
                        visualStyle = current.visualStyle,
                        lightingSetup = current.lightingSetup,
                        aspectRatioLabel = current.aspectRatioLabel,
                        modelUsed = current.modelUsed,
                        colorPaletteTags = current.colorPaletteTags,
                        sessionProjectId = current.sessionProjectId ?: _targetSessionProjectId.value
                    )
                }

                val draftId = draftsRepo.saveDraft(
                    ProjectDraftEntity(
                        draftTitle = current.boardTitle,
                        projectType = current.projectType,
                        conceptNotes = "AI Mood Board Prompt: ${current.rawPrompt}\nStyle: ${current.visualStyle} | Palette: ${current.colorPaletteTags}",
                        lightingSetupNotes = "${current.lightingSetup} • Framing: ${current.aspectRatioLabel}",
                        cameraGearNotes = "Mood Board Ref #${concept?.id ?: 0} (${current.modelUsed})"
                    )
                )

                if (concept != null) {
                    moodBoardRepository.linkToDraft(concept, draftId)
                }
                concept to draftId
            }.onSuccess { (concept, _) ->
                val latest = _uiState.value
                if (latest is MoodBoardArtUiState.Success && concept != null) {
                    _uiState.value = latest.copy(
                        isSavedToBoard = true,
                        savedConceptId = concept.id
                    )
                }
                _feedbackMessage.value = "Created Project Draft from \"${current.boardTitle}\"."
            }.onFailure { err ->
                Log.e(TAG, "Failed creating draft from mood board", err)
                _feedbackMessage.value = "Could not create draft: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun createProjectDraftFromSavedConcept(concept: MoodBoardConceptEntity) {
        val draftsRepo = draftsRepository ?: return
        viewModelScope.launch {
            runCatching {
                val draftId = draftsRepo.saveDraft(
                    ProjectDraftEntity(
                        draftTitle = concept.boardTitle,
                        projectType = concept.projectType,
                        conceptNotes = "AI Mood Board Prompt: ${concept.prompt}\nStyle: ${concept.visualStyle} | Palette: ${concept.colorPaletteTags}",
                        lightingSetupNotes = "${concept.lightingSetup} • Framing: ${concept.aspectRatioLabel}",
                        cameraGearNotes = "Mood Board Ref #${concept.id} (${concept.modelUsed})"
                    )
                )
                moodBoardRepository.linkToDraft(concept, draftId)
            }.onSuccess {
                _feedbackMessage.value = "Promoted \"${concept.boardTitle}\" to Project Drafts."
            }.onFailure { err ->
                _feedbackMessage.value = "Draft creation failed: ${err.localizedMessage ?: "Error"}"
            }
        }
    }

    fun loadConceptIntoCanvasForRemix(concept: MoodBoardConceptEntity) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(concept.localImagePath) }.getOrNull()
            }
            if (bitmap != null) {
                _uiState.value = MoodBoardArtUiState.Success(
                    bitmap = bitmap,
                    boardTitle = concept.boardTitle,
                    projectType = concept.projectType,
                    compiledPrompt = concept.prompt,
                    rawPrompt = concept.prompt,
                    visualStyle = concept.visualStyle,
                    lightingSetup = concept.lightingSetup,
                    aspectRatioLabel = concept.aspectRatioLabel,
                    modelUsed = concept.modelUsed,
                    colorPaletteTags = concept.colorPaletteTags,
                    sessionProjectId = concept.sessionProjectId,
                    isSavedToBoard = true,
                    savedConceptId = concept.id
                )
                _feedbackMessage.value = "Loaded \"${concept.boardTitle}\" onto canvas for remixing."
            } else {
                _feedbackMessage.value = "Saved artwork file could not be loaded."
            }
        }
    }

    fun toggleFavorite(concept: MoodBoardConceptEntity) {
        viewModelScope.launch {
            runCatching { moodBoardRepository.toggleFavorite(concept) }
        }
    }

    fun deleteConcept(concept: MoodBoardConceptEntity) {
        viewModelScope.launch {
            runCatching { moodBoardRepository.deleteConcept(concept) }
                .onSuccess {
                    _feedbackMessage.value = "Deleted \"${concept.boardTitle}\" from mood boards."
                }
        }
    }

    fun retryLastAction() {
        lastRetryBlock?.invoke()
    }

    fun resetCanvas() {
        _uiState.value = MoodBoardArtUiState.Idle
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    private fun buildArtDirectionPrompt(
        projectType: String,
        userPrompt: String,
        visualStyle: String,
        lightingSetup: String,
        aspectRatioLabel: String,
        colorPaletteTags: String
    ): String {
        return buildString {
            append("Creative studio photography & cinematography concept art for a $projectType project mood board. ")
            append("Scene concept: $userPrompt. ")
            append("Art direction & aesthetic: $visualStyle. ")
            append("Studio lighting setup: $lightingSetup. ")
            append("Color palette: $colorPaletteTags. ")
            append("Framing & composition style: $aspectRatioLabel, shot on full-frame cinema camera with editorial art direction, rich textures, no text overlays or watermarks.")
        }
    }

    private fun formatImageError(e: Throwable): String {
        val raw = e.localizedMessage ?: e.message ?: "Unknown error"
        return when {
            raw.contains("403", ignoreCase = true) || raw.contains("AppCheck", ignoreCase = true) ->
                "Firebase App Check attestation error (403): Ensure App Check debug token is registered and AI Logic is enabled."
            raw.contains("billing", ignoreCase = true) || raw.contains("Blaze", ignoreCase = true) ->
                "Gemini image generation requires the Firebase project to be on the Blaze plan: $raw"
            raw.contains("UnknownHost", ignoreCase = true) || raw.contains("ConnectException", ignoreCase = true) ->
                "Network connection error while reaching Firebase AI Logic. Check internet connectivity and retry."
            else -> raw
        }
    }

    companion object {
        fun factory(
            moodBoardRepository: MoodBoardRepository,
            draftsRepository: ArtistDraftsRepository? = null,
            generator: ConceptArtGenerator = FirebaseConceptArtGenerator()
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MoodBoardViewModel(
                    moodBoardRepository = moodBoardRepository,
                    draftsRepository = draftsRepository,
                    generator = generator
                )
            }
        }
    }
}
