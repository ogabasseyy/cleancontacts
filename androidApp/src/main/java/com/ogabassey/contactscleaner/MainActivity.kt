package com.ogabassey.contactscleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ogabassey.contactscleaner.App

/**
 * 2026 AGP 9.0: Migrated from Hilt to Koin.
 * Removed @AndroidEntryPoint annotation.
 *
 * 2026 Best Practice: Enables edge-to-edge layout for Android 15+ recommended behavior.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 2026 Best Practice: Enable edge-to-edge before setContent
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // 2026 KMP: Use shared App composable
            App()
        }
    }
}