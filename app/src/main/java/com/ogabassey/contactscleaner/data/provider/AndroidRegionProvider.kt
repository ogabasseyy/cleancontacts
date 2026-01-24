package com.ogabassey.contactscleaner.data.provider

import android.content.Context
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidRegionProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : RegionProvider {

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
