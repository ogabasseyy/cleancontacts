package com.ogabassey.contactscleaner.data.util

import com.ogabassey.contactscleaner.domain.model.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provides access to the latest scan result across the app.
 *
 * 2026 KMP Best Practice: Singleton state holder for cross-screen data sharing.
 */
class ScanResultProvider {
    private val _scanResultFlow = MutableStateFlow<ScanResult?>(null)
    val scanResultFlow: StateFlow<ScanResult?> = _scanResultFlow.asStateFlow()

    var scanResult: ScanResult?
        get() = _scanResultFlow.value
        set(value) {
            _scanResultFlow.value = value
        }

    fun clear() {
        _scanResultFlow.value = null
    }
}
