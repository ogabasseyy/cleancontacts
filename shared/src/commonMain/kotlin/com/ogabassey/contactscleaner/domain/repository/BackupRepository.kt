package com.ogabassey.contactscleaner.domain.repository

import com.ogabassey.contactscleaner.domain.model.Contact
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for backup/snapshot operations.
 *
 * 2026 KMP Best Practice: Platform-agnostic interface for undo functionality.
 */
interface BackupRepository {
    /**
     * Save a snapshot of contacts before a destructive action.
     */
    suspend fun performBackup(contacts: List<Contact>, actionType: String, description: String)

    /**
     * Get the most recent snapshot.
     */
    suspend fun getLastSnapshot(): Snapshot?

    /**
     * Get a snapshot by its ID.
     */
    suspend fun getSnapshotById(id: Int): Snapshot?

    /**
     * Get all snapshots as a Flow for reactive updates.
     */
    fun getAllSnapshots(): Flow<List<Snapshot>>

    /**
     * Delete a specific snapshot.
     */
    suspend fun clearLastSnapshot(id: Int)

    /**
     * Remove old snapshots to manage storage.
     */
    suspend fun cleanupOldSnapshots()
}

/**
 * Represents a backup snapshot of contacts.
 */
data class Snapshot(
    val id: Int,
    val contacts: List<Contact>,
    val actionType: String,
    val description: String,
    val timestamp: Long
)
