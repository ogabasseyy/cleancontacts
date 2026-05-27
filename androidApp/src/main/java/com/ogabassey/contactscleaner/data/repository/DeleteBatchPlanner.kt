package com.ogabassey.contactscleaner.data.repository

object DeleteBatchPlanner {
    fun batchSize(totalContacts: Int): Int {
        return when {
            totalContacts >= 5_000 -> 500
            totalContacts >= 1_000 -> 250
            else -> 50
        }
    }
}
