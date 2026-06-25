package com.ogabassey.contactscleaner.data.worker

import com.ogabassey.contactscleaner.platform.Logger

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
        Logger.d("ContactSyncWorker", "Starting sync...")
        try {
            val systemContacts = contactsSource.getAllContacts() // This is the slow part for 58k
            
            val currentTime = System.currentTimeMillis()
            var batch = ArrayList<LocalContact>(1000)

            for (i in systemContacts.indices) {
                val contact = systemContacts[i]
                val junkReason = junkDetector.getJunkReason(contact.name, contact.numbers.firstOrNull())
                batch.add(
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
                        lastSynced = currentTime
                    )
                )

                if (batch.size >= 1000) {
                    contactDao.insertContacts(batch)
                    batch = ArrayList<LocalContact>(1000)
                }
            }

            if (batch.isNotEmpty()) {
                contactDao.insertContacts(batch)
            }

            Logger.d("ContactSyncWorker", "Sync complete. Indexed ${systemContacts.size} contacts.")
            Result.success()
        } catch (e: SecurityException) {
            // 2026 Best Practice: Permanent failure - missing permissions, don't retry
            Logger.e("ContactSyncWorker", "Sync failed - permission denied", e)
            Result.failure()
        } catch (e: IllegalStateException) {
            // Permanent failure - content provider unavailable
            Logger.e("ContactSyncWorker", "Sync failed - provider unavailable", e)
            Result.failure()
        } catch (e: Exception) {
            // Transient failure - retry for other exceptions
            Logger.e("ContactSyncWorker", "Sync failed - will retry", e)
            Result.retry()
        }
    }
}
