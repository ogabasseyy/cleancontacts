package com.ogabassey.contactscleaner.domain.usecase

import com.ogabassey.contactscleaner.data.detector.DuplicateDetector
import com.ogabassey.contactscleaner.data.detector.JunkDetector
import com.ogabassey.contactscleaner.data.parser.ContactImportParser
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ImportResult


class ImportContactsUseCase(
    private val contactImportParser: ContactImportParser,
    private val junkDetector: JunkDetector,
    private val duplicateDetector: DuplicateDetector
) {
    suspend operator fun invoke(content: String, filename: String): ImportResult {
        // Parse the file
        val parseResult = contactImportParser.parseFile(content, filename)
        
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
