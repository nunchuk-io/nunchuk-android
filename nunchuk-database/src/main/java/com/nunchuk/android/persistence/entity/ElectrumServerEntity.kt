package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.persistence.TABLE_ELECTRUM_SERVER
import com.nunchuk.android.type.Chain

@Entity(tableName = TABLE_ELECTRUM_SERVER)
data class ElectrumServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Long,
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "chain")
    val chain: Chain
)