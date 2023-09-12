package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.persistence.TABLE_GROUP
import com.nunchuk.android.persistence.TABLE_KEY_HEALTH_STATUS
import com.nunchuk.android.type.Chain

@Entity(tableName = TABLE_KEY_HEALTH_STATUS)
data class KeyHealthStatusEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "xfp")
    val xfp: String,
    @ColumnInfo(name = "can_request_health_check")
    val canRequestHealthCheck: Boolean,
    @ColumnInfo(name = "last_health_check_time_millis")
    val lastHealthCheckTimeMillis: Long,
    @ColumnInfo(name = "chain", defaultValue = "MAIN")
    val chain: Chain = Chain.MAIN,
    @ColumnInfo(name = "chat_id")
    val chatId: String = "",
    @ColumnInfo(name = "group_id")
    val groupId: String = "",
    @ColumnInfo(name = "wallet_id")
    val walletId: String = "",
)