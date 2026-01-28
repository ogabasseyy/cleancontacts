package com.ogabassey.contactscleaner.ui.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.domain.model.PaywallPackage
import com.ogabassey.contactscleaner.domain.model.Resource
import com.ogabassey.contactscleaner.domain.repository.BillingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Paywall ViewModel for Compose Multiplatform.
 *
 * 2026 KMP Best Practice: Platform-agnostic ViewModel with Koin DI.
 */
class PaywallViewModel(
    private val billingRepository: BillingRepository
) : ViewModel() {

    val packages: StateFlow<Resource<List<PaywallPackage>>> = billingRepository.packages
    val isPremium: StateFlow<Boolean> = billingRepository.isPremium

    private val _uiState = MutableStateFlow<PaywallUiState>(PaywallUiState.Idle)
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    fun purchasePackage(packageId: String) {
        viewModelScope.launch {
            _uiState.value = PaywallUiState.Loading
            val result = billingRepository.purchasePremium(packageId)
            if (result.isSuccess) {
                _uiState.value = PaywallUiState.Success
            } else {
                _uiState.value = PaywallUiState.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error"
                )
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.value = PaywallUiState.Loading
            billingRepository.restorePurchases()
                .onSuccess {
                    if (billingRepository.isPremium.value) {
                        _uiState.value = PaywallUiState.Success
                    } else {
                        _uiState.value = PaywallUiState.Error(
                            "No active subscription found to restore."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = PaywallUiState.Error(
                        error.message ?: "Failed to restore purchases"
                    )
                }
        }
    }

    fun refresh() {
        billingRepository.refresh()
    }

    fun resetState() {
        _uiState.value = PaywallUiState.Idle
    }
}

sealed class PaywallUiState {
    data object Idle : PaywallUiState()
    data object Loading : PaywallUiState()
    data object Success : PaywallUiState()
    data class Error(val message: String) : PaywallUiState()
}
