package com.ogabassey.contactscleaner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Contacts Cleaner Theme for Compose Multiplatform.
 *
 * 2026 KMP Best Practice: Platform-agnostic theming.
 */

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryNeon,
    secondary = SecondaryNeon,
    tertiary = PrimaryNeonDim,
    background = SpaceBlack,
    surface = DeepSpace,
    onPrimary = SpaceBlack,
    onSecondary = SpaceBlack,
    onBackground = TextHigh,
    onSurface = TextHigh,
    error = ErrorNeon
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryNeonDim,
    secondary = SecondaryNeon,
    tertiary = PrimaryNeon,
    background = Color.White,
    surface = Color(0xFFF5F7F9),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1D21),
    onSurface = Color(0xFF1A1D21),
    error = ErrorNeon
)

@Composable
fun ContactsCleanerTheme(
    darkTheme: Boolean = true, // Force Dark Mode for Deep Space aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Platform-specific status bar handling is done in platform entry points
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
