package com.example.data.ai

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig

/**
 * Abstraction for generating and remixing concept art bitmaps via Firebase AI Logic.
 */
interface ConceptArtGenerator {
    suspend fun generateConceptBitmap(prompt: String, modelName: String): Bitmap?
    suspend fun remixConceptBitmap(baseBitmap: Bitmap, editInstruction: String, modelName: String): Bitmap?
}

/**
 * Production implementation backed by Firebase AI Logic Gemini image generation models
 * ("gemini-nano-banana-2.1" by default, or "gemini-3-pro-image").
 */
class FirebaseConceptArtGenerator : ConceptArtGenerator {

    private val nanoBananaFastModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NANO_BANANA_FAST,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    private val nanoBananaProModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NANO_BANANA_PRO,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    private fun resolveModel(modelName: String): GenerativeModel = when (modelName) {
        MODEL_NANO_BANANA_PRO -> nanoBananaProModel
        else -> nanoBananaFastModel
    }

    override suspend fun generateConceptBitmap(prompt: String, modelName: String): Bitmap? {
        val response = resolveModel(modelName).generateContent(prompt)
        return response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
            ?.image
    }

    override suspend fun remixConceptBitmap(
        baseBitmap: Bitmap,
        editInstruction: String,
        modelName: String
    ): Bitmap? {
        val response = resolveModel(modelName).generateContent(
            content {
                image(baseBitmap)
                text(editInstruction)
            }
        )
        return response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
            ?.image
    }

    companion object {
        const val MODEL_NANO_BANANA_FAST = "gemini-nano-banana-2.1"
        const val MODEL_NANO_BANANA_PRO = "gemini-3-pro-image"
    }
}
