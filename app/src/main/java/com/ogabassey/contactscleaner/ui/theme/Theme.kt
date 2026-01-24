package com.ogabassey.contactscleaner.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
fun CleanContactsAITheme(
    darkTheme: Boolean = true, // Force Dark Mode for Deep Space aesthetic
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar transparency handled in MainActivity; only set icon color here
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
