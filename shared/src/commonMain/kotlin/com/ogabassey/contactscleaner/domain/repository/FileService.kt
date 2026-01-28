package com.ogabassey.contactscleaner.domain.repository

interface FileService {
    suspend fun generateCsvFile(fileName: String, content: String): Result<String>
}
