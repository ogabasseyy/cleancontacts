package com.ogabassey.contactscleaner.domain.usecase

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import com.ogabassey.contactscleaner.domain.repository.FileService
import com.ogabassey.contactscleaner.util.getPlatformTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
class ExportUseCase(
    private val contactRepository: ContactRepository,
    private val fileService: FileService
) {

    suspend operator fun invoke(types: Set<ContactType>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val contactsToExport = mutableSetOf<Contact>()
            
            if (types.contains(ContactType.ALL)) {
                contactsToExport.addAll(contactRepository.getContactsAllSnapshot())
            } else {
                types.forEach { type ->
                    contactsToExport.addAll(contactRepository.getContactsSnapshotByType(type))
                }
            }
            
            if (contactsToExport.isEmpty()) {
                return@withContext Result.failure(Exception("No contacts found to export"))
            }

            val fileName = "contacts_export_${getPlatformTimeMillis()}.csv"
            generateAndSaveCsv(contactsToExport.sortedBy { it.name }, fileName)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportContactList(contacts: List<Contact>, groupName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sanitizedName = groupName.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
            val fileName = "contacts_${sanitizedName}_${getPlatformTimeMillis()}.csv"
            generateAndSaveCsv(contacts, fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun generateAndSaveCsv(contacts: List<Contact>, fileName: String): Result<String> {
        val csvHeader = "Name,Phone Number,Normalized Number,Email,WhatsApp,Telegram,Account\n"
        val stringBuilder = StringBuilder(csvHeader)

        contacts.forEach { contact ->
            val name = escapeCsv(contact.name ?: "")
            val phone = escapeCsv(contact.numbers.firstOrNull() ?: "")
            val normPhone = escapeCsv(contact.normalizedNumber ?: "")
            val email = escapeCsv(contact.emails.firstOrNull() ?: "")
            val wa = if (contact.isWhatsApp) "Yes" else "No"
            val tg = if (contact.isTelegram) "Yes" else "No"
            val account = escapeCsv("${contact.accountType ?: ""} ${contact.accountName ?: ""}")

            stringBuilder.append("$name,$phone,$normPhone,$email,$wa,$tg,$account\n")
        }

        return fileService.generateCsvFile(fileName, stringBuilder.toString())
    }

    private fun escapeCsv(value: String): String {
        var result = value.replace("\"", "\"\"")
        if (result.contains(",") || result.contains("\n") || result.contains("\"")) {
            result = "\"$result\""
        }
        return result
    }
}
