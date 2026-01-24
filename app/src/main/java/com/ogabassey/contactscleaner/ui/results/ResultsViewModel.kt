package com.ogabassey.contactscleaner.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
// Formatting imports
import androidx.paging.map
import androidx.paging.insertSeparators

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.model.ScanResult
import com.ogabassey.contactscleaner.domain.usecase.CleanupContactsUseCase
import com.ogabassey.contactscleaner.domain.usecase.GetContactsPagedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val getContactsPagedUseCase: GetContactsPagedUseCase,
    private val cleanupContactsUseCase: CleanupContactsUseCase,
    private val scanResultProvider: com.ogabassey.contactscleaner.data.util.ScanResultProvider,
    private val contactRepository: com.ogabassey.contactscleaner.domain.repository.ContactRepository,
    val billingRepository: com.ogabassey.contactscleaner.domain.repository.BillingRepository,
    private val formatDetector: com.ogabassey.contactscleaner.data.detector.FormatDetector,
    private val exportUseCase: com.ogabassey.contactscleaner.domain.usecase.ExportUseCase,
    private val undoUseCase: com.ogabassey.contactscleaner.domain.usecase.UndoUseCase,
    private val usageRepository: com.ogabassey.contactscleaner.data.repository.UsageRepository
) : ViewModel() {

    val scanResult: ScanResult? = scanResultProvider.scanResult

    private val _uiState = MutableStateFlow<ResultsUiState>(ResultsUiState.Idle)
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    val freeActionsRemaining: Flow<Int> = usageRepository.freeActionsUsed.map { 
        (2 - it).coerceAtLeast(0) 
    }
    
    private val _exportEvent = kotlinx.coroutines.flow.MutableSharedFlow<android.net.Uri>()
    val exportEvent = _exportEvent.asSharedFlow()
    
    private val _duplicateGroups = MutableStateFlow<List<com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary>>(emptyList())
    val duplicateGroups: StateFlow<List<com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary>> = _duplicateGroups.asStateFlow()

    fun getPagedContacts(type: ContactType): Flow<PagingData<Contact>> {
        return getContactsPagedUseCase(type).cachedIn(viewModelScope)
    }

    // New: Paged with Headers
    fun getFormatIssuesPaged(): Flow<PagingData<FormatIssueItem>> {
        return getContactsPagedUseCase(ContactType.FORMAT_ISSUE)
            .map { pagingData ->
                pagingData.map { FormatIssueItem.ContactItem(it) }
                    .insertSeparators { before: FormatIssueItem.ContactItem?, after: FormatIssueItem.ContactItem? ->
                        // Logic to determine if we need a header
                        if (after == null) return@insertSeparators null
                        
                        val afterRegion = getRegion(after.contact.normalizedNumber)
                        val beforeRegion = if (before == null) null else getRegion(before.contact.normalizedNumber)
                        
                        if (beforeRegion != afterRegion) {
                            FormatIssueItem.Header(afterRegion, java.util.UUID.randomUUID().toString())
                        } else {
                            null
                        }
                    }
            }
            .cachedIn(viewModelScope)
    }
    
    private fun getRegion(number: String?): String {
        return formatDetector.getCountryName(number ?: "")
    }
    
    fun loadDuplicateGroups(type: ContactType) {
        viewModelScope.launch {
            _duplicateGroups.value = contactRepository.getDuplicateGroups(type)
        }
    }
    
    private val _formatGroups = MutableStateFlow<List<FormatGroup>>(emptyList())
    val formatGroups: StateFlow<List<FormatGroup>> = _formatGroups.asStateFlow()

    fun loadFormatGroups() {
        viewModelScope.launch {
            val contacts = contactRepository.getContactsSnapshotByType(ContactType.FORMAT_ISSUE)
            val grouped = contacts.groupBy { getRegion(it.normalizedNumber) }
                .map { (region, groupContacts) ->
                    FormatGroup(region, groupContacts)
                }
                .sortedBy { it.region }
            _formatGroups.value = grouped
        }
    }

    private val _accountGroups = MutableStateFlow<List<com.ogabassey.contactscleaner.domain.model.AccountGroupSummary>>(emptyList())
    val accountGroups: StateFlow<List<com.ogabassey.contactscleaner.domain.model.AccountGroupSummary>> = _accountGroups.asStateFlow()

    fun loadAccountGroups() {
        viewModelScope.launch {
            _accountGroups.value = contactRepository.getAccountGroups()
        }
    }

    private var pendingAction: (() -> Unit)? = null

    fun retryPendingAction() {
        viewModelScope.launch {
            val freeUsed = usageRepository.freeActionsUsed.first()
            if (billingRepository.isPremium.value || freeUsed < 2) {
                pendingAction?.invoke()
                pendingAction = null
            }
            _uiState.value = ResultsUiState.Idle
        }
    }

    private suspend fun runWithPremiumCheck(action: suspend () -> Unit) {
        val isPremium = billingRepository.isPremium.value
        val freeUsed = usageRepository.freeActionsUsed.first()
        
        if (isPremium || freeUsed < 2) {
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

    fun performCleanup(type: ContactType) {
        viewModelScope.launch {
            runWithPremiumCheck {
                pendingAction = null // Clear if proceeding
                
                cleanupContactsUseCase.deleteContactsByType(type).collect { status ->
                    when (status) {
                        is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Progress -> {
                            _uiState.value = ResultsUiState.Processing(status.progress, status.message)
                        }
                        is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Success -> {
                            _uiState.value = ResultsUiState.Success(status.message, canUndo = true)
                            if (type.name.startsWith("DUP_")) {
                                loadDuplicateGroups(type)
                            } else if (type == ContactType.FORMAT_ISSUE) {
                                loadFormatGroups()
                            }
                            // Refresh persistent summary
                            contactRepository.updateScanResultSummary()
                        }
    
                        is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Error -> {
                            _uiState.value = ResultsUiState.Error(status.message)
                        }
                    }
                }
            }
        }
    }
    
    fun performSingleMerge(contactIds: List<Long>, customName: String, type: ContactType) {
        viewModelScope.launch {
            runWithPremiumCheck {
                _uiState.value = ResultsUiState.Processing(0f, "Merging group...")
                val result = cleanupContactsUseCase.mergeContacts(contactIds, customName)
                if (result.isSuccess) {
                    _uiState.value = ResultsUiState.Success("Successfully merged contact")
                    loadDuplicateGroups(type)
                    // Refresh persistent summary
                    contactRepository.updateScanResultSummary()
                } else {
                    _uiState.value = ResultsUiState.Error(result.exceptionOrNull()?.message ?: "Merge failed")
                }
            }
        }
    }

    fun performMerge(type: ContactType) {
        viewModelScope.launch {
            runWithPremiumCheck {
                cleanupContactsUseCase.mergeDuplicateGroups(type).collect { status ->
                    when (status) {
                        is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Progress -> {
                            _uiState.value = ResultsUiState.Processing(status.progress, status.message)
                        }
                        is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Success -> {
                            _uiState.value = ResultsUiState.Success(status.message, canUndo = true)
                            if (type.name.startsWith("DUP_")) {
                                loadDuplicateGroups(type)
                            }
                            // Refresh persistent summary
                            contactRepository.updateScanResultSummary()
                        }
    
                        is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Error -> {
                            _uiState.value = ResultsUiState.Error(status.message)
                        }
                    }
                }
            }
        }
    }

    fun performStandardizationAll() {
        viewModelScope.launch {
            runWithPremiumCheck {
                cleanupContactsUseCase.standardizeAllFormatIssues().collect { status ->
                    when (status) {
                        is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Progress -> {
                            _uiState.value = ResultsUiState.Processing(status.progress, status.message)
                        }
                        is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Success -> {
                            _uiState.value = ResultsUiState.Success(
                                message = "${status.message}\nRescan recommended to find new duplicates.",
                                shouldRescan = true
                            )
                            loadFormatGroups() // Refresh groups
                            // Refresh persistent summary
                            contactRepository.updateScanResultSummary()
                        }
                        is com.ogabassey.contactscleaner.domain.model.CleanupStatus.Error -> {
                            _uiState.value = ResultsUiState.Error(status.message)
                        }
                    }
                }
            }
        }
    }
    
    suspend fun getContactsInGroup(key: String, type: ContactType): List<Contact> {
        return contactRepository.getContactsInGroup(key, type)
    }

    fun performGroupExport(contacts: List<Contact>, groupName: String) {
        viewModelScope.launch {
            runWithPremiumCheck {
                pendingAction = null
    
                _uiState.value = ResultsUiState.Loading
                val result = exportUseCase.exportContactList(contacts, groupName)
                
                result.onSuccess { uri ->
                    _uiState.value = ResultsUiState.Idle
                    _exportEvent.emit(uri)
                }.onFailure { e ->
                    _uiState.value = ResultsUiState.Error(e.message ?: "Export failed")
                }
            }
        }
    }

    fun performBulkExport(type: ContactType) {
        viewModelScope.launch {
            runWithPremiumCheck {
                pendingAction = null
    
                _uiState.value = ResultsUiState.Loading
                // Get current snapshot of contacts for this type
                val contacts = contactRepository.getContactsSnapshotByType(type)
                if (contacts.isEmpty()) {
                    _uiState.value = ResultsUiState.Error("No contacts to export")
                    return@runWithPremiumCheck
                }
    
                val result = exportUseCase.exportContactList(contacts, type.name)
                
                result.onSuccess { uri ->
                    _uiState.value = ResultsUiState.Idle
                    _exportEvent.emit(uri)
                }.onFailure { e ->
                    _uiState.value = ResultsUiState.Error(e.message ?: "Export failed")
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = ResultsUiState.Idle
    }

    fun undoLastAction() {
        viewModelScope.launch {
            _uiState.value = ResultsUiState.Processing(0f, "Undoing changes...")
            val result = undoUseCase.undoLastAction()
            result.onSuccess {
                _uiState.value = ResultsUiState.Success("Undo successful: $it", canUndo = false)
                // Refresh all data
                contactRepository.updateScanResultSummary()
                // We might need to refresh groups explicitly if we knew context, but generic refresh is hard without re-scan status.
                // ideally reload all loaded groups
                // For now, scan result update + user pull-to-refresh or simple data reload is okay.
            }.onFailure {
                _uiState.value = ResultsUiState.Error(it.message ?: "Undo failed")
            }
        }
    }

    // Ignore List Methods
    val ignoredContacts = cleanupContactsUseCase.getIgnoredContacts()

    fun ignoreContact(contact: Contact, reason: String) {
        viewModelScope.launch {
            cleanupContactsUseCase.ignoreContact(contact.id.toString(), contact.name ?: "Unknown", reason)
            contactRepository.updateScanResultSummary()
            // After ignoring, we might want to refresh current lists
        }
    }

    fun unignoreContact(id: String) {
        viewModelScope.launch {
            cleanupContactsUseCase.unignoreContact(id)
            contactRepository.updateScanResultSummary()
        }
    }

    fun performProtectAll(type: ContactType) {
        viewModelScope.launch {
             _uiState.value = ResultsUiState.Processing(0f, "Protecting contacts...")
             val contacts = contactRepository.getContactsSnapshotByType(type)
             contacts.forEach { contact ->
                 cleanupContactsUseCase.ignoreContact(contact.id.toString(), contact.name ?: "Unknown", contact.sensitiveDescription ?: "Potential ID")
             }
             _uiState.value = ResultsUiState.Success("Successfully protected ${contacts.size} contacts", shouldRescan = true)
             contactRepository.updateScanResultSummary()
        }
    }
}

data class FormatGroup(
    val region: String,
    val contacts: List<Contact>
)

sealed class ResultsUiState {
    object Idle : ResultsUiState()
    object Loading : ResultsUiState()
    data class Processing(val progress: Float, val message: String? = null) : ResultsUiState()
    object ShowPaywall : ResultsUiState()
    data class Success(
        val message: String, 
        val canUndo: Boolean = false,
        val shouldRescan: Boolean = false
    ) : ResultsUiState()
    data class Error(val message: String) : ResultsUiState()
}

sealed class FormatIssueItem {
    data class ContactItem(val contact: Contact) : FormatIssueItem()
    data class Header(val title: String, val id: String) : FormatIssueItem()
}
