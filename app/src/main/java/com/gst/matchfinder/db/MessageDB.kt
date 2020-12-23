package com.gst.matchfinder.db

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = arrayOf(MessageEntry::class), version = 2)
abstract class MessageDB : RoomDatabase(){
    abstract fun messageDAO(): MessageDAO

    companion object{
        private var instance: MessageDB? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE MessageEntry ADD COLUMN msg_time TEXT default null")
                //database.execSQL("ALTER TABLE MessageEntry RENAME COLUMN \'msg_id\' TO \'message_id\'")
            }
        }

        @Synchronized
        fun getInstance(context: Context): MessageDB?{
            if(instance == null){
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    MessageDB::class.java,
                    "database-contacts"
                ).addMigrations(MIGRATION_1_2)
                 .allowMainThreadQueries()
                 .build()
            }

            return instance
        }

        //@VisibleForTesting
        //val MIGRATION_1_TO_2 = Migration1To2()
    }
}


















