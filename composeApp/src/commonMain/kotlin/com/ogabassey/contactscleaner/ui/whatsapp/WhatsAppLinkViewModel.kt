
package com.ogabassey.contactscleaner.ui.whatsapp
import com.ogabassey.contactscleaner.util.extractDigitsAndPlus


import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.data.api.PairingEvent
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import com.ogabassey.contactscleaner.domain.repository.WhatsAppCheckProgress
import com.ogabassey.contactscleaner.domain.repository.WhatsAppDetectorRepository
import com.ogabassey.contactscleaner.domain.repository.WhatsAppSyncProgress
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
 * State for WhatsApp contacts sync progress.
 */
@Immutable
sealed interface SyncState {
    data object Idle : SyncState
    @Immutable data class Syncing(val synced: Int, val total: Int, val percent: Int) : SyncState
    @Immutable data class Complete(val totalCount: Int, val businessCount: Int, val personalCount: Int) : SyncState
    @Immutable data class Error(val message: String) : SyncState
}

/**
 * State for explicit local-number WhatsApp checking.
 */
@Immutable
sealed interface AccuracyState {
    data object Idle : AccuracyState
    @Immutable data class Checking(
        val checked: Int,
        val total: Int,
        val whatsAppCount: Int,
        val percent: Int
    ) : AccuracyState
    @Immutable data class Complete(
        val totalChecked: Int,
        val whatsAppCount: Int,
        val updatedCount: Int
    ) : AccuracyState
    @Immutable data class Error(val message: String) : AccuracyState
}

/**
 * ViewModel for WhatsApp linking flow.
 * Manages the state of linking a user's WhatsApp account via pairing code.
 *
 * Multi-Session Support: Each device gets a unique session ID stored in Settings.
 * This allows multiple users to link their WhatsApp independently.
 */
class WhatsAppLinkViewModel(
    private val whatsAppRepository: WhatsAppDetectorRepository,
    private val settings: Settings,
    private val contactRepository: ContactRepository
) : ViewModel() {

    // 2026 Best Practice: Start with NotLinked to avoid spinner flash on screen open
    private val _state = MutableStateFlow<WhatsAppLinkState>(WhatsAppLinkState.NotLinked)
    val state: StateFlow<WhatsAppLinkState> = _state.asStateFlow()

    private val _pairingCodeExpiration = MutableStateFlow<Long?>(null)
    val pairingCodeExpiration: StateFlow<Long?> = _pairingCodeExpiration.asStateFlow()

    // Sync state for caching WhatsApp contacts after linking
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _accuracyState = MutableStateFlow<AccuracyState>(AccuracyState.Idle)
    val accuracyState: StateFlow<AccuracyState> = _accuracyState.asStateFlow()

    private var pollingJob: Job? = null
    private var timerJob: Job? = null
    private var wsJob: Job? = null
    private var syncJob: Job? = null
    private var accuracyJob: Job? = null

    /**
     * Unique device/session ID for multi-session support.
     * Generated once and persisted in Settings.
     */
    private val deviceId: String by lazy {
        settings.getStringOrNull(KEY_DEVICE_ID) ?: generateDeviceId().also {
            settings.putString(KEY_DEVICE_ID, it)
        }
    }

    init {
        checkConnectionStatus()
    }

    /**
     * Generate a unique device ID using UUID v4.
     * 2026 Best Practice: Use kotlin.uuid.Uuid for cryptographically secure IDs.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun generateDeviceId(): String = Uuid.random().toString()

    /**
     * Check if WhatsApp is already connected for this device.
     * 2026 Best Practice: Silent background check - no spinner, only update if connected.
     */
    fun checkConnectionStatus() {
        viewModelScope.launch {
            try {
                val status = whatsAppRepository.getSessionStatus(deviceId)
                if (status.connected) {
                    _state.update { WhatsAppLinkState.Connected }
                    restoreCacheStateOrSync()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Silently fail - stay on current state
            }
        }
    }

    private suspend fun restoreCacheStateOrSync() {
        val meta = whatsAppRepository.getCacheMeta()
        if (meta != null && whatsAppRepository.isCacheValid()) {
            contactRepository.recalculateWhatsAppCounts()
            _syncState.value = SyncState.Complete(
                totalCount = meta.totalCount,
                businessCount = meta.businessCount,
                personalCount = meta.personalCount
            )
        } else {
            startWhatsAppSync()
        }
    }

    /**
     * Request a pairing code for the given phone number.
     * 2026 Best Practice: Use HTTP directly - simpler, more reliable than WebSocket.
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

        // Cancel any existing jobs
        wsJob?.cancel()
        pollingJob?.cancel()
        timerJob?.cancel()

        _state.update { WhatsAppLinkState.RequestingCode(normalizedNumber) }

        wsJob = viewModelScope.launch {
            try {
                // Network check
                if (!whatsAppRepository.isServiceAvailable()) {
                    _state.update { WhatsAppLinkState.Error("WhatsApp service is currently unavailable. Please check your internet connection.") }
                    return@launch
                }

                // Use HTTP directly (no WebSocket on backend)
                requestPairingViaHttp(normalizedNumber)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { WhatsAppLinkState.Error(e.message ?: "Failed to request pairing code") }
            }
        }
    }

    /**
     * Request pairing code via HTTP and poll for connection status.
     */
    private suspend fun requestPairingViaHttp(normalizedNumber: String) {
        try {
            val code = whatsAppRepository.requestPairingCode(deviceId, normalizedNumber)
            if (code != null) {
                _state.update { WhatsAppLinkState.WaitingForPairing(code, normalizedNumber) }
                startPairingTimer()
                startPollingForConnection()
            } else {
                _state.update { WhatsAppLinkState.Error("Failed to get pairing code. Please try again.") }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { WhatsAppLinkState.Error(e.message ?: "Failed to request pairing code") }
        }
    }

    /**
     * Start a 20-minute countdown timer for the pairing code.
     * 2026 Best Practice: Use isActive for cancellable loops, ensureActive for cooperative cancellation.
     */
    private fun startPairingTimer() {
        timerJob?.cancel()
        val startTime = Clock.System.now().toEpochMilliseconds()
        val expirationMillis = startTime + (20 * 60 * 1000L)
        _pairingCodeExpiration.value = 1200L // 20 minutes in seconds

        timerJob = viewModelScope.launch {
            // 2026 Best Practice: Use isActive for cooperative cancellation
            while (isActive) {
                ensureActive() // Check for cancellation before each iteration
                val now = Clock.System.now().toEpochMilliseconds()
                val remainingSeconds = (expirationMillis - now) / 1000
                if (remainingSeconds <= 0) {
                    _pairingCodeExpiration.value = 0
                    _state.update { WhatsAppLinkState.Error("Pairing code expired. Please request a new one.") }
                    break
                }
                _pairingCodeExpiration.value = remainingSeconds
                delay(1000)
            }
        }
    }

    /**
     * Normalize phone number to E.164 format.
     */
    private fun normalizePhoneNumber(phone: String): String {
        // Remove spaces, dashes, parentheses
        val cleaned = phone.extractDigitsAndPlus()
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
     * 2026 Best Practice: Use isActive and ensureActive for cooperative cancellation.
     */
    private fun startPollingForConnection() {
        // Cancel any existing polling job
        pollingJob?.cancel()

        pollingJob = viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 40 // ~2 minutes with backoff
            var delayMs = 2000L

            while (isActive && attempts < maxAttempts) {
                ensureActive() // Check for cancellation before each iteration
                delay(delayMs)
                attempts++

                try {
                    val status = whatsAppRepository.getSessionStatus(deviceId)
                    if (status.connected) {
                        // Cancel timer when connected - job cleanup is cooperative
                        timerJob?.cancel()
                        timerJob = null
                        _pairingCodeExpiration.value = null
                        _state.update { WhatsAppLinkState.Connected }
                        // Auto-start sync after successful connection
                        startWhatsAppSync()
                        return@launch
                    }
                } catch (e: CancellationException) {
                    // 2026 Best Practice: Always rethrow CancellationException for cooperative cancellation
                    throw e
                } catch (e: Exception) {
                    // Continue polling on transient errors
                }

                // Exponential backoff: 2s -> 4s -> 8s (max)
                delayMs = (delayMs * 1.5).toLong().coerceAtMost(8000L)
            }

            // Only show timeout if we weren't cancelled
            if (isActive) {
                _state.update { WhatsAppLinkState.Error("Connection timed out. Please try again.") }
            }
        }
    }

    /**
     * Start syncing WhatsApp contacts to local cache.
     * 2026 Best Practice: Cache all 51k+ contacts locally for instant lookup during scan.
     */
    fun startWhatsAppSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _syncState.value = SyncState.Syncing(0, 0, 0)

            whatsAppRepository.syncAllContactsToCache(deviceId)
                .catch { e ->
                    // 2026 Best Practice: Rethrow CancellationException for structured concurrency
                    if (e is CancellationException) throw e
                    _syncState.value = SyncState.Error(e.message ?: "Sync failed")
                }
                .collect { progress ->
                    when (progress) {
                        is WhatsAppSyncProgress.InProgress -> {
                            val percent = if (progress.total > 0) {
                                ((progress.synced.toFloat() / progress.total) * 100).toInt()
                            } else 0
                            _syncState.value = SyncState.Syncing(
                                synced = progress.synced,
                                total = progress.total,
                                percent = percent
                            )
                        }
                        is WhatsAppSyncProgress.Complete -> {
                            contactRepository.recalculateWhatsAppCounts()
                            _syncState.value = SyncState.Complete(
                                totalCount = progress.totalCount,
                                businessCount = progress.businessCount,
                                personalCount = progress.personalCount
                            )
                        }
                        is WhatsAppSyncProgress.Error -> {
                            _syncState.value = SyncState.Error(progress.message)
                        }
                    }
                }
        }
    }

    fun improveAccuracy() {
        accuracyJob?.cancel()
        accuracyJob = viewModelScope.launch {
            try {
                val status = whatsAppRepository.getSessionStatus(deviceId)
                if (!status.connected) {
                    _accuracyState.value = AccuracyState.Error("WhatsApp is not connected")
                    return@launch
                }

                val numbers = contactRepository.getUniquePhoneNumbersForWhatsAppAccuracy()
                if (numbers.isEmpty()) {
                    _accuracyState.value = AccuracyState.Error("No phone numbers found to check")
                    return@launch
                }

                _accuracyState.value = AccuracyState.Checking(
                    checked = 0,
                    total = numbers.size,
                    whatsAppCount = 0,
                    percent = 0
                )

                whatsAppRepository.checkNumbersBatch(
                    userId = deviceId,
                    numbers = numbers,
                    batchSize = 1000
                )
                    .catch { e ->
                        if (e is CancellationException) throw e
                        _accuracyState.value = AccuracyState.Error(e.message ?: "Accuracy check failed")
                    }
                    .collect { progress ->
                        when (progress) {
                            is WhatsAppCheckProgress.InProgress -> {
                                val percent = if (progress.total > 0) {
                                    ((progress.checked.toFloat() / progress.total) * 100).toInt()
                                } else 0
                                _accuracyState.value = AccuracyState.Checking(
                                    checked = progress.checked,
                                    total = progress.total,
                                    whatsAppCount = progress.whatsappCount,
                                    percent = percent
                                )
                            }
                            is WhatsAppCheckProgress.Complete -> {
                                val detectedNumbers = progress.results
                                    .filter { it.value }
                                    .keys
                                    .toSet()

                                whatsAppRepository.replaceCacheWithDetectedNumbers(detectedNumbers)
                                val updatedCount = contactRepository.applyWhatsAppAccuracyResults(progress.results)

                                _accuracyState.value = AccuracyState.Complete(
                                    totalChecked = progress.results.size,
                                    whatsAppCount = progress.whatsappCount,
                                    updatedCount = updatedCount
                                )
                                _syncState.value = SyncState.Complete(
                                    totalCount = progress.whatsappCount,
                                    businessCount = 0,
                                    personalCount = progress.whatsappCount
                                )
                            }
                            is WhatsAppCheckProgress.Error -> {
                                _accuracyState.value = AccuracyState.Error(progress.message)
                            }
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _accuracyState.value = AccuracyState.Error(e.message ?: "Accuracy check failed")
            }
        }
    }

    /**
     * Stop any active pairing (WebSocket or polling) and reset to not linked state.
     */
    fun cancelLinking() {
        wsJob?.cancel()
        pollingJob?.cancel()
        timerJob?.cancel()
        syncJob?.cancel()
        accuracyJob?.cancel()
        wsJob = null
        pollingJob = null
        timerJob = null
        syncJob = null
        accuracyJob = null
        _pairingCodeExpiration.value = null
        _state.update { WhatsAppLinkState.NotLinked }
        // 2026 Best Practice: Reset sync state to avoid stale UI
        _syncState.value = SyncState.Idle
        _accuracyState.value = AccuracyState.Idle
    }

    /**
     * Disconnect the current WhatsApp session for this device.
     */
    fun disconnect() {
        viewModelScope.launch {
            try {
                syncJob?.cancelAndJoin()
                syncJob = null
                accuracyJob?.cancelAndJoin()
                accuracyJob = null

                val disconnected = whatsAppRepository.disconnect(deviceId)
                if (!disconnected) {
                    _syncState.value = SyncState.Error("Failed to disconnect")
                    return@launch
                }

                whatsAppRepository.clearCache()
                contactRepository.clearWhatsAppFlags()
                _state.update { WhatsAppLinkState.NotLinked }
                _syncState.value = SyncState.Idle
                _accuracyState.value = AccuracyState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _syncState.value = SyncState.Error("Failed to disconnect")
            }
        }
    }

    /**
     * Clear any error and return to not linked state.
     */
    fun clearError() {
        _state.update { WhatsAppLinkState.NotLinked }
    }

    /**
     * 2026 Best Practice: Clear sync error state separately from main state.
     * Allows UI to reset sync error without affecting pairing state.
     */
    fun clearSyncError() {
        _syncState.value = SyncState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        wsJob?.cancel()
        pollingJob?.cancel()
        timerJob?.cancel()
        syncJob?.cancel()
        wsJob = null
        pollingJob = null
        timerJob = null
        syncJob = null
    }

    companion object {
        private const val KEY_DEVICE_ID = "whatsapp_device_id"
    }
}
