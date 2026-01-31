package com.ogabassey.contactscleaner.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/**
 * 2026 Best Practice: iOS implementation of ShareLauncher using UIActivityViewController.
 * Supports sharing to system share sheet, Google Sheets, and Excel.
 */
class IosShareLauncher : ShareLauncher {

    companion object {
        private const val GOOGLE_SHEETS_URL_SCHEME = "googlesheets://"
        private const val EXCEL_URL_SCHEME = "ms-excel://"
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun share(content: String, fileName: String, mimeType: String) {
        val fileUrl = writeToTempFile(content, fileName) ?: return

        val activityItems = listOf(fileUrl)
        val activityViewController = UIActivityViewController(
            activityItems = activityItems,
            applicationActivities = null
        )

        presentViewController(activityViewController)
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun openInGoogleSheets(content: String, fileName: String) {
        if (!isGoogleSheetsAvailable()) {
            // Fallback to system share sheet
            share(content, fileName, "text/csv")
            return
        }

        // Write file and share - Google Sheets will be in the share sheet options
        // Direct URL scheme opening with file content isn't straightforward on iOS
        // The share sheet allows users to select Google Sheets
        share(content, fileName, "text/csv")
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun openInExcel(content: String, fileName: String) {
        if (!isExcelAvailable()) {
            // Fallback to system share sheet
            share(content, fileName, "text/csv")
            return
        }

        // Write file and share - Excel will be in the share sheet options
        share(content, fileName, "text/csv")
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun isGoogleSheetsAvailable(): Boolean {
        val url = NSURL.URLWithString(GOOGLE_SHEETS_URL_SCHEME) ?: return false
        return UIApplication.sharedApplication.canOpenURL(url)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun isExcelAvailable(): Boolean {
        val url = NSURL.URLWithString(EXCEL_URL_SCHEME) ?: return false
        return UIApplication.sharedApplication.canOpenURL(url)
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun writeToTempFile(content: String, fileName: String): NSURL? {
        val tempDir = NSTemporaryDirectory()
        val filePath = (tempDir as NSString).stringByAppendingPathComponent(fileName)

        // Write content to file
        val nsContent = content as NSString
        val success = nsContent.writeToFile(
            filePath,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )

        if (!success) {
            println("⚠️ Failed to write export file to: $filePath")
            return null
        }

        return NSURL.fileURLWithPath(filePath)
    }

    private fun presentViewController(viewController: UIActivityViewController) {
        // Get root view controller
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootViewController == null) {
            println("⚠️ Cannot present share sheet: rootViewController is null")
            return
        }

        // For iPad, we need to set popover presentation
        viewController.popoverPresentationController?.sourceView = rootViewController.view

        rootViewController.presentViewController(
            viewController,
            animated = true,
            completion = null
        )
    }
}

@Composable
actual fun rememberShareLauncher(): ShareLauncher {
    return remember { IosShareLauncher() }
}
