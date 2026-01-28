package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.domain.model.PaywallPackage
import com.ogabassey.contactscleaner.domain.model.Resource
import com.ogabassey.contactscleaner.domain.repository.BillingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mock BillingRepository for development, testing, and iOS.
 *
 * 2026 KMP Best Practice: Shared mock implementation for platforms without
 * native billing SDK integration.
 */
class MockBillingRepository : BillingRepository {

    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _packages = MutableStateFlow<Resource<List<PaywallPackage>>>(
        Resource.Success(
            listOf(
                PaywallPackage(
                    id = "monthly_001",
                    title = "Monthly",
                    price = "$4.99/month",
                    description = "Billed monthly",
                    identifier = "monthly"
                ),
                PaywallPackage(
                    id = "annual_001",
                    title = "Annual",
                    price = "$29.99/year",
                    description = "Save 50%",
                    identifier = "annual"
                ),
                PaywallPackage(
                    id = "lifetime_001",
                    title = "Lifetime",
                    price = "$79.99",
                    description = "One-time purchase",
                    identifier = "lifetime"
                )
            )
        )
    )
    override val packages: StateFlow<Resource<List<PaywallPackage>>> = _packages.asStateFlow()

    override suspend fun purchasePremium(packageId: String): Result<Unit> {
        // Simulate purchase delay
        delay(2000)
        _isPremium.value = true
        return Result.success(Unit)
    }

    override suspend fun restorePurchases(): Result<Unit> {
        // Simulate restore delay
        delay(1500)
        // For mock, simulate no purchases to restore
        return Result.success(Unit)
    }

    override fun refresh() {
        // Already loaded mock data
    }
}
