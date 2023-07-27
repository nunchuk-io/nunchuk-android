package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.persistence.TABLE_GROUP
import com.nunchuk.android.type.Chain

@Entity(tableName = TABLE_GROUP)
data class GroupEntity(
    @PrimaryKey
    @ColumnInfo(name = "group_id")
    val groupId: String,
    @ColumnInfo(name = "chatId")
    val chatId: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "createdTimeMillis")
    val createdTimeMillis: Long,
    @ColumnInfo(name = "members") // ByzantineMemberBrief
    val members: String,
    @ColumnInfo(name = "is_view_pending_wallet", defaultValue = "false")
    val isViewPendingWallet: Boolean = false,
    @ColumnInfo(name = "chain")
    val chain: Chain = Chain.MAIN
)