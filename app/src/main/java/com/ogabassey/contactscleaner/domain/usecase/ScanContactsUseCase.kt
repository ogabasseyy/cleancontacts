package com.ogabassey.contactscleaner.domain.usecase

import com.ogabassey.contactscleaner.domain.model.ScanResult
import com.ogabassey.contactscleaner.domain.model.ScanStatus
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanContactsUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(): Flow<ScanStatus> {
        return contactRepository.scanContacts()
    }
}
