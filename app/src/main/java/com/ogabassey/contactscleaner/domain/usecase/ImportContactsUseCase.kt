package com.ogabassey.contactscleaner.domain.usecase

import com.ogabassey.contactscleaner.data.detector.DuplicateDetector
import com.ogabassey.contactscleaner.data.detector.JunkDetector
import com.ogabassey.contactscleaner.data.parser.ContactImportParser
import com.ogabassey.contactscleaner.domain.model.ImportResult
import java.io.InputStream
import javax.inject.Inject

class ImportContactsUseCase @Inject constructor(
    private val contactImportParser: ContactImportParser,
    private val junkDetector: JunkDetector,
    private val duplicateDetector: DuplicateDetector
) {
    suspend operator fun invoke(inputStream: InputStream, filename: String): ImportResult {
        // Parse the file
        val parseResult = contactImportParser.parseFile(inputStream, filename)
        
        // Detect junk in imported contacts
        val junkContacts = junkDetector.detectJunk(parseResult.validContacts)
        
        // Detect duplicates
        val duplicates = duplicateDetector.detectDuplicates(parseResult.validContacts)
        
        return ImportResult(
            validContacts = parseResult.validContacts.filterNot { contact ->
                junkContacts.any { it.id == contact.id }
            },
            junkContacts = junkContacts,
            duplicates = duplicates
        )
    }
}
