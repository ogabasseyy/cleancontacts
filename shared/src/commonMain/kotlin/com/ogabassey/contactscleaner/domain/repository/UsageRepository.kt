package com.ogabassey.contactscleaner.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for tracking usage and free trial actions.
 *
 * 2026 KMP Best Practice: Platform-agnostic interface for preferences.
 */
interface UsageRepository {
    /**
     * Flow of how many free actions have been used.
     */
    val freeActionsUsed: Flow<Int>

    /**
     * Flow of remaining free trial actions.
     * Calculated as MAX_FREE_ACTIONS - freeActionsUsed.
     */
    val freeActionsRemaining: Flow<Int>

    /**
     * Increment the free actions counter.
     * Called when a user performs a cleanup action.
     */
    suspend fun incrementFreeActions()

    /**
     * Check if the user can perform a free action.
     */
    suspend fun canPerformFreeAction(): Boolean

    /**
     * Flow of the total number of contacts scanned in the last session.
     */
    val rawScannedCount: Flow<Int>

    /**
     * Update the raw scanned count.
     */
    suspend fun updateRawScannedCount(count: Int)

    companion object {
        /**
         * Maximum number of free trial actions allowed.
         * Set to 1 for strict trial limit.
         */
        const val MAX_FREE_ACTIONS = 1
    }
}
