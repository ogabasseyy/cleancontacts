package com.ogabassey.contactscleaner.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BackgroundOperationManagerTest {
    @AfterTest
    fun tearDown() {
        BackgroundOperationManager.cancel(reason = "test_teardown")
        BackgroundOperationManager.dismiss()
    }

    @Test
    fun launchedOperationContinuesAfterCallerScopeIsCancelled() = runBlocking {
        val callerJob = Job()
        val operationStarted = CompletableDeferred<Unit>()
        val finishOperation = CompletableDeferred<Unit>()

        val operationJob = withContext(callerJob) {
            BackgroundOperationManager.launchOperation(
                type = OperationType.STANDARDIZE_FORMAT,
                totalItems = 10,
                title = "Test operation"
            ) {
                operationStarted.complete(Unit)
                finishOperation.await()
                BackgroundOperationManager.complete(true, "finished")
            }
        }

        assertNotNull(operationJob)
        withTimeout(1_000) {
            operationStarted.await()
        }
        callerJob.cancel()
        delay(50)

        assertEquals(OperationStatus.Running, BackgroundOperationManager.currentOperation.value?.status)

        finishOperation.complete(Unit)
        withTimeout(1_000) {
            while (BackgroundOperationManager.currentOperation.value?.status != OperationStatus.Completed) {
                delay(10)
            }
        }
    }

    @Test
    fun explicitCancelStopsLaunchedOperation() = runBlocking {
        val operationStarted = CompletableDeferred<Unit>()
        val operationCancelled = CompletableDeferred<Unit>()

        BackgroundOperationManager.launchOperation(
            type = OperationType.DELETE_CONTACTS,
            totalItems = 1,
            title = "Test cancel"
        ) {
            try {
                operationStarted.complete(Unit)
                awaitCancellation()
            } finally {
                operationCancelled.complete(Unit)
            }
        }

        withTimeout(1_000) {
            operationStarted.await()
        }
        BackgroundOperationManager.cancel(reason = "test_cancel")

        withTimeout(1_000) {
            operationCancelled.await()
        }
        assertEquals(OperationStatus.Cancelled, BackgroundOperationManager.currentOperation.value?.status)
    }

    @Test
    fun launchedOperationCompletesWhenBlockReturnsNormally() = runBlocking {
        BackgroundOperationManager.launchOperation(
            type = OperationType.STANDARDIZE_FORMAT,
            totalItems = 1,
            title = "Test normal return"
        ) {
            BackgroundOperationManager.updateProgress(processed = 1)
        }

        withTimeout(1_000) {
            while (BackgroundOperationManager.currentOperation.value?.status == OperationStatus.Running) {
                delay(10)
            }
        }
        assertEquals(OperationStatus.Completed, BackgroundOperationManager.currentOperation.value?.status)
    }
}
