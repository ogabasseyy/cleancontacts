package com.ogabassey.contactscleaner.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for contacts that should be ignored during scanning.
 *
 * 2026 KMP Best Practice: Room 2.7.0+ entities work across platforms.
 */
@Entity(tableName = "ignored_contacts")
data class IgnoredContact(
    @PrimaryKey val id: String, // String lookup key or phone number
    val displayName: String,
    val reason: String, // e.g., "Detected SensitiveID", "Manual Ignore"
    val timestamp: Long
)
