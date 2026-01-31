package com.ogabassey.contactscleaner.util

import androidx.compose.runtime.Composable

/**
 * 2026 Best Practice: Platform abstraction for native file sharing.
 * Supports sharing to system share sheet, Google Sheets, and Excel.
 */
interface ShareLauncher {
    /**
     * Share content via the native system share sheet.
     * @param content The text content to share
     * @param fileName The suggested filename (e.g., "contacts.csv")
     * @param mimeType The MIME type (e.g., "text/csv", "text/vcard")
     */
    fun share(content: String, fileName: String, mimeType: String)

    /**
     * Open content directly in Google Sheets app if available.
     * Falls back to system share sheet if not installed.
     * @param content CSV content to open
     * @param fileName The suggested filename
     */
    fun openInGoogleSheets(content: String, fileName: String)

    /**
     * Open content in Excel app if available.
     * Falls back to system share sheet if not installed.
     * @param content CSV content to open
     * @param fileName The suggested filename
     */
    fun openInExcel(content: String, fileName: String)

    /**
     * Check if Google Sheets app is installed.
     */
    fun isGoogleSheetsAvailable(): Boolean

    /**
     * Check if Excel app is installed.
     */
    fun isExcelAvailable(): Boolean
}

/**
 * Export format for sharing.
 */
enum class ExportFormat(val extension: String, val mimeType: String) {
    CSV("csv", "text/csv"),
    VCARD("vcf", "text/vcard"),
    // Note: True xlsx requires platform-specific libraries
    // CSV with xlsx extension works for import in most apps
    EXCEL_COMPATIBLE("csv", "application/vnd.ms-excel")
}

/**
 * Remember a ShareLauncher instance for the current composition.
 */
@Composable
expect fun rememberShareLauncher(): ShareLauncher
