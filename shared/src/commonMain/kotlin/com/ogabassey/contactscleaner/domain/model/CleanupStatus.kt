package com.ogabassey.contactscleaner.domain.model

sealed class CleanupStatus {
    data class Progress(val progress: Float, val message: String? = null) : CleanupStatus()
    data class Success(val message: String) : CleanupStatus()
    data class Error(val message: String) : CleanupStatus()
}
