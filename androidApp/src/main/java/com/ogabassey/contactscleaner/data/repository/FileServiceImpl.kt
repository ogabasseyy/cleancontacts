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
            val cacheCanonical = context.cacheDir.canonicalPath
            val fileCanonical = file.canonicalPath
            if (!fileCanonical.startsWith(cacheCanonical)) {
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
     * Removes path separators and parent directory references.
     */
    private fun sanitizeFileName(fileName: String): String {
        // Remove any path separators and normalize
        return fileName
            .replace(Regex("[/\\\\]"), "_") // Replace path separators
            .replace("..", "_")              // Remove parent directory traversal
            .replace(Regex("^[._]+"), "")    // Remove leading dots/underscores
            .ifEmpty { "export.csv" }        // Fallback for empty names
    }
}
