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
import com.ogabassey.contactscleaner.platform.Logger
import com.ogabassey.contactscleaner.util.ExportUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Shared ViewModel for Category Detail Screens (Junk, Duplicates, Accounts, etc.)
 */
class CategoryViewModel(
    private val contactRepository: ContactRepository,
    // 2026 Fix: Make private to encapsulate implementation detail
    private val billingRepository: BillingRepository,
    private val usageRepository: UsageRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CategoryViewModel"
    }

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

    // 2026 Best Practice: Track single contact deletion for proper dialog dismissal
    private val _deletingContactId = MutableStateFlow<Long?>(null)
    val deletingContactId: StateFlow<Long?> = _deletingContactId.asStateFlow()

    // 2026 Best Practice: Export data for sharing contacts as CSV/vCard
    private val _exportData = MutableStateFlow<String?>(null)
    val exportData: StateFlow<String?> = _exportData.asStateFlow()

    // 2026 Best Practice: Track export job to cancel previous exports and prevent race conditions
    private var exportJob: Job? = null

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
        Logger.d(TAG, "runWithPremiumCheck: isPremium=$isPremium, canPerform=$canPerform")

        if (isPremium || canPerform) {
            Logger.d(TAG, "Premium check passed, executing action...")
            action()
            if (!isPremium) {
                usageRepository.incrementFreeActions()
            }
        } else {
            Logger.d(TAG, "Premium check FAILED - showing paywall")
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
            } catch (e: CancellationException) {
                throw e
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
            } catch (e: CancellationException) {
                throw e
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
                } catch (e: CancellationException) {
                    throw e
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value = CategoryUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun deleteSingleContact(contact: Contact, type: ContactType) {
        Logger.d(TAG, "deleteSingleContact called: id=${contact.id}, name=${contact.name}, platform_uid=${contact.platform_uid}")
        viewModelScope.launch {
            // 2026 Best Practice: Track which contact is being deleted for proper dialog state
            _deletingContactId.value = contact.id
            Logger.d(TAG, "Set deletingContactId to ${contact.id}")

            runWithPremiumCheck {
                Logger.d(TAG, "runWithPremiumCheck passed, executing delete...")
                pendingAction = null
                _uiState.value = CategoryUiState.Processing(0f, "Deleting contact...")
                try {
                    Logger.d(TAG, "Calling contactRepository.deleteContacts...")
                    val success = contactRepository.deleteContacts(listOf(contact)).isSuccess
                    Logger.d(TAG, "deleteContacts result: $success")
                    if (success) {
                        _uiState.value = CategoryUiState.Success
                        loadCategory(type)
                    } else {
                        _uiState.value = CategoryUiState.Error("Failed to delete contact")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value = CategoryUiState.Error(e.message ?: "Deletion failed")
                } finally {
                    // Clear deletion tracking INSIDE the action to avoid clearing on paywall
                    _deletingContactId.value = null
                }
            }

            // If paywall was shown (action not executed), clear the tracking
            if (_uiState.value is CategoryUiState.ShowPaywall) {
                _deletingContactId.value = null
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
     * Helper function to perform export with proper job cancellation and exception handling.
     * 2026 Best Practice: Centralized export logic to reduce duplication.
     * - Cancels any previous export job first to prevent race conditions
     * - Runs export on Default dispatcher to avoid UI jank with large contact lists (50k+)
     * - Handles exceptions gracefully
     */
    private fun performExport(contacts: List<Contact>, exporter: (List<Contact>) -> String) {
        // Cancel previous job FIRST, before any other logic
        exportJob?.cancel()

        if (contacts.isEmpty()) {
            _exportData.value = null
            return
        }

        exportJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    exporter(contacts)
                }
                _exportData.value = result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, "Export failed: ${e.message}", e)
                _exportData.value = null
            }
        }
    }

    /** Export current contacts to CSV format. */
    fun exportToCsv() = performExport(_contacts.value, ExportUtils::contactsToCsv)

    /** Export current contacts to vCard format. */
    fun exportToVCard() = performExport(_contacts.value, ExportUtils::contactsToVCard)

    /** Export group contacts to CSV format. */
    fun exportGroupToCsv() = performExport(_groupContacts.value, ExportUtils::contactsToCsv)

    /** Export group contacts to vCard format. */
    fun exportGroupToVCard() = performExport(_groupContacts.value, ExportUtils::contactsToVCard)

    /**
     * Clear export data after user has copied/shared.
     */
    fun clearExportData() {
        _exportData.value = null
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
                    action.invoke()
                    // 2026 Fix: Increment AFTER action to match runWithPremiumCheck behavior
                    // This way, failed actions don't consume the user's free trial
                    if (!isPremium) {
                        usageRepository.incrementFreeActions()
                    }
                } else {
                    // No pending action - safe to set Success
                    _uiState.value = CategoryUiState.Success
                }
            } else {
                // Still can't perform - show paywall again
                _uiState.value = CategoryUiState.ShowPaywall
            }
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
