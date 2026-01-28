package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.repository.BackupRepository
import com.ogabassey.contactscleaner.domain.repository.Snapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

/**
 * Mock BackupRepository for development and testing.
 *
 * 2026 KMP Best Practice: Mock implementations for preview and testing.
 */
class MockBackupRepository : BackupRepository {

    private val _snapshots = MutableStateFlow<List<Snapshot>>(
        listOf(
            Snapshot(
                id = 1,
                contacts = listOf(
                    Contact(
                        id = 101,
                        name = "John Doe",
                        numbers = listOf("+1234567890"),
                        emails = listOf("john@example.com"),
                        normalizedNumber = "+1234567890",
                        isJunk = true
                    ),
                    Contact(
                        id = 102,
                        name = "Jane Smith",
                        numbers = listOf("+0987654321"),
                        emails = emptyList(),
                        normalizedNumber = "+0987654321",
                        isJunk = true
                    )
                ),
                actionType = "DELETE",
                description = "Deleted 2 junk contacts",
                timestamp = Clock.System.now().toEpochMilliseconds() - (1000 * 60 * 30) // 30 min ago
            ),
            Snapshot(
                id = 2,
                contacts = listOf(
                    Contact(
                        id = 201,
                        name = "Bob Wilson",
                        numbers = listOf("+1122334455"),
                        emails = listOf("bob@example.com"),
                        normalizedNumber = "+1122334455"
                    )
                ),
                actionType = "MERGE",
                description = "Merged 3 duplicate contacts into Bob Wilson",
                timestamp = Clock.System.now().toEpochMilliseconds() - (1000 * 60 * 60 * 2) // 2 hours ago
            )
        )
    )

    override suspend fun performBackup(contacts: List<Contact>, actionType: String, description: String) {
        val newSnapshot = Snapshot(
            id = _snapshots.value.size + 1,
            contacts = contacts,
            actionType = actionType,
            description = description,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        _snapshots.value = listOf(newSnapshot) + _snapshots.value
    }

    override suspend fun getLastSnapshot(): Snapshot? {
        return _snapshots.value.firstOrNull()
    }

    override suspend fun getSnapshotById(id: Int): Snapshot? {
        return _snapshots.value.find { it.id == id }
    }

    override fun getAllSnapshots(): Flow<List<Snapshot>> {
        return _snapshots.asStateFlow()
    }

    override suspend fun clearLastSnapshot(id: Int) {
        _snapshots.value = _snapshots.value.filter { it.id != id }
    }

    override suspend fun cleanupOldSnapshots() {
        // Keep only last 20 snapshots
        if (_snapshots.value.size > 20) {
            _snapshots.value = _snapshots.value.take(20)
        }
    }
}
