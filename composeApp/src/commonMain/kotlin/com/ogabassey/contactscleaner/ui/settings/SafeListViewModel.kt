package com.ogabassey.contactscleaner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.data.db.entity.IgnoredContact
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the Safe List (Ignored Contacts).
 */
class SafeListViewModel(
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SafeListUiState>(SafeListUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val ignoredContacts: StateFlow<List<IgnoredContact>> = contactRepository.getIgnoredContacts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Mark as loaded once flow starts emitting
        viewModelScope.launch {
            try {
                ignoredContacts.collect {
                    if (_uiState.value is SafeListUiState.Loading) {
                        _uiState.value = SafeListUiState.Success
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SafeListUiState.Error(e.message ?: "Failed to load safe list")
            }
        }
    }

    fun unignoreContact(id: String) {
        viewModelScope.launch {
            try {
                val success = contactRepository.unignoreContact(id)
                if (!success) {
                    _uiState.value = SafeListUiState.Error("Failed to remove from safe list")
                }
            } catch (e: Exception) {
                _uiState.value = SafeListUiState.Error(e.message ?: "Failed to remove from safe list")
            }
        }
    }

    fun resetState() {
        _uiState.value = SafeListUiState.Success
    }
}

sealed class SafeListUiState {
    data object Loading : SafeListUiState()
    data object Success : SafeListUiState()
    data class Error(val message: String) : SafeListUiState()
}
