package com.ogabassey.contactscleaner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.data.db.entity.IgnoredContact
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the Safe List (Ignored Contacts).
 *
 * 2026 KMP Best Practice: Using Koin for injection and StateFlow for UI state.
 */
class SafeListViewModel(
    private val contactRepository: ContactRepository
) : ViewModel() {

    val ignoredContacts: StateFlow<List<IgnoredContact>> = contactRepository.getIgnoredContacts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun unignoreContact(id: String) {
        viewModelScope.launch {
            contactRepository.unignoreContact(id)
        }
    }
}
