package com.ogabassey.contactscleaner.di

import com.ogabassey.contactscleaner.data.db.ContactDatabase
import com.ogabassey.contactscleaner.data.db.createDatabaseBuilder
import com.ogabassey.contactscleaner.platform.AndroidRegionProvider
import com.ogabassey.contactscleaner.platform.RegionProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android-specific Koin module.
 *
 * 2026 KMP Best Practice: Platform modules provide platform-specific implementations.
 */
val androidModule = module {
    // Region Provider (Android uses TelephonyManager)
    single<RegionProvider> { AndroidRegionProvider(androidContext()) }

    // Database Builder (Android needs Context)
    single<androidx.room.RoomDatabase.Builder<ContactDatabase>> {
        createDatabaseBuilder(androidContext())
    }

    // Settings (Android-specific)
    single<com.russhwolf.settings.Settings> {
        com.russhwolf.settings.SharedPreferencesSettings(
            androidContext().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        )
    }
}

actual fun platformModule(): Module = androidModule
