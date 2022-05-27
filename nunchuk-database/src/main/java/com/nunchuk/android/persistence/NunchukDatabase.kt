package com.nunchuk.android.persistence

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nunchuk.android.persistence.dao.ContactDao
import com.nunchuk.android.persistence.dao.SyncFileDao
import com.nunchuk.android.persistence.entity.ContactEntity
import com.nunchuk.android.persistence.entity.SyncFileEntity

@Database(
    entities = [
        ContactEntity::class,
        SyncFileEntity::class
    ],
    version = DATABASE_VERSION,
    exportSchema = true
)

internal abstract class NunchukDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun syncFileDao(): SyncFileDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `sync_file` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` TEXT NOT NULL, `action` TEXT NOT NULL, `file_name` TEXT, `file_url` TEXT, `file_json_info` TEXT NOT NULL, `file_mine_type` TEXT, `file_length` INTEGER, `file_data` BLOB)")
            }
        }

        @Volatile
        private var INSTANCE: NunchukDatabase? = null

        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

        private fun buildDatabase(applicationContext: Context): NunchukDatabase {
            return Room.databaseBuilder(applicationContext, NunchukDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}

