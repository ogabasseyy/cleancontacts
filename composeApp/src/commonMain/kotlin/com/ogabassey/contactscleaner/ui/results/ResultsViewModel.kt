package com.ogabassey.contactscleaner.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.data.util.ScanResultProvider
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary
import com.ogabassey.contactscleaner.domain.model.ScanResult
import com.ogabassey.contactscleaner.domain.model.ScanStatus
import com.ogabassey.contactscleaner.domain.repository.BillingRepository
import com.ogabassey.contactscleaner.domain.repository.UsageRepository
import com.ogabassey.contactscleaner.domain.usecase.ScanContactsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Results ViewModel for Compose Multiplatform.
 *
 * 2026 KMP Best Practice: Simplified ViewModel without PagingData for cross-platform.
 */
class ResultsViewModel(
    private val scanResultProvider: ScanResultProvider,
    private val contactRepository: com.ogabassey.contactscleaner.domain.repository.ContactRepository,
    private val cleanupContactsUseCase: com.ogabassey.contactscleaner.domain.usecase.CleanupContactsUseCase,
    // 2026 Fix: Make private to encapsulate implementation detail
    private val billingRepository: BillingRepository,
    private val usageRepository: UsageRepository,
    private val undoUseCase: com.ogabassey.contactscleaner.domain.usecase.UndoUseCase,
    // 2026 Feature: Pull-to-refresh rescan support
    private val scanContactsUseCase: ScanContactsUseCase
) : ViewModel() {

    // ... (rest of class) ...

    fun undoLastAction() {
        viewModelScope.launch {
            _uiState.value = ResultsUiState.Processing(0f, "Undoing changes...")
            // Fix: undoLastAction returns Result<String>
            val result = undoUseCase.undoLastAction() 
            result.onSuccess {
                _uiState.value = ResultsUiState.Success("Undo successful: $it", canUndo = false)
                contactRepository.updateScanResultSummary()
            }.onFailure {
                _uiState.value = ResultsUiState.Error(it.message ?: "Undo failed")
            }
        }
    }

    val scanResult: StateFlow<ScanResult?> = scanResultProvider.scanResultFlow

    private val _uiState = MutableStateFlow<ResultsUiState>(ResultsUiState.Idle)
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    // Trial actions tracking (limit is 1)
    val freeActionsRemaining: Flow<Int> = usageRepository.freeActionsRemaining

    // 2026 Best Practice: Pull-to-refresh state for rescan with progress
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshProgress = MutableStateFlow(RefreshProgress())
    val refreshProgress: StateFlow<RefreshProgress> = _refreshProgress.asStateFlow()

    /**
     * Trigger a full rescan of contacts from device.
     * Used by pull-to-refresh gesture on Results screen.
     *
     * 2026 Best Practice: Full rescan ensures fresh data from device,
     * detecting any contacts added/edited/deleted in native Contacts app.
     */
    fun rescan() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _refreshProgress.value = RefreshProgress(0f, "Starting scan...")
            try {
                scanContactsUseCase().collect { status ->
                    when (status) {
                        is ScanStatus.Success -> {
                            _refreshProgress.value = RefreshProgress(1f, "Scan complete!")
                            _isRefreshing.value = false
                        }
                        is ScanStatus.Error -> {
                            _isRefreshing.value = false
                            _refreshProgress.value = RefreshProgress()
                            _uiState.value = ResultsUiState.Error(status.message)
                        }
                        is ScanStatus.Progress -> {
                            _refreshProgress.value = RefreshProgress(status.progress, status.message)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isRefreshing.value = false
                _refreshProgress.value = RefreshProgress()
                _uiState.value = ResultsUiState.Error(e.message ?: "Rescan failed")
            }
        }
    }

    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroupSummary>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroupSummary>> = _duplicateGroups.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _accountGroups = MutableStateFlow<List<com.ogabassey.contactscleaner.domain.model.AccountGroupSummary>>(emptyList())
    val accountGroups: StateFlow<List<com.ogabassey.contactscleaner.domain.model.AccountGroupSummary>> = _accountGroups.asStateFlow()

    fun loadAccountGroups() {
        viewModelScope.launch {
            try {
                _accountGroups.value = contactRepository.getAccountGroups()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    // Expose counts for UI
    val accountsCount: Flow<Int> = contactRepository.getAccountCount()
    
    val allIssuesCount: Flow<Int> = scanResult.map { it?.let { res ->
        res.junkCount + res.duplicateCount + res.formatIssueCount + res.sensitiveCount + res.fancyFontCount
    } ?: 0 }

    // 2026 Best Practice: Mutex for thread-safe access to pendingAction
    private val actionMutex = Mutex()
    private var pendingAction: (suspend () -> Unit)? = null

    /**
     * Run an action with premium/trial check.
     * Shows paywall if user has exhausted free actions and is not premium.
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
            _uiState.value = ResultsUiState.ShowPaywall
        }
    }

    /**
     * Retry pending action after paywall dismissal.
     * 2026 Fix: Don't unconditionally set Idle - let the action determine final state
     * 2026 Fix: Increment free-action usage on retry for non-premium users
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
                    // No pending action - safe to set Idle
                    _uiState.value = ResultsUiState.Idle
                }
            } else {
                // Still can't perform - show paywall again
                _uiState.value = ResultsUiState.ShowPaywall
            }
        }
    }

    fun loadContacts(type: ContactType) {
        viewModelScope.launch {
            _uiState.value = ResultsUiState.Loading
            try {
                val result = contactRepository.getContactsSnapshotByType(type)
                _contacts.value = result
                _uiState.value = ResultsUiState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ResultsUiState.Error("Failed to load contacts: ${e.message}")
            }
        }
    }

    fun loadDuplicateGroups(type: ContactType) {
        viewModelScope.launch {
            try {
                _duplicateGroups.value = contactRepository.getDuplicateGroups(type)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Error loading duplicate groups: ${e.message}")
                _duplicateGroups.value = emptyList()
            }
        }
    }

    fun performCleanup(type: ContactType) {
        viewModelScope.launch {
            runWithPremiumCheck {
                pendingAction = null
                _uiState.value = ResultsUiState.Processing(0f, "Cleaning up...")
                try {
                    cleanupContactsUseCase.deleteByType(type).collect { status ->
                        when (status) {
                            is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Progress -> {
                                _uiState.value = ResultsUiState.Processing(status.progress, status.message)
                            }
                            is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Success -> {
                                _uiState.value = ResultsUiState.Success(status.message, canUndo = true)
                                loadContacts(type)
                            }
                            is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Error -> {
                                _uiState.value = ResultsUiState.Error(status.message)
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value = ResultsUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun performMerge(type: ContactType) {
        viewModelScope.launch {
            runWithPremiumCheck {
                pendingAction = null
                _uiState.value = ResultsUiState.Processing(0f, "Merging duplicates...")
                try {
                    cleanupContactsUseCase.mergeDuplicates(type).collect { status ->
                        when (status) {
                            is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Progress -> {
                                _uiState.value = ResultsUiState.Processing(status.progress, status.message)
                            }
                            is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Success -> {
                                _uiState.value = ResultsUiState.Success(status.message, canUndo = true)
                                loadDuplicateGroups(type)
                            }
                            is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Error -> {
                                _uiState.value = ResultsUiState.Error(status.message)
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value = ResultsUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }



    fun resetState() {
        _uiState.value = ResultsUiState.Idle
    }

    /**
     * Recalculate WhatsApp/Non-WhatsApp counts after sync completes.
     * Updates contact flags in DB based on cached WhatsApp numbers.
     */
    fun recalculateWhatsAppCounts() {
        viewModelScope.launch {
            try {
                contactRepository.recalculateWhatsAppCounts()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("❌ Failed to recalculate WhatsApp counts: ${e.message}")
            }
        }
    }
}

sealed class ResultsUiState {
    data object Idle : ResultsUiState()
    data object Loading : ResultsUiState()
    data class Processing(val progress: Float, val message: String? = null) : ResultsUiState()
    data object ShowPaywall : ResultsUiState()
    data class Success(
        val message: String,
        val canUndo: Boolean = false,
        val shouldRescan: Boolean = false
    ) : ResultsUiState()
    data class Error(val message: String) : ResultsUiState()
}

data class FormatGroup(
    val region: String,
    val contacts: List<Contact>
)

/**
 * Progress state for pull-to-refresh rescan.
 * Shows scan progress and status message during refresh.
 */
data class RefreshProgress(
    val progress: Float = 0f,
    val message: String? = null
)
