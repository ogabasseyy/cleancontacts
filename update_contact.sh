#!/bin/bash
awk '
/^data class AccountGroupSummary\(/ {
    print
    print "    val accountType: String?,"
    print "    val accountName: String?,"
    print "    val count: Int"
    print ") {"
    print "    // 2026 Security Fix: Override toString to prevent accidental logging of PII (CWE-532)"
    print "    override fun toString(): String {"
    print "        return \"AccountGroupSummary(accountType=$accountType, count=$count, accountName=***REDACTED***)\""
    print "    }"
    print "}"

    # skip the next 4 lines
    getline
    getline
    getline
    getline
    next
}
{ print }
' shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/domain/model/Contact.kt > tmp.kt && mv tmp.kt shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/domain/model/Contact.kt
