package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.data.db.dao.UndoDao
import com.ogabassey.contactscleaner.data.db.entity.UndoLog
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.repository.BackupRepository
import com.ogabassey.contactscleaner.domain.repository.Snapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * 2026 KMP Best Practice: Cross-platform BackupRepository implementation.
 *
 * Uses kotlinx-datetime Clock for timestamps instead of System.currentTimeMillis().
 */
class BackupRepositoryImpl(
    private val undoDao: UndoDao
) : BackupRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun performBackup(contacts: List<Contact>, actionType: String, description: String) {
        val jsonString = json.encodeToString(contacts)
        undoDao.insert(UndoLog(
            actionType = actionType,
            originalDataJson = jsonString,
            description = description,
            timestamp = Clock.System.now().toEpochMilliseconds()
        ))
    }

    override suspend fun getLastSnapshot(): Snapshot? {
        val log = undoDao.getLastLog() ?: return null
        return try {
            val contacts: List<Contact> = json.decodeFromString(log.originalDataJson)
            Snapshot(log.id, contacts, log.actionType, log.description, log.timestamp)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearLastSnapshot(id: Int) {
        undoDao.delete(id)
    }

    override suspend fun getSnapshotById(id: Int): Snapshot? {
        val log = undoDao.getLogById(id) ?: return null
        return try {
            val contacts: List<Contact> = json.decodeFromString(log.originalDataJson)
            Snapshot(log.id, contacts, log.actionType, log.description, log.timestamp)
        } catch (e: Exception) {
            null
        }
    }

    override fun getAllSnapshots(): Flow<List<Snapshot>> {
        return undoDao.getAllLogs().map { logs ->
            logs.mapNotNull { log ->
                try {
                    val contacts: List<Contact> = json.decodeFromString(log.originalDataJson)
                    Snapshot(log.id, contacts, log.actionType, log.description, log.timestamp)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override suspend fun cleanupOldSnapshots() {
        // Delete logs older than 7 days
        val oneWeekAgo = Clock.System.now().toEpochMilliseconds() - (7 * 24 * 60 * 60 * 1000L)
        undoDao.deleteOlderThan(oneWeekAgo)
    }
}
