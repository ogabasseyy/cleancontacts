package com.ogabassey.contactscleaner.data.db

import androidx.room.RoomDatabase

/**
 * Platform-specific database builder.
 *
 * 2026 KMP Best Practice: Use expect/actual for platform-specific Room setup.
 * - Android: Uses standard Room.databaseBuilder with ApplicationContext
 * - iOS: Uses Room.databaseBuilder with file path and BundledSQLiteDriver
 */
expect fun getDatabaseBuilder(): RoomDatabase.Builder<ContactDatabase>
