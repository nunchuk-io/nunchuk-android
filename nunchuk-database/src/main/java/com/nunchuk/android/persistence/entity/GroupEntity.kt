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
    @ColumnInfo(name = "members") // ByzantineMember
    val members: String,
    @ColumnInfo(name = "is_view_pending_wallet", defaultValue = "false")
    val isViewPendingWallet: Boolean = false,
    @ColumnInfo(name = "chain", defaultValue = "MAIN")
    val chain: Chain = Chain.MAIN,
    @ColumnInfo(name = "walletConfig", defaultValue = "") // ByzantineWalletConfig
    val walletConfig: String,
    @ColumnInfo("setup_preference", defaultValue = "")
    val setupPreference: String,
    @ColumnInfo("is_locked", defaultValue = "false")
    val isLocked: Boolean,
    @ColumnInfo("slug", defaultValue = "")
    val slug: String,
)