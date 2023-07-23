package com.nunchuk.android.repository

import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.byzantine.DummyTransactionPayload

interface DummyTransactionRepository {
    suspend fun getDummyTransaction(
        groupId: String,
        walletId: String,
        dummyTransactionId: String
    ): Transaction

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
    )

    suspend fun deleteDummyTransaction(
        groupId: String,
        walletId: String,
        dummyTransactionId: String
    )
}