package com.example.ui.ai

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entity.GenerationHistoryEntity
import com.example.data.local.entity.ProjectDraftEntity
import com.example.data.local.entity.SessionHistoryEntity
import com.example.data.repository.ArtistDraftsRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiStudioVM"

sealed interface AiStudioUiState {
    data object Idle : AiStudioUiState
    data class Loading(val progressMessage: String = "Generating creative production guide...") : AiStudioUiState
    data class Success(
        val generatedConcept: String,
        val category: String,
        val title: String,
        val parametersSummary: String,
        val isOfflineFallback: Boolean = false,
        val engineBadge: String = if (isOfflineFallback) "OFFLINE TEMPLATE FALLBACK" else "GEMINI 3.8 FLASH"
    ) : AiStudioUiState
    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val offerOfflineFallback: Boolean = false
    ) : AiStudioUiState
}

class AiStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val historyDao = database.generationHistoryDao()
    val draftsRepository = ArtistDraftsRepository(database.projectDraftDao(), database.sessionHistoryDao())

    val history: StateFlow<List<GenerationHistoryEntity>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drafts: StateFlow<List<ProjectDraftEntity>> = draftsRepository.allDrafts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<SessionHistoryEntity>> = draftsRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Primary free-tier model: gemini-3.8-flash (General-use flagship model)
    private val primaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
            }
        )
    }

    // High-speed fallback free-tier model: gemini-3.5-flash-lite (Cost/volume sensitive, resilient under high demand)
    private val fallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                temperature = 0.7f
            }
        )
    }

    private val _uiState = MutableStateFlow<AiStudioUiState>(AiStudioUiState.Idle)
    val uiState: StateFlow<AiStudioUiState> = _uiState.asStateFlow()

    // Last requested action for instant retry
    private var lastAction: (() -> Unit)? = null
    private var lastVideoParams: VideoParams? = null

    data class VideoParams(
        val genre: String,
        val songVibe: String,
        val durationSeconds: Int,
        val targetPlatform: String,
        val creativeBrief: String
    )

    fun generatePhotoConcept(
        theme: String,
        modelType: String,
        setting: String,
        lightingStyle: String,
        additionalNotes: String
    ) {
        lastAction = {
            generatePhotoConcept(theme, modelType, setting, lightingStyle, additionalNotes)
        }

        viewModelScope.launch {
            _uiState.value = AiStudioUiState.Loading("Directing Photoshoot Concept with Gemini 3.8 Flash...")
            val prompt = buildString {
                appendLine("You are the Lead Creative Director at STAR ARTIST STUDIO (Talwara Khurd, Sirsa, Haryana).")
                appendLine("Design a professional, high-fashion luxury photoshoot concept package:")
                appendLine("Theme / Occasion: $theme")
                appendLine("Model Style: $modelType")
                appendLine("Setting / Backdrop: $setting")
                appendLine("Lighting Preference: $lightingStyle")
                if (additionalNotes.isNotBlank()) {
                    appendLine("Additional Client Notes: $additionalNotes")
                }
                appendLine()
                appendLine("Provide a comprehensive, production-ready guide with:")
                appendLine("1. Creative Visual Concept & Mood")
                appendLine("2. 4 Specific Dynamic Posing Directions for the Model")
                appendLine("3. Professional Studio Lighting Setup (Key, Rim, Fill, Modifiers)")
                appendLine("4. Recommended Camera Specs & Lenses (e.g. 85mm f/1.4, ISO, Aperture)")
                appendLine("5. Wardrobe, Hair & Makeup Recommendations (Black & Gold luxury aesthetics)")
                appendLine("Notice: This is a professional creative text production guide for on-set photographers.")
            }

            try {
                val text = executeWithExponentialBackoff(prompt, isStoryboard = false)

                if (!text.isNullOrBlank()) {
                    val summary = "$modelType • $setting"
                    val item = GenerationHistoryEntity(
                        category = "photo",
                        title = theme,
                        parametersSummary = summary,
                        content = text,
                        timestamp = System.currentTimeMillis()
                    )
                    historyDao.insertHistory(item)
                    saveToFirebaseHistoryIfConfigured("photo", theme, summary, text, isOfflineFallback = false)

                    _uiState.value = AiStudioUiState.Success(
                        generatedConcept = text,
                        category = "photo",
                        title = theme,
                        parametersSummary = summary,
                        isOfflineFallback = false
                    )
                } else {
                    _uiState.value = AiStudioUiState.Error(
                        message = "Model returned an empty response. Please adjust parameters and try again.",
                        canRetry = true,
                        offerOfflineFallback = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Photo concept generation failed", e)
                _uiState.value = AiStudioUiState.Error(
                    message = mapAiExceptionToUserMessage(e, isVideoStoryboard = false),
                    canRetry = true,
                    offerOfflineFallback = false
                )
            }
        }
    }

    fun generateVideoStoryboard(
        videoGenre: String,
        songVibe: String,
        durationSeconds: Int,
        targetPlatform: String,
        creativeBrief: String
    ) {
        lastVideoParams = VideoParams(videoGenre, songVibe, durationSeconds, targetPlatform, creativeBrief)
        lastAction = {
            generateVideoStoryboard(videoGenre, songVibe, durationSeconds, targetPlatform, creativeBrief)
        }

        viewModelScope.launch {
            _uiState.value = AiStudioUiState.Loading("Writing 4K Cinema Video Storyboard with Gemini...")
            val prompt = buildString {
                appendLine("You are the Chief Cinematographer at STAR ARTIST STUDIO (Talwara Khurd, Sirsa, Haryana).")
                appendLine("Create a professional 4K cinema video production storyboard and shot list:")
                appendLine("Video Genre: $videoGenre")
                appendLine("Mood & Music Vibe: $songVibe")
                appendLine("Target Duration: $durationSeconds seconds")
                appendLine("Platform: $targetPlatform (e.g. Cinema Music Video, Instagram 9:16 Reel, Commercial Ad)")
                if (creativeBrief.isNotBlank()) {
                    appendLine("Creative Brief: $creativeBrief")
                }
                appendLine()
                appendLine("Format with:")
                appendLine("1. Production Logline & Visual Theme")
                appendLine("2. Shot-by-Shot Table (Timecode, Shot Type, Camera Movement, Lighting, Model Action)")
                appendLine("3. Gimbal & Steadicam Camera Trajectories")
                appendLine("4. Color Grading LUT & Post-Production Instructions (Moody Black & Gold Tone)")
                appendLine("5. Audio & Beat Synchronization Cues")
                appendLine("Notice: This is a professional creative text storyboard and shot list for directors. No video file is generated.")
            }

            try {
                val text = executeWithExponentialBackoff(prompt, isStoryboard = true)

                if (!text.isNullOrBlank()) {
                    val summary = "$songVibe • ${durationSeconds}s • $targetPlatform"
                    val item = GenerationHistoryEntity(
                        category = "video",
                        title = videoGenre,
                        parametersSummary = summary,
                        content = text,
                        timestamp = System.currentTimeMillis()
                    )
                    historyDao.insertHistory(item)
                    saveToFirebaseHistoryIfConfigured("video", videoGenre, summary, text, isOfflineFallback = false)

                    _uiState.value = AiStudioUiState.Success(
                        generatedConcept = text,
                        category = "video",
                        title = videoGenre,
                        parametersSummary = summary,
                        isOfflineFallback = false
                    )
                } else {
                    _uiState.value = AiStudioUiState.Error(
                        message = "Model returned an empty storyboard. Please try again or generate using the local Cinema Template Engine.",
                        canRetry = true,
                        offerOfflineFallback = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Video storyboard generation failed", e)
                val isHighDemandOrQuota = isHighDemandOrQuotaException(e)
                _uiState.value = AiStudioUiState.Error(
                    message = mapAiExceptionToUserMessage(e, isVideoStoryboard = true),
                    canRetry = true,
                    offerOfflineFallback = isHighDemandOrQuota || e is TimeoutCancellationException
                )
            }
        }
    }

    /**
     * Executes generation with exponential backoff retry across supported free-tier Gemini models.
     * Attempts primaryModel first, then switches to resilient fallbackModel (gemini-3.5-flash-lite).
     */
    private suspend fun executeWithExponentialBackoff(
        prompt: String,
        isStoryboard: Boolean
    ): String? {
        val maxAttempts = 3
        var currentAttempt = 1
        var lastException: Exception? = null

        while (currentAttempt <= maxAttempts) {
            val isFirstAttempt = currentAttempt == 1
            val modelToUse = if (isFirstAttempt) primaryModel else fallbackModel
            val modelName = if (isFirstAttempt) "Gemini 3.8 Flash" else "Gemini 3.5 Flash Lite"

            try {
                _uiState.value = AiStudioUiState.Loading(
                    if (isFirstAttempt) {
                        if (isStoryboard) "Writing 4K Cinema Video Storyboard with $modelName..."
                        else "Directing Photoshoot Concept with $modelName..."
                    } else {
                        "High server demand. Retrying with high-speed $modelName (Attempt $currentAttempt of $maxAttempts)..."
                    }
                )

                // 45-second timeout per attempt
                val text = withTimeout(45_000L) {
                    modelToUse.generateContent(prompt).text
                }

                if (!text.isNullOrBlank()) {
                    return text
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Inference attempt $currentAttempt failed with $modelName: ${e.message}")

                val isHighDemandOrQuota = isHighDemandOrQuotaException(e)
                if (currentAttempt < maxAttempts && (isHighDemandOrQuota || e is TimeoutCancellationException)) {
                    val delayMs = (1500L * (1 shl (currentAttempt - 1))) + (100L..400L).random()
                    _uiState.value = AiStudioUiState.Loading(
                        "AI servers busy. Backing off for ${delayMs / 1000}s (Attempt ${currentAttempt + 1} of $maxAttempts)..."
                    )
                    delay(delayMs)
                } else if (!isHighDemandOrQuota && e !is TimeoutCancellationException) {
                    // Fatal client/auth error, break out of retry loop
                    break
                }
            }
            currentAttempt++
        }

        throw lastException ?: IllegalStateException("Generation failed after $maxAttempts attempts.")
    }

    /**
     * Functional local template-based storyboard generator.
     * Produces a comprehensive scene-by-scene cinematography shot list with camera angles,
     * lighting setups, durations, and transitions without calling external AI APIs.
     * Clearly labeled as OFFLINE TEMPLATE FALLBACK.
     */
    fun generateLocalVideoStoryboard(
        videoGenre: String,
        songVibe: String,
        durationSeconds: Int,
        targetPlatform: String,
        creativeBrief: String
    ) {
        lastVideoParams = VideoParams(videoGenre, songVibe, durationSeconds, targetPlatform, creativeBrief)
        viewModelScope.launch {
            _uiState.value = AiStudioUiState.Loading("Generating Cinema Director Storyboard (Local Template Engine)...")
            delay(300L) // Visual cue of production compilation

            val storyboardText = LocalStoryboardGenerator.generate(
                videoGenre = videoGenre,
                songVibe = songVibe,
                durationSeconds = durationSeconds,
                targetPlatform = targetPlatform,
                creativeBrief = creativeBrief
            )

            val summary = "$songVibe • ${durationSeconds}s • OFFLINE TEMPLATE"
            val item = GenerationHistoryEntity(
                category = "video",
                title = videoGenre,
                parametersSummary = summary,
                content = storyboardText,
                timestamp = System.currentTimeMillis()
            )
            historyDao.insertHistory(item)
            saveToFirebaseHistoryIfConfigured("video", videoGenre, summary, storyboardText, isOfflineFallback = true)

            _uiState.value = AiStudioUiState.Success(
                generatedConcept = storyboardText,
                category = "video",
                title = videoGenre,
                parametersSummary = summary,
                isOfflineFallback = true
            )
        }
    }

    /**
     * Fallback triggered directly from error state using previous parameters.
     */
    fun triggerLocalStoryboardFallback() {
        val params = lastVideoParams ?: VideoParams(
            genre = "Cinematic Punjabi/Hindi Music Video",
            songVibe = "High-Energy Bass & Royal Aesthetic",
            durationSeconds = 30,
            targetPlatform = "4K Instagram Reel (9:16) & YouTube 4K",
            creativeBrief = "Signature black & gold luxury theme"
        )
        generateLocalVideoStoryboard(
            videoGenre = params.genre,
            songVibe = params.songVibe,
            durationSeconds = params.durationSeconds,
            targetPlatform = params.targetPlatform,
            creativeBrief = params.creativeBrief
        )
    }

    /**
     * Saves generation history to Firebase Firestore if the user is authenticated
     * and Firebase is configured. Safely handles errors without crashing or blocking local flow.
     */
    private fun saveToFirebaseHistoryIfConfigured(
        category: String,
        title: String,
        parametersSummary: String,
        content: String,
        isOfflineFallback: Boolean
    ) {
        try {
            val user = Firebase.auth.currentUser ?: return
            val app = getApplication<Application>()
            val dbId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)

            val historyDoc = mapOf(
                "userId" to user.uid,
                "category" to category,
                "title" to title,
                "parametersSummary" to parametersSummary,
                "content" to content,
                "isOfflineFallback" to isOfflineFallback,
                "timestamp" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(user.uid).collection("history").add(historyDoc)
                .addOnSuccessListener {
                    Log.d(TAG, "Storyboard synced to Firebase history.")
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "Firebase history sync skipped (optional): ${e.message}")
                }
        } catch (e: Exception) {
            Log.d(TAG, "Firebase history skipped (not configured): ${e.message}")
        }
    }

    fun retryLastRequest() {
        lastAction?.invoke()
    }

    fun deleteHistoryItem(item: GenerationHistoryEntity) {
        viewModelScope.launch {
            historyDao.deleteHistory(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyDao.clearAll()
        }
    }

    fun resetState() {
        _uiState.value = AiStudioUiState.Idle
    }

    // --- ARTIST PROJECT DRAFTS & LOCAL SESSION HISTORY ---

    fun saveProjectDraft(
        title: String,
        projectType: String,
        clientOrModelName: String = "",
        conceptNotes: String = "",
        lightingSetupNotes: String = "",
        cameraGearNotes: String = "",
        targetShootDate: String = "",
        estimatedBudgetInr: Long = 0L,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val draft = ProjectDraftEntity(
                draftTitle = title.trim(),
                projectType = projectType,
                clientOrModelName = clientOrModelName.trim(),
                conceptNotes = conceptNotes.trim(),
                lightingSetupNotes = lightingSetupNotes.trim(),
                cameraGearNotes = cameraGearNotes.trim(),
                targetShootDate = targetShootDate.trim(),
                estimatedBudgetInr = estimatedBudgetInr
            )
            val id = draftsRepository.saveDraft(draft)
            onSuccess(id)
        }
    }

    fun toggleFavoriteDraft(draft: ProjectDraftEntity) {
        viewModelScope.launch {
            draftsRepository.toggleFavorite(draft)
        }
    }

    fun deleteProjectDraft(draft: ProjectDraftEntity) {
        viewModelScope.launch {
            draftsRepository.deleteDraft(draft)
        }
    }

    fun clearAllProjectDrafts() {
        viewModelScope.launch {
            draftsRepository.clearAllDrafts()
        }
    }

    fun logLocalSession(
        sessionType: String,
        sessionTitle: String,
        summaryDetails: String,
        durationSeconds: Long = 0L
    ) {
        viewModelScope.launch {
            draftsRepository.logSession(sessionType, sessionTitle, summaryDetails, durationSeconds)
        }
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            draftsRepository.clearAllSessions()
        }
    }

    private fun isHighDemandOrQuotaException(e: Throwable): Boolean {
        if (e is TimeoutCancellationException) return true
        val msg = e.localizedMessage ?: e.message ?: ""
        return msg.contains("429", ignoreCase = true) ||
               msg.contains("503", ignoreCase = true) ||
               msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
               msg.contains("quota", ignoreCase = true) ||
               msg.contains("overloaded", ignoreCase = true) ||
               msg.contains("high demand", ignoreCase = true) ||
               msg.contains("UNAVAILABLE", ignoreCase = true) ||
               msg.contains("try again later", ignoreCase = true)
    }

    private fun mapAiExceptionToUserMessage(e: Throwable, isVideoStoryboard: Boolean): String {
        val raw = e.localizedMessage ?: e.message ?: "Unknown error"
        return when {
            isHighDemandOrQuotaException(e) ->
                "The live Gemini model is currently experiencing high demand on Google servers (or free-tier quota is exhausted). You can tap 'Retry with Backoff' or tap 'Generate with Cinema Template Engine (Offline Fallback)' below to instantly generate a complete scene-by-scene 4K storyboard."
            e is TimeoutCancellationException ->
                "The AI generation request timed out after multiple attempts due to slow network or high server latency. Tap 'Retry with Backoff' or generate instantly with the offline Cinema Template Engine."
            raw.contains("AppCheck", ignoreCase = true) || raw.contains("403", ignoreCase = true) || raw.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Firebase AI Logic App Check attestation is pending backend registration. Use the Cinema Template Engine (Offline Fallback) for instant director shot lists."
            raw.contains("UnknownHost", ignoreCase = true) || raw.contains("SocketTimeout", ignoreCase = true) || raw.contains("ConnectException", ignoreCase = true) ->
                "Network connection error. Please verify your internet connection. You can also generate offline using the Cinema Template Engine."
            else -> "AI service notice: $raw. You can use the offline Cinema Template Engine to generate a complete storyboard."
        }
    }
}
