package com.ogabassey.contactscleaner.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Android database builder.
 *
 * Note: Context must be provided via DI (Koin). This is a factory function
 * that will be called from the Koin module with the Android context.
 */
private var appContext: Context? = null

/**
 * Initialize the database builder with Android context.
 * Must be called before getDatabaseBuilder() - typically from Application.onCreate()
 * or from the Koin Android module.
 */
fun initializeDatabaseContext(context: Context) {
    appContext = context.applicationContext
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<ContactDatabase> {
    val context = appContext ?: throw IllegalStateException(
        "Database context not initialized. Call initializeDatabaseContext() first."
    )
    return Room.databaseBuilder(
        context,
        ContactDatabase::class.java,
        ContactDatabase.DATABASE_NAME
    )
}

/**
 * Alternative factory function for Koin integration.
 * Call this from androidMain Koin module.
 */
fun createDatabaseBuilder(context: Context): RoomDatabase.Builder<ContactDatabase> {
    return Room.databaseBuilder(
        context.applicationContext,
        ContactDatabase::class.java,
        ContactDatabase.DATABASE_NAME
    )
}
