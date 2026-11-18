package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.data.local.dao.MoodBoardConceptDao
import com.example.data.local.dao.MoodBoardReferenceDao
import com.example.data.local.entity.MoodBoardConceptEntity
import com.example.data.local.entity.MoodBoardReferenceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Mediates local image file persistence and Room metadata for AI-generated project mood boards
 * alongside visual reference photos organized per ongoing studio project.
 */
class MoodBoardRepository(
    private val dao: MoodBoardConceptDao,
    private val appContext: Context,
    private val referenceDao: MoodBoardReferenceDao? = null
) {
    val allConcepts: Flow<List<MoodBoardConceptEntity>> = dao.observeAllConcepts()

    val favoriteConcepts: Flow<List<MoodBoardConceptEntity>> = dao.observeFavoriteConcepts()

    val allReferences: Flow<List<MoodBoardReferenceEntity>> =
        referenceDao?.observeAllReferences() ?: flowOf(emptyList())

    fun observeByProjectType(projectType: String): Flow<List<MoodBoardConceptEntity>> =
        dao.observeConceptsByType(projectType)

    fun observeConceptsForProject(projectId: Long): Flow<List<MoodBoardConceptEntity>> =
        dao.observeConceptsForProject(projectId)

    fun observeReferencesForProject(projectId: Long): Flow<List<MoodBoardReferenceEntity>> =
        referenceDao?.observeReferencesForProject(projectId) ?: flowOf(emptyList())

    suspend fun getConceptById(id: Long): MoodBoardConceptEntity? = dao.getConceptById(id)

    suspend fun getReferenceById(id: Long): MoodBoardReferenceEntity? = referenceDao?.getReferenceById(id)

    suspend fun saveGeneratedConcept(
        bitmap: Bitmap,
        boardTitle: String,
        projectType: String,
        prompt: String,
        visualStyle: String,
        lightingSetup: String,
        aspectRatioLabel: String,
        modelUsed: String,
        colorPaletteTags: String,
        linkedDraftId: Long? = null,
        sessionProjectId: Long? = null,
        boardSectionTag: String = "Concept Art"
    ): MoodBoardConceptEntity = withContext(Dispatchers.IO) {
        val dir = File(appContext.filesDir, "moodboard_art").apply {
            if (!exists()) mkdirs()
        }
        val now = System.currentTimeMillis()
        val imageFile = File(dir, "moodboard_${now}_${(100..999).random()}.jpg")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            out.flush()
        }

        val entity = MoodBoardConceptEntity(
            boardTitle = boardTitle.trim().ifEmpty { "$projectType Mood Board" },
            projectType = projectType.trim().ifEmpty { "Photography" },
            prompt = prompt.trim(),
            visualStyle = visualStyle.trim(),
            lightingSetup = lightingSetup.trim(),
            aspectRatioLabel = aspectRatioLabel.trim(),
            modelUsed = modelUsed,
            localImagePath = imageFile.absolutePath,
            colorPaletteTags = colorPaletteTags.trim().ifEmpty { "Obsidian Black, Burnished Gold" },
            linkedDraftId = linkedDraftId,
            sessionProjectId = sessionProjectId,
            boardSectionTag = boardSectionTag.trim().ifEmpty { "Concept Art" },
            createdAt = now
        )
        val insertedId = dao.insertConcept(entity)
        entity.copy(id = insertedId)
    }

    suspend fun toggleFavorite(concept: MoodBoardConceptEntity) {
        dao.updateConcept(concept.copy(isFavorite = !concept.isFavorite))
    }

    suspend fun linkToDraft(concept: MoodBoardConceptEntity, draftId: Long) {
        dao.updateConcept(concept.copy(linkedDraftId = draftId))
    }

    suspend fun assignConceptToProject(concept: MoodBoardConceptEntity, projectId: Long?) {
        dao.updateConcept(concept.copy(sessionProjectId = projectId))
    }

    suspend fun updateConceptOrganization(
        concept: MoodBoardConceptEntity,
        sectionTag: String,
        sessionProjectId: Long?
    ) {
        dao.updateConcept(
            concept.copy(
                boardSectionTag = sectionTag.trim().ifEmpty { "Concept Art" },
                sessionProjectId = sessionProjectId
            )
        )
    }

    suspend fun deleteConcept(concept: MoodBoardConceptEntity) {
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(concept.localImagePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            dao.deleteConcept(concept)
        }
    }

    suspend fun addReferencePhotoFromUri(
        uri: Uri,
        title: String,
        sectionTag: String,
        notes: String = "",
        sessionProjectId: Long? = null,
        colorSwatches: String = "Warm Gold, Deep Shadow"
    ): MoodBoardReferenceEntity = withContext(Dispatchers.IO) {
        val decoded = appContext.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalArgumentException("Could not read selected image file.")

        val persistedPath = writeReferenceBitmapToStorage(decoded)
        if (decoded.isRecycled.not()) {
            decoded.recycle()
        }
        persistReferenceRecord(
            imagePathOrUrl = persistedPath,
            title = title,
            sectionTag = sectionTag,
            sourceType = MoodBoardReferenceEntity.SOURCE_GALLERY_PHOTO,
            notes = notes,
            sessionProjectId = sessionProjectId,
            colorSwatches = colorSwatches
        )
    }

    suspend fun addReferencePhotoFromBitmap(
        bitmap: Bitmap,
        title: String,
        sectionTag: String,
        sourceType: String = MoodBoardReferenceEntity.SOURCE_CAMERA_SHOT,
        notes: String = "",
        sessionProjectId: Long? = null,
        colorSwatches: String = "Studio Key Light, Charcoal"
    ): MoodBoardReferenceEntity = withContext(Dispatchers.IO) {
        val persistedPath = writeReferenceBitmapToStorage(bitmap)
        persistReferenceRecord(
            imagePathOrUrl = persistedPath,
            title = title,
            sectionTag = sectionTag,
            sourceType = sourceType,
            notes = notes,
            sessionProjectId = sessionProjectId,
            colorSwatches = colorSwatches
        )
    }

    suspend fun addReferenceFromUrlOrDataUrl(
        imagePathOrUrl: String,
        title: String,
        sectionTag: String,
        sourceType: String,
        notes: String = "",
        sessionProjectId: Long? = null,
        colorSwatches: String = "Editorial Gold, Slate"
    ): MoodBoardReferenceEntity = withContext(Dispatchers.IO) {
        val trimmed = imagePathOrUrl.trim()
        require(trimmed.isNotEmpty()) { "Reference image source cannot be empty." }

        val resolvedPathOrUrl = if (trimmed.startsWith("data:image", ignoreCase = true) && trimmed.contains(",")) {
            val base64Part = trimmed.substringAfter(",")
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                val path = writeReferenceBitmapToStorage(bitmap)
                if (!bitmap.isRecycled) bitmap.recycle()
                path
            } else {
                trimmed
            }
        } else {
            trimmed
        }

        persistReferenceRecord(
            imagePathOrUrl = resolvedPathOrUrl,
            title = title,
            sectionTag = sectionTag,
            sourceType = sourceType,
            notes = notes,
            sessionProjectId = sessionProjectId,
            colorSwatches = colorSwatches
        )
    }

    suspend fun toggleReferencePin(reference: MoodBoardReferenceEntity) {
        referenceDao?.updateReference(reference.copy(isPinned = !reference.isPinned))
    }

    suspend fun assignReferenceToProject(reference: MoodBoardReferenceEntity, projectId: Long?) {
        referenceDao?.updateReference(reference.copy(sessionProjectId = projectId))
    }

    suspend fun updateReferenceMetadata(
        reference: MoodBoardReferenceEntity,
        title: String,
        sectionTag: String,
        notes: String,
        colorSwatches: String,
        sessionProjectId: Long?
    ) {
        referenceDao?.updateReference(
            reference.copy(
                title = title.trim().ifEmpty { reference.title },
                sectionTag = sectionTag.trim().ifEmpty { reference.sectionTag },
                notes = notes.trim(),
                colorSwatches = colorSwatches.trim().ifEmpty { reference.colorSwatches },
                sessionProjectId = sessionProjectId
            )
        )
    }

    suspend fun deleteReference(reference: MoodBoardReferenceEntity) {
        withContext(Dispatchers.IO) {
            runCatching {
                val refsDirPath = File(appContext.filesDir, "moodboard_refs").absolutePath
                val file = File(reference.imagePathOrUrl)
                if (file.absolutePath.startsWith(refsDirPath) && file.exists()) {
                    file.delete()
                }
            }
            referenceDao?.deleteReference(reference)
        }
    }

    private suspend fun persistReferenceRecord(
        imagePathOrUrl: String,
        title: String,
        sectionTag: String,
        sourceType: String,
        notes: String,
        sessionProjectId: Long?,
        colorSwatches: String
    ): MoodBoardReferenceEntity {
        val refDao = requireNotNull(referenceDao) {
            "MoodBoardReferenceDao must be provided to store reference photos."
        }
        val entity = MoodBoardReferenceEntity(
            sessionProjectId = sessionProjectId,
            title = title.trim().ifEmpty { "Studio Reference Shot" },
            sectionTag = sectionTag.trim().ifEmpty { "Lighting Ref" },
            sourceType = sourceType,
            imagePathOrUrl = imagePathOrUrl,
            notes = notes.trim(),
            colorSwatches = colorSwatches.trim().ifEmpty { "Warm Gold, Deep Shadow" },
            createdAt = System.currentTimeMillis()
        )
        val insertedId = refDao.insertReference(entity)
        return entity.copy(id = insertedId)
    }

    private fun writeReferenceBitmapToStorage(source: Bitmap): String {
        val dir = File(appContext.filesDir, "moodboard_refs").apply {
            if (!exists()) mkdirs()
        }
        val maxDimension = max(source.width, source.height)
        val scaled = if (maxDimension > 1280) {
            val scale = 1280f / maxDimension.toFloat()
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).roundToInt().coerceAtLeast(1),
                (source.height * scale).roundToInt().coerceAtLeast(1),
                true
            )
        } else {
            source
        }

        val now = System.currentTimeMillis()
        val file = File(dir, "ref_${now}_${(100..999).random()}.jpg")
        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            out.flush()
        }
        if (scaled !== source && !scaled.isRecycled) {
            scaled.recycle()
        }
        return file.absolutePath
    }
}
