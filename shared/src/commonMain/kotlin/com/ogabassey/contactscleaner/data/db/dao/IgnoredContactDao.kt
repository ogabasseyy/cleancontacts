package com.ogabassey.contactscleaner.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ogabassey.contactscleaner.data.db.entity.IgnoredContact
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for ignored contacts.
 *
 * 2026 KMP Best Practice: Room 2.7.0+ DAOs work across platforms.
 */
@Dao
interface IgnoredContactDao {
    @Query("SELECT * FROM ignored_contacts ORDER BY timestamp DESC")
    fun getAll(): Flow<List<IgnoredContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ignoredContact: IgnoredContact)

    @Query("DELETE FROM ignored_contacts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT id FROM ignored_contacts")
    suspend fun getAllIds(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM ignored_contacts WHERE id = :id)")
    suspend fun isIgnored(id: String): Boolean
}
