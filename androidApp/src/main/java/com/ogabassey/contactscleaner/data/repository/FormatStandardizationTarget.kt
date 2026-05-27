package com.ogabassey.contactscleaner.data.repository

/**
 * Resolves the exact value to write for one phone-number row during format standardization.
 *
 * Detection may mark a row because Android's Contacts provider already exposes a normalized
 * international number, even when the app's missing-plus detector intentionally ignores local
 * numbers such as "080...". Standardization must honor the same provider signal.
 */
object FormatStandardizationTarget {
    fun resolve(
        rawNumber: String,
        providerNormalizedNumber: String?,
        detectMissingPlus: (String) -> String?
    ): String? {
        val raw = rawNumber.trim()
        if (raw.isBlank()) return null

        when (raw.firstOrNull()) {
            '+', '*', '#' -> return null
        }

        val providerNormalized = providerNormalizedNumber
            ?.trim()
            ?.takeIf { it.isNotBlank() && it.startsWith("+") && it != raw }
        if (providerNormalized != null) {
            return providerNormalized
        }

        return detectMissingPlus(raw)
            ?.trim()
            ?.takeIf { it.isNotBlank() && it != raw }
    }
}
