package com.ogabassey.contactscleaner.data.repository

import android.content.Context
import com.ogabassey.contactscleaner.domain.repository.FileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * 2026 AGP 9.0: Migrated from Hilt to Koin.
 */
class FileServiceImpl(
    private val context: Context
) : FileService {

    override suspend fun generateCsvFile(fileName: String, content: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 2026 Best Practice: Sanitize filename to prevent path traversal attacks
            val sanitizedName = sanitizeFileName(fileName)
            val file = File(context.cacheDir, sanitizedName)

            // Verify the resolved path is inside cache directory
            val cachePath = context.cacheDir.canonicalPath
            val safeCachePath = if (cachePath.endsWith(File.separator)) cachePath else cachePath + File.separator
            val filePath = file.canonicalPath

            // 2026 Security Fix: Use canonicalPath with trailing separator to prevent partial path traversal
            // e.g. "/cache_extra" starting with "/cache"
            if (!filePath.startsWith(safeCachePath)) {
                return@withContext Result.failure(SecurityException("Invalid file path: path traversal detected"))
            }

            file.writeText(content)
            Result.success(file.absolutePath)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }

    /**
     * Sanitizes a filename to prevent path traversal attacks.
     * Throws SecurityException if a traversal attempt is detected.
     */
    private fun sanitizeFileName(fileName: String): String {
        return try {
            sanitizeExportFileName(fileName)
        } catch (e: IllegalArgumentException) {
            throw SecurityException(e.message ?: "Path traversal attempt detected")
        }
    }
}
