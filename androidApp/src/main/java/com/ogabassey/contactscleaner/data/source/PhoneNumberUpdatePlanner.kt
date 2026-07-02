package com.ogabassey.contactscleaner.data.source

object PhoneNumberUpdatePlanner {
    data class PhoneRow(
        val dataId: Long,
        val contactId: Long,
        val rawNumber: String,
        val providerNormalizedNumber: String?
    )

    data class PhoneUpdate(
        val dataId: Long,
        val contactId: Long,
        val targetNumber: String
    )

    fun planUpdates(
        rows: List<PhoneRow>,
        resolveTarget: (rawNumber: String, providerNormalizedNumber: String?) -> String?
    ): List<PhoneUpdate> {
        if (rows.isEmpty()) return emptyList()

        // ⚡ Bolt Optimization: Replace implicit iterator allocation and list resizing (.mapNotNull)
        // with a single-pass indexed loop and a pre-allocated ArrayList to minimize garbage collection
        // overhead during bulk batch processing of phone numbers.
        val result = java.util.ArrayList<PhoneUpdate>(rows.size)
        for (i in rows.indices) {
            val row = rows[i]
            val raw = row.rawNumber
            if (raw.isBlank()) continue

            val target = resolveTarget(raw, row.providerNormalizedNumber)
                ?.trim()
                ?.takeIf { it.isNotBlank() && it != raw }
                ?: continue

            result.add(
                PhoneUpdate(
                    dataId = row.dataId,
                    contactId = row.contactId,
                    targetNumber = target
                )
            )
        }
        return result
    }
}
