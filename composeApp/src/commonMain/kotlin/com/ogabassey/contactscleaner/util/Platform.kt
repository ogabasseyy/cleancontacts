package com.ogabassey.contactscleaner.util

/**
 * Platform detection for Compose Multiplatform.
 * Used to conditionally render platform-specific UI.
 */
expect val isIOS: Boolean
expect val isAndroid: Boolean
