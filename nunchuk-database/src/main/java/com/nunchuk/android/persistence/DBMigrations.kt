/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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
            database.execSQL("ALTER TABLE `contact` ADD COLUMN `login_type` TEXT")
            database.execSQL("ALTER TABLE `contact` ADD COLUMN `username` TEXT")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {

        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `membership_flow` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `email` TEXT NOT NULL, `step` INTEGER NOT NULL, `master_signer_id` TEXT NOT NULL, `key_id_in_server` TEXT NOT NULL, `key_id_check_sum` TEXT NOT NULL, `is_verify` INTEGER NOT NULL)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {

        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `membership_flow` ADD COLUMN `plan` INT DEFAULT 0")
        }
    }
}
