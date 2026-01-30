package com.ogabassey.contactscleaner.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ogabassey.contactscleaner.data.db.entity.WhatsAppCacheEntry
import com.ogabassey.contactscleaner.data.db.entity.WhatsAppCacheMeta
import kotlinx.coroutines.flow.Flow

/**
 * DAO for WhatsApp phone numbers cache.
 *
 * 2026 Best Practice: Cache WhatsApp numbers locally for instant
 * offline lookups during contact scanning.
 */
@Dao
interface WhatsAppCacheDao {

    // ============================================
    // Phone Numbers Cache
    // ============================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WhatsAppCacheEntry>)

    @Query("SELECT normalizedNumber FROM whatsapp_cache")
    suspend fun getAllNumbers(): List<String>

    @Query("SELECT normalizedNumber FROM whatsapp_cache WHERE isBusiness = 1")
    suspend fun getBusinessNumbers(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM whatsapp_cache WHERE normalizedNumber = :number)")
    suspend fun hasNumber(number: String): Boolean

    @Query("SELECT COUNT(*) FROM whatsapp_cache")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM whatsapp_cache WHERE isBusiness = 1")
    suspend fun getBusinessCount(): Int

    @Query("DELETE FROM whatsapp_cache")
    suspend fun deleteAll()

    // ============================================
    // Sync Metadata
    // ============================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateMeta(meta: WhatsAppCacheMeta)

    @Query("SELECT * FROM whatsapp_cache_meta WHERE `key` = 'sync_status'")
    suspend fun getMeta(): WhatsAppCacheMeta?

    @Query("SELECT * FROM whatsapp_cache_meta WHERE `key` = 'sync_status'")
    fun getMetaFlow(): Flow<WhatsAppCacheMeta?>

    @Query("UPDATE whatsapp_cache_meta SET syncInProgress = :inProgress WHERE `key` = 'sync_status'")
    suspend fun setSyncInProgress(inProgress: Boolean)

    @Query("SELECT lastFullSync FROM whatsapp_cache_meta WHERE `key` = 'sync_status'")
    suspend fun getLastSyncTime(): Long?

    // --- 2026 Best Practice: Transaction Methods for Atomic Operations ---

    /**
     * Atomically replace entire cache - deletes existing and inserts new entries in single transaction.
     * Prevents cache loss if insert fails after delete.
     */
    @Transaction
    suspend fun replaceAllEntries(entries: List<WhatsAppCacheEntry>, meta: WhatsAppCacheMeta) {
        deleteAll()
        insertAll(entries)
        updateMeta(meta)
    }
}
