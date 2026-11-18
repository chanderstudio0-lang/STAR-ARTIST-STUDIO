package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Supported Star Artist Studio theme modes:
 * - [BLACK_AND_GOLD]: Authentic bespoke luxury black-and-gold design.
 * - [STUDIO_LIGHT]: High-contrast light mode with deep burnished gold accents and pure black text for bright outdoor environments.
 */
enum class StudioThemeMode {
    BLACK_AND_GOLD,
    STUDIO_LIGHT
}

val LocalStudioThemeMode = staticCompositionLocalOf { StudioThemeMode.BLACK_AND_GOLD }

object StudioTheme {
    val isLight: Boolean
        @Composable
        get() = LocalStudioThemeMode.current == StudioThemeMode.STUDIO_LIGHT

    val currentMode: StudioThemeMode
        @Composable
        get() = LocalStudioThemeMode.current
}

private val BlackAndGoldColorScheme = darkColorScheme(
    primary = DarkGoldPrimary,
    onPrimary = Color(0xFF1B1500),
    primaryContainer = DarkGoldContainer,
    onPrimaryContainer = DarkOnGoldContainer,
    inversePrimary = DarkGoldDark,
    secondary = DarkGoldLight,
    onSecondary = Color(0xFF1B1500),
    secondaryContainer = Color(0xFF282317),
    onSecondaryContainer = Color(0xFFFFE8A3),
    tertiary = Color(0xFFE5C158),
    onTertiary = Color(0xFF1B1500),
    background = DarkStudioBlack,
    onBackground = DarkTextPrimary,
    surface = DarkStudioDarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkStudioCardSurface,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainer = DarkStudioCardSurface,
    surfaceContainerHigh = DarkStudioElevatedSurface,
    outline = DarkStudioOutline,
    outlineVariant = Color(0xFF262626),
    error = ErrorRed,
    onError = Color.White
)

private val StudioLightColorScheme = lightColorScheme(
    primary = LightGoldPrimary,
    onPrimary = Color.White,
    primaryContainer = LightGoldContainer,
    onPrimaryContainer = LightOnGoldContainer,
    inversePrimary = LightGoldDark,
    secondary = LightGoldLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6EDD8),
    onSecondaryContainer = Color(0xFF3B2A04),
    tertiary = Color(0xFF9E7005),
    onTertiary = Color.White,
    background = LightStudioBackground,
    onBackground = LightTextPrimary,
    surface = LightStudioDarkSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightStudioCardSurface,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightStudioCardSurface,
    surfaceContainerHigh = LightStudioElevatedSurface,
    outline = LightStudioOutline,
    outlineVariant = Color(0xFFB0B0B0),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun StarArtistTheme(
    isStudioLight: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isStudioLight) StudioLightColorScheme else BlackAndGoldColorScheme
    val themeMode = if (isStudioLight) StudioThemeMode.STUDIO_LIGHT else StudioThemeMode.BLACK_AND_GOLD

    CompositionLocalProvider(
        LocalStudioThemeMode provides themeMode
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
