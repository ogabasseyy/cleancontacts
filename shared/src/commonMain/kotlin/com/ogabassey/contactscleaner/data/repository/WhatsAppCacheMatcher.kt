package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.util.extractDigits

object WhatsAppCacheMatcher {
    fun hasCachedWhatsAppNumber(rawNumbers: String, cachedNumbers: Set<String>): Boolean {
        if (rawNumbers.isBlank() || cachedNumbers.isEmpty()) return false

        return rawNumbers
            .split(",")
            .asSequence()
            .map { it.extractDigits() }
            .filter { it.isNotBlank() }
            .any { it in cachedNumbers }
    }
}
