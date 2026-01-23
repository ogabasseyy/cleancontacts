package com.ogabassey.contactscleaner.domain.repository

import android.net.Uri

interface FileService {
    suspend fun generateCsvFile(fileName: String, content: String): Result<Uri>
}
