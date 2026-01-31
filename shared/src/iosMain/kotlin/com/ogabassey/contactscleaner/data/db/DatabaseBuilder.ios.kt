package com.ogabassey.contactscleaner.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS database builder using file-based storage.
 *
 * 2026 KMP Best Practice: Use NSFileManager to locate the Documents directory.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun getDatabaseBuilder(): RoomDatabase.Builder<ContactDatabase> {
    // 2026 Best Practice: Capture NSError for proper error handling
    val dbFilePath = memScoped {
        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = errorPtr.ptr
        )

        val nsError = errorPtr.value
        if (nsError != null) {
            println("⚠️ Database directory error: ${nsError.localizedDescription}")
        }

        // 2026 Fix: Handle null documentDirectory properly - use Application Support fallback
        val path = documentDirectory?.path
        if (path.isNullOrEmpty()) {
            // 2026 Best Practice: Use Application Support for persistent database storage
            // NSTemporaryDirectory is purgeable by iOS - not suitable for databases
            errorPtr.value = null
            val supportDirectory = NSFileManager.defaultManager.URLForDirectory(
                directory = NSApplicationSupportDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = errorPtr.ptr
            )
            val supportPath = supportDirectory?.path
            require(!supportPath.isNullOrEmpty()) {
                "Failed to resolve Application Support directory for database"
            }
            println("⚠️ Using Application Support fallback path: $supportPath")
            "$supportPath/${ContactDatabase.DATABASE_NAME}.db"
        } else {
            "$path/${ContactDatabase.DATABASE_NAME}.db"
        }
    }

    return Room.databaseBuilder<ContactDatabase>(
        name = dbFilePath
    )
}
