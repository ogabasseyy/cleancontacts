package com.ogabassey.contactscleaner.data.util

import com.ogabassey.contactscleaner.data.repository.ScanSettingsRepository

import com.ogabassey.contactscleaner.domain.model.ScanResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanResultProvider @Inject constructor(
    private val scanSettingsRepository: ScanSettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Reactive flow of scan results
    val scanResultFlow: StateFlow<ScanResult?> = scanSettingsRepository.scanResult
        .stateIn(scope, SharingStarted.Eagerly, null)

    // Legacy support for direct access (snapshot)
    var scanResult: ScanResult?
        get() = scanResultFlow.value
        set(value) {
            scope.launch {
                value?.let { scanSettingsRepository.saveScanResult(it) }
                    ?: scanSettingsRepository.clear()
            }
        }
}

