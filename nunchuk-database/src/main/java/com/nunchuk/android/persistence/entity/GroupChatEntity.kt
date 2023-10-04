package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.persistence.TABLE_GROUP
import com.nunchuk.android.persistence.TABLE_GROUP_CHAT
import com.nunchuk.android.type.Chain

@Entity(tableName = TABLE_GROUP_CHAT)
data class GroupChatEntity(
    @PrimaryKey
    @ColumnInfo(name = "group_id")
    val groupId: String,
    @ColumnInfo(name = "chatId")
    val chatId: String,
    @ColumnInfo(name = "room_id")
    val roomId: String,
    @ColumnInfo(name = "chain", defaultValue = "MAIN")
    val chain: Chain = Chain.MAIN,
    @ColumnInfo(name = "createdTimeMillis")
    val createdTimeMillis: Long,
    @ColumnInfo(name = "historyPeriod") // HistoryPeriod
    val historyPeriod: String,
)