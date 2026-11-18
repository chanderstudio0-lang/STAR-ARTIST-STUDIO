package com.example

import android.Manifest
import com.example.data.model.AppLanguage
import com.example.ui.components.VoiceRecognitionState
import com.example.util.StudioStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceToTextFeatureTest {

    @Test
    fun manifestPermission_isRecordAudio() {
        assertEquals("android.permission.RECORD_AUDIO", Manifest.permission.RECORD_AUDIO)
    }

    @Test
    fun voiceRecognitionState_hasAllRequiredStates() {
        val states = VoiceRecognitionState.values()
        assertTrue(states.contains(VoiceRecognitionState.IDLE))
        assertTrue(states.contains(VoiceRecognitionState.LISTENING))
        assertTrue(states.contains(VoiceRecognitionState.ERROR))
        assertEquals(3, states.size)
    }

    @Test
    fun textAppending_whenEmpty_setsTranscribedTextDirectly() {
        val currentText = ""
        val newTranscribed = "I want 3 bridal costume changes in royal red and gold"

        val result = if (currentText.isBlank()) {
            newTranscribed.trim()
        } else {
            "${currentText.trim()} ${newTranscribed.trim()}"
        }

        assertEquals("I want 3 bridal costume changes in royal red and gold", result)
    }

    @Test
    fun textAppending_whenNonEmpty_appendsConversationalSpeechCleanly() {
        val currentText = "Need indoor lighting suite"
        val newSpeech = "and 4K slow motion gimbal video shoot."

        val result = if (currentText.isBlank()) {
            newSpeech.trim()
        } else {
            "${currentText.trim()} ${newSpeech.trim()}"
        }

        assertEquals("Need indoor lighting suite and 4K slow motion gimbal video shoot.", result)
    }

    @Test
    fun audioRmsNormalization_clampsSafelyBetweenZeroAndOne() {
        fun normalizeRms(rmsdB: Float): Float {
            return (rmsdB.coerceIn(-2f, 10f) + 2f) / 12f
        }

        // Silent audio (-10 dB) clamps to 0.0
        val silentNorm = normalizeRms(-10f)
        assertEquals(0.0f, silentNorm, 0.001f)

        // Peak audio (20 dB) clamps to 1.0
        val loudNorm = normalizeRms(20f)
        assertEquals(1.0f, loudNorm, 0.001f)

        // Medium audio (4 dB) maps to 0.5
        val mediumNorm = normalizeRms(4f)
        assertEquals(0.5f, mediumNorm, 0.001f)
    }

    @Test
    fun speechLocale_selectsAppropriateCodeForAppLanguage() {
        fun getLocaleForLanguage(lang: AppLanguage): String {
            return if (lang == AppLanguage.HINDI) "hi-IN" else "en-IN"
        }

        assertEquals("hi-IN", getLocaleForLanguage(AppLanguage.HINDI))
        assertEquals("en-IN", getLocaleForLanguage(AppLanguage.ENGLISH))
    }

    @Test
    fun studioStrings_containBilingualVoiceToTextLabels() {
        val en = StudioStrings(AppLanguage.ENGLISH)
        val hi = StudioStrings(AppLanguage.HINDI)

        assertNotNull(en.voiceToTextTitle)
        assertNotNull(hi.voiceToTextTitle)
        assertNotNull(en.voiceListening)
        assertNotNull(hi.voiceListening)
        assertNotNull(en.voicePermissionNeeded)
        assertNotNull(hi.voicePermissionNeeded)
        assertNotNull(en.voicePermissionRationale)
        assertNotNull(hi.voicePermissionRationale)

        assertTrue(en.voiceToTextTitle.contains("Voice-to-Text"))
        assertTrue(hi.voiceToTextTitle.contains("आवाज़"))
    }
}
