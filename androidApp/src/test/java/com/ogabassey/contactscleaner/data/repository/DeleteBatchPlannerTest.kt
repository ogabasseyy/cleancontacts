package com.ogabassey.contactscleaner.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteBatchPlannerTest {
    @Test
    fun `keeps small deletes responsive`() {
        assertEquals(50, DeleteBatchPlanner.batchSize(totalContacts = 300))
    }

    @Test
    fun `uses larger batches for medium deletes`() {
        assertEquals(250, DeleteBatchPlanner.batchSize(totalContacts = 1_200))
    }

    @Test
    fun `uses bulk batches for large deletes`() {
        assertEquals(500, DeleteBatchPlanner.batchSize(totalContacts = 20_290))
    }
}
