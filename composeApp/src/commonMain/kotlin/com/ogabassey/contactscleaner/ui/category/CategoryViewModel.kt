package com.ogabassey.contactscleaner.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.domain.model.CleanupStatus
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary
import com.ogabassey.contactscleaner.domain.repository.BillingRepository
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import com.ogabassey.contactscleaner.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Shared ViewModel for Category Detail Screens (Junk, Duplicates, Accounts, etc.)
 */
class CategoryViewModel(
    private val contactRepository: ContactRepository,
    val billingRepository: BillingRepository,
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    // Trial actions tracking (limit is 1)
    val freeActionsRemaining: Flow<Int> = usageRepository.freeActionsRemaining

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroupSummary>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroupSummary>> = _duplicateGroups.asStateFlow()

    private val _groupContacts = MutableStateFlow<List<Contact>>(emptyList())
    val groupContacts: StateFlow<List<Contact>> = _groupContacts.asStateFlow()

    // 2026 Best Practice: Mutex for thread-safe access to pendingAction
    private val actionMutex = Mutex()
    private var pendingAction: (suspend () -> Unit)? = null

    /**
     * Run an action with premium/trial check.
     */
    private suspend fun runWithPremiumCheck(action: suspend () -> Unit) {
        // 2026 Best Practice: Use .first() instead of .value for fresh reads in suspend context
        val isPremium = billingRepository.isPremium.first()
        val canPerform = usageRepository.canPerformFreeAction()

        if (isPremium || canPerform) {
            action()
            if (!isPremium) {
                usageRepository.incrementFreeActions()
            }
        } else {
            actionMutex.withLock {
                pendingAction = action
            }
            _uiState.value = CategoryUiState.ShowPaywall
        }
    }

    fun loadCategory(type: ContactType) {
        viewModelScope.launch {
            _uiState.value = CategoryUiState.Loading
            try {
                // For duplicate types, load groups instead of flat list
                if (type.name.startsWith("DUP_") || type == ContactType.DUPLICATE) {
                    val groups = contactRepository.getDuplicateGroups(type)
                    _duplicateGroups.value = groups
                    // Also load flat list for count purposes
                    val result = contactRepository.getContactsSnapshotByType(type)
                    _contacts.value = result
                } else {
                    // Fetch actual contacts from repository
                    val result = contactRepository.getContactsSnapshotByType(type)
                    _contacts.value = result
                }
                _uiState.value = CategoryUiState.Success
            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error("Failed to load contacts: ${e.message}")
            }
        }
    }

    fun loadGroupContacts(groupKey: String, type: ContactType) {
        viewModelScope.launch {
            // 2026 Best Practice: Clear stale state immediately when navigating to new group
            _groupContacts.value = emptyList()
            try {
                val contacts = contactRepository.getContactsInGroup(groupKey, type)
                _groupContacts.value = contacts
            } catch (e: Exception) {
                _groupContacts.value = emptyList()
            }
        }
    }

    fun performSingleMerge(contactIds: List<Long>, customName: String, type: ContactType) {
        viewModelScope.launch {
            runWithPremiumCheck {
                pendingAction = null
                _uiState.value = CategoryUiState.Processing(0f, "Merging contacts...")
                try {
                    val success = contactRepository.mergeContacts(contactIds, customName)
                    if (success) {
                        _uiState.value = CategoryUiState.Success
                        // Refresh the list
                        loadCategory(type)
                    } else {
                        _uiState.value = CategoryUiState.Error("Failed to merge contacts")
                    }
                } catch (e: Exception) {
                    _uiState.value = CategoryUiState.Error(e.message ?: "Merge failed")
                }
            }
        }
    }

    fun clearGroupContacts() {
        _groupContacts.value = emptyList()
    }

    fun performAction(type: ContactType) {
        viewModelScope.launch {
            runWithPremiumCheck {
                pendingAction = null
                _uiState.value = CategoryUiState.Processing(0f, "Starting...")

                try {
                    when (type) {
                        ContactType.JUNK -> {
                            contactRepository.deleteContactsByType(type).collect { status ->
                                updateStatus(status)
                            }
                        }
                        ContactType.DUPLICATE,
                        ContactType.DUP_NUMBER,
                        ContactType.DUP_EMAIL,
                        ContactType.DUP_NAME,
                        ContactType.DUP_SIMILAR_NAME -> {
                            contactRepository.mergeDuplicateGroups(type).collect { status ->
                                updateStatus(status)
                            }
                        }
                        ContactType.FORMAT_ISSUE -> {
                            contactRepository.standardizeAllFormatIssues().collect { status ->
                                updateStatus(status)
                            }
                        }
                        else -> {
                            // For other types, maybe deletion?
                            contactRepository.deleteContactsByType(type).collect { status ->
                                updateStatus(status)
                            }
                        }
                    }
                    // Refresh list
                    loadCategory(type)
                } catch (e: Exception) {
                    _uiState.value = CategoryUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun deleteSingleContact(contact: Contact, type: ContactType) {
        viewModelScope.launch {
            runWithPremiumCheck {
                pendingAction = null
                _uiState.value = CategoryUiState.Processing(0f, "Deleting contact...")
                try {
                    val success = contactRepository.deleteContacts(listOf(contact)).isSuccess
                    if (success) {
                        _uiState.value = CategoryUiState.Success
                        loadCategory(type)
                    } else {
                        _uiState.value = CategoryUiState.Error("Failed to delete contact")
                    }
                } catch (e: Exception) {
                    _uiState.value = CategoryUiState.Error(e.message ?: "Deletion failed")
                }
            }
        }
    }

    private fun updateStatus(status: CleanupStatus) {
        when (status) {
            is CleanupStatus.Progress -> {
                _uiState.value = CategoryUiState.Processing(status.progress, status.message)
            }
            is CleanupStatus.Success -> {
                _uiState.value = CategoryUiState.Success
            }
            is CleanupStatus.Error -> {
                _uiState.value = CategoryUiState.Error(status.message)
            }
        }
    }

    /**
     * Reset UI state to Success (clears error/processing states).
     */
    fun resetState() {
        _uiState.value = CategoryUiState.Success
    }

    /**
     * Retry pending action after paywall dismissal.
     */
    fun retryPendingAction() {
        viewModelScope.launch {
            val isPremium = billingRepository.isPremium.first()
            val canPerform = usageRepository.canPerformFreeAction()

            if (isPremium || canPerform) {
                val action = actionMutex.withLock {
                    val temp = pendingAction
                    pendingAction = null
                    temp
                }
                action?.invoke()
            }
            _uiState.value = CategoryUiState.Success
        }
    }
}

sealed class CategoryUiState {
    data object Loading : CategoryUiState()
    data object Success : CategoryUiState()
    data object ShowPaywall : CategoryUiState()
    data class Error(val message: String) : CategoryUiState()
    data class Processing(val progress: Float, val message: String? = null) : CategoryUiState()
}
