package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "taproot_transaction",
)
data class TaprootTransactionEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "transaction_id")
    val transactionId: String,
    @ColumnInfo(name = "key_set_index")
    val keySetIndex: Int
) 