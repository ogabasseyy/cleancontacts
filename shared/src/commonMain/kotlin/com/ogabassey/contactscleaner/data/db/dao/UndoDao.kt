package com.ogabassey.contactscleaner.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ogabassey.contactscleaner.data.db.entity.UndoLog
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for undo/redo operations.
 *
 * 2026 KMP Best Practice: Room 2.7.0+ DAOs work across platforms.
 */
@Dao
interface UndoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: UndoLog): Long

    @Query("SELECT * FROM undo_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLog(): UndoLog?

    @Query("SELECT * FROM undo_logs ORDER BY timestamp DESC LIMIT 10")
    fun getAllLogs(): Flow<List<UndoLog>>

    @Query("SELECT * FROM undo_logs WHERE id = :id")
    suspend fun getLogById(id: Int): UndoLog?

    @Query("DELETE FROM undo_logs WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM undo_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM undo_logs")
    suspend fun clearAll()
}
