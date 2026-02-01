package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.domain.model.PaywallPackage
import com.ogabassey.contactscleaner.domain.model.Resource
import com.ogabassey.contactscleaner.domain.repository.BillingRepository
import com.ogabassey.contactscleaner.platform.Logger
import com.ogabassey.contactscleaner.platform.RevenueCatConfig
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PackageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * RevenueCat KMP BillingRepository implementation.
 *
 * 2026 Best Practice: Official RevenueCat KMP SDK for cross-platform IAP.
 * Works on both Android and iOS with a single codebase.
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

    override fun refresh() {
        _packages.value = Resource.Loading
        scope.launch {
            refreshEntitlements()
            refreshOfferings()
        }
    }

    private suspend fun refreshEntitlements() {
        try {
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            updatePremiumState(customerInfo)
        } catch (e: Exception) {
            Logger.e(TAG, "Error fetching customer info: ${e.message}")
        }
    }

    private suspend fun refreshOfferings() {
        try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val current = offerings.current

            if (current == null) {
                Logger.d(TAG, "No current offering available")
                _packages.value = Resource.Success(emptyList())
                return
            }

            val packageList = current.availablePackages.map { pkg ->
                pkg.toPaywallPackage()
            }
            Logger.d(TAG, "Loaded ${packageList.size} packages")
            _packages.value = Resource.Success(packageList)
        } catch (e: Exception) {
            Logger.e(TAG, "Error fetching offerings: ${e.message}")
            _packages.value = Resource.Error(e.message ?: "Failed to load packages")
        }
    }

    override suspend fun purchasePremium(packageId: String): Result<Unit> {
        return try {
            Logger.d(TAG, "Starting purchase for package: $packageId")

            // First, get the offerings to find the package
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val current = offerings.current
                ?: return Result.failure(Exception("No offerings available"))

            // Find the package by ID or identifier
            val packageToBuy = current.availablePackages.find { pkg ->
                pkg.product.id == packageId || pkg.identifier == packageId ||
                        getPackageIdentifier(pkg.packageType) == packageId
            } ?: return Result.failure(Exception("Package not found: $packageId"))

            // Make the purchase
            val purchaseResult = Purchases.sharedInstance.awaitPurchase(packageToBuy)
            updatePremiumState(purchaseResult.customerInfo)
            Logger.d(TAG, "Purchase successful!")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Purchase failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun restorePurchases(): Result<Unit> {
        return try {
            Logger.d(TAG, "Restoring purchases...")
            val customerInfo = Purchases.sharedInstance.awaitRestore()
            updatePremiumState(customerInfo)
            Logger.d(TAG, "Restore successful, premium=${_isPremium.value}")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Restore failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun updatePremiumState(customerInfo: CustomerInfo) {
        val entitlement = customerInfo.entitlements[RevenueCatConfig.premiumEntitlementId]
        val isActive = entitlement?.isActive == true
        Logger.d(TAG, "Premium entitlement active: $isActive")
        _isPremium.value = isActive
    }

    private fun Package.toPaywallPackage(): PaywallPackage {
        return PaywallPackage(
            id = product.id,
            title = product.title,
            price = product.price.formatted,
            description = product.description,
            identifier = getPackageIdentifier(packageType)
        )
    }

    private fun getPackageIdentifier(type: PackageType): String {
        return when (type) {
            PackageType.MONTHLY -> "monthly"
            PackageType.ANNUAL -> "annual"
            PackageType.LIFETIME -> "lifetime"
            PackageType.WEEKLY -> "weekly"
            PackageType.SIX_MONTH -> "six_month"
            PackageType.THREE_MONTH -> "three_month"
            PackageType.TWO_MONTH -> "two_month"
            else -> "custom"
        }
    }
}
