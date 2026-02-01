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
 * All ViewModels use named parameters to prevent silent dependency swaps if constructor order changes.
 */
val viewModelModule = module {
    // DashboardViewModel
    viewModel {
        DashboardViewModel(
            scanResultProvider = get(),
            scanContactsUseCase = get(),
            contactRepository = get()
        )
    }

    // ResultsViewModel
    viewModel {
        ResultsViewModel(
            scanResultProvider = get(),
            contactRepository = get(),
            cleanupContactsUseCase = get(),
            billingRepository = get(),
            usageRepository = get(),
            undoUseCase = get(),
            scanContactsUseCase = get()
        )
    }

    // PaywallViewModel
    viewModel { PaywallViewModel(billingRepository = get()) }

    // RecentActionsViewModel
    viewModel {
        RecentActionsViewModel(
            backupRepository = get(),
            contactRepository = get()
        )
    }

    // SafeListViewModel
    viewModel { com.ogabassey.contactscleaner.ui.settings.SafeListViewModel(contactRepository = get()) }

    // ReviewViewModel for Sensitive Data
    viewModel { com.ogabassey.contactscleaner.ui.tools.ReviewViewModel(contactRepository = get()) }

    // CategoryViewModel for Lists (Junk, Duplicates, etc.)
    viewModel {
        com.ogabassey.contactscleaner.ui.category.CategoryViewModel(
            contactRepository = get(),
            billingRepository = get(),
            usageRepository = get()
        )
    }

    // WhatsAppLinkViewModel
    viewModel<WhatsAppLinkViewModel> {
        WhatsAppLinkViewModel(
            whatsAppRepository = get(),
            settings = get()
        )
    }

    // WhatsAppContactsViewModel
    viewModel<com.ogabassey.contactscleaner.ui.whatsapp.WhatsAppContactsViewModel> {
        com.ogabassey.contactscleaner.ui.whatsapp.WhatsAppContactsViewModel(
            whatsAppRepository = get(),
            settings = get(),
            contactDao = get()
        )
    }

    // CrossAccountViewModel for cross-account duplicates
    viewModel {
        com.ogabassey.contactscleaner.ui.duplicates.CrossAccountViewModel(
            contactRepository = get(),
            billingRepository = get(),
            usageRepository = get()
        )
    }
}
