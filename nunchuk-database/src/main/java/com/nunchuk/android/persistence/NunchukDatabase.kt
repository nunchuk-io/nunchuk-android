package com.nunchuk.android.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nunchuk.android.persistence.dao.ContactDao
import com.nunchuk.android.persistence.entity.ContactEntity

@Database(
    entities = [
        ContactEntity::class
    ],
    version = DATABASE_VERSION,
    exportSchema = true
)

internal abstract class NunchukDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    companion object {

        @Volatile
        private var INSTANCE: NunchukDatabase? = null

        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

        private fun buildDatabase(applicationContext: Context): NunchukDatabase {
            return Room.databaseBuilder(applicationContext, NunchukDatabase::class.java, DATABASE_NAME)
                .build()
        }
    }
}

