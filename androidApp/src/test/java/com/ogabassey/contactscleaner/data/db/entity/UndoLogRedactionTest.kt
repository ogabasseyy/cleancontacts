package com.ogabassey.contactscleaner.data.db.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UndoLogRedactionTest {

    @Test
    fun `UndoLog toString should redact originalDataJson`() {
        val undoLog = UndoLog(
            id = 1,
            timestamp = 1234567890L,
            actionType = "DELETE",
            originalDataJson = """[{"name":"John Doe","numbers":["+1234567890"]}]""",
            description = "Deleted 1 contact"
        )

        val stringRep = undoLog.toString()

        // Verify sensitive data is NOT present
        assertFalse("Should not contain originalDataJson content", stringRep.contains("John Doe"))
        assertFalse("Should not contain originalDataJson content", stringRep.contains("+1234567890"))

        // Verify redaction marker is present
        // Since originalDataJson is a String, we might replace it with ***REDACTED***
        // or ensure it's not printed entirely.
        // We expect it to be redacted.
        // If toString is default, it will contain the JSON.
        // So this test should fail initially.
    }
}
