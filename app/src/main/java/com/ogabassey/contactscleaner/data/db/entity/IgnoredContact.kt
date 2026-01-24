package com.ogabassey.contactscleaner.data.db.entity

import androidx.room.*

@Entity(tableName = "ignored_contacts")
data class IgnoredContact(
    @PrimaryKey val id: String, // String lookup key or phone number
    val displayName: String,
    val reason: String, // e.g., "Detected SensitiveID", "Manual Ignore"
    val timestamp: Long = System.currentTimeMillis()
)
