package com.ogabassey.contactscleaner.data.repository

object WhatsAppCacheMatcher {
    // ⚡ Bolt Optimization: Replaced `.split(",").asSequence().map { ... }` with a
    // single-pass loop to eliminate intermediate `List` and `Sequence` wrapper allocations
    // during high-frequency bulk contact sync, significantly reducing GC pressure.
    fun hasCachedWhatsAppNumber(rawNumbers: String, cachedNumbers: Set<String>): Boolean {
        if (rawNumbers.isBlank() || cachedNumbers.isEmpty()) return false

        val sb = StringBuilder()
        for (i in rawNumbers.indices) {
            val c = rawNumbers[i]
            if (c == ',') {
                if (sb.isNotEmpty()) {
                    if (cachedNumbers.contains(sb.toString())) return true
                    sb.clear()
                }
            } else if (c.isDigit()) {
                sb.append(c.digitToInt().digitToChar())
            }
        }
        return sb.isNotEmpty() && cachedNumbers.contains(sb.toString())
    }
}
