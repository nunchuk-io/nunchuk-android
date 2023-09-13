package com.nunchuk.android.core.repository

import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.model.DummyTransaction
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.byzantine.toDummyTransactionType
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.DummyTransactionRepository
import com.nunchuk.android.type.TransactionStatus
import javax.inject.Inject

internal class DummyTransactionRepositoryImpl @Inject constructor(
    private val userWalletApiManager: UserWalletApiManager,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : DummyTransactionRepository {
    override suspend fun getDummyTransaction(
        groupId: String,
        walletId: String,
        dummyTransactionId: String
    ): DummyTransaction {
        val response = userWalletApiManager.groupWalletApi.getDummyTransaction(
            groupId,
            walletId,
            dummyTransactionId
        )
        val dummyTransaction = response.data.dummyTransaction ?: throw NullPointerException("Dummy transaction null")
        val requestBody = dummyTransaction.requestBody.orEmpty()
        return DummyTransaction(
            psbt = nunchukNativeSdk.getHealthCheckDummyTxMessage(walletId, requestBody),
            pendingSignature = dummyTransaction.pendingSignatures,
            dummyTransactionType = dummyTransaction.type.toDummyTransactionType,
            payload = dummyTransaction.payload?.toString().orEmpty(),
            isDraft = dummyTransaction.isDraft,
        )
    }

    override suspend fun getDummyTransactionPayload(
        groupId: String,
        walletId: String,
        dummyTransactionId: String
    ): DummyTransactionPayload {
        val response = userWalletApiManager.groupWalletApi.getDummyTransaction(
            groupId,
            walletId,
            dummyTransactionId
        )

        val dummyTransaction = response.data.dummyTransaction ?: throw NullPointerException("dummyTransaction null")
        return DummyTransactionPayload(
            payload = dummyTransaction.payload?.toString().orEmpty(),
            walletId = dummyTransaction.walletLocalId.orEmpty(),
            type = dummyTransaction.type.toDummyTransactionType,
            requiredSignatures = dummyTransaction.requiredSignatures,
            pendingSignatures = dummyTransaction.pendingSignatures,
            requestByUserId = dummyTransaction.requesterUserId.orEmpty(),
            dummyTransactionId = dummyTransaction.id.orEmpty(),
        )
    }

    override suspend fun updateDummyTransaction(
        signatures: Map<String, String>,
        groupId: String,
        walletId: String,
        dummyTransactionId: String
    ) : TransactionStatus {
        val headers = mutableMapOf<String, String>()
        signatures.map { (masterFingerprint, signature) ->
            nunchukNativeSdk.createRequestToken(signature, masterFingerprint)
        }.forEachIndexed { index, signerToken ->
            headers["${PremiumWalletRepositoryImpl.AUTHORIZATION_X}-${index + 1}"] = signerToken
        }
        val response = userWalletApiManager.groupWalletApi.updateDummyTransaction(
            headers,
            groupId,
            walletId,
            dummyTransactionId
        )
        return TransactionStatus.values().find { it.name == response.data.dummyTransaction?.status } ?: TransactionStatus.PENDING_SIGNATURES
    }

    override suspend fun deleteDummyTransaction(
        groupId: String,
        walletId: String,
        dummyTransactionId: String
    ) {
        val response = userWalletApiManager.groupWalletApi.deleteDummyTransaction(
            groupId,
            walletId,
            dummyTransactionId
        )
        if (response.isSuccess.not()) {
            throw response.error
        }
    }
}