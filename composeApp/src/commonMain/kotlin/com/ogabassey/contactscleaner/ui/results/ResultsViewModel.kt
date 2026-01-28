package com.ogabassey.contactscleaner.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.data.util.ScanResultProvider
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary
import com.ogabassey.contactscleaner.domain.model.ScanResult
import com.ogabassey.contactscleaner.domain.repository.BillingRepository
import com.ogabassey.contactscleaner.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Results ViewModel for Compose Multiplatform.
 *
 * 2026 KMP Best Practice: Simplified ViewModel without PagingData for cross-platform.
 */
class ResultsViewModel(
    private val scanResultProvider: ScanResultProvider,
    private val contactRepository: com.ogabassey.contactscleaner.domain.repository.ContactRepository,
    private val cleanupContactsUseCase: com.ogabassey.contactscleaner.domain.usecase.CleanupContactsUseCase,
    val billingRepository: BillingRepository,
    private val usageRepository: UsageRepository,
    private val undoUseCase: com.ogabassey.contactscleaner.domain.usecase.UndoUseCase // Added
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

    private var pendingAction: (() -> Unit)? = null

    /**
     * Run an action with premium/trial check.
     * Shows paywall if user has exhausted free actions and is not premium.
     */
    private suspend fun runWithPremiumCheck(action: suspend () -> Unit) {
        val isPremium = billingRepository.isPremium.value
        val canPerform = usageRepository.canPerformFreeAction()

        if (isPremium || canPerform) {
            action()
            if (!isPremium) {
                usageRepository.incrementFreeActions()
            }
        } else {
            pendingAction = {
                viewModelScope.launch { runWithPremiumCheck(action) }
            }
            _uiState.value = ResultsUiState.ShowPaywall
        }
    }

    fun retryPendingAction() {
        viewModelScope.launch {
            val isPremium = billingRepository.isPremium.value
            val canPerform = usageRepository.canPerformFreeAction()

            if (isPremium || canPerform) {
                pendingAction?.invoke()
                pendingAction = null
            }
            _uiState.value = ResultsUiState.Idle
        }
    }

    fun loadContacts(type: ContactType) {
        viewModelScope.launch {
            _uiState.value = ResultsUiState.Loading
            try {
                val result = contactRepository.getContactsSnapshotByType(type)
                _contacts.value = result
                _uiState.value = ResultsUiState.Idle
            } catch (e: Exception) {
                _uiState.value = ResultsUiState.Error("Failed to load contacts: ${e.message}")
            }
        }
    }

    fun loadDuplicateGroups(type: ContactType) {
        viewModelScope.launch {
            try {
                _duplicateGroups.value = contactRepository.getDuplicateGroups(type)
            } catch (e: Exception) {
                 // Silent error for groups for now
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
                } catch (e: Exception) {
                    _uiState.value = ResultsUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }



    fun resetState() {
        _uiState.value = ResultsUiState.Idle
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
