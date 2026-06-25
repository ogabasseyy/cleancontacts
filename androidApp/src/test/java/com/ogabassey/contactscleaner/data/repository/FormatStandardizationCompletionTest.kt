package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.domain.model.CleanupStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatStandardizationCompletionTest {
    @Test
    fun `returns success when cache rescan shows all format issues were resolved`() {
        val status = FormatStandardizationCompletion.status(
            total = 5,
            remainingCount = 0
        )

        assertTrue(status is CleanupStatus.Success)
        assertEquals("Standardized 5 contacts successfully", (status as CleanupStatus.Success).message)
    }

    @Test
    fun `returns success with review count when cache rescan shows partial progress`() {
        val status = FormatStandardizationCompletion.status(
            total = 5,
            remainingCount = 2
        )

        assertTrue(status is CleanupStatus.Success)
        assertEquals("Standardized 3 contacts; 2 need review", (status as CleanupStatus.Success).message)
    }

    @Test
    fun `returns error when cache rescan shows no progress`() {
        val status = FormatStandardizationCompletion.status(
            total = 5,
            remainingCount = 5
        )

        assertTrue(status is CleanupStatus.Error)
        assertEquals("Failed to standardize contacts", (status as CleanupStatus.Error).message)
    }
}
