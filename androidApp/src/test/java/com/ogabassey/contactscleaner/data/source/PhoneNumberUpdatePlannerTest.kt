package com.ogabassey.contactscleaner.data.source

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumberUpdatePlannerTest {
    @Test
    fun `plans updates for rows from multiple contacts in one chunk`() {
        val rows = listOf(
            PhoneNumberUpdatePlanner.PhoneRow(
                dataId = 10,
                contactId = 1,
                rawNumber = "08079182146",
                providerNormalizedNumber = "+2348079182146"
            ),
            PhoneNumberUpdatePlanner.PhoneRow(
                dataId = 11,
                contactId = 2,
                rawNumber = "2347036263414",
                providerNormalizedNumber = null
            ),
            PhoneNumberUpdatePlanner.PhoneRow(
                dataId = 12,
                contactId = 3,
                rawNumber = "+2348086142963",
                providerNormalizedNumber = "+2348086142963"
            )
        )

        val updates = PhoneNumberUpdatePlanner.planUpdates(rows) { raw, providerNormalized ->
            when {
                providerNormalized != null && raw != providerNormalized -> providerNormalized
                raw == "2347036263414" -> "+2347036263414"
                else -> null
            }
        }

        assertEquals(
            listOf(
                PhoneNumberUpdatePlanner.PhoneUpdate(
                    dataId = 10,
                    contactId = 1,
                    targetNumber = "+2348079182146"
                ),
                PhoneNumberUpdatePlanner.PhoneUpdate(
                    dataId = 11,
                    contactId = 2,
                    targetNumber = "+2347036263414"
                )
            ),
            updates
        )
    }

    @Test
    fun `skips blank unchanged and empty target rows`() {
        val rows = listOf(
            PhoneNumberUpdatePlanner.PhoneRow(
                dataId = 20,
                contactId = 1,
                rawNumber = "",
                providerNormalizedNumber = "+2348079182146"
            ),
            PhoneNumberUpdatePlanner.PhoneRow(
                dataId = 21,
                contactId = 2,
                rawNumber = "+2347036263414",
                providerNormalizedNumber = "+2347036263414"
            ),
            PhoneNumberUpdatePlanner.PhoneRow(
                dataId = 22,
                contactId = 3,
                rawNumber = "2348086142963",
                providerNormalizedNumber = null
            )
        )

        val updates = PhoneNumberUpdatePlanner.planUpdates(rows) { _, _ -> null }

        assertEquals(emptyList<PhoneNumberUpdatePlanner.PhoneUpdate>(), updates)
    }
}
