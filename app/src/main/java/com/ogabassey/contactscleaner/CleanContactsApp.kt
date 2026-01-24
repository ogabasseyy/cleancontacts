package com.ogabassey.contactscleaner

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

@HiltAndroidApp
class CleanContactsApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var billingRepository: com.ogabassey.contactscleaner.domain.repository.BillingRepository

    override fun onCreate() {
        // Configure RevenueCat BEFORE injection (if possible) or at least ensure it's first thing
        // Note: Hilt injection likely happens in super.onCreate(), so repository init might run before this.
        // Therefore we MUST explicitly refresh() after config to ensure repository is in valid state.
        Purchases.configure(PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY).build())
        
        super.onCreate()
        
        // Force refresh now that we know RevenueCat is configured
        billingRepository.refresh()
        
        // Eagerly initialize billing flow (accessing property)
        billingRepository.packages.value
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}