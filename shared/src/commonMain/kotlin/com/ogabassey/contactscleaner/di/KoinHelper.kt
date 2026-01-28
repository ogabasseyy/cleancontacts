package com.ogabassey.contactscleaner.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/**
 * Helper for initializing Koin across platforms.
 *
 * 2026 KMP Best Practice: Centralized Koin setup for multiplatform.
 */
object KoinHelper {

    /**
     * Initialize Koin with shared and platform modules.
     * Call this from the platform-specific application entry point.
     *
     * @param appDeclaration Platform-specific Koin configuration (e.g., androidContext())
     * @param additionalModules Additional modules to include (e.g., ViewModel modules)
     */
    fun initKoin(
        appDeclaration: KoinAppDeclaration = {},
        additionalModules: List<Module> = emptyList()
    ) {
        startKoin {
            allowOverride(true)
            appDeclaration()
            modules(
                listOf(sharedModule, platformModule()) + additionalModules
            )
        }
    }
}

/**
 * Get all KMP modules for Koin.
 * Useful for unit testing or custom Koin initialization.
 */
fun getKmpModules(): List<Module> = listOf(sharedModule, platformModule())
