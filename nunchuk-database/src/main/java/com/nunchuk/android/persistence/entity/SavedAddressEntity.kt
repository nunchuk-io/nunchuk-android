package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.persistence.TABLE_SAVED_ADDRESS
import com.nunchuk.android.type.Chain

@Entity(tableName = TABLE_SAVED_ADDRESS)
class SavedAddressEntity(
    @PrimaryKey
    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "chat_id")
    val chatId: String,

    @ColumnInfo(name = "chain", defaultValue = "MAIN")
    val chain: Chain = Chain.MAIN,
)