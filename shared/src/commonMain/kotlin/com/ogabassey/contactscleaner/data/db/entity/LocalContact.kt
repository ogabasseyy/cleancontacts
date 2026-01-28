package com.ogabassey.contactscleaner.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for locally cached contacts.
 *
 * 2026 KMP Best Practice: Room 2.7.0+ entities work across platforms.
 */
@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["normalized_number"]),
        Index(value = ["display_name"]),
        Index(value = ["raw_emails"])
    ]
)
data class LocalContact(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "display_name") val displayName: String?,
    @ColumnInfo(name = "normalized_number") val normalizedNumber: String?,
    @ColumnInfo(name = "raw_numbers") val rawNumbers: String, // Comma separated
    @ColumnInfo(name = "raw_emails") val rawEmails: String, // Comma separated
    @ColumnInfo(name = "is_whatsapp") val isWhatsApp: Boolean,
    @ColumnInfo(name = "is_telegram") val isTelegram: Boolean,
    @ColumnInfo(name = "account_type") val accountType: String?, // e.g. com.google, com.whatsapp
    @ColumnInfo(name = "account_name") val accountName: String?,
    @ColumnInfo(name = "is_junk") val isJunk: Boolean,
    @ColumnInfo(name = "junk_type") val junkType: String?, // Enum name
    @ColumnInfo(name = "duplicate_type") val duplicateType: String?, // Enum name
    @ColumnInfo(name = "is_format_issue") val isFormatIssue: Boolean,
    @ColumnInfo(name = "detected_region") val detectedRegion: String?, // "US", "NG", etc.
    @ColumnInfo(name = "is_sensitive") val isSensitive: Boolean = false,
    @ColumnInfo(name = "sensitive_description") val sensitiveDescription: String? = null,
    @ColumnInfo(name = "matching_key") val matchingKey: String? = null,
    @ColumnInfo(name = "last_synced") val lastSynced: Long
)
