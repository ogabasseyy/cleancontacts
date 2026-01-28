package com.ogabassey.contactscleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * 2026 AGP 9.0: Migrated from Hilt to Koin.
 * Removed @AndroidEntryPoint annotation.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 2026 KMP: Use shared App composable
            App()
        }
    }
}