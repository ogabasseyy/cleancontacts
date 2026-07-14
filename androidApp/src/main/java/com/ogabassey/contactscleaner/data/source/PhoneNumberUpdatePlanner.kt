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

        val result = ArrayList<PhoneUpdate>(rows.size)
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
