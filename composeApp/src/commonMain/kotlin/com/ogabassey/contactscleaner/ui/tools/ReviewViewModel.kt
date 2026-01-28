package com.ogabassey.contactscleaner.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for reviewing specific types of contacts (Sensitive, Format Issues, etc.).
 */
class ReviewViewModel(
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts = _contacts.asStateFlow()

    fun loadContacts(type: ContactType) {
        viewModelScope.launch {
            _contacts.value = contactRepository.getContactsSnapshotByType(type)
        }
    }

    fun ignoreContact(contact: Contact) {
        viewModelScope.launch {
            contactRepository.ignoreContact(
                id = contact.id.toString(),
                displayName = contact.name ?: "Unknown",
                reason = "User Reviewed: ${contact.sensitiveDescription ?: "Manual"}"
            )
            // Remove from local list
            _contacts.value = _contacts.value.filter { it.id != contact.id }
        }
    }
    
    fun removeFromList(contact: Contact) {
         // Just remove from view for now (implementation for "Not Junk" or "Not Sensitive")
         _contacts.value = _contacts.value.filter { it.id != contact.id }
    }
}
