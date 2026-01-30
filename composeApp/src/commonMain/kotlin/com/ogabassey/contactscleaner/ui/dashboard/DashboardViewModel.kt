package com.ogabassey.contactscleaner.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.data.util.ScanResultProvider
import com.ogabassey.contactscleaner.domain.model.ScanResult
import com.ogabassey.contactscleaner.domain.model.ScanStatus
import com.ogabassey.contactscleaner.platform.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * Dashboard ViewModel for Compose Multiplatform.
 *
 * 2026 KMP Best Practice: Platform-agnostic ViewModel with Koin injection.
 */
class DashboardViewModel(
    private val scanResultProvider: ScanResultProvider,
    private val scanContactsUseCase: com.ogabassey.contactscleaner.domain.usecase.ScanContactsUseCase,
    private val contactRepository: com.ogabassey.contactscleaner.domain.repository.ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Idle)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // 2026 Best Practice: One-Shot Events for Navigation
    sealed class DashboardEvent {
        data object NavigateToResults : DashboardEvent()
    }

    private val _events = Channel<DashboardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // Restore last scan result from DB on app launch
        viewModelScope.launch {
            contactRepository.updateScanResultSummary()
        }

        // Observe existing scan results from provider
        viewModelScope.launch {
            try {
                scanResultProvider.scanResultFlow.collect { result ->
                    if (result != null) {
                        val currentState = _uiState.value
                        if (currentState is DashboardUiState.Idle || currentState is DashboardUiState.ShowingResults) {
                            _uiState.value = DashboardUiState.ShowingResults(result)
                        }
                    }
                }
            } catch (e: CancellationException) {
                // 2026 Best Practice: Always re-throw CancellationException
                throw e
            } catch (e: Exception) {
                Logger.e("DashboardViewModel", "Error observing scan results: ${e.message}")
            }
        }
    }

    fun showDetails() {
        viewModelScope.launch {
            _events.send(DashboardEvent.NavigateToResults)
        }
    }

    fun startScan() {
        Logger.d("DashboardViewModel", "startScan called")
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Scanning(0f)

            try {
                scanContactsUseCase().collect { status ->
                     when(status) {
                         is ScanStatus.Progress -> {
                             _uiState.value = DashboardUiState.Scanning(status.progress, status.message)
                         }
                         is ScanStatus.Success -> {
                             _uiState.value = DashboardUiState.Scanning(1.0f)
                             delay(500)
                             // 2026 Best Practice: Check coroutine active after delay to avoid stale updates
                             ensureActive()
                             // Guard against state changes during delay
                             if (_uiState.value is DashboardUiState.Scanning) {
                                 _events.send(DashboardEvent.NavigateToResults)
                                 _uiState.value = DashboardUiState.ShowingResults(status.result)
                             }
                         }
                         is ScanStatus.Error -> {
                             Logger.e("DashboardViewModel", "Scan error: ${status.message}")
                             _uiState.value = DashboardUiState.Error(status.message)
                         }
                     }
                }
            } catch (e: Exception) {
                Logger.e("DashboardViewModel", "Scan failed: ${e.message}")
                _uiState.value = DashboardUiState.Error(e.message ?: "Scan failed unexpectedly")
            }
        }
    }

    fun resetState() {
        _uiState.value = DashboardUiState.Idle
    }
}

sealed class DashboardUiState {
    data object Idle : DashboardUiState()
    data class Scanning(val progress: Float, val message: String? = null) : DashboardUiState()
    data class ShowingResults(val result: ScanResult) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
