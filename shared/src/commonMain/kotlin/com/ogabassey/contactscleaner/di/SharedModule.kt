package com.ogabassey.contactscleaner.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ogabassey.contactscleaner.data.api.WhatsAppDetectorApi
import com.ogabassey.contactscleaner.data.db.ContactDatabase
import com.ogabassey.contactscleaner.data.db.dao.ContactDao
import com.ogabassey.contactscleaner.data.db.dao.IgnoredContactDao
import com.ogabassey.contactscleaner.data.db.dao.UndoDao
import com.ogabassey.contactscleaner.data.db.dao.WhatsAppCacheDao
import com.ogabassey.contactscleaner.data.detector.DuplicateDetector
import com.ogabassey.contactscleaner.data.detector.FormatDetector
import com.ogabassey.contactscleaner.data.detector.JunkDetector
import com.ogabassey.contactscleaner.data.detector.SensitiveDataDetector
import com.ogabassey.contactscleaner.data.repository.BackupRepositoryImpl
import com.ogabassey.contactscleaner.data.repository.UsageRepositoryImpl
import com.ogabassey.contactscleaner.data.repository.WhatsAppDetectorRepositoryImpl
import com.ogabassey.contactscleaner.data.util.ScanResultProvider
import com.ogabassey.contactscleaner.domain.repository.BackupRepository
import com.ogabassey.contactscleaner.domain.repository.UsageRepository
import com.ogabassey.contactscleaner.domain.repository.WhatsAppDetectorRepository
import com.ogabassey.contactscleaner.domain.usecase.CleanupContactsUseCase
import com.ogabassey.contactscleaner.domain.usecase.ScanContactsUseCase
import com.ogabassey.contactscleaner.domain.usecase.UndoUseCase
import com.ogabassey.contactscleaner.domain.usecase.ExportUseCase
import com.ogabassey.contactscleaner.domain.usecase.ImportContactsUseCase
import com.ogabassey.contactscleaner.data.parser.ContactImportParser
import com.ogabassey.contactscleaner.platform.PhoneNumberHandler
import com.ogabassey.contactscleaner.platform.TextAnalyzer
import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Shared Koin module for cross-platform dependencies.
 *
 * 2026 KMP Best Practice: Koin replaces Hilt for KMP DI.
 */
val sharedModule = module {
    // Platform Helpers
    single { PhoneNumberHandler() }
    single { TextAnalyzer() }

    // Detectors
    single { JunkDetector(get()) }
    single { DuplicateDetector(get(), get()) }
    single { FormatDetector(get(), get()) }
    single { SensitiveDataDetector(get()) }

    // Database (built from platform-specific builder)
    single<ContactDatabase> {
        get<androidx.room.RoomDatabase.Builder<ContactDatabase>>()
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    // DAOs
    single<ContactDao> { get<ContactDatabase>().contactDao() }
    single<UndoDao> { get<ContactDatabase>().undoDao() }
    single<IgnoredContactDao> { get<ContactDatabase>().ignoredContactDao() }
    single<WhatsAppCacheDao> { get<ContactDatabase>().whatsAppCacheDao() }

    // Parsers
    single { ContactImportParser() }

    // Usage Repository (trial tracking with limit of 1)
    single<UsageRepository> { UsageRepositoryImpl(get()) }

    // Backup Repository (undo/snapshot functionality)
    single<BackupRepository> { BackupRepositoryImpl(get()) }

    // Use Cases (repository will be provided by platform module)
    single { ScanContactsUseCase(get()) }
    single { CleanupContactsUseCase(get(), get()) }
    single { UndoUseCase(get(), get()) }
    single { ExportUseCase(get(), get()) }
    single { ImportContactsUseCase(get(), get(), get()) }

    // State Providers
    single { ScanResultProvider() }

    // WhatsApp Detector (VPS-hosted Baileys service)
    // 2026 Best Practice: Local caching for 51k+ WhatsApp contacts
    single { WhatsAppDetectorApi() }
    single<WhatsAppDetectorRepository> { WhatsAppDetectorRepositoryImpl(get(), get()) }
}

/**
 * Expect function to get platform-specific modules.
 * Each platform provides its own module with platform-specific dependencies.
 */
expect fun platformModule(): Module
