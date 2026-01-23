package com.ogabassey.contactscleaner.domain.usecase

import com.ogabassey.contactscleaner.domain.repository.BackupRepository
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import javax.inject.Inject

class UndoUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val contactRepository: ContactRepository
) {
    suspend fun undoLastAction(): Result<String> {
        val snapshot = backupRepository.getLastSnapshot() ?: return Result.failure(Exception("Nothing to undo"))
        
        return try {
            // Restore contacts
            // Note: This will create NEW contacts with NEW IDs, which is safer than trying to force-insert old IDs
            // that might conflict or be handled strangely by the ContactsProvider.
            // Ideally we'd map them back, but for "Undo Delete", re-insertion is the standard behavior.
            val success = contactRepository.restoreContacts(snapshot.contacts)
            
            if (success) {
                backupRepository.clearLastSnapshot(snapshot.id)
                Result.success(snapshot.description)
            } else {
                Result.failure(Exception("Failed to restore contacts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun hasPendingUndo(): Boolean {
        return backupRepository.getLastSnapshot() != null
    }
}
