package com.ogabassey.contactscleaner

import android.content.Context
import com.ogabassey.contactscleaner.di.KoinHelper
import com.ogabassey.contactscleaner.di.mockModule
import com.ogabassey.contactscleaner.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

/**
 * Android Koin initializer.
 *
 * Call this from your Application class or Activity.
 */
object KoinInitializer {
    private var initialized = false

    fun init(context: Context) {
        if (!initialized) {
            KoinHelper.initKoin(
                appDeclaration = {
                    androidLogger(Level.DEBUG)
                    androidContext(context)
                },
                additionalModules = listOf(viewModelModule)
            )
            initialized = true
        }
    }
}
