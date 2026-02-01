package com.ogabassey.contactscleaner.domain.model

sealed class CleanupStatus {
    /**
     * Progress update during cleanup operation.
     * @param progress Float between 0 and 1
     * @param message User-friendly status message
     * @param details Optional streaming details for enhanced UI
     */
    data class Progress(
        val progress: Float,
        val message: String? = null,
        val details: CleanupDetails? = null
    ) : CleanupStatus()

    data class Success(val message: String) : CleanupStatus()
    data class Error(val message: String) : CleanupStatus()
}

/**
 * 2026 Best Practice: Streaming details for enhanced progress UI.
 * Allows modal to show real-time item-by-item updates.
 */
data class CleanupDetails(
    val processed: Int,
    val total: Int,
    val currentItem: String? = null,
    val recentItems: List<String> = emptyList()
)
