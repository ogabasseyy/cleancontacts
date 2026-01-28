package com.ogabassey.contactscleaner.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ogabassey.contactscleaner.data.db.dao.ContactDao
import com.ogabassey.contactscleaner.data.db.dao.UndoDao
import com.ogabassey.contactscleaner.data.db.dao.IgnoredContactDao
import com.ogabassey.contactscleaner.data.db.entity.IgnoredContact
import com.ogabassey.contactscleaner.data.db.entity.LocalContact
import com.ogabassey.contactscleaner.data.db.entity.UndoLog

/**
 * Room database for contact management.
 *
 * 2026 KMP Best Practice: Room 2.7.0+ databases work across platforms with BundledSQLiteDriver.
 * @ConstructedBy is required for non-Android platforms (iOS, Desktop, etc.)
 */
@Database(
    entities = [
        LocalContact::class,
        UndoLog::class,
        IgnoredContact::class
    ],
    version = 6,
    exportSchema = true
)
@ConstructedBy(ContactDatabaseConstructor::class)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun undoDao(): UndoDao
    abstract fun ignoredContactDao(): IgnoredContactDao

    companion object {
        const val DATABASE_NAME = "contacts_db"
    }
}

/**
 * Room database constructor for KMP.
 *
 * 2026 KMP Best Practice: expect/actual pattern allows Room to generate
 * platform-specific instantiation code.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ContactDatabaseConstructor : RoomDatabaseConstructor<ContactDatabase> {
    override fun initialize(): ContactDatabase
}
