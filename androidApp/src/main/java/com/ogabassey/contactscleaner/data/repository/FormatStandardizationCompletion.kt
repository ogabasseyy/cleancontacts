package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.domain.model.CleanupStatus

object FormatStandardizationCompletion {
    fun status(
        total: Int,
        remainingCount: Int
    ): CleanupStatus {
        val safeTotal = total.coerceAtLeast(0)
        val safeRemaining = remainingCount.coerceIn(0, safeTotal)
        val resolvedCount = safeTotal - safeRemaining

        return when {
            safeTotal == 0 -> CleanupStatus.Success("No formatting issues found")
            safeRemaining == 0 -> CleanupStatus.Success("Standardized $resolvedCount contacts successfully")
            resolvedCount > 0 -> CleanupStatus.Success("Standardized $resolvedCount contacts; $safeRemaining need review")
            else -> CleanupStatus.Error("Failed to standardize contacts")
        }
    }
}
