package com.ogabassey.contactscleaner.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE whatsapp_cache ADD COLUMN hasWhatsApp INTEGER NOT NULL DEFAULT 1"
        )
    }
}
