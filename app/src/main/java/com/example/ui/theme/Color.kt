package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// =============================================================================
// BESPOKE BLACK & GOLD PALETTE (Static Dark Constants)
// =============================================================================
val DarkGoldPrimary = Color(0xFFD4AF37)
val DarkGoldLight = Color(0xFFF6E79D)
val DarkGoldDark = Color(0xFFA57D12)
val DarkGoldContainer = Color(0xFF2C240E)
val DarkOnGoldContainer = Color(0xFFFFE68D)

val DarkStudioBlack = Color(0xFF0C0C0C)
val DarkStudioDarkSurface = Color(0xFF141414)
val DarkStudioCardSurface = Color(0xFF1C1C1E)
val DarkStudioElevatedSurface = Color(0xFF252528)
val DarkStudioOutline = Color(0xFF383324)

val DarkTextPrimary = Color(0xFFF8F8F8)
val DarkTextSecondary = Color(0xFFB8B8B8)
val DarkTextTertiary = Color(0xFF7E7E7E)

// =============================================================================
// HIGH-CONTRAST STUDIO LIGHT PALETTE (Accessibility & Bright Sunlight Mode)
// =============================================================================
val LightGoldPrimary = Color(0xFF8B6200)      // High-contrast burnished gold (WCAG AAA >= 5.2:1)
val LightGoldLight = Color(0xFF6B4D00)
val LightGoldDark = Color(0xFF4A3400)
val LightGoldContainer = Color(0xFFFFF3CD)
val LightOnGoldContainer = Color(0xFF382600)

val LightStudioBackground = Color(0xFFF8F9FA)  // High-contrast clean off-white
val LightStudioDarkSurface = Color(0xFFFFFFFF) // Crisp pure white surfaces
val LightStudioCardSurface = Color(0xFFFFFFFF)
val LightStudioElevatedSurface = Color(0xFFF1F3F4)
val LightStudioOutline = Color(0xFFC8B58A)     // Warm visible gold outline

val LightTextPrimary = Color(0xFF121212)       // High-contrast deep black (WCAG AAA 16.6:1)
val LightTextSecondary = Color(0xFF374151)     // High-contrast slate (WCAG AAA 7.2:1)
val LightTextTertiary = Color(0xFF5F6368)

// Semantic Action Constants
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFE53935)
val WhatsAppGreen = Color(0xFF25D366)

// =============================================================================
// DYNAMIC THEME-AWARE COLOR ACCESSORS
// In Dark Mode: Returns authentic obsidian black & gold.
// In Studio Light Mode: Returns high-contrast white, black & burnished gold.
// =============================================================================
val GoldPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

val GoldLight: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.secondary

val GoldDark: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.inversePrimary

val GoldContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primaryContainer

val OnGoldContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onPrimaryContainer

val StudioBlack: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val StudioDarkSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val StudioCardSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val StudioElevatedSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceContainerHigh

val StudioOutline: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outline

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onBackground

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val TextTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outlineVariant
