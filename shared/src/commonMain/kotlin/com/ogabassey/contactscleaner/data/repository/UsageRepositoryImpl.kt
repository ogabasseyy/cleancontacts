package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.domain.repository.UsageRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Implementation of UsageRepository using Multiplatform Settings.
 *
 * 2026 KMP Best Practice: Cross-platform key-value storage.
 */
class UsageRepositoryImpl(
    private val settings: Settings
) : UsageRepository {

    // Use StateFlow to track changes locally (Settings doesn't have built-in flow support on all platforms)
    private val _freeActionsUsed = MutableStateFlow(settings.getInt(KEY_FREE_ACTIONS_USED, 0))

    override val freeActionsUsed: Flow<Int> = _freeActionsUsed

    override val freeActionsRemaining: Flow<Int> = freeActionsUsed.map { used ->
        (UsageRepository.MAX_FREE_ACTIONS - used).coerceAtLeast(0)
    }

    override suspend fun incrementFreeActions() {
        val newValue = _freeActionsUsed.value + 1
        settings.putInt(KEY_FREE_ACTIONS_USED, newValue)
        _freeActionsUsed.value = newValue
    }

    override suspend fun canPerformFreeAction(): Boolean {
        return _freeActionsUsed.value < UsageRepository.MAX_FREE_ACTIONS
    }

    private val _rawScannedCount = MutableStateFlow(settings.getInt(KEY_RAW_SCANNED_COUNT, 0))

    override val rawScannedCount: Flow<Int> = _rawScannedCount

    override suspend fun updateRawScannedCount(count: Int) {
        settings.putInt(KEY_RAW_SCANNED_COUNT, count)
        _rawScannedCount.value = count
    }

    companion object {
        private const val KEY_FREE_ACTIONS_USED = "free_actions_used"
        private const val KEY_RAW_SCANNED_COUNT = "raw_scanned_count"
    }
}
