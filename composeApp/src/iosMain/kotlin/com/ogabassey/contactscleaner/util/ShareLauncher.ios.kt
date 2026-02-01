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
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPopoverPresentationController
import platform.UIKit.popoverPresentationController

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
        // iOS cannot directly open files in specific apps via URL schemes
        // The share sheet allows users to select Google Sheets if installed
        share(content, fileName, "text/csv")
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun openInExcel(content: String, fileName: String) {
        // iOS cannot directly open files in specific apps via URL schemes
        // The share sheet allows users to select Excel if installed
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

    @OptIn(ExperimentalForeignApi::class)
    private fun presentViewController(viewController: UIActivityViewController) {
        // Get root view controller
        // Note: keyWindow is deprecated but universally available; scene-based APIs have
        // complex Kotlin/Native interop issues
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootViewController == null) {
            println("⚠️ Cannot present share sheet: rootViewController is null")
            return
        }

        // For iPad, we need to set popover presentation with both sourceView and sourceRect
        // to avoid crashes. Using the root view centers the popover.
        (viewController.popoverPresentationController as? UIPopoverPresentationController)?.let { popover ->
            popover.setSourceView(rootViewController.view)
            popover.setSourceRect(CGRectMake(0.0, 0.0, 0.0, 0.0))
        }

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
