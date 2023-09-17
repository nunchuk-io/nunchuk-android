package com.nunchuk.android.repository

import com.nunchuk.android.model.DummyTransaction
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.type.TransactionStatus

interface DummyTransactionRepository {
    suspend fun getDummyTransaction(
        groupId: String,
        walletId: String,
        dummyTransactionId: String
    ): DummyTransaction

    suspend fun getDummyTransactionPayload(
        groupId: String,
        walletId: String,
        dummyTransactionId: String
    ): DummyTransactionPayload

    suspend fun updateDummyTransaction(
        signatures: Map<String, String>,
        groupId: String,
        walletId: String,
        dummyTransactionId: String
    ): TransactionStatus

    suspend fun deleteDummyTransaction(
        groupId: String,
        walletId: String,
        dummyTransactionId: String
    )

    suspend fun finalizeDummyTransaction(
        groupId: String,
        walletId: String,
        dummyTransactionId: String,
    )

    suspend fun getDummyTxRequestToken(
        walletId: String,
        dummyTransactionId: String,
    ): Map<String, Boolean>
}