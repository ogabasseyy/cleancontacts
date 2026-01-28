package com.ogabassey.contactscleaner.ui.whatsapp

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.domain.repository.WhatsAppDetectorRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for WhatsApp linking flow.
 * 2026 Best Practice: Use sealed interface with data object for stateless states.
 * @Immutable helps Compose compiler skip unnecessary recompositions.
 */
@Immutable
sealed interface WhatsAppLinkState {
    data object NotLinked : WhatsAppLinkState
    data object Checking : WhatsAppLinkState
    @Immutable data class RequestingCode(val phoneNumber: String) : WhatsAppLinkState
    @Immutable data class WaitingForPairing(val code: String, val phoneNumber: String) : WhatsAppLinkState
    data object Connected : WhatsAppLinkState
    @Immutable data class Error(val message: String) : WhatsAppLinkState
}

/**
 * ViewModel for WhatsApp linking flow.
 * Manages the state of linking a user's WhatsApp account via pairing code.
 */
class WhatsAppLinkViewModel(
    private val whatsAppRepository: WhatsAppDetectorRepository
) : ViewModel() {

    private val _state = MutableStateFlow<WhatsAppLinkState>(WhatsAppLinkState.Checking)
    val state: StateFlow<WhatsAppLinkState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    init {
        checkConnectionStatus()
    }

    /**
     * Check if WhatsApp is already connected.
     */
    fun checkConnectionStatus() {
        viewModelScope.launch {
            _state.update { WhatsAppLinkState.Checking }
            try {
                val status = whatsAppRepository.getSessionStatus()
                _state.update {
                    if (status.connected) WhatsAppLinkState.Connected
                    else WhatsAppLinkState.NotLinked
                }
            } catch (e: Exception) {
                _state.update { WhatsAppLinkState.NotLinked }
            }
        }
    }

    /**
     * Request a pairing code for the given phone number.
     * User will receive a notification on WhatsApp to enter this code.
     *
     * @param phoneNumber Phone number in E.164 format (e.g., +1234567890)
     */
    fun requestPairingCode(phoneNumber: String) {
        // Normalize phone number
        val normalizedNumber = normalizePhoneNumber(phoneNumber)
        if (!isValidPhoneNumber(normalizedNumber)) {
            _state.update { WhatsAppLinkState.Error("Invalid phone number format. Include country code (e.g., +1234567890)") }
            return
        }

        viewModelScope.launch {
            _state.update { WhatsAppLinkState.RequestingCode(normalizedNumber) }
            try {
                val code = whatsAppRepository.requestPairingCode(normalizedNumber)
                if (code != null) {
                    _state.update { WhatsAppLinkState.WaitingForPairing(code, normalizedNumber) }
                    startPollingForConnection()
                } else {
                    _state.update { WhatsAppLinkState.Error("Failed to get pairing code. Please try again.") }
                }
            } catch (e: Exception) {
                _state.update { WhatsAppLinkState.Error(e.message ?: "Failed to request pairing code") }
            }
        }
    }

    /**
     * Normalize phone number to E.164 format.
     */
    private fun normalizePhoneNumber(phone: String): String {
        // Remove spaces, dashes, parentheses
        val cleaned = phone.filter { it.isDigit() || it == '+' }
        // Ensure it starts with +
        return if (cleaned.startsWith("+")) cleaned else "+$cleaned"
    }

    /**
     * Validate phone number format (basic E.164 validation).
     */
    private fun isValidPhoneNumber(phone: String): Boolean {
        // E.164: starts with +, 8-15 digits total (including country code)
        if (!phone.startsWith("+")) return false
        val digits = phone.drop(1)
        return digits.length in 8..15 && digits.all { it.isDigit() }
    }

    /**
     * Start polling for connection status after pairing code is displayed.
     * Uses exponential backoff starting at 2s, max 8s.
     */
    private fun startPollingForConnection() {
        // Cancel any existing polling job
        pollingJob?.cancel()

        pollingJob = viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 40 // ~2 minutes with backoff
            var delayMs = 2000L

            while (attempts < maxAttempts) {
                delay(delayMs)
                attempts++

                try {
                    val status = whatsAppRepository.getSessionStatus()
                    if (status.connected) {
                        _state.update { WhatsAppLinkState.Connected }
                        return@launch
                    }
                } catch (e: Exception) {
                    // Continue polling on transient errors
                }

                // Exponential backoff: 2s -> 4s -> 8s (max)
                delayMs = (delayMs * 1.5).toLong().coerceAtMost(8000L)
            }

            // Timeout reached - show error with retry option
            _state.update { WhatsAppLinkState.Error("Connection timed out. Please try again.") }
        }
    }

    /**
     * Stop any active polling and reset to not linked state.
     */
    fun cancelLinking() {
        pollingJob?.cancel()
        pollingJob = null
        _state.update { WhatsAppLinkState.NotLinked }
    }

    /**
     * Disconnect the current WhatsApp session.
     */
    fun disconnect() {
        viewModelScope.launch {
            try {
                whatsAppRepository.disconnect()
                _state.update { WhatsAppLinkState.NotLinked }
            } catch (e: Exception) {
                _state.update { WhatsAppLinkState.Error("Failed to disconnect") }
            }
        }
    }

    /**
     * Clear any error and return to not linked state.
     */
    fun clearError() {
        _state.update { WhatsAppLinkState.NotLinked }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        pollingJob = null
    }
}
