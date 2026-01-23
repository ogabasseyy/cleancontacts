package com.ogabassey.contactscleaner.domain.repository

import com.ogabassey.contactscleaner.domain.model.Contact

interface BackupRepository {
    suspend fun saveSnapshot(contacts: List<Contact>, actionType: String, description: String)
    suspend fun getLastSnapshot(): Snapshot?
    suspend fun getSnapshotById(id: Int): Snapshot?
    fun getAllSnapshots(): kotlinx.coroutines.flow.Flow<List<Snapshot>>
    suspend fun clearLastSnapshot(id: Int)
    suspend fun cleanupOldSnapshots()
}

data class Snapshot(
    val id: Int,
    val contacts: List<Contact>,
    val actionType: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
