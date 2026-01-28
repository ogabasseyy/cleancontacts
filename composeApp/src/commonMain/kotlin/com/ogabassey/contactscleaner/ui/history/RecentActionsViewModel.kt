package com.ogabassey.contactscleaner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.domain.repository.BackupRepository
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import com.ogabassey.contactscleaner.domain.repository.Snapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Recent Actions (Undo History) ViewModel for Compose Multiplatform.
 *
 * 2026 KMP Best Practice: Platform-agnostic ViewModel with Koin DI.
 */
class RecentActionsViewModel(
    private val backupRepository: BackupRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    val actions: StateFlow<List<Snapshot>> = backupRepository.getAllSnapshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _undoState = MutableStateFlow<UndoState>(UndoState.Idle)
    val undoState: StateFlow<UndoState> = _undoState.asStateFlow()

    init {
        // Cleanup old snapshots on ViewModel init
        viewModelScope.launch {
            backupRepository.cleanupOldSnapshots()
        }
    }

    fun undoAction(snapshot: Snapshot) {
        viewModelScope.launch {
            _undoState.value = UndoState.Loading
            try {
                val success = contactRepository.restoreContacts(snapshot.contacts)
                if (success) {
                    backupRepository.clearLastSnapshot(snapshot.id)
                    // 2026 KMP Best Practice: Proactively refresh the scan summary so 
                    // the dashboard UI reflects the restored state immediately.
                    contactRepository.updateScanResultSummary()
                    _undoState.value = UndoState.Success("Restored ${snapshot.contacts.size} contacts")
                } else {
                    _undoState.value = UndoState.Error("Failed to restore contacts")
                }
            } catch (e: Exception) {
                _undoState.value = UndoState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _undoState.value = UndoState.Idle
    }
}

/**
 * State for undo operations.
 */
sealed interface UndoState {
    data object Idle : UndoState
    data object Loading : UndoState
    data class Success(val message: String) : UndoState
    data class Error(val message: String) : UndoState
}
