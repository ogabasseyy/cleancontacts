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

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _addedToSafeListContact = MutableStateFlow<String?>(null)
    val addedToSafeListContact = _addedToSafeListContact.asStateFlow()

    // 2026 Best Practice: Track processing state for individual operations
    private val _processingContactId = MutableStateFlow<Long?>(null)
    val processingContactId = _processingContactId.asStateFlow()

    fun loadContacts(type: ContactType) {
        viewModelScope.launch {
            _uiState.value = ReviewUiState.Loading
            try {
                _contacts.value = contactRepository.getContactsSnapshotByType(type)
                _uiState.value = ReviewUiState.Success
            } catch (e: Exception) {
                _uiState.value = ReviewUiState.Error(e.message ?: "Failed to load contacts")
            }
        }
    }

    fun ignoreContact(contact: Contact) {
        viewModelScope.launch {
            // 2026 Best Practice: Show loading state for individual operations
            _processingContactId.value = contact.id
            try {
                val success = contactRepository.ignoreContact(
                    id = contact.id.toString(),
                    displayName = contact.name ?: "Unknown",
                    reason = "User Reviewed: ${contact.sensitiveDescription ?: "Manual"}"
                )
                if (success) {
                    // Show confirmation
                    _addedToSafeListContact.value = contact.name ?: "Unknown"
                    // Remove from local list
                    _contacts.value = _contacts.value.filter { it.id != contact.id }
                } else {
                    _uiState.value = ReviewUiState.Error("Failed to add contact to safe list")
                }
            } catch (e: Exception) {
                _uiState.value = ReviewUiState.Error(e.message ?: "Failed to add to safe list")
            } finally {
                _processingContactId.value = null
            }
        }
    }

    fun dismissConfirmation() {
        _addedToSafeListContact.value = null
    }

    fun removeFromList(contact: Contact) {
         // Just remove from view for now (implementation for "Not Junk" or "Not Sensitive")
         _contacts.value = _contacts.value.filter { it.id != contact.id }
    }

    fun resetState() {
        _uiState.value = ReviewUiState.Success
    }
}

sealed class ReviewUiState {
    data object Loading : ReviewUiState()
    data object Success : ReviewUiState()
    data class Error(val message: String) : ReviewUiState()
}
