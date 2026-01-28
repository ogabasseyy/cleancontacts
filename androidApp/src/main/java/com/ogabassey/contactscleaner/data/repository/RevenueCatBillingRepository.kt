package com.ogabassey.contactscleaner.data.repository

import android.app.Activity
import android.util.Log
import com.ogabassey.contactscleaner.domain.model.Resource
import com.ogabassey.contactscleaner.domain.repository.BillingRepository
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.Offerings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class RevenueCatBillingRepository @Inject constructor() : BillingRepository {

    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _packages = MutableStateFlow<Resource<List<com.ogabassey.contactscleaner.domain.model.PaywallPackage>>>(Resource.Loading)
    override val packages: StateFlow<Resource<List<com.ogabassey.contactscleaner.domain.model.PaywallPackage>>> = _packages.asStateFlow()

    private fun refreshOfferings() {
        if (!Purchases.isConfigured) {
             _packages.value = Resource.Error("RevenueCat not configured")
             return
        }
        
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                val current = offerings.current
                if (current == null) {
                    _packages.value = Resource.Success(emptyList()) // Valid but empty
                    return
                }
                
                val packages = current.availablePackages.map { pkg ->
                    com.ogabassey.contactscleaner.domain.model.PaywallPackage(
                        id = pkg.product.id,
                        title = pkg.product.title,
                        price = pkg.product.price.formatted,
                        description = pkg.product.description,
                        identifier = when (pkg.packageType) {
                            com.revenuecat.purchases.PackageType.MONTHLY -> "monthly"
                            com.revenuecat.purchases.PackageType.ANNUAL -> "annual"
                            com.revenuecat.purchases.PackageType.LIFETIME -> "lifetime"
                            else -> pkg.identifier
                        }
                    )
                }
                _packages.value = Resource.Success(packages)
            }
            override fun onError(error: PurchasesError) {
                if (com.ogabassey.contactscleaner.BuildConfig.DEBUG) {
                    // MOCK SANDBOX FOR TESTING WITHOUT PLAY STORE
                    val mockPackages = listOf(
                        com.ogabassey.contactscleaner.domain.model.PaywallPackage("mock_monthly", "Monthly", "$4.99", "Billed monthly", "monthly"),
                        com.ogabassey.contactscleaner.domain.model.PaywallPackage("mock_annual", "Annual", "$39.99", "Billed annually", "annual"),
                        com.ogabassey.contactscleaner.domain.model.PaywallPackage("mock_lifetime", "Lifetime", "$99.99", "One-time payment", "lifetime")
                    )
                    _packages.value = Resource.Success(mockPackages)
                } else {
                    Log.e("Billing", "Error fetching offerings: ${error.message}")
                    _packages.value = Resource.Error(error.message)
                }
            }
        })
    }

    private fun refreshEntitlements() {
        if (!Purchases.isConfigured) return
        
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updatePremiumState(customerInfo)
            }
            override fun onError(error: PurchasesError) {
                Log.e("Billing", "Error fetching info: ${error.message}")
            }
        })
    }
    
    override fun refresh() {
        _packages.value = Resource.Loading
        refreshEntitlements()
        refreshOfferings()
    }
    
    private fun updatePremiumState(info: CustomerInfo) {
        _isPremium.value = info.entitlements["premium"]?.isActive == true
    }

    override suspend fun purchasePremium(activity: Activity, packageId: String): Result<Unit> = suspendCoroutine { continuation ->
        if (com.ogabassey.contactscleaner.BuildConfig.DEBUG && packageId.startsWith("mock_")) {
             // MOCK PURCHASE SUCCESS
             _isPremium.value = true
             continuation.resume(Result.success(Unit))
             return@suspendCoroutine
        }
    
        if (!Purchases.isConfigured) {
             continuation.resume(Result.failure(Exception("RevenueCat not configured")))
             return@suspendCoroutine
        }
        
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                val packageToBuy = offerings.current?.availablePackages?.find { it.product.id == packageId }
                    ?: offerings.current?.availablePackages?.find { 
                        when (it.packageType) {
                            com.revenuecat.purchases.PackageType.MONTHLY -> "monthly"
                            com.revenuecat.purchases.PackageType.ANNUAL -> "annual"
                            com.revenuecat.purchases.PackageType.LIFETIME -> "lifetime"
                            else -> it.identifier
                        } == packageId 
                    }

                if (packageToBuy != null) {
                    val params = com.revenuecat.purchases.PurchaseParams.Builder(activity, packageToBuy).build()
                    Purchases.sharedInstance.purchase(
                        params,
                        object : PurchaseCallback {
                            override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                                updatePremiumState(customerInfo)
                                continuation.resume(Result.success(Unit))
                            }
                            override fun onError(error: PurchasesError, userCancelled: Boolean) {
                                 continuation.resume(Result.failure(Exception(error.message)))
                            }
                        }
                    )
                } else {
                     continuation.resume(Result.failure(Exception("Package not found: $packageId")))
                }
            }
            override fun onError(error: PurchasesError) {
                 continuation.resume(Result.failure(Exception("Failed to get offerings: ${error.message}")))
            }
        })
    }

    override suspend fun restorePurchases(): Result<Unit> = suspendCoroutine { continuation ->
        if (!Purchases.isConfigured) {
            continuation.resume(Result.failure(Exception("RevenueCat not configured")))
            return@suspendCoroutine
        }

        Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updatePremiumState(customerInfo)
                continuation.resume(Result.success(Unit))
            }
            override fun onError(error: PurchasesError) {
                continuation.resume(Result.failure(Exception(error.message)))
            }
        })
    }
}
