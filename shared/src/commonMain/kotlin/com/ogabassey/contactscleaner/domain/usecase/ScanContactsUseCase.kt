package com.ogabassey.contactscleaner.domain.usecase

import com.ogabassey.contactscleaner.domain.model.ScanStatus
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for scanning contacts.
 *
 * 2026 KMP Best Practice: Clean Architecture use cases for business logic.
 */
class ScanContactsUseCase(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(): Flow<ScanStatus> {
        return contactRepository.scanContacts()
    }
}
