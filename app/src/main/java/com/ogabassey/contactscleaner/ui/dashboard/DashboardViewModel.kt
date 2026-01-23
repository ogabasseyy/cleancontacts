package com.ogabassey.contactscleaner.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.domain.model.ScanResult
import com.ogabassey.contactscleaner.domain.model.ScanStatus
import com.ogabassey.contactscleaner.domain.usecase.ScanContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val scanContactsUseCase: ScanContactsUseCase,
    private val scanResultProvider: com.ogabassey.contactscleaner.data.util.ScanResultProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Idle)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // 2026 Best Practice: Reactively observe persistent results
        viewModelScope.launch {
            scanResultProvider.scanResultFlow.collect { result ->
                if (result != null && _uiState.value == DashboardUiState.Idle) {
                    _uiState.value = DashboardUiState.ShowingResults(result)
                }
            }
        }
    }

    // 2026 Best Practice: One-Shot Events for Navigation
    sealed class DashboardEvent {
        object NavigateToResults : DashboardEvent()
    }
    
    private val _events = Channel<DashboardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun showDetails() {
        viewModelScope.launch {
            _events.send(DashboardEvent.NavigateToResults)
        }
    }


    fun startScan() {
        android.util.Log.d("DashboardViewModel", "startScan called")
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Scanning(0f)
            try {
                scanContactsUseCase().collect { status ->
                    when (status) {
                        is ScanStatus.Progress -> {
                            _uiState.value = DashboardUiState.Scanning(status.progress, status.message)
                        }
                        is ScanStatus.Success -> {
                            val result = status.result
                            android.util.Log.d("DashboardViewModel", "Scan success: ${result.total} contacts")
                            scanResultProvider.scanResult = result
                            
                            // Show 100% briefly
                            _uiState.value = DashboardUiState.Scanning(1.0f) 
                            
                            kotlinx.coroutines.delay(500)
                            
                            // Trigger Navigation
                            _events.send(DashboardEvent.NavigateToResults)
                            
                            // Reset UI to Idle
                            _uiState.value = DashboardUiState.Idle
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Scan error", e)
                _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _uiState.value = DashboardUiState.Idle
    }
}

sealed class DashboardUiState {
    object Idle : DashboardUiState()
    data class Scanning(val progress: Float, val message: String? = null) : DashboardUiState()
    data class ShowingResults(val result: ScanResult) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}