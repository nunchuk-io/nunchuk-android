package com.nunchuk.android.core.repository

import com.google.gson.Gson
import com.nunchuk.android.core.data.model.byzantine.DummyTransactionDto
import com.nunchuk.android.core.data.model.membership.SigninDummyRequest
import com.nunchuk.android.core.data.model.membership.toModel
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.core.mapper.toModel
import com.nunchuk.android.model.DummyTransaction
import com.nunchuk.android.model.SignInDummyTransaction
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.byzantine.DummyTransactionUpdate
import com.nunchuk.android.model.byzantine.SignInDummyTransactionUpdate
import com.nunchuk.android.model.byzantine.toDummyTransactionType
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.persistence.dao.DummyTransactionDao
import com.nunchuk.android.persistence.entity.DummyTransactionEntity
import com.nunchuk.android.repository.DummyTransactionRepository
import com.nunchuk.android.type.TransactionStatus
import javax.inject.Inject

internal class DummyTransactionRepositoryImpl @Inject constructor(
    private val userWalletApiManager: UserWalletApiManager,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val gson: Gson,
    private val dummyTransactionDao: DummyTransactionDao,
) : DummyTransactionRepository {
    override suspend fun getDummyTransaction(
        groupId: String,
        walletId: String,
        dummyTransactionId: String,
    ): DummyTransaction {
        return runCatching {
            val response = if (groupId.isEmpty()) {
                userWalletApiManager.walletApi.getDummyTransaction(
                    walletId,
                    dummyTransactionId
                )
            } else {
                userWalletApiManager.groupWalletApi.getDummyTransaction(
                    groupId,
                    walletId,
                    dummyTransactionId
                )
            }
            val dummyTransaction = response.data.dummyTransaction
                ?: throw NullPointerException("Can not load dummy transaction")
            nunchukNativeSdk.importDummyTx(gson.toJson(dummyTransaction))
            saveDummyTransactionEntity(dummyTransactionId, dummyTransaction)
            val requestBody = dummyTransaction.requestBody.orEmpty()
            return DummyTransaction(
                psbt = nunchukNativeSdk.getHealthCheckDummyTxMessage(walletId, requestBody),
                pendingSignature = dummyTransaction.pendingSignatures,
                dummyTransactionType = dummyTransaction.type.toDummyTransactionType,
                payload = dummyTransaction.payload?.toString().orEmpty(),
                isDraft = dummyTransaction.isDraft,
            )
        }.getOrElse {
            val entity = dummyTransactionDao.getById(dummyTransactionId)
            DummyTransaction(
                psbt = nunchukNativeSdk.getHealthCheckDummyTxMessage(walletId, entity.payload),
                pendingSignature = entity.pendingSignature,
                dummyTransactionType = entity.dummyTransactionType,
                payload = entity.payload,
                isDraft = false,
            )
        }
    }

    override suspend fun getDummyTransactionPayload(
        groupId: String,
        walletId: String,
        dummyTransactionId: String,
    ): DummyTransactionPayload {
        return runCatching {
            val response = if (groupId.isEmpty()) {
                userWalletApiManager.walletApi.getDummyTransaction(
                    walletId,
                    dummyTransactionId
                )
            } else {
                userWalletApiManager.groupWalletApi.getDummyTransaction(
                    groupId,
                    walletId,
                    dummyTransactionId
                )
            }
            val dummyTransaction = response.data.dummyTransaction
                ?: throw NullPointerException("Can not load dummy transaction")
            saveDummyTransactionEntity(dummyTransactionId, dummyTransaction)
            nunchukNativeSdk.importDummyTx(gson.toJson(dummyTransaction))
            DummyTransactionPayload(
                payload = dummyTransaction.payload?.toString().orEmpty(),
                walletId = dummyTransaction.walletLocalId.orEmpty(),
                type = dummyTransaction.type.toDummyTransactionType,
                requiredSignatures = dummyTransaction.requiredSignatures,
                pendingSignatures = dummyTransaction.pendingSignatures,
                requestByUserId = dummyTransaction.requesterUserId.orEmpty(),
                dummyTransactionId = dummyTransaction.id.orEmpty(),
            )
        }.getOrElse {
            val entity = dummyTransactionDao.getById(dummyTransactionId)
            DummyTransactionPayload(
                payload = entity.payload,
                walletId = entity.walletId,
                type = entity.dummyTransactionType,
                requiredSignatures = entity.requiredSignature,
                pendingSignatures = entity.pendingSignature,
                requestByUserId = entity.requesterUserId,
                dummyTransactionId = entity.id,
            )
        }
    }

    override suspend fun updateDummyTransaction(
        signatures: Map<String, String>,
        groupId: String,
        walletId: String,
        dummyTransactionId: String,
    ): DummyTransactionUpdate {
        val headers = mutableMapOf<String, String>()
        signatures.map { (masterFingerprint, signature) ->
            nunchukNativeSdk.createRequestToken(signature, masterFingerprint)
        }.forEachIndexed { index, signerToken ->
            headers["${PremiumWalletRepositoryImpl.AUTHORIZATION_X}-${index + 1}"] = signerToken
        }
        headers.values.forEach { signatureToken ->
            nunchukNativeSdk.saveDummyTxRequestToken(walletId, dummyTransactionId, signatureToken)
        }
        val response = if (groupId.isEmpty()) {
            userWalletApiManager.walletApi.updateDummyTransaction(
                headers,
                walletId,
                dummyTransactionId
            )
        } else {
            userWalletApiManager.groupWalletApi.updateDummyTransaction(
                headers,
                groupId,
                walletId,
                dummyTransactionId
            )
        }
        return DummyTransactionUpdate(
            TransactionStatus.values().find { it.name == response.data.dummyTransaction?.status }
                ?: TransactionStatus.PENDING_SIGNATURES,
            response.data.dummyTransaction?.pendingSignatures ?: 0,
        )
    }

    override suspend fun updateDummyTransactionSignIn(
        signatures: Map<String, String>,
        dummyTransactionId: String
    ): SignInDummyTransactionUpdate {
        val headers = mutableMapOf<String, String>()
        signatures.map { (masterFingerprint, signature) ->
            nunchukNativeSdk.createRequestToken(signature, masterFingerprint)
        }.forEachIndexed { index, signerToken ->
            headers["${PremiumWalletRepositoryImpl.AUTHORIZATION_X}-${index + 1}"] = signerToken
        }
        val response = userWalletApiManager.walletApi.updateDummyTransactionSignIn(
            headers,
            dummyTransactionId
        )
        return SignInDummyTransactionUpdate(
            status = TransactionStatus.entries.find { it.name == response.data.dummyTransaction?.status }
                ?: TransactionStatus.PENDING_SIGNATURES,
            pendingSignatures = response.data.dummyTransaction?.pendingSignatures ?: 0,
            tokenId =  response.data.token?.tokenId.orEmpty(),
            userId = response.data.token?.userId.orEmpty(),
            deviceId = response.data.token?.deviceId.orEmpty(),
        )
    }

    override suspend fun deleteDummyTransaction(
        groupId: String,
        walletId: String,
        dummyTransactionId: String,
    ) {
        val response = if (groupId.isEmpty()) {
            userWalletApiManager.walletApi.deleteDummyTransaction(
                walletId,
                dummyTransactionId
            )
        } else {
            userWalletApiManager.groupWalletApi.deleteDummyTransaction(
                groupId,
                walletId,
                dummyTransactionId
            )
        }
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun finalizeDummyTransaction(
        groupId: String,
        walletId: String,
        dummyTransactionId: String,
    ) {
        val response = if (groupId.isEmpty()) {
            userWalletApiManager.walletApi.finalizeDummyTransaction(
                walletId, dummyTransactionId
            )
        } else {
            userWalletApiManager.groupWalletApi.finalizeDummyTransaction(
                groupId, walletId, dummyTransactionId
            )
        }
        val transaction = response.data.dummyTransaction
            ?: throw NullPointerException("Can not get dummy transaction")
        nunchukNativeSdk.importDummyTx(gson.toJson(transaction))
        saveDummyTransactionEntity(dummyTransactionId, transaction)
    }

    override suspend fun getDummyTxRequestToken(
        walletId: String,
        dummyTransactionId: String,
    ): Map<String, Boolean> {
        return nunchukNativeSdk.getDummyTxRequestToken(walletId, dummyTransactionId)
    }

    override suspend fun getSignInDummyTransaction(data: String): SignInDummyTransaction {
        val response =
            userWalletApiManager.walletApi.signinDummy(payload = SigninDummyRequest(data))
        val dummyTransaction = response.data.dummyTransaction
            ?: throw NullPointerException("Can not load dummy transaction")
        return SignInDummyTransaction(
            psbt = dummyTransaction.psbt.orEmpty(),
            pendingSignature = dummyTransaction.pendingSignatures,
            requiredSignatures = dummyTransaction.requiredSignatures,
            dummyTransactionId = dummyTransaction.id.orEmpty(),
            signerServers = response.data.walletDto?.signerServerDtos?.map { it.toModel() } ?: emptyList(),
            signatures = dummyTransaction.signatures.map { it.toModel() }
        )
    }

    private suspend fun saveDummyTransactionEntity(
        dummyTransactionId: String,
        dummyTransaction: DummyTransactionDto,
    ) {
        val entity = DummyTransactionEntity(
            id = dummyTransactionId,
            walletId = dummyTransaction.walletLocalId.orEmpty(),
            pendingSignature = dummyTransaction.pendingSignatures,
            requiredSignature = dummyTransaction.requiredSignatures,
            dummyTransactionType = dummyTransaction.type.toDummyTransactionType,
            payload = dummyTransaction.payload?.toString().orEmpty(),
            requesterUserId = dummyTransaction.requesterUserId.orEmpty(),
        )
        dummyTransactionDao.insert(entity)
    }
}