package com.ogabassey.contactscleaner.domain.usecase

import androidx.paging.PagingData
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetContactsPagedUseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    operator fun invoke(type: ContactType): Flow<PagingData<Contact>> {
        return contactRepository.getContactsPaged(type)
    }
}
