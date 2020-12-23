package com.gst.matchfinder.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration1To2: Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE MessageEntry ADD message_blocked STRING NOT NULL Default 'n'")
    }
}