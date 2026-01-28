package com.ogabassey.contactscleaner.data.repository

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class FileServiceImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Mock
    lateinit var context: Context

    private lateinit var fileService: FileServiceImpl

    @Before
    fun setUp() {
        // Mock cacheDir to use the temporary folder
        `when`(context.cacheDir).thenReturn(tempFolder.root)
        fileService = FileServiceImpl(context)
    }

    @Test
    fun `generateCsvFile creates file securely`() = runBlocking {
        val fileName = "test_contacts.csv"
        val content = "Name,Number\nJohn,123"

        val result = fileService.generateCsvFile(fileName, content)

        assertTrue("Result should be success", result.isSuccess)
        val path = result.getOrNull()
        assertTrue("Path should not be null", path != null)
        assertTrue("File should exist", File(path!!).exists())
        assertTrue("Content should match", File(path).readText() == content)
    }

    @Test
    fun `generateCsvFile sanitizes path traversal attempts`() = runBlocking {
        // Attempt a path traversal
        // The sanitizeFileName function should neutralize this, and the file should be written safely
        val fileName = "../../../evil.csv"
        val content = "evil content"

        val result = fileService.generateCsvFile(fileName, content)

        assertTrue("Result should be success (sanitized)", result.isSuccess)
        val path = result.getOrNull()
        assertTrue("Path should not be null", path != null)

        // Ensure it is INSIDE the cache dir (tempFolder)
        val file = File(path!!)
        // Using Path.startsWith logic from the implementation implicitly via test
        val cachePath = tempFolder.root.toPath().toAbsolutePath().normalize()
        val filePath = file.toPath().toAbsolutePath().normalize()

        assertTrue("File should be inside cache dir", filePath.startsWith(cachePath))
    }
}
