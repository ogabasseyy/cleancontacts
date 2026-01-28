package com.ogabassey.contactscleaner.di

import com.ogabassey.contactscleaner.data.repository.MockBackupRepository
import com.ogabassey.contactscleaner.data.repository.MockBillingRepository
import com.ogabassey.contactscleaner.data.repository.MockContactRepository
import com.ogabassey.contactscleaner.domain.repository.BackupRepository
import com.ogabassey.contactscleaner.domain.repository.BillingRepository
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import org.koin.dsl.module

/**
 * Mock module for development and testing.
 *
 * 2026 KMP Best Practice: Separate mock module for development builds.
 * In production, this would be replaced with real implementations.
 */
val mockModule = module {
    // Mock Repositories
    single<BillingRepository> { MockBillingRepository() }
    single<BackupRepository> { MockBackupRepository() }
    single<ContactRepository> { MockContactRepository() }
}
