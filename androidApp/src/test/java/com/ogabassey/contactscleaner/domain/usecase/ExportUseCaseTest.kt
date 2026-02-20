package com.ogabassey.contactscleaner.domain.usecase

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import com.ogabassey.contactscleaner.domain.repository.FileService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ExportUseCaseTest {

    private val contactRepository: ContactRepository = mock()
    private val fileService: FileService = mock()
    private val exportUseCase = ExportUseCase(contactRepository, fileService)

    @Test
    fun `export contacts with malicious name should escape CSV injection`() = runBlocking {
        // Arrange
        val maliciousName = "=cmd|' /C calc'!A0"
        val contact = Contact(
            id = 1,
            name = maliciousName,
            numbers = listOf("1234567890"),
            normalizedNumber = "1234567890"
        )

        whenever(contactRepository.getContactsAllSnapshot()).thenReturn(listOf(contact))
        whenever(fileService.generateCsvFile(any(), any())).thenReturn(Result.success("path/to/file.csv"))

        // Act
        exportUseCase(setOf(ContactType.ALL))

        // Assert
        val contentCaptor = argumentCaptor<String>()
        verify(fileService).generateCsvFile(any(), contentCaptor.capture())

        val csvContent = contentCaptor.firstValue

        // This assertion expects the secure behavior (escaped with single quote)
        // It should fail if the code is vulnerable (which it is)
        // Vulnerable output: "=cmd|' /C calc'!A0"
        // Secure output: "'=cmd|' /C calc'!A0"

        assertTrue(
            "CSV content should contain escaped malicious name, but was: $csvContent",
            csvContent.contains("'$maliciousName")
        )
    }
}
