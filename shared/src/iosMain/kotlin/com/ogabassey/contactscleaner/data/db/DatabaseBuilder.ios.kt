package com.ogabassey.contactscleaner.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSFileManager
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * iOS database builder using file-based storage.
 *
 * 2026 KMP Best Practice: Use NSFileManager to locate the Documents directory.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun getDatabaseBuilder(): RoomDatabase.Builder<ContactDatabase> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    val path = documentDirectory?.path ?: ""
    val dbFilePath = "$path/${ContactDatabase.DATABASE_NAME}.db"
    
    return Room.databaseBuilder<ContactDatabase>(
        name = dbFilePath
    )
}
