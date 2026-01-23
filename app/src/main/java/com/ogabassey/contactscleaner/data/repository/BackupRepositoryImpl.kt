package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.data.db.dao.UndoDao
import com.ogabassey.contactscleaner.data.db.entity.UndoLog
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.repository.BackupRepository
import com.ogabassey.contactscleaner.domain.repository.Snapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val undoDao: UndoDao
) : BackupRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun saveSnapshot(contacts: List<Contact>, actionType: String, description: String) {
        val jsonString = json.encodeToString(contacts)
        undoDao.insert(UndoLog(
            actionType = actionType,
            originalDataJson = jsonString,
            description = description
        ))
    }

    override suspend fun getLastSnapshot(): Snapshot? {
        val log = undoDao.getLastLog() ?: return null
        return try {
            val contacts: List<Contact> = json.decodeFromString(log.originalDataJson)
            Snapshot(log.id, contacts, log.actionType, log.description, log.timestamp)
        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
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
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        undoDao.deleteOlderThan(oneWeekAgo)
    }
}
