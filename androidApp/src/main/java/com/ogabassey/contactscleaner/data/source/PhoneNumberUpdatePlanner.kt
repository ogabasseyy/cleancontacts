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

        return rows.mapNotNull { row ->
            val raw = row.rawNumber
            if (raw.isBlank()) return@mapNotNull null

            val target = resolveTarget(raw, row.providerNormalizedNumber)
                ?.trim()
                ?.takeIf { it.isNotBlank() && it != raw }
                ?: return@mapNotNull null

            PhoneUpdate(
                dataId = row.dataId,
                contactId = row.contactId,
                targetNumber = target
            )
        }
    }
}
