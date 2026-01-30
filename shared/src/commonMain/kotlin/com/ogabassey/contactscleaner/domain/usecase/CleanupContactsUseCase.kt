package com.ogabassey.contactscleaner.domain.usecase

import com.ogabassey.contactscleaner.domain.model.CleanupStatus
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.repository.BackupRepository
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for cleaning up contacts (delete junk, duplicates, etc.).
 */
class CleanupContactsUseCase(
    private val contactRepository: ContactRepository,
    private val backupRepository: BackupRepository
) {
    suspend fun deleteByType(type: ContactType): Flow<CleanupStatus> {
        // 1. Get contacts to be deleted
        val contactsToDelete = contactRepository.getContactsSnapshotByType(type)
        if (contactsToDelete.isNotEmpty()) {
             // 2. Backup (non-blocking - don't fail operation if backup fails)
             try {
                 backupRepository.performBackup(contactsToDelete, "Delete", "Deleted ${contactsToDelete.size} ${type.name} contacts")
             } catch (e: Exception) {
                 println("Warning: Backup failed before delete: ${e.message}")
             }
        }
        // 3. Delete
        return contactRepository.deleteContactsByType(type)
    }

    suspend fun deleteByIds(ids: List<Long>): Boolean {
        // 1. Get contacts
        val allContacts = contactRepository.getContactsAllSnapshot()
        val contactsToDelete = allContacts.filter { ids.contains(it.id) }

        if (contactsToDelete.isNotEmpty()) {
            // Backup (non-blocking - don't fail operation if backup fails)
            try {
                backupRepository.performBackup(contactsToDelete, "Delete", "Deleted ${contactsToDelete.size} contacts")
            } catch (e: Exception) {
                println("Warning: Backup failed before delete: ${e.message}")
            }
        }
        return contactRepository.deleteContactsByIds(ids)
    }

    suspend fun mergeDuplicates(type: ContactType): Flow<CleanupStatus> {
        // 1. Get contacts before merge (simplified backup for now)
        val contactsToMerge = contactRepository.getContactsSnapshotByType(type)
        if (contactsToMerge.isNotEmpty()) {
             // Backup (non-blocking - don't fail operation if backup fails)
             try {
                 backupRepository.performBackup(contactsToMerge, "Merge", "Merged duplicates")
             } catch (e: Exception) {
                 println("Warning: Backup failed before merge: ${e.message}")
             }
        }
        return contactRepository.mergeDuplicateGroups(type)
    }

    suspend fun standardizeFormats(): Flow<CleanupStatus> {
        return contactRepository.standardizeAllFormatIssues()
    }
}
