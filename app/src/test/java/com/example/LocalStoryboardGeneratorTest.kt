package com.example

import com.example.ui.ai.LocalStoryboardGenerator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalStoryboardGeneratorTest {

    @Test
    fun generate_producesSceneBySceneCinematographyBreakdown() {
        val result = LocalStoryboardGenerator.generate(
            videoGenre = "Cinematic Punjabi/Hindi Music Video",
            songVibe = "High-Energy Bass & Royal Aesthetic",
            durationSeconds = 30,
            targetPlatform = "4K Instagram Reel (9:16)",
            creativeBrief = "Royal black & gold theme with heavy bass drops"
        )

        // Must be clearly labeled as offline template fallback
        assertTrue("Must be clearly labeled as offline fallback", result.contains("OFFLINE TEMPLATE FALLBACK"))
        assertTrue("Must declare not live AI generated", result.contains("NOT LIVE AI GENERATED"))

        // Must explicitly state no video file is generated
        assertTrue("Must state no video file is generated", result.contains("No video file (MP4/MOV) is generated or rendered"))

        // Must include scene-by-scene descriptions, camera movements, and lighting
        assertTrue("Must contain SCENE 1", result.contains("SCENE 1"))
        assertTrue("Must contain Shot Type & Framing", result.contains("Shot Type & Framing"))
        assertTrue("Must contain Camera Movement", result.contains("Camera Movement"))
        assertTrue("Must contain Lighting & Atmosphere", result.contains("Lighting & Atmosphere"))
        assertTrue("Must contain Transition to Next", result.contains("Transition to Next"))
        assertTrue("Must contain Audio & Beat Sync", result.contains("Audio & Beat Sync"))

        // Must contain color grading LUT and specifications
        assertTrue("Must contain color grading info", result.contains("COLOR GRADING"))
        assertTrue("Must contain camera trajectory instructions", result.contains("CAMERA TRAJECTORIES"))

        // Studio invariants
        assertTrue("Must contain studio location", result.contains("Talwara Khurd"))
        assertTrue("Must contain studio mobile number", result.contains("9812890081"))
        assertTrue("Must contain studio WhatsApp number", result.contains("9541024098"))
    }

    @Test
    fun generate_adaptsToDifferentDurations() {
        val shortResult = LocalStoryboardGenerator.generate(
            videoGenre = "Teaser",
            songVibe = "Fast",
            durationSeconds = 15,
            targetPlatform = "Reel",
            creativeBrief = ""
        )
        val longResult = LocalStoryboardGenerator.generate(
            videoGenre = "Full Film",
            songVibe = "Dramatic",
            durationSeconds = 60,
            targetPlatform = "Cinema 4K",
            creativeBrief = ""
        )

        assertTrue(shortResult.contains("SCENE 1"))
        assertTrue(shortResult.contains("SCENE 3"))
        assertFalse(shortResult.contains("SCENE 7"))

        assertTrue(longResult.contains("SCENE 1"))
        assertTrue(longResult.contains("SCENE 7"))
    }
}
