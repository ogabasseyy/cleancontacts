package com.ogabassey.contactscleaner.domain.usecase

import com.ogabassey.contactscleaner.domain.model.CleanupStatus
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for cleaning up contacts (delete junk, duplicates, etc.).
 */
class CleanupContactsUseCase(
    private val contactRepository: ContactRepository
) {
    suspend fun deleteByType(type: ContactType): Flow<CleanupStatus> {
        // Backups are performed by repository implementations after write-path execution.
        return contactRepository.deleteContactsByType(type)
    }

    suspend fun deleteByIds(ids: List<Long>): Boolean {
        // Backups are performed by repository implementations after write-path execution.
        return contactRepository.deleteContactsByIds(ids)
    }

    suspend fun mergeDuplicates(type: ContactType): Flow<CleanupStatus> {
        // Backups are performed by repository implementations after write-path execution.
        return contactRepository.mergeDuplicateGroups(type)
    }

    suspend fun standardizeFormats(): Flow<CleanupStatus> {
        return contactRepository.standardizeAllFormatIssues()
    }
}
