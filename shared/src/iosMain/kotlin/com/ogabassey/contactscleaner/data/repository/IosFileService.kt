package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.domain.repository.FileService
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.darwin.NSObject
import platform.darwin.NSObjectMeta
import kotlinx.cinterop.ObjCObjectVar

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

            val filePath = "$cachesDir/$fileName"

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
}
