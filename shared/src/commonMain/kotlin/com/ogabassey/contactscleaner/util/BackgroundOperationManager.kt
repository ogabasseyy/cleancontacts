package com.ogabassey.contactscleaner.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 2026 Best Practice: Singleton manager for background operations with streaming progress.
 * Allows operations to run in background while user navigates the app.
 * Supports minimize-to-bubble and real-time progress streaming.
 */
object BackgroundOperationManager {

    private val _currentOperation = MutableStateFlow<BackgroundOperation?>(null)
    val currentOperation: StateFlow<BackgroundOperation?> = _currentOperation.asStateFlow()

    private val _isMinimized = MutableStateFlow(false)
    val isMinimized: StateFlow<Boolean> = _isMinimized.asStateFlow()

    private val _logEntries = MutableStateFlow<List<OperationLogEntry>>(emptyList())
    val logEntries: StateFlow<List<OperationLogEntry>> = _logEntries.asStateFlow()

    // Maximum log entries to keep for UI scrolling
    private const val MAX_LOG_ENTRIES = 100

    /**
     * Start a new background operation.
     * Only one operation can run at a time.
     * 2026 Fix: Use atomic update to prevent race conditions.
     */
    fun startOperation(type: OperationType, totalItems: Int, title: String? = null) {
        // Create the new operation before the atomic update
        val newOperation = BackgroundOperation(
            type = type,
            totalItems = totalItems,
            title = title ?: type.displayName,
            startTime = currentTimeMillis()
        )

        // Atomic check-and-set to prevent race conditions
        var wasStarted = false
        _currentOperation.update { current ->
            if (current?.status == OperationStatus.Running) {
                current // Already running, keep existing operation
            } else {
                wasStarted = true
                newOperation
            }
        }

        // Only reset side-effect state if we actually started a new operation
        if (wasStarted) {
            _logEntries.value = emptyList()
            _isMinimized.value = false
        }
    }

    /**
     * Update progress with current item being processed.
     */
    fun updateProgress(processed: Int, currentItem: String? = null) {
        _currentOperation.update { current ->
            current?.copy(
                processedItems = processed,
                currentItem = currentItem,
                progress = if (current.totalItems > 0) {
                    (processed.toFloat() / current.totalItems.toFloat()).coerceIn(0f, 1f)
                } else 0f
            )
        }
    }

    /**
     * Add a log entry for streaming display.
     */
    fun addLogEntry(message: String, isSuccess: Boolean = true) {
        val entry = OperationLogEntry(
            timestamp = currentTimeMillis(),
            message = message,
            isSuccess = isSuccess
        )
        _logEntries.update { current ->
            (listOf(entry) + current).take(MAX_LOG_ENTRIES)
        }
    }

    /**
     * Mark operation as complete.
     */
    fun complete(success: Boolean, message: String? = null) {
        _currentOperation.update { current ->
            current?.copy(
                status = if (success) OperationStatus.Completed else OperationStatus.Failed,
                completionMessage = message,
                progress = if (success) 1f else current.progress
            )
        }
    }

    /**
     * Cancel the current operation.
     */
    fun cancel() {
        _currentOperation.update { current ->
            current?.copy(
                status = OperationStatus.Cancelled,
                completionMessage = "Operation cancelled"
            )
        }
    }

    /**
     * Minimize the modal to a floating bubble.
     */
    fun minimize() {
        _isMinimized.value = true
    }

    /**
     * Maximize from bubble back to full modal.
     */
    fun maximize() {
        _isMinimized.value = false
    }

    /**
     * Dismiss/clear the operation (after completion or user dismissal).
     */
    fun dismiss() {
        _currentOperation.value = null
        _logEntries.value = emptyList()
        _isMinimized.value = false
    }

    /**
     * Check if an operation is currently active.
     */
    fun isActive(): Boolean {
        return _currentOperation.value?.status == OperationStatus.Running
    }

    /**
     * Get estimated time remaining based on processing rate.
     */
    fun getEstimatedTimeRemaining(): Long? {
        val operation = _currentOperation.value ?: return null
        if (operation.processedItems == 0) return null

        val elapsed = currentTimeMillis() - operation.startTime
        val rate = operation.processedItems.toFloat() / elapsed.toFloat()
        val remaining = operation.totalItems - operation.processedItems

        return if (rate > 0) (remaining / rate).toLong() else null
    }
}

/**
 * Represents a background operation with progress tracking.
 */
data class BackgroundOperation(
    val id: String = randomUUID(),
    val type: OperationType,
    val title: String,
    val totalItems: Int,
    val processedItems: Int = 0,
    val currentItem: String? = null,
    val progress: Float = 0f,
    val startTime: Long,
    val status: OperationStatus = OperationStatus.Running,
    val completionMessage: String? = null
)

/**
 * Log entry for streaming operation progress.
 * 2026 Fix: Added unique id to prevent key collisions when multiple entries
 * have the same timestamp (within the same millisecond).
 */
data class OperationLogEntry(
    val id: String = randomUUID(),
    val timestamp: Long,
    val message: String,
    val isSuccess: Boolean = true
)

/**
 * Types of background operations.
 */
enum class OperationType(val displayName: String) {
    STANDARDIZE_FORMAT("Standardizing Formats"),
    DELETE_CONTACTS("Deleting Contacts"),
    MERGE_DUPLICATES("Merging Duplicates"),
    CONSOLIDATE_ACCOUNTS("Consolidating Accounts"),
    SCAN_CONTACTS("Scanning Contacts")
}

/**
 * Status of a background operation.
 */
enum class OperationStatus {
    Running,
    Completed,
    Failed,
    Cancelled
}

// Use platform-specific implementations from Platform.kt
internal fun currentTimeMillis(): Long = getPlatformTimeMillis()
internal fun randomUUID(): String = getPlatformUUID()
