package com.ogabassey.contactscleaner.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * 2026 Best Practice: Android implementation of ShareLauncher using FileProvider.
 * Supports sharing to system share sheet, Google Sheets, and Excel.
 */
class AndroidShareLauncher(private val context: Context) : ShareLauncher {

    companion object {
        private const val GOOGLE_SHEETS_PACKAGE = "com.google.android.apps.docs.editors.sheets"
        private const val EXCEL_PACKAGE = "com.microsoft.office.excel"
        private const val EXPORTS_DIR = "exports"
    }

    override fun share(content: String, fileName: String, mimeType: String) {
        val file = writeToTempFile(content, fileName)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share contacts")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    override fun openInGoogleSheets(content: String, fileName: String) {
        if (!isGoogleSheetsAvailable()) {
            // Fallback to system share sheet
            share(content, fileName, "text/csv")
            return
        }

        val file = writeToTempFile(content, fileName)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            setPackage(GOOGLE_SHEETS_PACKAGE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to share sheet if direct open fails
            share(content, fileName, "text/csv")
        }
    }

    override fun openInExcel(content: String, fileName: String) {
        if (!isExcelAvailable()) {
            // Fallback to system share sheet with Excel-compatible MIME type
            share(content, fileName, "application/vnd.ms-excel")
            return
        }

        val file = writeToTempFile(content, fileName)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            setPackage(EXCEL_PACKAGE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to share sheet if direct open fails
            share(content, fileName, "application/vnd.ms-excel")
        }
    }

    override fun isGoogleSheetsAvailable(): Boolean {
        return isPackageInstalled(GOOGLE_SHEETS_PACKAGE)
    }

    override fun isExcelAvailable(): Boolean {
        return isPackageInstalled(EXCEL_PACKAGE)
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun writeToTempFile(content: String, fileName: String): File {
        val exportsDir = File(context.cacheDir, EXPORTS_DIR)
        if (!exportsDir.exists()) {
            exportsDir.mkdirs()
        }

        // Clean up old export files (older than 1 hour)
        cleanupOldExports(exportsDir)

        val file = File(exportsDir, fileName)
        file.writeText(content)
        return file
    }

    private fun cleanupOldExports(exportsDir: File) {
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        exportsDir.listFiles()?.forEach { file ->
            if (file.lastModified() < oneHourAgo) {
                file.delete()
            }
        }
    }
}

@Composable
actual fun rememberShareLauncher(): ShareLauncher {
    val context = LocalContext.current
    return remember { AndroidShareLauncher(context) }
}
