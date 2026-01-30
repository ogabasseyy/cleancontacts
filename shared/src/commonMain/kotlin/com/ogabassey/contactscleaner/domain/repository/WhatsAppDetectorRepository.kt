package com.ogabassey.contactscleaner.domain.repository

import com.ogabassey.contactscleaner.data.api.BatchCheckResponse
import com.ogabassey.contactscleaner.data.api.NumberCheckResult
import com.ogabassey.contactscleaner.data.api.PairingEvent
import com.ogabassey.contactscleaner.data.api.SessionStatus
import com.ogabassey.contactscleaner.data.api.WhatsAppContactsResponse
import com.ogabassey.contactscleaner.data.db.entity.WhatsAppCacheMeta
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for WhatsApp detection via linked device.
 *
 * This uses a VPS-hosted Baileys service that allows users to link their
 * WhatsApp account to enable accurate WhatsApp detection for their contacts.
 *
 * Multi-Session Support: Each user gets their own isolated WhatsApp session
 * identified by userId. This allows multiple users to link their WhatsApp
 * accounts independently without conflicts.
 */
interface WhatsAppDetectorRepository {

    /**
     * Check if the WhatsApp detection service is available.
     */
    suspend fun isServiceAvailable(): Boolean

    /**
     * Get the session status for a specific user.
     * Returns whether a WhatsApp account is linked for this user.
     *
     * @param userId Unique identifier for the user's session
     */
    suspend fun getSessionStatus(userId: String): SessionStatus

    /**
     * Request a pairing code to link WhatsApp for a specific user.
     * The user will receive a notification on their WhatsApp to enter this code.
     *
     * @param userId Unique identifier for the user's session
     * @param phoneNumber User's phone number in international format
     * @return The 8-digit pairing code, or null if failed
     */
    suspend fun requestPairingCode(userId: String, phoneNumber: String): String?

    /**
     * Disconnect the linked WhatsApp session for a specific user.
     *
     * @param userId Unique identifier for the user's session
     */
    suspend fun disconnect(userId: String): Boolean

    /**
     * Check if a list of phone numbers are on WhatsApp using a user's session.
     *
     * @param userId Unique identifier for the user's session
     * @param numbers List of phone numbers to check
     * @return Map of phone number to hasWhatsApp status
     */
    suspend fun checkNumbers(userId: String, numbers: List<String>): Map<String, Boolean>

    /**
     * Batch check numbers with progress updates using a user's session.
     * Use this for checking entire contact lists.
     *
     * @param userId Unique identifier for the user's session
     * @param numbers List of phone numbers
     * @return Flow emitting progress updates and final results
     */
    fun checkNumbersBatch(userId: String, numbers: List<String>): Flow<WhatsAppCheckProgress>

    /**
     * Connect via WebSocket for real-time pairing events.
     * Provides instant notifications instead of polling.
     *
     * @param userId Unique identifier for the user's session
     * @param phoneNumber User's phone number in international format
     * @return Flow emitting pairing events (code, connected, error)
     */
    fun connectForPairing(userId: String, phoneNumber: String): Flow<PairingEvent>

    /**
     * Get all WhatsApp contacts with business detection.
     * Returns contacts synced from the linked WhatsApp session.
     *
     * Privacy-compliant: No contacts are sent TO the server.
     * The server returns WhatsApp contacts FROM the linked session.
     *
     * @param userId Unique identifier for the user's session
     * @return WhatsAppContactsResponse with contacts and business detection
     */
    suspend fun getContacts(userId: String): WhatsAppContactsResponse

    // ============================================
    // WhatsApp Cache Methods (2026 Best Practice)
    // ============================================

    /**
     * Sync all WhatsApp contacts to local cache.
     * Fetches all contacts paginated and stores normalized phone numbers.
     *
     * @param userId Unique identifier for the user's session
     * @return Flow emitting sync progress updates
     */
    fun syncAllContactsToCache(userId: String): Flow<WhatsAppSyncProgress>

    /**
     * Get all cached WhatsApp phone numbers.
     * Returns normalized phone numbers for fast lookup during scan.
     *
     * @return Set of normalized phone numbers
     */
    suspend fun getCachedNumbers(): Set<String>

    /**
     * Get business phone numbers from cache.
     *
     * @return Set of normalized business phone numbers
     */
    suspend fun getCachedBusinessNumbers(): Set<String>

    /**
     * Check if a phone number exists in cache.
     *
     * @param normalizedNumber The normalized phone number (digits only)
     * @return True if the number is in cache
     */
    suspend fun isNumberCached(normalizedNumber: String): Boolean

    /**
     * Get cache metadata (sync status, counts).
     *
     * @return WhatsAppCacheMeta or null if no sync has occurred
     */
    suspend fun getCacheMeta(): WhatsAppCacheMeta?

    /**
     * Observe cache metadata changes.
     *
     * @return Flow of cache metadata
     */
    fun getCacheMetaFlow(): Flow<WhatsAppCacheMeta?>

    /**
     * Check if cache is valid (synced within last 24 hours).
     *
     * @return True if cache is valid
     */
    suspend fun isCacheValid(): Boolean

    /**
     * 2026 Best Practice: Get valid cache snapshot atomically.
     * Combines validity check and data retrieval in single operation
     * to prevent race conditions where sync starts between check and retrieval.
     *
     * @return CacheSnapshot with numbers if valid, or invalid status
     */
    suspend fun getValidCacheSnapshot(): CacheSnapshot

    /**
     * Clear the WhatsApp cache.
     */
    suspend fun clearCache()
}

/**
 * Progress update for WhatsApp sync.
 */
sealed class WhatsAppSyncProgress {
    data class InProgress(
        val synced: Int,
        val total: Int,
        val businessCount: Int,
        val personalCount: Int
    ) : WhatsAppSyncProgress()

    data class Complete(
        val totalCount: Int,
        val businessCount: Int,
        val personalCount: Int
    ) : WhatsAppSyncProgress()

    data class Error(val message: String) : WhatsAppSyncProgress()
}

/**
 * Progress update for batch WhatsApp checking.
 */
sealed class WhatsAppCheckProgress {
    data class InProgress(
        val checked: Int,
        val total: Int,
        val whatsappCount: Int
    ) : WhatsAppCheckProgress()

    data class Complete(
        val results: Map<String, Boolean>,
        val whatsappCount: Int
    ) : WhatsAppCheckProgress()

    data class Error(val message: String) : WhatsAppCheckProgress()
}

/**
 * 2026 Best Practice: Atomic cache snapshot result.
 * Prevents race conditions by combining validity check with data retrieval.
 */
sealed class CacheSnapshot {
    data class Valid(
        val numbers: Set<String>,
        val businessCount: Int,
        val personalCount: Int
    ) : CacheSnapshot()

    data object Invalid : CacheSnapshot()
    data object SyncInProgress : CacheSnapshot()
}
