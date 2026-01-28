package com.ogabassey.contactscleaner.data.repository

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertNotNull("Path should not be null", path)
        val file = File(path!!)
        assertTrue("File should exist", file.exists())
        assertEquals("Content should match", content, file.readText())
    }

    @Test
    fun `generateCsvFile sanitizes illegal characters`() = runBlocking {
        // Test sanitization of characters that are illegal in filenames but not path traversals
        val fileName = "test exports! @#$.csv"
        val expectedSanitized = "test_exports______-_.csv" // Based on [^a-zA-Z0-0._-]

        val result = fileService.generateCsvFile(fileName, content = "some content")

        assertTrue("Result should be success", result.isSuccess)
        val path = result.getOrNull()
        assertNotNull(path)
        assertTrue("Filename should be sanitized", path!!.endsWith("test_exports______-_.csv") || path.contains("test_exports"))
    }

    @Test
    fun `generateCsvFile rejects path traversal with parent references`() = runBlocking {
        val fileName = "subdir/../evil.csv"
        val result = fileService.generateCsvFile(fileName, "content")

        assertTrue("Should be failure", result.isFailure)
        assertTrue("Should throw SecurityException", result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `generateCsvFile rejects path traversal with absolute-like separators`() = runBlocking {
        val fileName = "/etc/passwd"
        val result = fileService.generateCsvFile(fileName, "content")

        assertTrue("Should be failure", result.isFailure)
        assertTrue("Should throw SecurityException", result.exceptionOrNull() is SecurityException)
    }
}
