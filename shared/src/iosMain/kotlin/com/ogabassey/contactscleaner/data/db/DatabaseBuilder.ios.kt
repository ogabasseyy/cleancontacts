package com.ogabassey.contactscleaner.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
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

        val path = documentDirectory?.path ?: ""
        "$path/${ContactDatabase.DATABASE_NAME}.db"
    }

    return Room.databaseBuilder<ContactDatabase>(
        name = dbFilePath
    )
}
