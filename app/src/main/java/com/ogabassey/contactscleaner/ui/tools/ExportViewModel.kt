package com.ogabassey.contactscleaner.ui.tools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.usecase.ExportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportUseCase: ExportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val _selectedTypes = MutableStateFlow<Set<ContactType>>(setOf(ContactType.ALL))
    val selectedTypes: StateFlow<Set<ContactType>> = _selectedTypes.asStateFlow()

    fun toggleType(type: ContactType) {
        val current = _selectedTypes.value.toMutableSet()
        if (current.contains(type)) {
            current.remove(type)
        } else {
            current.add(type)
        }
        _selectedTypes.value = current
    }

    fun export() {
        if (_selectedTypes.value.isEmpty()) return
        
        viewModelScope.launch {
            _uiState.value = ExportUiState.Loading
            val result = exportUseCase(_selectedTypes.value)
            
            result.onSuccess { uri ->
                _uiState.value = ExportUiState.Success(uri)
            }.onFailure { e ->
                _uiState.value = ExportUiState.Error(e.message ?: "Unknown Export Error")
            }
        }
    }
    
    fun reset() {
        _uiState.value = ExportUiState.Idle
    }
}

sealed class ExportUiState {
    object Idle : ExportUiState()
    object Loading : ExportUiState()
    data class Success(val fileUri: Uri) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
}
