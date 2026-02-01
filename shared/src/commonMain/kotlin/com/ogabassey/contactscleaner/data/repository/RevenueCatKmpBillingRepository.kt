package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.domain.model.PaywallPackage
import com.ogabassey.contactscleaner.domain.model.Resource
import com.ogabassey.contactscleaner.domain.repository.BillingRepository
import com.ogabassey.contactscleaner.platform.Logger
import com.ogabassey.contactscleaner.platform.RevenueCatConfig
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.result.awaitCustomerInfo
import com.revenuecat.purchases.kmp.result.awaitOfferings
import com.revenuecat.purchases.kmp.result.awaitPurchase
import com.revenuecat.purchases.kmp.result.awaitRestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Billing-specific exceptions for better error handling.
 */
sealed class BillingException(message: String) : Exception(message) {
    class NoOfferingsAvailable : BillingException("No offerings available")
    class PackageNotFound(packageId: String) : BillingException("Package not found: $packageId")
}

/**
 * RevenueCat KMP BillingRepository implementation.
 *
 * 2026 Best Practice: Official RevenueCat KMP SDK for cross-platform IAP.
 * Works on both Android and iOS with a single codebase.
 * Provides close() for proper lifecycle management (call when disposed).
 */
class RevenueCatKmpBillingRepository : BillingRepository {

    companion object {
        private const val TAG = "RevenueCatKmp"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _packages = MutableStateFlow<Resource<List<PaywallPackage>>>(Resource.Loading)
    override val packages: StateFlow<Resource<List<PaywallPackage>>> = _packages.asStateFlow()

    init {
        // Initial refresh on creation
        refresh()
    }

    /**
     * Cancel the coroutine scope when disposed.
     * Call this when the repository is no longer needed.
     */
    fun close() {
        scope.cancel()
    }

    override fun refresh() {
        _packages.value = Resource.Loading
        scope.launch {
            // Run both refreshes concurrently for faster load time
            coroutineScope {
                launch { refreshEntitlements() }
                launch { refreshOfferings() }
            }
        }
    }

    private suspend fun refreshEntitlements() {
        Purchases.sharedInstance.awaitCustomerInfo()
            .onSuccess { customerInfo ->
                val entitlement = customerInfo.entitlements[RevenueCatConfig.premiumEntitlementId]
                val isActive = entitlement?.isActive == true
                Logger.d(TAG, "Premium entitlement active: $isActive")
                _isPremium.value = isActive
            }
            .onFailure { error ->
                Logger.e(TAG, "Error fetching customer info: ${error.message}")
            }
    }

    private suspend fun refreshOfferings() {
        Purchases.sharedInstance.awaitOfferings()
            .onSuccess { offerings ->
                val current = offerings.current
                if (current == null) {
                    Logger.d(TAG, "No current offering available")
                    _packages.value = Resource.Success(emptyList())
                    return@onSuccess
                }

                val packageList = current.availablePackages.map { pkg ->
                    PaywallPackage(
                        id = pkg.storeProduct.id,
                        title = pkg.storeProduct.title,
                        price = pkg.storeProduct.price.formatted,
                        description = pkg.storeProduct.description,
                        identifier = getPackageIdentifier(pkg.identifier)
                    )
                }
                Logger.d(TAG, "Loaded ${packageList.size} packages")
                _packages.value = Resource.Success(packageList)
            }
            .onFailure { error ->
                Logger.e(TAG, "Error fetching offerings: ${error.message}")
                _packages.value = Resource.Error(error.message ?: "Failed to load packages")
            }
    }

    override suspend fun purchasePremium(packageId: String): Result<Unit> {
        Logger.d(TAG, "Starting purchase for package: $packageId")

        return Purchases.sharedInstance.awaitOfferings()
            .mapCatching { offerings ->
                val current = offerings.current
                    ?: throw BillingException.NoOfferingsAvailable()

                // Find the package by ID or identifier
                val packageToBuy = current.availablePackages.find { pkg ->
                    pkg.storeProduct.id == packageId ||
                    pkg.identifier == packageId ||
                    getPackageIdentifier(pkg.identifier) == packageId
                } ?: throw BillingException.PackageNotFound(packageId)

                // Make the purchase
                val purchaseResult = Purchases.sharedInstance.awaitPurchase(packageToBuy)
                    .getOrThrow()

                // Update premium state
                val entitlement = purchaseResult.customerInfo.entitlements[RevenueCatConfig.premiumEntitlementId]
                _isPremium.value = entitlement?.isActive == true
                Logger.d(TAG, "Purchase successful!")
            }
    }

    override suspend fun restorePurchases(): Result<Unit> {
        Logger.d(TAG, "Restoring purchases...")
        return Purchases.sharedInstance.awaitRestore()
            .map { customerInfo ->
                val entitlement = customerInfo.entitlements[RevenueCatConfig.premiumEntitlementId]
                val isActive = entitlement?.isActive == true
                Logger.d(TAG, "Restore successful, premium=$isActive")
                _isPremium.value = isActive
            }
    }

    private fun getPackageIdentifier(identifier: String): String {
        return when {
            identifier.contains("monthly", ignoreCase = true) -> "monthly"
            identifier.contains("annual", ignoreCase = true) -> "annual"
            identifier.contains("lifetime", ignoreCase = true) -> "lifetime"
            identifier.contains("weekly", ignoreCase = true) -> "weekly"
            else -> identifier
        }
    }
}
