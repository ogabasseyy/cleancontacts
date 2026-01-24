package com.ogabassey.contactscleaner.domain.usecase

import com.ogabassey.contactscleaner.domain.model.CleanupStatus
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.repository.BackupRepository
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CleanupContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val backupRepository: BackupRepository
) {
    suspend fun deleteContacts(contactIds: List<Long>): Boolean {
        // Manual deletion - Ideally should back up, but assumes UI/Caller handles context
        // or we rely on the caller to fetch contacts first if backup is needed.
        // For now, we skip backup for manual delete to be safe/fast, or we can fetch them.
        // Let's implement fetch-then-backup for robustness.
        return contactRepository.deleteContacts(contactIds)
    }

    suspend fun deleteContactsByType(type: ContactType): Flow<CleanupStatus> = flow {
        // Fetch ALL contacts of this type to Backup first
        val contacts = contactRepository.getContactsSnapshotByType(type)
        if (contacts.isEmpty()) {
            emit(CleanupStatus.Success("No contacts to delete"))
            return@flow
        }
        
        // Backup
        emit(CleanupStatus.Progress(0.0f, "Backing up ${contacts.size} contacts..."))
        backupRepository.saveSnapshot(contacts, "DELETE", "Deleted ${contacts.size} ${type.name} contacts")
        
        // Delegating execution to repository
        contactRepository.deleteContactsByType(type).collect { status ->
             emit(status)
        }
    }

    suspend fun mergeDuplicateGroups(type: ContactType): Flow<CleanupStatus> = flow {
         val groups = contactRepository.getDuplicateGroups(type)
         if (groups.isEmpty()) {
             emit(CleanupStatus.Success("No duplicates found"))
             return@flow
         }
         
         val contactsToBackup = mutableListOf<Contact>()
         groups.forEach { group ->
             val contacts = contactRepository.getContactsInGroup(group.groupKey, type)
             if (contacts.size > 1) {
                 contactsToBackup.addAll(contacts)
             }
         }
         
         if (contactsToBackup.isNotEmpty()) {
             emit(CleanupStatus.Progress(0f, "Backing up ${contactsToBackup.size} contacts..."))
             backupRepository.saveSnapshot(contactsToBackup, "MERGE", "Merged ${groups.size} groups")
         }
         
         contactRepository.mergeDuplicateGroups(type).collect {
             emit(it)
         }
    }

    suspend fun mergeContacts(contactIds: List<Long>, customName: String?): Result<Boolean> {
        return try {
            val success = contactRepository.mergeContacts(contactIds, customName)
            if (success) Result.success(true) else Result.failure(Exception("Failed to merge contacts"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun standardizeAllFormatIssues(): Flow<CleanupStatus> = flow {
        val contacts = contactRepository.getContactsSnapshotByType(ContactType.FORMAT_ISSUE)
        if (contacts.isNotEmpty()) {
             backupRepository.saveSnapshot(contacts, "FORMAT", "Standardized ${contacts.size} numbers")
        }
        contactRepository.standardizeAllFormatIssues().collect { emit(it) }
    }

    suspend fun ignoreContact(id: String, displayName: String, reason: String): Boolean {
        return contactRepository.ignoreContact(id, displayName, reason)
    }

    suspend fun unignoreContact(id: String): Boolean {
        return contactRepository.unignoreContact(id)
    }

    fun getIgnoredContacts(): Flow<List<com.ogabassey.contactscleaner.data.db.entity.IgnoredContact>> {
        return contactRepository.getIgnoredContacts()
    }
}
