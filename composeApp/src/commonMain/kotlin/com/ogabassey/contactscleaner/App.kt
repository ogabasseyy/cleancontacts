package com.ogabassey.contactscleaner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ogabassey.contactscleaner.ui.components.FeedbackBottomSheet
import com.ogabassey.contactscleaner.ui.components.FloatingFeedbackButton
import com.ogabassey.contactscleaner.ui.navigation.AppNavigation
import com.ogabassey.contactscleaner.ui.theme.ContactsCleanerTheme
import com.ogabassey.contactscleaner.ui.theme.SpaceBlack

/**
 * Main App composable for Contacts Cleaner.
 *
 * 2026 KMP Best Practice: Shared App composable for Compose Multiplatform.
 * Platform-specific entry points call this composable.
 */
@Composable
fun App() {
    ContactsCleanerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SpaceBlack
        ) {
            var showFeedbackSheet by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxSize()) {
                AppNavigation()

                FloatingFeedbackButton(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 24.dp),
                    onOpenSheet = { showFeedbackSheet = true }
                )
            }

            if (showFeedbackSheet) {
                FeedbackBottomSheet(onDismiss = { showFeedbackSheet = false })
            }
        }
    }
}
