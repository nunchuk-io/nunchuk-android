package com.nunchuk.android.persistence

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DBMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {

        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `sync_file` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` TEXT NOT NULL, `action` TEXT NOT NULL, `file_name` TEXT, `file_url` TEXT, `file_json_info` TEXT NOT NULL, `file_mine_type` TEXT, `file_length` INTEGER, `file_data` BLOB)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {

        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `sync_event` (`event_id` TEXT NOT NULL, PRIMARY KEY(`event_id`))")
        }
    }

}
