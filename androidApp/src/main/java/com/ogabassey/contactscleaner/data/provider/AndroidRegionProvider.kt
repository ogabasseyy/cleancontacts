package com.ogabassey.contactscleaner.data.provider

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

import com.ogabassey.contactscleaner.platform.RegionProvider

/**
 * 2026 AGP 9.0: Migrated from Hilt to Koin.
 */
class AndroidRegionProvider(
    private val context: Context
) : RegionProvider {

    override fun getDisplayCountry(regionCode: String): String {
        return try {
            Locale("", regionCode).displayCountry
        } catch (e: Exception) {
            regionCode
        }
    }

    override fun getRegionIso(): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simCountry = tm?.simCountryIso
            val networkCountry = tm?.networkCountryIso

            when {
                !simCountry.isNullOrBlank() -> simCountry.uppercase(Locale.getDefault())
                !networkCountry.isNullOrBlank() -> networkCountry.uppercase(Locale.getDefault())
                else -> Locale.getDefault().country
            }
        } catch (e: Exception) {
            Locale.getDefault().country
        }
    }
}
