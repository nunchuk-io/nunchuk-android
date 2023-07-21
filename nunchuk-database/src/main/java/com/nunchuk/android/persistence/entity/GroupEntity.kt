package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.persistence.TABLE_ASSISTED_WALLET
import com.nunchuk.android.persistence.TABLE_GROUP

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
)