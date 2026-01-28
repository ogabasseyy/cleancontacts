package com.ogabassey.contactscleaner.platform

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

/**
 * Android implementation using TelephonyManager and Locale.
 */
class AndroidRegionProvider(private val context: Context) : RegionProvider {

    override fun getRegionIso(): String {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            telephonyManager?.networkCountryIso?.uppercase()?.takeIf { it.isNotEmpty() }
                ?: telephonyManager?.simCountryIso?.uppercase()?.takeIf { it.isNotEmpty() }
                ?: Locale.getDefault().country.takeIf { it.isNotEmpty() }
                ?: "US"
        } catch (e: Exception) {
            Locale.getDefault().country.takeIf { it.isNotEmpty() } ?: "US"
        }
    }

    override fun getDisplayCountry(regionCode: String): String {
        return try {
            Locale.Builder().setRegion(regionCode).build().displayCountry
        } catch (e: Exception) {
            regionCode
        }
    }
}
