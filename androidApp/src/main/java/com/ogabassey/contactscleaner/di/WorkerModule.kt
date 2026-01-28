package com.ogabassey.contactscleaner.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.ogabassey.contactscleaner.data.db.dao.ContactDao
import com.ogabassey.contactscleaner.data.detector.JunkDetector
import com.ogabassey.contactscleaner.data.source.ContactsProviderSource
import com.ogabassey.contactscleaner.data.worker.ContactSyncWorker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module

/**
 * 2026 AGP 9.0: Koin WorkerFactory for WorkManager integration.
 * Replaces HiltWorkerFactory which is incompatible with AGP 9.0.
 */
class KoinWorkerFactory : WorkerFactory(), KoinComponent {

    private val contactDao: ContactDao by inject()
    private val contactsSource: ContactsProviderSource by inject()
    private val junkDetector: JunkDetector by inject()

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            ContactSyncWorker::class.java.name -> ContactSyncWorker(
                appContext = appContext,
                workerParams = workerParameters,
                contactDao = contactDao,
                contactsSource = contactsSource,
                junkDetector = junkDetector
            )
            else -> null
        }
    }
}

val workerModule = module {
    single<WorkerFactory> { KoinWorkerFactory() }
}
