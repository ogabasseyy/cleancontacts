package com.ogabassey.contactscleaner

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkerFactory
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import com.ogabassey.contactscleaner.di.sharedModule
import com.ogabassey.contactscleaner.di.androidModule
import com.ogabassey.contactscleaner.di.appModule
import com.ogabassey.contactscleaner.di.androidDataModule
import com.ogabassey.contactscleaner.di.viewModelModule
import com.ogabassey.contactscleaner.di.workerModule

/**
 * 2026 AGP 9.0: Migrated from Hilt to Koin.
 * AGP 9.0 removed legacy Android APIs that Hilt depends on.
 *
 * 2026 Best Practice: Koin initialization moved to attachBaseContext to ensure
 * DI is ready before WorkManager's Configuration.Provider is accessed.
 * Note: AndroidManifest.xml disables automatic WorkManagerInitializer.
 */
class CleanContactsApp : Application(), Configuration.Provider {

    private val billingRepository: com.ogabassey.contactscleaner.domain.repository.BillingRepository by inject()
    private val workerFactory: WorkerFactory by inject()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        // 2026 Best Practice: Initialize Koin early to ensure DI is ready
        // before WorkManager's Configuration.Provider could be accessed
        startKoin {
            androidLogger()
            androidContext(this@CleanContactsApp)
            modules(
                sharedModule,
                androidModule,
                appModule,
                androidDataModule,
                viewModelModule,
                workerModule
            )
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Configure RevenueCat
        Purchases.configure(
            PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY).build()
        )

        // Force refresh billing repository
        billingRepository.refresh()
        billingRepository.packages.value
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}