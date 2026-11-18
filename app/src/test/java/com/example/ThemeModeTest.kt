package com.example

import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.DarkGoldPrimary
import com.example.ui.theme.DarkStudioBlack
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.LightGoldPrimary
import com.example.ui.theme.LightStudioBackground
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.StudioThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun studioThemeMode_enumValues() {
        val modes = StudioThemeMode.values()
        assertEquals(2, modes.size)
        assertTrue(modes.contains(StudioThemeMode.BLACK_AND_GOLD))
        assertTrue(modes.contains(StudioThemeMode.STUDIO_LIGHT))
    }

    @Test
    fun studioPalette_darkGoldContrast() {
        // Obsidian black background is deep dark
        assertTrue(DarkStudioBlack.luminance() < 0.02f)
        // Text primary is bright high-contrast against dark
        assertTrue(DarkTextPrimary.luminance() > 0.90f)
        // Primary gold has rich brand luminance
        assertTrue(DarkGoldPrimary.luminance() > 0.40f)
    }

    @Test
    fun studioPalette_studioLightHighContrast() {
        // High-contrast light background is bright off-white
        assertTrue(LightStudioBackground.luminance() > 0.90f)
        // Text primary is deep black (WCAG AAA contrast against light background)
        assertTrue(LightTextPrimary.luminance() < 0.02f)
        // Light primary gold is deep and rich (not pale yellow), maintaining strong contrast on light background
        assertTrue(LightGoldPrimary.luminance() < 0.20f)

        // Compute relative contrast ratio between LightTextPrimary and LightStudioBackground
        val lumLight = LightStudioBackground.luminance()
        val lumDark = LightTextPrimary.luminance()
        val contrastRatio = (lumLight + 0.05f) / (lumDark + 0.05f)
        assertTrue("Contrast ratio must be >= 7.0 for WCAG AAA accessibility", contrastRatio >= 7.0f)
    }
}
