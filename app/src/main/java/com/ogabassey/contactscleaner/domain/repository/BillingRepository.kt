package com.ogabassey.contactscleaner.domain.repository

import android.app.Activity
import com.ogabassey.contactscleaner.domain.model.PaywallPackage
import com.ogabassey.contactscleaner.domain.model.Resource
import kotlinx.coroutines.flow.StateFlow

interface BillingRepository {
    val isPremium: StateFlow<Boolean>
    val packages: StateFlow<Resource<List<PaywallPackage>>>
    
    suspend fun purchasePremium(activity: Activity, packageId: String): Result<Unit>
    suspend fun restorePurchases(): Result<Unit>
    fun refresh()
}
