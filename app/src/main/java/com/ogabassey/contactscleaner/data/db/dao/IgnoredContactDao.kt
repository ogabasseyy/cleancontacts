package com.ogabassey.contactscleaner.data.db.dao

import androidx.room.*
import com.ogabassey.contactscleaner.data.db.entity.IgnoredContact
import kotlinx.coroutines.flow.Flow

@Dao
interface IgnoredContactDao {
    @Query("SELECT * FROM ignored_contacts ORDER BY timestamp DESC")
    fun getAll(): Flow<List<IgnoredContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ignoredContact: IgnoredContact)

    @Query("DELETE FROM ignored_contacts WHERE id = :id")
    suspend fun delete(id: String)
    
    @Query("SELECT EXISTS(SELECT 1 FROM ignored_contacts WHERE id = :id)")
    suspend fun isIgnored(id: String): Boolean
}
