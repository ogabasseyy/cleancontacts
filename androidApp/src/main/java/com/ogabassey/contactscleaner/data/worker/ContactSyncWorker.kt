package com.ogabassey.contactscleaner.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ogabassey.contactscleaner.data.db.dao.ContactDao
import com.ogabassey.contactscleaner.data.db.entity.LocalContact
import com.ogabassey.contactscleaner.data.detector.JunkDetector
import com.ogabassey.contactscleaner.data.source.ContactsProviderSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 2026 AGP 9.0: Migrated from Hilt to Koin.
 * Dependencies are injected via KoinWorkerFactory.
 */
class ContactSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val contactDao: ContactDao,
    private val contactsSource: ContactsProviderSource,
    private val junkDetector: JunkDetector
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        android.util.Log.d("ContactSyncWorker", "Starting sync...")
        try {
            val systemContacts = contactsSource.getAllContacts() // This is the slow part for 58k
            
            // Convert to LocalContact entities
            val entities = systemContacts.map { contact ->
                val junkReason = junkDetector.getJunkReason(contact.name, contact.numbers.firstOrNull())
                LocalContact(
                    id = contact.id,
                    displayName = contact.name,
                    normalizedNumber = contact.normalizedNumber,
                    rawNumbers = contact.numbers.joinToString(","),
                    rawEmails = contact.emails.joinToString(","),
                    isWhatsApp = contact.isWhatsApp,
                    isTelegram = contact.isTelegram,
                    isJunk = junkReason != null,
                    junkType = junkReason,
                    duplicateType = null,
                    isFormatIssue = false, // Simplified for SyncWorker, ideally logic should be shared
                    accountType = contact.accountType,
                    accountName = contact.accountName,
                    detectedRegion = null, // SyncWorker doesn't run full analysis
                    lastSynced = System.currentTimeMillis()
                )
            }

            // Batch insert to Room
            entities.chunked(1000).forEach { batch ->
                contactDao.insertContacts(batch)
            }

            android.util.Log.d("ContactSyncWorker", "Sync complete. Indexed ${entities.size} contacts.")
            Result.success()
        } catch (e: SecurityException) {
            // 2026 Best Practice: Permanent failure - missing permissions, don't retry
            android.util.Log.e("ContactSyncWorker", "Sync failed - permission denied", e)
            Result.failure()
        } catch (e: IllegalStateException) {
            // Permanent failure - content provider unavailable
            android.util.Log.e("ContactSyncWorker", "Sync failed - provider unavailable", e)
            Result.failure()
        } catch (e: Exception) {
            // Transient failure - retry for other exceptions
            android.util.Log.e("ContactSyncWorker", "Sync failed - will retry", e)
            Result.retry()
        }
    }
}
