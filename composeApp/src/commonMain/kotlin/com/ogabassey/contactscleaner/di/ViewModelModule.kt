package com.ogabassey.contactscleaner.di

import com.ogabassey.contactscleaner.ui.dashboard.DashboardViewModel
import com.ogabassey.contactscleaner.ui.history.RecentActionsViewModel
import com.ogabassey.contactscleaner.ui.paywall.PaywallViewModel
import com.ogabassey.contactscleaner.ui.results.ResultsViewModel
import com.ogabassey.contactscleaner.ui.whatsapp.WhatsAppLinkViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for ViewModels.
 *
 * 2026 KMP Best Practice: Use Koin's viewModel DSL for cross-platform ViewModel injection.
 */
val viewModelModule = module {
    // DashboardViewModel with ScanResultProvider, ScanContactsUseCase, ContactRepository
    viewModel { DashboardViewModel(get(), get(), get()) }

    // ResultsViewModel with ScanResultProvider, ContactRepository, CleanupContactsUseCase, BillingRepository, UsageRepository, UndoUseCase
    viewModel { ResultsViewModel(get(), get(), get(), get(), get(), get()) }

    // PaywallViewModel with BillingRepository
    viewModel { PaywallViewModel(get()) }

    // RecentActionsViewModel with BackupRepository and ContactRepository
    viewModel { RecentActionsViewModel(get(), get()) }

    // SafeListViewModel with ContactRepository
    viewModel { com.ogabassey.contactscleaner.ui.settings.SafeListViewModel(get()) }

    // ReviewViewModel for Sensitive Data
    viewModel { com.ogabassey.contactscleaner.ui.tools.ReviewViewModel(get()) }

    // CategoryViewModel for Lists (Junk, Duplicates, etc.) with ContactRepository, BillingRepository, UsageRepository
    viewModel { com.ogabassey.contactscleaner.ui.category.CategoryViewModel(get(), get(), get()) }

    // WhatsAppLinkViewModel with WhatsAppDetectorRepository
    viewModel { WhatsAppLinkViewModel(get()) }
}
