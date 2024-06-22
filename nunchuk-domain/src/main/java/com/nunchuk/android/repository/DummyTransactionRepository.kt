package com.nunchuk.android.repository

import com.nunchuk.android.model.DummyTransaction
import com.nunchuk.android.model.SignInDummyTransaction
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.byzantine.DummyTransactionUpdate
import com.nunchuk.android.model.byzantine.SignInDummyTransactionUpdate

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
    ): DummyTransactionUpdate

    suspend fun updateDummyTransactionSignIn(
        signatures: Map<String, String>,
        dummyTransactionId: String
    ): SignInDummyTransactionUpdate

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

    suspend fun getSignInDummyTransaction(
        data: String,
    ): SignInDummyTransaction
}