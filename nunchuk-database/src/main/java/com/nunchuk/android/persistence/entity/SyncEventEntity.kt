package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.persistence.TABLE_SYNC_EVENT

@Entity(tableName = TABLE_SYNC_EVENT)
data class SyncEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String
)