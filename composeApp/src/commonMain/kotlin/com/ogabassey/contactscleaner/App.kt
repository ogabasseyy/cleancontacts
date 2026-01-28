package com.ogabassey.contactscleaner

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ogabassey.contactscleaner.ui.navigation.AppNavigation
import com.ogabassey.contactscleaner.ui.theme.CleanContactsAITheme
import com.ogabassey.contactscleaner.ui.theme.SpaceBlack

/**
 * Main App composable for CleanContactsAI.
 *
 * 2026 KMP Best Practice: Shared App composable for Compose Multiplatform.
 * Platform-specific entry points call this composable.
 */
@Composable
fun App() {
    CleanContactsAITheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SpaceBlack
        ) {
            AppNavigation()
        }
    }
}
