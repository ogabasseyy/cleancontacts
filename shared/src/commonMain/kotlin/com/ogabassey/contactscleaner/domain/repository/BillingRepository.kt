package com.ogabassey.contactscleaner.domain.repository

import com.ogabassey.contactscleaner.domain.model.PaywallPackage
import com.ogabassey.contactscleaner.domain.model.Resource
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for billing operations.
 *
 * 2026 KMP Best Practice: Platform-agnostic interface.
 * Platform-specific implementations handle Activity/UIViewController references internally.
 */
interface BillingRepository {
    val isPremium: StateFlow<Boolean>
    val packages: StateFlow<Resource<List<PaywallPackage>>>

    /**
     * Purchase a premium package.
     * Platform implementations handle activity/context internally.
     */
    suspend fun purchasePremium(packageId: String): Result<Unit>

    /**
     * Restore previous purchases.
     */
    suspend fun restorePurchases(): Result<Unit>

    /**
     * Refresh available packages.
     */
    fun refresh()
}
