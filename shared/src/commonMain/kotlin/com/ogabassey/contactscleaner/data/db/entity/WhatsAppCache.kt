package com.ogabassey.contactscleaner.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached WhatsApp phone numbers for offline comparison.
 *
 * 2026 Best Practice: Cache WhatsApp numbers locally to avoid
 * fetching 51k+ contacts from API during every scan.
 *
 * Only stores normalized phone numbers (digits only) for fast lookup.
 */
@Entity(
    tableName = "whatsapp_cache",
    indices = [Index(value = ["normalizedNumber"], unique = true)]
)
data class WhatsAppCacheEntry(
    @PrimaryKey
    val normalizedNumber: String,  // e.g., "2349169449282" (digits only)
    val isBusiness: Boolean = false,
    val lastSynced: Long = 0L
)

/**
 * Metadata about the WhatsApp cache sync status.
 */
@Entity(tableName = "whatsapp_cache_meta")
data class WhatsAppCacheMeta(
    @PrimaryKey
    val key: String = "sync_status",
    val lastFullSync: Long = 0L,
    val totalCount: Int = 0,
    val businessCount: Int = 0,
    val personalCount: Int = 0,
    val syncInProgress: Boolean = false
)
