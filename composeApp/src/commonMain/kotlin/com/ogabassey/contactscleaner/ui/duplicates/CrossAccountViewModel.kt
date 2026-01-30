package com.ogabassey.contactscleaner.ui.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.domain.model.AccountInstance
import com.ogabassey.contactscleaner.domain.model.CleanupStatus
import com.ogabassey.contactscleaner.domain.model.CrossAccountContact
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
 * ViewModel for the Cross-Account Duplicates feature.
 * Handles loading, selection, and consolidation of contacts across accounts.
 */
class CrossAccountViewModel(
    private val contactRepository: ContactRepository,
    val billingRepository: BillingRepository,
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CrossAccountUiState>(CrossAccountUiState.Loading)
    val uiState: StateFlow<CrossAccountUiState> = _uiState.asStateFlow()

    // Trial actions tracking
    val freeActionsRemaining: Flow<Int> = usageRepository.freeActionsRemaining

    private val _crossAccountContacts = MutableStateFlow<List<CrossAccountContact>>(emptyList())
    val crossAccountContacts: StateFlow<List<CrossAccountContact>> = _crossAccountContacts.asStateFlow()

    private val _selectedMatchingKeys = MutableStateFlow<Set<String>>(emptySet())
    val selectedMatchingKeys: StateFlow<Set<String>> = _selectedMatchingKeys.asStateFlow()

    private val _selectedContact = MutableStateFlow<CrossAccountContact?>(null)
    val selectedContact: StateFlow<CrossAccountContact?> = _selectedContact.asStateFlow()

    private val _selectedAccountToKeep = MutableStateFlow<AccountInstance?>(null)
    val selectedAccountToKeep: StateFlow<AccountInstance?> = _selectedAccountToKeep.asStateFlow()

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
            _uiState.value = CrossAccountUiState.ShowPaywall
        }
    }

    /**
     * Load all cross-account contacts.
     */
    fun loadCrossAccountContacts() {
        viewModelScope.launch {
            _uiState.value = CrossAccountUiState.Loading
            try {
                val contacts = contactRepository.getCrossAccountContacts()
                _crossAccountContacts.value = contacts
                _uiState.value = CrossAccountUiState.Success
            } catch (e: Exception) {
                _uiState.value = CrossAccountUiState.Error("Failed to load contacts: ${e.message}")
            }
        }
    }

    /**
     * Toggle selection of a contact by matching key.
     */
    fun toggleSelection(matchingKey: String) {
        val current = _selectedMatchingKeys.value.toMutableSet()
        if (current.contains(matchingKey)) {
            current.remove(matchingKey)
        } else {
            current.add(matchingKey)
        }
        _selectedMatchingKeys.value = current
    }

    /**
     * Select all contacts.
     */
    fun selectAll() {
        _selectedMatchingKeys.value = _crossAccountContacts.value.map { it.matchingKey }.toSet()
    }

    /**
     * Clear all selections.
     */
    fun clearSelection() {
        _selectedMatchingKeys.value = emptySet()
    }

    /**
     * Set the contact being viewed in the detail sheet.
     */
    fun setSelectedContact(contact: CrossAccountContact?) {
        _selectedContact.value = contact
        // Auto-select first account as default
        _selectedAccountToKeep.value = contact?.accounts?.firstOrNull()
    }

    /**
     * Set the account to keep when consolidating.
     */
    fun setSelectedAccountToKeep(account: AccountInstance?) {
        _selectedAccountToKeep.value = account
    }

    /**
     * Consolidate a single contact to the selected account.
     */
    fun consolidateSingleContact() {
        val contact = _selectedContact.value ?: return
        val accountToKeep = _selectedAccountToKeep.value ?: return

        viewModelScope.launch {
            runWithPremiumCheck {
                pendingAction = null
                _uiState.value = CrossAccountUiState.Processing(0f, "Consolidating contact...")
                try {
                    val success = contactRepository.consolidateContactToAccount(
                        matchingKey = contact.matchingKey,
                        keepAccountType = accountToKeep.accountType,
                        keepAccountName = accountToKeep.accountName
                    )
                    if (success) {
                        // Clear selection and reload
                        _selectedContact.value = null
                        _selectedAccountToKeep.value = null
                        loadCrossAccountContacts()
                    } else {
                        _uiState.value = CrossAccountUiState.Error("Failed to consolidate contact")
                    }
                } catch (e: Exception) {
                    _uiState.value = CrossAccountUiState.Error(e.message ?: "Consolidation failed")
                }
            }
        }
    }

    /**
     * Consolidate all selected contacts to a specific account.
     */
    fun consolidateSelectedContacts(keepAccountType: String?, keepAccountName: String?) {
        val keys = _selectedMatchingKeys.value.toList()
        if (keys.isEmpty()) return

        viewModelScope.launch {
            runWithPremiumCheck {
                pendingAction = null
                _uiState.value = CrossAccountUiState.Processing(0f, "Consolidating contacts...")
                try {
                    contactRepository.consolidateContactsToAccount(
                        matchingKeys = keys,
                        keepAccountType = keepAccountType,
                        keepAccountName = keepAccountName
                    ).collect { status ->
                        when (status) {
                            is CleanupStatus.Progress -> {
                                _uiState.value = CrossAccountUiState.Processing(status.progress, status.message)
                            }
                            is CleanupStatus.Success -> {
                                clearSelection()
                                loadCrossAccountContacts()
                            }
                            is CleanupStatus.Error -> {
                                _uiState.value = CrossAccountUiState.Error(status.message)
                            }
                        }
                    }
                } catch (e: Exception) {
                    _uiState.value = CrossAccountUiState.Error(e.message ?: "Consolidation failed")
                }
            }
        }
    }

    /**
     * Get all unique accounts across all contacts (for bulk consolidation dialog).
     */
    fun getAllUniqueAccounts(): List<AccountInstance> {
        val seen = mutableSetOf<String>()
        return _crossAccountContacts.value
            .flatMap { it.accounts }
            .filter { account ->
                val key = "${account.accountType}:${account.accountName}"
                if (seen.contains(key)) {
                    false
                } else {
                    seen.add(key)
                    true
                }
            }
    }

    /**
     * Reset UI state to Success (clears error/processing states).
     */
    fun resetState() {
        _uiState.value = CrossAccountUiState.Success
    }

    /**
     * Retry pending action after paywall dismissal.
     * 2026 Fix: Don't unconditionally set Success - let the action determine final state
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
                if (action != null) {
                    // Increment free action usage for non-premium users
                    if (!isPremium) {
                        usageRepository.incrementFreeActions()
                    }
                    action.invoke()
                    // Note: action.invoke() will set the appropriate state (Success/Error)
                } else {
                    // No pending action - safe to set Success
                    _uiState.value = CrossAccountUiState.Success
                }
            } else {
                // Still can't perform - show paywall again
                _uiState.value = CrossAccountUiState.ShowPaywall
            }
        }
    }
}

sealed class CrossAccountUiState {
    data object Loading : CrossAccountUiState()
    data object Success : CrossAccountUiState()
    data object ShowPaywall : CrossAccountUiState()
    data class Error(val message: String) : CrossAccountUiState()
    data class Processing(val progress: Float, val message: String? = null) : CrossAccountUiState()
}
