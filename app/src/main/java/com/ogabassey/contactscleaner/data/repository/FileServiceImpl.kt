package com.ogabassey.contactscleaner.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.ogabassey.contactscleaner.domain.repository.FileService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class FileServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileService {

    override suspend fun generateCsvFile(fileName: String, content: String): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, fileName)
            file.writeText(content)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
