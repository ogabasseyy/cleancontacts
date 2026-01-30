package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.data.api.PairingEvent
import com.ogabassey.contactscleaner.data.api.SessionStatus
import com.ogabassey.contactscleaner.data.api.WhatsAppContactsResponse
import com.ogabassey.contactscleaner.data.api.WhatsAppDetectorApi
import com.ogabassey.contactscleaner.data.db.dao.WhatsAppCacheDao
import com.ogabassey.contactscleaner.data.db.entity.WhatsAppCacheEntry
import com.ogabassey.contactscleaner.data.db.entity.WhatsAppCacheMeta
import com.ogabassey.contactscleaner.domain.repository.WhatsAppCheckProgress
import com.ogabassey.contactscleaner.domain.repository.WhatsAppDetectorRepository
import com.ogabassey.contactscleaner.domain.repository.WhatsAppSyncProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

/**
 * Implementation of WhatsAppDetectorRepository using the VPS-hosted Baileys service.
 *
 * 2026 Best Practice: Repository pattern with clean API abstraction.
 * Multi-Session Support: Each user gets their own isolated WhatsApp session.
 * Local Caching: WhatsApp contacts are cached locally for instant lookups.
 */
class WhatsAppDetectorRepositoryImpl(
    private val api: WhatsAppDetectorApi = WhatsAppDetectorApi(),
    private val cacheDao: WhatsAppCacheDao? = null
) : WhatsAppDetectorRepository {

    companion object {
        private const val CACHE_VALIDITY_HOURS = 24
        private const val PAGE_SIZE = 500
    }

    override suspend fun isServiceAvailable(): Boolean {
        val health = api.checkHealth()
        return health.status == "ok"
    }

    override suspend fun getSessionStatus(userId: String): SessionStatus {
        return api.getSessionStatus(userId)
    }

    override suspend fun requestPairingCode(userId: String, phoneNumber: String): String? {
        val response = api.requestPairingCode(userId, phoneNumber)
        return if (response.success) response.code else null
    }

    override suspend fun disconnect(userId: String): Boolean {
        val response = api.disconnect(userId)
        return response.success
    }

    /**
     * Connect via WebSocket for real-time pairing events.
     * Provides instant notifications instead of polling.
     */
    override fun connectForPairing(userId: String, phoneNumber: String): Flow<PairingEvent> {
        return api.connectForPairing(userId, phoneNumber)
    }

    override suspend fun checkNumbers(userId: String, numbers: List<String>): Map<String, Boolean> {
        if (numbers.isEmpty()) return emptyMap()

        val response = api.checkNumbers(userId, numbers)
        if (!response.success) return emptyMap()

        return response.results.associate { it.number to it.hasWhatsApp }
    }

    override fun checkNumbersBatch(userId: String, numbers: List<String>): Flow<WhatsAppCheckProgress> = flow {
        if (numbers.isEmpty()) {
            emit(WhatsAppCheckProgress.Complete(emptyMap(), 0))
            return@flow
        }

        val batchSize = 50
        val delayMs = 2000L // Increased delay for rate limiting
        val allResults = mutableMapOf<String, Boolean>()
        var whatsappCount = 0

        val batches = numbers.chunked(batchSize)
        var checkedCount = 0

        for (batch in batches) {
            val response = api.checkNumbers(userId, batch)

            if (!response.success) {
                emit(WhatsAppCheckProgress.Error(response.error ?: "Failed to check numbers"))
                return@flow
            }

            for (result in response.results) {
                allResults[result.number] = result.hasWhatsApp
                if (result.hasWhatsApp) whatsappCount++
            }

            checkedCount += batch.size
            emit(WhatsAppCheckProgress.InProgress(
                checked = checkedCount,
                total = numbers.size,
                whatsappCount = whatsappCount
            ))

            // Rate limiting between batches
            if (checkedCount < numbers.size) {
                delay(delayMs)
            }
        }

        emit(WhatsAppCheckProgress.Complete(allResults, whatsappCount))
    }

    /**
     * Get all WhatsApp contacts with business detection.
     * Privacy-compliant: No contacts are sent TO the server.
     */
    override suspend fun getContacts(userId: String): WhatsAppContactsResponse {
        return api.getContacts(userId)
    }

    // ============================================
    // WhatsApp Cache Implementation
    // ============================================

    /**
     * Sync all WhatsApp contacts to local cache.
     * Fetches contacts in pages of 500 until all are retrieved.
     */
    override fun syncAllContactsToCache(userId: String): Flow<WhatsAppSyncProgress> = flow {
        if (cacheDao == null) {
            emit(WhatsAppSyncProgress.Error("Cache not available"))
            return@flow
        }

        try {
            // Mark sync as in progress
            cacheDao.setSyncInProgress(true)

            // 2026 Best Practice: Don't delete early - use atomic replace at end
            var offset = 0
            var totalCount = 0
            var businessCount = 0
            var personalCount = 0
            var hasMore = true
            val allEntries = mutableListOf<WhatsAppCacheEntry>()

            // Fetch all contacts paginated
            while (hasMore) {
                val response = api.getContacts(userId, limit = PAGE_SIZE, offset = offset)

                if (!response.success) {
                    emit(WhatsAppSyncProgress.Error(response.error ?: "Failed to fetch contacts"))
                    cacheDao.setSyncInProgress(false)
                    return@flow
                }

                // Update total from first response
                if (offset == 0) {
                    totalCount = response.total
                }

                // Convert contacts to cache entries
                val entries = response.contacts.map { contact ->
                    val normalized = contact.phoneNumber.filter { it.isDigit() }
                    WhatsAppCacheEntry(
                        normalizedNumber = normalized,
                        isBusiness = contact.isBusiness,
                        lastSynced = Clock.System.now().toEpochMilliseconds()
                    )
                }
                allEntries.addAll(entries)

                // Update counts
                businessCount += response.contacts.count { it.isBusiness }
                personalCount += response.contacts.count { !it.isBusiness }

                // Emit progress
                emit(WhatsAppSyncProgress.InProgress(
                    synced = allEntries.size,
                    total = totalCount,
                    businessCount = businessCount,
                    personalCount = personalCount
                ))

                // Check if we have more pages
                offset += PAGE_SIZE
                hasMore = response.contacts.size == PAGE_SIZE && offset < totalCount

                // Small delay between pages to avoid overwhelming the server
                if (hasMore) {
                    delay(100)
                }
            }

            // 2026 Best Practice: Atomic replace - delete old + insert new in single transaction
            // Prevents cache loss if operation fails partway through
            val meta = WhatsAppCacheMeta(
                key = "sync_status",
                lastFullSync = Clock.System.now().toEpochMilliseconds(),
                totalCount = allEntries.size,
                businessCount = businessCount,
                personalCount = personalCount,
                syncInProgress = false
            )
            cacheDao.replaceAllEntries(allEntries, meta)

            emit(WhatsAppSyncProgress.Complete(
                totalCount = allEntries.size,
                businessCount = businessCount,
                personalCount = personalCount
            ))
        } catch (e: Exception) {
            cacheDao.setSyncInProgress(false)
            emit(WhatsAppSyncProgress.Error(e.message ?: "Sync failed"))
        }
    }

    override suspend fun getCachedNumbers(): Set<String> {
        return cacheDao?.getAllNumbers()?.toSet() ?: emptySet()
    }

    override suspend fun getCachedBusinessNumbers(): Set<String> {
        return cacheDao?.getBusinessNumbers()?.toSet() ?: emptySet()
    }

    override suspend fun isNumberCached(normalizedNumber: String): Boolean {
        return cacheDao?.hasNumber(normalizedNumber) ?: false
    }

    override suspend fun getCacheMeta(): WhatsAppCacheMeta? {
        return cacheDao?.getMeta()
    }

    override fun getCacheMetaFlow(): Flow<WhatsAppCacheMeta?> {
        return cacheDao?.getMetaFlow() ?: flow { emit(null) }
    }

    override suspend fun isCacheValid(): Boolean {
        val meta = cacheDao?.getMeta() ?: return false
        if (meta.syncInProgress) return false

        val now = Clock.System.now().toEpochMilliseconds()
        val hoursSinceSync = (now - meta.lastFullSync) / (1000 * 60 * 60)
        return hoursSinceSync < CACHE_VALIDITY_HOURS
    }

    /**
     * 2026 Best Practice: Atomic cache snapshot retrieval.
     * Gets validity and data in single operation to prevent race conditions.
     */
    override suspend fun getValidCacheSnapshot(): com.ogabassey.contactscleaner.domain.repository.CacheSnapshot {
        val dao = cacheDao ?: return com.ogabassey.contactscleaner.domain.repository.CacheSnapshot.Invalid
        val meta = dao.getMeta() ?: return com.ogabassey.contactscleaner.domain.repository.CacheSnapshot.Invalid

        // Check if sync is in progress
        if (meta.syncInProgress) {
            return com.ogabassey.contactscleaner.domain.repository.CacheSnapshot.SyncInProgress
        }

        // Check cache age
        val now = Clock.System.now().toEpochMilliseconds()
        val hoursSinceSync = (now - meta.lastFullSync) / (1000 * 60 * 60)
        if (hoursSinceSync >= CACHE_VALIDITY_HOURS) {
            return com.ogabassey.contactscleaner.domain.repository.CacheSnapshot.Invalid
        }

        // Cache is valid - get numbers atomically with the check
        val numbers = dao.getAllNumbers().toSet()
        return com.ogabassey.contactscleaner.domain.repository.CacheSnapshot.Valid(
            numbers = numbers,
            businessCount = meta.businessCount,
            personalCount = meta.personalCount
        )
    }

    override suspend fun clearCache() {
        cacheDao?.deleteAll()
    }
}
