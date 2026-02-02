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
import com.ogabassey.contactscleaner.util.BackgroundOperationManager
import com.ogabassey.contactscleaner.util.ExportUtils
import com.ogabassey.contactscleaner.util.OperationType
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

    // Trial actions tracking (limit is 2)
    val freeActionsRemaining: Flow<Int> = usageRepository.freeActionsRemaining

    // Premium status for UI to hide free actions pill
    val isPremium: StateFlow<Boolean> = billingRepository.isPremium

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

    fun loadCategory(type: ContactType, successMessage: String? = null) {
        viewModelScope.launch {
            // 2026 Safety: Clear any stale pending actions when loading category
            // This prevents leftover actions from previous sessions from executing unexpectedly
            actionMutex.withLock {
                pendingAction = null
            }
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
                _uiState.value = CategoryUiState.Success(successMessage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error("Failed to load contacts: ${e.message}")
            }
        }
    }

    /**
     * 2026 Fix: Reload category from database.
     * Called when returning from native Contacts app.
     * Note: A full rescan is heavy and should only be done from the dashboard.
     * For now, just reload the cached data - the next scan will pick up device changes.
     */
    fun refreshAndLoadCategory(type: ContactType) {
        // Simply delegate to loadCategory for now
        // A full rescan is too heavy and can cause UI issues
        loadCategory(type)
    }

    /**
     * Refresh a specific contact after editing in native app.
     * This calls the repository to fetch fresh data from the provider and update the DB.
     * Then it silently updates the UI state.
     */
    fun refreshSpecificContact(contact: Contact, type: ContactType) {
        viewModelScope.launch {
            try {
                // 1. Refresh data in DB
                contactRepository.refreshContacts(listOf(contact))

                // 2. Silently reload list (no Loading state)
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
            } catch (e: CancellationException) {
                // 2026 Fix: Propagate cancellation properly
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to refresh specific contact", e)
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
                        // 2026 Best Practice: Hint to refresh Results after merge
                        loadCategory(type, "Pull down on Results to refresh counts")
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
        // 2026 Fix: Capture job reference to enable proper cancellation
        val job = viewModelScope.launch {
            runWithPremiumCheck {
                pendingAction = null

                // Determine operation type and get item count for BackgroundOperationManager
                val operationType = when (type) {
                    ContactType.FORMAT_ISSUE -> OperationType.STANDARDIZE_FORMAT
                    ContactType.DUPLICATE, ContactType.DUP_NUMBER, ContactType.DUP_EMAIL,
                    ContactType.DUP_NAME, ContactType.DUP_SIMILAR_NAME -> OperationType.MERGE_DUPLICATES
                    else -> OperationType.DELETE_CONTACTS
                }

                // Get count for progress tracking
                val itemCount = when {
                    type.name.startsWith("DUP") || type == ContactType.DUPLICATE -> _duplicateGroups.value.size
                    else -> _contacts.value.size
                }

                // Start background operation with streaming progress UI
                BackgroundOperationManager.startOperation(
                    type = operationType,
                    totalItems = itemCount,
                    title = operationType.displayName
                )

                // Hide the old processing overlay - BackgroundOperationManager handles UI now
                _uiState.value = CategoryUiState.Success()

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

                    // Mark operation as complete
                    BackgroundOperationManager.complete(true, "Operation completed successfully")

                    // 2026 Best Practice: Refresh list with hint to pull-to-refresh on Results page
                    loadCategory(type, "Pull down on Results to refresh counts")
                } catch (e: CancellationException) {
                    BackgroundOperationManager.cancel()
                    throw e
                } catch (e: Exception) {
                    BackgroundOperationManager.complete(false, e.message ?: "Unknown error")
                    _uiState.value = CategoryUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
        // 2026 Fix: Register job for proper cancellation support
        BackgroundOperationManager.registerJob(job)
    }

    fun deleteSingleContact(contact: Contact, type: ContactType) {
        Logger.d(TAG, "deleteSingleContact called: id=${contact.id}")
        viewModelScope.launch {
            // 2026 Best Practice: Track which contact is being deleted for proper dialog state
            _deletingContactId.value = contact.id
            Logger.d(TAG, "Set deletingContactId to ${contact.id}")

            runWithPremiumCheck {
                Logger.d(TAG, "runWithPremiumCheck passed, executing delete...")
                pendingAction = null
                // 2026 Fix: Don't show Processing overlay for single delete - causes layout shift
                // The dialog already shows "Deleting..." state via deletingContactId tracking
                try {
                    Logger.d(TAG, "Calling contactRepository.deleteContacts...")
                    val success = contactRepository.deleteContacts(listOf(contact)).isSuccess
                    Logger.d(TAG, "deleteContacts result: $success")
                    if (success) {
                        // 2026 Fix: Use optimistic update - remove from local list directly
                        // instead of calling loadCategory() which triggers Loading state
                        // This prevents the visible layout shift on Android
                        _contacts.value = _contacts.value.filter { it.id != contact.id }
                        _groupContacts.value = _groupContacts.value.filter { it.id != contact.id }

                        // Update duplicate groups if applicable
                        if (type.name.startsWith("DUP_") || type == ContactType.DUPLICATE) {
                            // Reload groups in background without changing UI state
                            try {
                                val groups = contactRepository.getDuplicateGroups(type)
                                _duplicateGroups.value = groups
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Logger.e(TAG, "Failed to refresh duplicate groups: ${e.message}", e)
                            }
                        }

                        // Update scan result summaries in background
                        // 2026 Fix: Wrap in try-catch to prevent exceptions from breaking deletion flow
                        try {
                            contactRepository.updateScanResultSummary()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Logger.e(TAG, "Failed to update scan result summary: ${e.message}", e)
                        }

                        // Stay in Success state - no Loading transition
                        _uiState.value = CategoryUiState.Success()
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
                // Update BackgroundOperationManager for streaming UI
                val details = status.details
                if (details != null) {
                    BackgroundOperationManager.updateProgress(
                        processed = details.processed,
                        currentItem = details.currentItem
                    )
                    // Add recent items to log for streaming display
                    details.recentItems.firstOrNull()?.let { recentItem ->
                        BackgroundOperationManager.addLogEntry(recentItem, isSuccess = true)
                    }
                } else {
                    // Fallback for old-style progress without details
                    // 2026 Fix: Calculate processed items correctly based on totalItems
                    val totalItems = BackgroundOperationManager.currentOperation.value?.totalItems ?: 100
                    val processed = (status.progress * totalItems).toInt()
                    BackgroundOperationManager.updateProgress(processed, status.message)
                }

                // Still update local state for screens not using BackgroundOperationManager
                _uiState.value = CategoryUiState.Processing(status.progress, status.message)
            }
            is CleanupStatus.Success -> {
                // Note: This gets overwritten by loadCategory, which provides the hint
                _uiState.value = CategoryUiState.Success()
            }
            is CleanupStatus.Error -> {
                BackgroundOperationManager.complete(false, status.message)
                _uiState.value = CategoryUiState.Error(status.message)
            }
        }
    }

    /**
     * Reset UI state to Success (clears error/processing states).
     */
    fun resetState() {
        _uiState.value = CategoryUiState.Success()
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
                    _uiState.value = CategoryUiState.Success()
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
    // 2026 Best Practice: Include optional message for success feedback (e.g., refresh hints)
    data class Success(val message: String? = null) : CategoryUiState()
    data object ShowPaywall : CategoryUiState()
    data class Error(val message: String) : CategoryUiState()
    data class Processing(val progress: Float, val message: String? = null) : CategoryUiState()
}
