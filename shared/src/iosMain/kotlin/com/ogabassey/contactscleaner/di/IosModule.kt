package com.ogabassey.contactscleaner.di

import com.ogabassey.contactscleaner.data.db.ContactDatabase
import com.ogabassey.contactscleaner.data.db.getDatabaseBuilder
import com.ogabassey.contactscleaner.data.repository.IosContactRepository
import com.ogabassey.contactscleaner.data.repository.IosFileService
import com.ogabassey.contactscleaner.data.repository.MockBillingRepository
import com.ogabassey.contactscleaner.data.source.IosContactsSource
import com.ogabassey.contactscleaner.domain.repository.BackupRepository
import com.ogabassey.contactscleaner.domain.repository.BillingRepository
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import com.ogabassey.contactscleaner.domain.repository.FileService
import com.ogabassey.contactscleaner.domain.repository.UsageRepository
import com.ogabassey.contactscleaner.platform.IosRegionProvider
import com.ogabassey.contactscleaner.platform.RegionProvider
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

/**
 * iOS-specific Koin module.
 *
 * 2026 KMP Best Practice: Platform modules provide platform-specific implementations
 * with real device access instead of mocks.
 */
val iosModule = module {
    // Region Provider (iOS uses NSLocale)
    single<RegionProvider> { IosRegionProvider() }

    // Settings (iOS uses NSUserDefaults via multiplatform-settings)
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }

    // Database Builder (iOS uses file path)
    single<androidx.room.RoomDatabase.Builder<ContactDatabase>> {
        getDatabaseBuilder()
    }

    // iOS Contacts Source (CNContactStore)
    single { IosContactsSource() }

    // Billing Repository (Mock for iOS - replace with StoreKit implementation later)
    single<BillingRepository> { MockBillingRepository() }

    // File Service (iOS file system)
    single<FileService> { IosFileService() }

    // Real ContactRepository using iOS Contacts framework
    single<ContactRepository> {
        IosContactRepository(
            contactDao = get(),
            contactsSource = get(),
            junkDetector = get(),
            duplicateDetector = get(),
            formatDetector = get(),
            sensitiveDetector = get(),
            ignoredContactDao = get(),
            scanResultProvider = get(),
            usageRepository = get(),
            backupRepository = get()
        )
    }
}

actual fun platformModule(): Module = iosModule
