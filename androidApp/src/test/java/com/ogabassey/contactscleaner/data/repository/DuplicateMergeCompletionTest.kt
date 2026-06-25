package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.domain.model.CleanupStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateMergeCompletionTest {
    @Test
    fun `returns success when all mergeable groups were merged`() {
        val status = DuplicateMergeCompletion.status(
            totalGroups = 29,
            mergedGroups = 29,
            skippedSyncedGroups = 0,
            failedMergeableGroups = 0
        )

        assertTrue(status is CleanupStatus.Success)
        assertEquals("Merged 29 groups successfully", (status as CleanupStatus.Success).message)
    }

    @Test
    fun `returns success with review count when synced groups were skipped`() {
        val status = DuplicateMergeCompletion.status(
            totalGroups = 42,
            mergedGroups = 29,
            skippedSyncedGroups = 13,
            failedMergeableGroups = 0
        )

        assertTrue(status is CleanupStatus.Success)
        assertEquals("Merged 29 groups; 13 synced groups need review", (status as CleanupStatus.Success).message)
    }

    @Test
    fun `returns error when no mergeable groups exist`() {
        val status = DuplicateMergeCompletion.status(
            totalGroups = 13,
            mergedGroups = 0,
            skippedSyncedGroups = 13,
            failedMergeableGroups = 0
        )

        assertTrue(status is CleanupStatus.Error)
        assertEquals("No mergeable duplicate groups found", (status as CleanupStatus.Error).message)
    }

    @Test
    fun `returns partial when a provider-backed merge fails after progress`() {
        val status = DuplicateMergeCompletion.status(
            totalGroups = 5,
            mergedGroups = 3,
            skippedSyncedGroups = 0,
            failedMergeableGroups = 2
        )

        assertTrue(status is CleanupStatus.Partial)
        assertEquals("Merged 3 of 5 groups", (status as CleanupStatus.Partial).message)
    }
}
