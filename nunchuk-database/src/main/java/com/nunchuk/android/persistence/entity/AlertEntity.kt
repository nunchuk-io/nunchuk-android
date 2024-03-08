package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.persistence.TABLE_ALERT
import com.nunchuk.android.type.Chain

@Entity(tableName = TABLE_ALERT)
data class AlertEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "viewable")
    val viewable: Boolean = false,

    @ColumnInfo(name = "body")
    val body: String,

    @ColumnInfo(name = "payload")
    val payload: String,

    @ColumnInfo(name = "created_time_millis")
    val createdTimeMillis: Long = 0,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "chat_id")
    val chatId: String = "",

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "group_id")
    val groupId: String = "",

    @ColumnInfo(name = "wallet_id", defaultValue = "")
    val walletId: String = "",

    @ColumnInfo(name = "chain", defaultValue = "MAIN")
    val chain: Chain = Chain.MAIN,
)