package com.ogabassey.contactscleaner.di

import com.ogabassey.contactscleaner.data.repository.ContactRepositoryImpl
import com.ogabassey.contactscleaner.data.repository.RevenueCatKmpBillingRepository
import com.ogabassey.contactscleaner.data.repository.ScanSettingsRepository
import com.ogabassey.contactscleaner.data.source.ContactsProviderSource
import com.ogabassey.contactscleaner.domain.repository.BillingRepository
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * 2026 AGP 9.0: Android-specific Koin module.
 */
val androidDataModule = module {
    // Contacts Provider Source
    single { ContactsProviderSource(androidContext(), androidContext().contentResolver) }

    // Repository Implementation
    single<ContactRepository> {
        ContactRepositoryImpl(
            contactDao = get(),
            contactsProviderSource = get(),
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

    // Billing Repository (uses KMP SDK - consistent with iOS)
    single<BillingRepository> { RevenueCatKmpBillingRepository() }

    // File Service
    single<com.ogabassey.contactscleaner.domain.repository.FileService> {
        com.ogabassey.contactscleaner.data.repository.FileServiceImpl(androidContext())
    }

    // Scan Settings Repository
    single { ScanSettingsRepository(androidContext()) }
}
