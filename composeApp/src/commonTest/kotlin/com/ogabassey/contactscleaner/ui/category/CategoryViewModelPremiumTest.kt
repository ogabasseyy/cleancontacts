package com.ogabassey.contactscleaner.ui.category

import com.ogabassey.contactscleaner.data.db.entity.IgnoredContact
import com.ogabassey.contactscleaner.domain.model.AccountGroupSummary
import com.ogabassey.contactscleaner.domain.model.CleanupStatus
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.model.CrossAccountContact
import com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary
import com.ogabassey.contactscleaner.domain.model.PaywallPackage
import com.ogabassey.contactscleaner.domain.model.Resource
import com.ogabassey.contactscleaner.domain.model.ScanStatus
import com.ogabassey.contactscleaner.domain.repository.BillingRepository
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import com.ogabassey.contactscleaner.domain.repository.UsageRepository
import com.ogabassey.contactscleaner.util.BackgroundOperationManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModelPremiumTest {
    @AfterTest
    fun tearDown() {
        BackgroundOperationManager.cancel(reason = "test_teardown")
        BackgroundOperationManager.dismiss()
        Dispatchers.resetMain()
    }

    @Test
    fun performActionChargesFreeActionOnlyAfterSuccessfulCleanup() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val contacts = FakeContactRepository()
        val usage = FakeUsageRepository()
        val viewModel = CategoryViewModel(
            contactRepository = contacts,
            billingRepository = FakeBillingRepository(isPremium = false),
            usageRepository = usage
        )

        viewModel.performAction(ContactType.FORMAT_ISSUE)
        advanceUntilIdle()

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(1_000) {
                contacts.cleanupStarted.await()
            }
        }
        assertEquals(0, usage.incrementCount)

        contacts.cleanupResult.complete(CleanupStatus.Success("Standardized 1 contact"))

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(1_000) {
                while (usage.incrementCount == 0) {
                    delay(10)
                }
            }
        }
        assertEquals(1, usage.incrementCount)
    }

    @Test
    fun performActionDoesNotChargeWhenDuplicateGroupsAreEmpty() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val contacts = FakeContactRepository()
        contacts.mergeDuplicateResult = CleanupStatus.Success("No duplicates found")
        val usage = FakeUsageRepository()
        val viewModel = CategoryViewModel(
            contactRepository = contacts,
            billingRepository = FakeBillingRepository(isPremium = false),
            usageRepository = usage
        )

        viewModel.performAction(ContactType.DUP_NUMBER)
        advanceUntilIdle()

        assertEquals(0, usage.incrementCount)
        assertEquals(false, contacts.mergeDuplicateStarted.isCompleted)
    }

    private class FakeBillingRepository(isPremium: Boolean) : BillingRepository {
        override val isPremium = MutableStateFlow(isPremium)
        override val packages = MutableStateFlow<Resource<List<PaywallPackage>>>(Resource.Success(emptyList()))

        override suspend fun purchasePremium(packageId: String): Result<Unit> = Result.success(Unit)
        override suspend fun restorePurchases(): Result<Unit> = Result.success(Unit)
        override fun refresh() = Unit
    }

    private class FakeUsageRepository : UsageRepository {
        private val used = MutableStateFlow(0)
        var incrementCount = 0
            private set

        override val freeActionsUsed: Flow<Int> = used
        override val freeActionsRemaining: Flow<Int> = used.map { UsageRepository.MAX_FREE_ACTIONS - it }
        override val rawScannedCount: Flow<Int> = MutableStateFlow(0)

        override suspend fun incrementFreeActions() {
            incrementCount += 1
            used.value += 1
        }

        override suspend fun canPerformFreeAction(): Boolean = used.value < UsageRepository.MAX_FREE_ACTIONS
        override suspend fun updateRawScannedCount(count: Int) = Unit
    }

    private class FakeContactRepository : ContactRepository {
        val cleanupStarted = CompletableDeferred<Unit>()
        val cleanupResult = CompletableDeferred<CleanupStatus>()
        val mergeDuplicateStarted = CompletableDeferred<Unit>()
        var mergeDuplicateResult: CleanupStatus? = null

        override suspend fun standardizeAllFormatIssues(): Flow<CleanupStatus> = flow {
            cleanupStarted.complete(Unit)
            emit(CleanupStatus.Progress(0.5f, "Standardizing"))
            emit(cleanupResult.await())
        }

        override suspend fun scanContacts(): Flow<ScanStatus> = emptyFlow()
        override suspend fun deleteContacts(contacts: List<Contact>): Result<Unit> = Result.success(Unit)
        override suspend fun deleteContactsByIds(contactIds: List<Long>): Boolean = true
        override suspend fun deleteContactsByType(type: ContactType): Flow<CleanupStatus> = emptyFlow()
        override suspend fun mergeContacts(contactIds: List<Long>, customName: String?): Boolean = true
        override suspend fun saveContacts(contacts: List<Contact>): Boolean = true
        override suspend fun getDuplicateGroups(type: ContactType): List<DuplicateGroupSummary> = emptyList()
        override suspend fun getAccountGroups(): List<AccountGroupSummary> = emptyList()
        override suspend fun getContactsInGroup(key: String, type: ContactType): List<Contact> = emptyList()
        override suspend fun mergeDuplicateGroups(type: ContactType): Flow<CleanupStatus> = flow {
            mergeDuplicateStarted.complete(Unit)
            mergeDuplicateResult?.let { emit(it) }
        }
        override suspend fun standardizeFormat(ids: List<Long>): Boolean = true
        override suspend fun getContactsSnapshotByIds(ids: List<Long>): List<Contact> = emptyList()
        override suspend fun getContactsAllSnapshot(): List<Contact> = emptyList()
        override suspend fun getContactsSnapshotByType(type: ContactType): List<Contact> = emptyList()
        override suspend fun restoreContacts(contacts: List<Contact>): Boolean = true
        override suspend fun ignoreContact(id: String, displayName: String, reason: String): Boolean = true
        override suspend fun unignoreContact(id: String): Boolean = true
        override fun getIgnoredContacts(): Flow<List<IgnoredContact>> = emptyFlow()
        override fun getContactsFlow(type: ContactType): Flow<List<Contact>> = emptyFlow()
        override fun getAccountCount(): Flow<Int> = MutableStateFlow(0)
        override suspend fun updateScanResultSummary() = Unit
        override suspend fun recalculateWhatsAppCounts() = Unit
        override suspend fun getCrossAccountContacts(): List<CrossAccountContact> = emptyList()
        override suspend fun getContactInstancesByMatchingKey(matchingKey: String): List<Contact> = emptyList()
        override suspend fun consolidateContactToAccount(
            matchingKey: String,
            keepAccountType: String?,
            keepAccountName: String?
        ): Boolean = true
        override suspend fun consolidateContactsToAccount(
            matchingKeys: List<String>,
            keepAccountType: String?,
            keepAccountName: String?
        ): Flow<CleanupStatus> = emptyFlow()
        override suspend fun refreshContacts(contacts: List<Contact>): Boolean = true
    }
}
