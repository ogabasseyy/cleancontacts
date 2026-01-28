package com.ogabassey.contactscleaner.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for undo/redo history.
 *
 * 2026 KMP Best Practice: Room 2.7.0+ entities work across platforms.
 */
@Entity(tableName = "undo_logs")
data class UndoLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val actionType: String, // "DELETE" or "MERGE"
    val originalDataJson: String, // Serialized List<Contact>
    val description: String // e.g., "Deleted 5 duplicates"
)
