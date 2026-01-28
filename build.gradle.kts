// Top-level build file for CleanContactsAI KMP project
plugins {
    // Android plugins (apply false - applied in subprojects)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // 2026 Best Practice: New KMP-native Android library plugin
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false

    // Kotlin plugins
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    // Compose Multiplatform
    alias(libs.plugins.compose.multiplatform) apply false

    // Build tools
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}
