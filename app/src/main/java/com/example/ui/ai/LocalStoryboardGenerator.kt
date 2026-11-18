package com.example.ui.ai

import com.example.util.StudioConstants
import com.example.util.StudioContactDetails
import kotlin.math.max

/**
 * Functional local cinematography template-based storyboard generator for STAR ARTIST STUDIO.
 * Used when the live Gemini AI service is experiencing high demand, quota limits,
 * or when offline.
 *
 * Provides a production-grade, scene-by-scene cinematography shot list with
 * camera angles, lighting plots, exact durations, and transitions.
 *
 * NOTE: Clearly labeled as OFFLINE TEMPLATE FALLBACK (non-AI). Never fakes video file rendering.
 */
object LocalStoryboardGenerator {

    data class StoryboardScene(
        val sceneNumber: Int,
        val timecode: String,
        val durationSeconds: Int,
        val shotType: String,
        val cameraMovement: String,
        val sceneDescription: String,
        val modelAction: String,
        val lightingSetup: String,
        val transition: String,
        val audioCue: String
    )

    fun generate(
        videoGenre: String,
        songVibe: String,
        durationSeconds: Int,
        targetPlatform: String,
        creativeBrief: String
    ): String {
        val totalSec = max(15, durationSeconds)
        // Determine number of scenes (each scene between 4 to 8 seconds)
        val sceneCount = when {
            totalSec <= 20 -> 3
            totalSec <= 35 -> 5
            totalSec <= 60 -> 7
            totalSec <= 90 -> 9
            else -> 12
        }

        val scenes = buildScenes(
            videoGenre = videoGenre,
            songVibe = songVibe,
            totalDuration = totalSec,
            sceneCount = sceneCount,
            brief = creativeBrief
        )

        return buildString {
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine("⚡ STAR ARTIST STUDIO - 4K CINEMATOGRAPHY STORYBOARD")
            appendLine("MODE: [OFFLINE TEMPLATE FALLBACK - NOT LIVE AI GENERATED]")
            appendLine("Studio Location: ${StudioContactDetails.ADDRESS}")
            appendLine("Technical Notice: Text-based creative director shot list & production guide.")
            appendLine("NOTICE: No video file (MP4/MOV) is generated or rendered.")
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine()
            appendLine("■ 1. PRODUCTION OVERVIEW & SPECIFICATIONS")
            appendLine("• Project Title: $videoGenre")
            appendLine("• Musical Vibe & Atmosphere: $songVibe")
            appendLine("• Target Duration: $totalSec Seconds (${sceneCount} Directed Scenes)")
            appendLine("• Delivery Aspect Ratio: $targetPlatform")
            if (creativeBrief.isNotBlank()) {
                appendLine("• Client Creative Brief: $creativeBrief")
            }
            appendLine("• Studio Signature Aesthetic: Deep Velvet Black, Polished Gold Reflections & Cinematic Hazer")
            appendLine("• Master Camera Rig: RED V-Raptor / Sony FX6 on DJI Ronin RS3 Pro Gimbal")
            appendLine("• Lenses: Master Anamorphic 35mm T1.4 & 85mm T1.5 High-Speed Primes")
            appendLine()
            appendLine("■ 2. SCENE-BY-SCENE CINEMA SHOT LIST (${sceneCount} SCENES)")
            appendLine("───────────────────────────────────────────────────────────────")

            scenes.forEach { scene ->
                appendLine("SCENE ${scene.sceneNumber} [${scene.timecode} | ${scene.durationSeconds}s]")
                appendLine("  • Shot Type & Framing: ${scene.shotType}")
                appendLine("  • Camera Movement: ${scene.cameraMovement}")
                appendLine("  • Scene Setting & Action: ${scene.sceneDescription}")
                appendLine("  • Talent & Model Direction: ${scene.modelAction}")
                appendLine("  • Lighting & Atmosphere: ${scene.lightingSetup}")
                appendLine("  • Audio & Beat Sync: ${scene.audioCue}")
                appendLine("  • Transition to Next: ${scene.transition}")
                appendLine("───────────────────────────────────────────────────────────────")
            }

            appendLine()
            appendLine("■ 3. CAMERA TRAJECTORIES & GIMBAL OPERATING INSTRUCTIONS")
            appendLine("• Intro Trajectory: Low-ground sweeping tilt-up establishing model silhouette against back-lit gold rim.")
            appendLine("• Dynamic Verse/Drop: 180-degree continuous orbital push-in at 60fps slow-motion for dramatic hair and garment flutter.")
            appendLine("• Climax / Hook: Fast push-in with simultaneous focal pull to model's eyes followed by high-speed whip pan.")
            appendLine()
            appendLine("■ 4. COLOR GRADING (LUT) & POST-PRODUCTION PALETTE")
            appendLine("• LUT Name: StarArtist_GoldNoir_3DLUT.cube")
            appendLine("• Shadows: Crushed midnight blacks with zero green noise tint.")
            appendLine("• Midtones: Rich amber-gold warmth on skin tones (3200K key accent).")
            appendLine("• Highlights: Soft halation roll-off with 1/8 Black Pro-Mist filter texture.")
            appendLine("• Master Deliverable: ProRes 422 HQ / 4K UHD Master at 24fps.")
            appendLine()
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine("Directed & Designed for Production at STAR ARTIST STUDIO")
            appendLine("Talwara Khurd, Sirsa, Haryana | Call: ${StudioContactDetails.MOBILE_NUMBER} | WhatsApp: ${StudioContactDetails.WHATSAPP_NUMBER}")
            appendLine("═══════════════════════════════════════════════════════════════")
        }
    }

    private fun buildScenes(
        videoGenre: String,
        songVibe: String,
        totalDuration: Int,
        sceneCount: Int,
        brief: String
    ): List<StoryboardScene> {
        val baseDuration = totalDuration / sceneCount
        var remainder = totalDuration % sceneCount
        var currentTime = 0

        val shotTemplates = getGenreShotTemplates(videoGenre)

        return (0 until sceneCount).map { i ->
            val sceneDuration = baseDuration + if (remainder > 0) { remainder--; 1 } else 0
            val startTime = currentTime
            val endTime = currentTime + sceneDuration
            currentTime = endTime

            val startTc = formatTimecode(startTime)
            val endTc = formatTimecode(endTime)
            val template = shotTemplates[i % shotTemplates.size]

            StoryboardScene(
                sceneNumber = i + 1,
                timecode = "$startTc - $endTc",
                durationSeconds = sceneDuration,
                shotType = template.shotType,
                cameraMovement = template.cameraMovement,
                sceneDescription = "${template.description} (Vibe: $songVibe${if (brief.isNotBlank()) " | Ref: $brief" else ""})",
                modelAction = template.modelAction,
                lightingSetup = template.lightingSetup,
                transition = if (i == sceneCount - 1) "Fade to Black with Studio Gold Emblem Logo" else template.transition,
                audioCue = template.audioCue
            )
        }
    }

    private fun formatTimecode(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    private data class TemplateShot(
        val shotType: String,
        val cameraMovement: String,
        val description: String,
        val modelAction: String,
        val lightingSetup: String,
        val transition: String,
        val audioCue: String
    )

    private fun getGenreShotTemplates(genre: String): List<TemplateShot> {
        return listOf(
            TemplateShot(
                shotType = "Extreme Wide Establishing Shot (EWS) - Low Angle",
                cameraMovement = "Slow forward dolly push on track through atmospheric haze",
                description = "Dark studio floor with reflective gloss finish; golden particle projection in background",
                modelAction = "Model stands motionless in regal couture posture, back turned partially to lens",
                lightingSetup = "Aputure 1200D Pro backlit rim spotlight + dual warm gold strip softboxes at 45 degrees",
                transition = "Quick match cut on beat drop to Medium Hero Shot",
                audioCue = "Intro ambient riser swells; sub-bass hit triggers cut"
            ),
            TemplateShot(
                shotType = "Medium Hero Close-Up (MCU) - Eye Level Dutch Angle",
                cameraMovement = "Ronin RS3 Pro gimbal 90-degree lateral arc around model",
                description = "Dramatic spotlight catches metallic gold embellishments and jewelry accents",
                modelAction = "Model delivers confident, intense eye contact with lens, slow head turn",
                lightingSetup = "Key: 150cm Octabox with honey grid at 3200K warm; Fill: Negative fill black velvet flag",
                transition = "Whip pan right following talent gesture",
                audioCue = "Main vocal entrance; kick drum sync"
            ),
            TemplateShot(
                shotType = "Low-Angle Tracking Full Body Shot",
                cameraMovement = "Backward tracking gimbal walk matching model pace",
                description = "Model strides down the studio runway bathed in warm golden rim backlight",
                modelAction = "Power walk towards camera; flowing train or jacket catches air draft from studio wind machine",
                lightingSetup = "Overhead 600c RGB LED wash pulsing soft warm amber; dual floor uplights",
                transition = "Speed ramp whip-zoom into extreme macro detail",
                audioCue = "Heavy 808 bass rhythm drive"
            ),
            TemplateShot(
                shotType = "Extreme Close-Up (ECU) - Macro 100mm Lens",
                cameraMovement = "Micro slow-motion tilt over facial expression and jewelry",
                description = "Ultra high-definition texture shot focusing on gaze and couture details",
                modelAction = "Subtle lip movement or hand adjusting gold lapel / jewelry piece",
                lightingSetup = "Edge light bounce reflector + pinpoint snoot focusing sharp specular highlight on eyes",
                transition = "Light leak flash transition with gold optical flare",
                audioCue = "Vocal inflection inflection point; syncopated hi-hat pattern"
            ),
            TemplateShot(
                shotType = "High-Angle Overhead Top-Down Shot (Bird's Eye)",
                cameraMovement = "Jib crane descending slowly with rotational spin",
                description = "Geometric floor pattern surrounded by warm ambient haze and gold prop accents",
                modelAction = "Model positioned dynamically in center; fluid movement or garment spin",
                lightingSetup = "Ring light cluster overhead + 4 corner tungsten soft lights",
                transition = "Cross dissolve on tempo drop",
                audioCue = "Bridge section / instrumental solo transition"
            ),
            TemplateShot(
                shotType = "Profile Silhouette Medium Shot with Smoke Texture",
                cameraMovement = "Static camera with rack focus from foreground haze to model profile",
                description = "High-contrast rim illumination cutting sharp edge against absolute studio darkness",
                modelAction = "Model raises hand slowly; dramatic breath in haze",
                lightingSetup = "Sole 1200W HMI Fresnel with amber gel placed directly behind talent",
                transition = "Snap cut on drum snare",
                audioCue = "Snare hit and pause in percussion"
            ),
            TemplateShot(
                shotType = "Dynamic 360-Degree Orbital Tracking Shot",
                cameraMovement = "High-speed circular gimbal orbit at 120fps slow-motion capture",
                description = "Full studio space alive with dramatic golden backlights and cinematic lens flares",
                modelAction = "Dynamic 360 spin with fabric movement and bold charismatic gesture",
                lightingSetup = "Full studio concert lighting rig; warm gold backlight array + soft wrap key",
                transition = "Motion blur whip pan to final climax hero shot",
                audioCue = "Peak chorus / drop crescendo"
            ),
            TemplateShot(
                shotType = "Final Hero Climax - Medium Low-Angle Power Stance",
                cameraMovement = "Slow dolly push-in decelerating to absolute stop",
                description = "Model commands center frame, golden backlight crowns silhouette with royal glow",
                modelAction = "Signature gaze directly down camera barrel, holds fierce iconic pose",
                lightingSetup = "Key: Beauty dish with diffusion; Rim: Dual 600D gold-filtered spots",
                transition = "Slow fade to velvet black with gold studio emblem watermark",
                audioCue = "Final musical chord reverberating to silence"
            )
        )
    }
}
