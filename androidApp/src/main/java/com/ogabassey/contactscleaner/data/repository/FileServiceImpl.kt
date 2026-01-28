package com.ogabassey.contactscleaner.data.repository

import android.content.Context
import com.ogabassey.contactscleaner.domain.repository.FileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 2026 AGP 9.0: Migrated from Hilt to Koin.
 */
class FileServiceImpl(
    private val context: Context
) : FileService {

    override suspend fun generateCsvFile(fileName: String, content: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, fileName)
            file.writeText(content)
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
