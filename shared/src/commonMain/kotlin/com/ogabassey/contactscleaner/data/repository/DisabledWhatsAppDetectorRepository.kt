package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.data.api.PairingEvent
import com.ogabassey.contactscleaner.data.api.SessionStatus
import com.ogabassey.contactscleaner.data.api.WhatsAppContactsResponse
import com.ogabassey.contactscleaner.data.db.entity.WhatsAppCacheMeta
import com.ogabassey.contactscleaner.domain.repository.CacheSnapshot
import com.ogabassey.contactscleaner.domain.repository.WhatsAppCheckProgress
import com.ogabassey.contactscleaner.domain.repository.WhatsAppDetectorRepository
import com.ogabassey.contactscleaner.domain.repository.WhatsAppSyncProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Safe fallback used when the WhatsApp detector is not configured for the build.
 *
 * This keeps app startup and non-WhatsApp features working instead of crashing
 * the whole process because an optional integration secret is missing.
 */
class DisabledWhatsAppDetectorRepository(
    private val reason: String = DEFAULT_REASON
) : WhatsAppDetectorRepository {

    override suspend fun isServiceAvailable(): Boolean = false

    override suspend fun getSessionStatus(userId: String): SessionStatus {
        return SessionStatus(connected = false, error = reason)
    }

    override suspend fun requestPairingCode(userId: String, phoneNumber: String): String? = null

    override suspend fun disconnect(userId: String): Boolean = false

    override suspend fun checkNumbers(userId: String, numbers: List<String>): Map<String, Boolean> = emptyMap()

    override fun checkNumbersBatch(
        userId: String,
        numbers: List<String>,
        batchSize: Int
    ): Flow<WhatsAppCheckProgress> = flow {
        emit(WhatsAppCheckProgress.Error(reason))
    }

    override fun connectForPairing(userId: String, phoneNumber: String): Flow<PairingEvent> = flow {
        emit(PairingEvent.Error(reason))
    }

    override suspend fun getContacts(userId: String): WhatsAppContactsResponse {
        return WhatsAppContactsResponse(success = false, error = reason)
    }

    override fun syncAllContactsToCache(userId: String): Flow<WhatsAppSyncProgress> = flow {
        emit(WhatsAppSyncProgress.Error(reason))
    }

    override suspend fun getCachedNumbers(): Set<String> = emptySet()

    override suspend fun getCachedBusinessNumbers(): Set<String> = emptySet()

    override suspend fun isNumberCached(normalizedNumber: String): Boolean = false

    override suspend fun getCacheMeta(): WhatsAppCacheMeta? = null

    override fun getCacheMetaFlow(): Flow<WhatsAppCacheMeta?> = flow {
        emit(null)
    }

    override suspend fun isCacheValid(): Boolean = false

    override suspend fun getValidCacheSnapshot(): CacheSnapshot = CacheSnapshot.Invalid

    override suspend fun clearCache() = Unit

    companion object {
        const val DEFAULT_REASON =
            "WhatsApp linking is unavailable in this build because the detector configuration is missing."
    }
}
