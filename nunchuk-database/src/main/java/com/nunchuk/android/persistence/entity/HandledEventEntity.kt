package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.persistence.TABLE_HANDLED_EVENT

@Entity(tableName = TABLE_HANDLED_EVENT)
data class HandledEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String
)