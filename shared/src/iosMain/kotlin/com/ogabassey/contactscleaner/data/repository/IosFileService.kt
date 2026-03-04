package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.domain.repository.FileService
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSError
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

/**
 * iOS FileService implementation using Foundation framework.
 *
 * 2026 KMP Best Practice: Platform-specific file handling via Kotlin/Native.
 */
class IosFileService : FileService {

    @OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
    override suspend fun generateCsvFile(fileName: String, content: String): Result<String> {
        return try {
            // Get caches directory path using NSSearchPathForDirectoriesInDomains
            val cachesPaths = NSSearchPathForDirectoriesInDomains(
                NSCachesDirectory,
                NSUserDomainMask,
                true
            )
            val cachesDir = cachesPaths.firstOrNull() as? String
                ?: return Result.failure(Exception("Could not find caches directory"))

            // 2026 Best Practice: Sanitize filename to prevent path traversal attacks
            val sanitizedName = sanitizeFileName(fileName)
            val filePath = "$cachesDir/$sanitizedName"

            val nsString = NSString.create(string = content)

            // 2026 Best Practice: Capture NSError for proper error handling
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val success = nsString.writeToFile(
                    filePath,
                    atomically = true,
                    encoding = NSUTF8StringEncoding,
                    error = errorPtr.ptr
                )

                if (success) {
                    Result.success(filePath)
                } else {
                    val nsError = errorPtr.value
                    val errorMessage = nsError?.localizedDescription ?: "Unknown write error"
                    Result.failure(Exception("Failed to write file: $errorMessage"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sanitizes a filename to prevent path traversal attacks.
     * Throws Exception if a traversal attempt is detected.
     */
    private fun sanitizeFileName(fileName: String): String {
        // 2026 Security Best Practice: Fail fast on obvious traversal attempts
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw IllegalArgumentException("Invalid filename")
        }

        // Further sanitize to ensure a safe filename (remove leading dots, weird chars)
        return fileName
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .replace(Regex("^[._]+"), "")
            .ifEmpty { "export.csv" }
    }
}
