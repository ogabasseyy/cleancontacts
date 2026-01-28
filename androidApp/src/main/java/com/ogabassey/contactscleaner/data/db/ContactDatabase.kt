package com.ogabassey.contactscleaner.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ogabassey.contactscleaner.data.db.dao.ContactDao
import com.ogabassey.contactscleaner.data.db.dao.UndoDao
import com.ogabassey.contactscleaner.data.db.dao.IgnoredContactDao
import com.ogabassey.contactscleaner.data.db.entity.IgnoredContact
import com.ogabassey.contactscleaner.data.db.entity.LocalContact
import com.ogabassey.contactscleaner.data.db.entity.UndoLog
@Database(
    entities = [
        LocalContact::class,
        UndoLog::class,
        IgnoredContact::class
    ],
    version = 5, // Incrementing version
    exportSchema = false
)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun undoDao(): UndoDao
    abstract fun ignoredContactDao(): IgnoredContactDao

    companion object {
        const val DATABASE_NAME = "contacts_db"
    }
}
