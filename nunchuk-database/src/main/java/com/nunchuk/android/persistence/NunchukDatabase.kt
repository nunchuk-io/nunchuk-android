package com.nunchuk.android.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nunchuk.android.persistence.dao.ContactDao
import com.nunchuk.android.persistence.dao.SyncEventDao
import com.nunchuk.android.persistence.dao.SyncFileDao
import com.nunchuk.android.persistence.entity.ContactEntity
import com.nunchuk.android.persistence.entity.SyncEventEntity
import com.nunchuk.android.persistence.entity.SyncFileEntity

@Database(
    entities = [
        ContactEntity::class,
        SyncFileEntity::class,
        SyncEventEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = true
)

internal abstract class NunchukDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun syncFileDao(): SyncFileDao
    abstract fun syncEventDao(): SyncEventDao

    companion object {

        @Volatile
        private var INSTANCE: NunchukDatabase? = null

        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

        private fun buildDatabase(applicationContext: Context) =
            Room.databaseBuilder(applicationContext, NunchukDatabase::class.java, DATABASE_NAME)
                .addMigrations(DBMigrations.MIGRATION_1_2)
                .addMigrations(DBMigrations.MIGRATION_2_3)
                .build()
    }
}

