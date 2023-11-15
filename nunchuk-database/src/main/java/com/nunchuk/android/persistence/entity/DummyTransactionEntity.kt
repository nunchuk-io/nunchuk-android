package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.persistence.TABLE_DUMMY_TRANSACTION

@Entity(tableName = TABLE_DUMMY_TRANSACTION)
data class DummyTransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "wallet_id")
    val walletId: String,

    @ColumnInfo(name = "pending_signature")
    val pendingSignature: Int,

    @ColumnInfo(name = "required_signature")
    val requiredSignature: Int,

    @ColumnInfo(name = "dummy_transaction_type")
    val dummyTransactionType: DummyTransactionType,

    @ColumnInfo(name = "payload")
    val payload: String,

    @ColumnInfo(name = "requester_user_id")
    val requesterUserId: String,
)