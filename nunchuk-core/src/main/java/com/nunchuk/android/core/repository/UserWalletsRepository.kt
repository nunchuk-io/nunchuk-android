package com.nunchuk.android.core.repository

import com.google.gson.Gson
import com.nunchuk.android.core.data.api.UserWalletsApi
import com.nunchuk.android.core.data.model.*
import com.nunchuk.android.core.data.model.membership.*
import com.nunchuk.android.core.data.model.membership.CreateWalletRequest
import com.nunchuk.android.core.data.model.membership.KeyPoliciesDto
import com.nunchuk.android.core.data.model.membership.SignerServerDto
import com.nunchuk.android.core.data.model.membership.TapSignerDto
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.util.ONE_HOUR_TO_SECONDS
import com.nunchuk.android.model.*
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.MembershipRepository
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.TransactionStatus
import javax.inject.Inject

internal class PremiumWalletRepositoryImpl @Inject constructor(
    private val userWalletsApi: UserWalletsApi,
    private val membershipRepository: MembershipRepository,
    private val gson: Gson,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val ncDataStore: NcDataStore
) : PremiumWalletRepository {

    override suspend fun getSecurityQuestions(): List<SecurityQuestion> {
        val questions = userWalletsApi.getSecurityQuestion().data.questions.map {
            SecurityQuestion(
                it.id,
                it.question
            )
        }
        return questions
    }

    override suspend fun configSecurityQuestions(
        questions: List<QuestionsAndAnswer>,
    ) {
        val result = userWalletsApi.configSecurityQuestion(
            ConfigSecurityQuestionPayload(
                questionsAndAnswerRequests = questions.map {
                    QuestionsAndAnswerRequest(
                        questionId = it.questionId,
                        answer = it.answer
                    )
                }
            )
        )
        if (result.isSuccess) {
            membershipRepository.saveStepInfo(
                MembershipStepInfo(
                    step = MembershipStep.SETUP_KEY_RECOVERY,
                    isVerify = true
                )
            )
        }
        throw result.error
    }

    override suspend fun createServerKeys(name: String, keyPolicy: KeyPolicy): KeyPolicy {
        val data = userWalletsApi.createServerKey(
            CreateServerKeysPayload(
                name = name, keyPoliciesDtoPayload = KeyPoliciesDto(
                    autoBroadcastTransaction = keyPolicy.autoBroadcastTransaction,
                    signingDelaySeconds = keyPolicy.signingDelayInHour * 60 * 60
                )
            )
        ).data
        val key = data.key ?: throw NullPointerException("Response from server empty")
        membershipRepository.saveStepInfo(
            MembershipStepInfo(
                step = MembershipStep.ADD_SEVER_KEY,
                isVerify = true,
                keyIdInServer = key.id.orEmpty(),
                extraData = gson.toJson(
                    ServerKey(
                        name = key.name.orEmpty(),
                        xfp = key.xfp.orEmpty(),
                        derivationPath = key.derivationPath.orEmpty(),
                        xpub = key.xpub.orEmpty()
                    )
                )
            )
        )
        return keyPolicy
    }

    override suspend fun getServerKey(xfp: String): KeyPolicy {
        val response = userWalletsApi.getServerKey(xfp)
        val policy =
            response.data.key?.policies ?: throw NullPointerException("Can not find key policy")
        return KeyPolicy(
            autoBroadcastTransaction = policy.autoBroadcastTransaction,
            signingDelayInHour = policy.signingDelaySeconds / ONE_HOUR_TO_SECONDS
        )
    }

    override suspend fun updateServerKeys(
        keyIdOrXfp: String,
        name: String,
        policy: KeyPolicy
    ): KeyPolicy {
        val response = userWalletsApi.updateServerKeys(
            keyIdOrXfp,
            UpdateServerKeysPayload(
                name = name,
                keyPoliciesDtoPayload = KeyPoliciesDto(
                    autoBroadcastTransaction = policy.autoBroadcastTransaction,
                    signingDelaySeconds = policy.signingDelayInHour * 60 * 60
                )
            )
        )
        val serverPolicy =
            response.data.key?.policies ?: throw NullPointerException("Can not find key policy")
        return KeyPolicy(
            autoBroadcastTransaction = serverPolicy.autoBroadcastTransaction,
            signingDelayInHour = serverPolicy.signingDelaySeconds / ONE_HOUR_TO_SECONDS
        )
    }

    override suspend fun createSecurityQuestion(question: String): SecurityQuestion {
        val response =
            userWalletsApi.createSecurityQuestion(CreateSecurityQuestionRequest(question)).data.question
        return SecurityQuestion(id = response.id, question = response.question)
    }

    override suspend fun createServerWallet(wallet: Wallet, serverKeyId: String): SeverWallet {
        val signers = wallet.signers.map {
            if (it.type == SignerType.NFC) {
                val status = nunchukNativeSdk.getTapSignerStatusFromMasterSigner(it.masterSignerId)
                SignerServerDto(
                    name = it.name,
                    xfp = it.masterFingerprint,
                    derivationPath = it.derivationPath,
                    xpub = it.xpub,
                    pubkey = it.publicKey,
                    type = SignerType.NFC.name,
                    tapsigner = TapSignerDto(
                        cardId = status.ident.orEmpty(),
                        version = status.version.orEmpty(),
                        birthHeight = status.birthHeight,
                        isTestnet = status.isTestNet
                    )
                )
            } else {
                SignerServerDto(
                    name = it.name,
                    xfp = it.masterFingerprint,
                    derivationPath = it.derivationPath,
                    xpub = it.xpub,
                    pubkey = it.publicKey,
                    type = SignerType.SERVER.name,
                )
            }
        }
        val bsms = nunchukNativeSdk.exportWalletToBsms(wallet.id)
        val request = CreateWalletRequest(
            name = wallet.name,
            description = wallet.description,
            bsms = bsms,
            signers = signers,
            localId = wallet.id,
            serverKeyId = serverKeyId
        )
        val response = userWalletsApi.createWallet(request)
        if (response.isSuccess) {
            membershipRepository.saveStepInfo(
                MembershipStepInfo(
                    step = MembershipStep.CREATE_WALLET,
                    isVerify = true
                )
            )
        }
        return SeverWallet(response.data.wallet.id.orEmpty())
    }

    override suspend fun getServerWallet(): WalletServerSync {
        val result = userWalletsApi.getServerWallet()

        ncDataStore.setAssistedWalletIds(result.data.wallets.map { it.localId.orEmpty() }.toSet())
        var isNeedReload = false

        val keyPolicyMap = hashMapOf<String, KeyPolicy>()
        result.data.wallets.forEach { walletServer ->
            keyPolicyMap[walletServer.localId.orEmpty()] = KeyPolicy(
                walletServer.serverKeyDto?.policies?.autoBroadcastTransaction ?: false,
                (walletServer.serverKeyDto?.policies?.signingDelaySeconds
                    ?: 0) / ONE_HOUR_TO_SECONDS
            )
            if (nunchukNativeSdk.hasWallet(walletServer.localId.orEmpty()).not()) {
                isNeedReload = true
                walletServer.signerServerDtos.forEach {
                    if (it.tapsigner != null) {
                        nunchukNativeSdk.addTapSigner(
                            cardId = it.tapsigner.cardId,
                            name = it.name.orEmpty(),
                            xfp = it.xfp.orEmpty(),
                            version = it.tapsigner.version,
                            brithHeight = it.tapsigner.birthHeight,
                            isTestNet = it.tapsigner.isTestnet
                        )
                    } else {
                        if (nunchukNativeSdk.hasSigner(
                                SingleSigner(
                                    name = it.name.orEmpty(),
                                    xpub = it.xpub.orEmpty(),
                                    publicKey = it.pubkey.orEmpty(),
                                    derivationPath = it.derivationPath.orEmpty(),
                                    masterFingerprint = it.xfp.orEmpty(),
                                )
                            ).not()
                        ) {
                            nunchukNativeSdk.createSigner(
                                name = it.name.orEmpty(),
                                xpub = it.xpub.orEmpty(),
                                publicKey = it.pubkey.orEmpty(),
                                derivationPath = it.derivationPath.orEmpty(),
                                masterFingerprint = it.xfp.orEmpty(),
                                type = nunchukNativeSdk.signerTypeFromStr(it.type.orEmpty())
                            )
                        }
                    }
                }

                val wallet =
                    nunchukNativeSdk.parseWalletDescriptor(walletServer.bsms.orEmpty()).apply {
                        name = walletServer.name.orEmpty()
                        description = walletServer.description.orEmpty()
                    }
                nunchukNativeSdk.createWallet2(wallet)
            } else {
                val wallet =
                    nunchukNativeSdk.parseWalletDescriptor(walletServer.bsms.orEmpty()).apply {
                        name = walletServer.name.orEmpty()
                        description = walletServer.description.orEmpty()
                    }
                nunchukNativeSdk.updateWallet(wallet)
            }
        }
        return WalletServerSync(
            isHasWallet = result.data.hasWalletCreated,
            keyPolicyMap = keyPolicyMap,
            isNeedReload = isNeedReload
        )
    }

    override suspend fun updateServerWallet(walletLocalId: String, name: String): SeverWallet {
        val response = userWalletsApi.updateWallet(walletLocalId, UpdateWalletPayload(name = name))
        return SeverWallet(response.data.wallet.id.orEmpty())
    }

    override suspend fun getServerTransaction(
        walletId: String,
        transactionId: String
    ): Transaction? {
        val response = userWalletsApi.getTransaction(walletId, transactionId)
        return handleTransactionFromServer(walletId, response.data.transaction)
    }

    override suspend fun createServerTransaction(
        walletId: String,
        psbt: String,
        note: String?
    ) {
        val response = userWalletsApi.createTransaction(
            walletId, CreateServerTransactionRequest(
                note = note, psbt = psbt
            )
        )
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun signServerTransaction(
        walletId: String,
        txId: String,
        psbt: String
    ): Transaction? {
        val response = userWalletsApi.signServerTransaction(
            walletId, txId, SignServerTransactionRequest(psbt = psbt)
        )
        return handleTransactionFromServer(walletId, response.data.transaction)
    }

    override suspend fun deleteServerTransaction(walletId: String, transactionId: String) {
        val response = userWalletsApi.deleteTransaction(walletId, transactionId)
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    private fun handleTransactionFromServer(
        walletId: String,
        transaction: TransactionServer?
    ): Transaction? {
        return when (transaction?.status) {
            TransactionStatus.PENDING_CONFIRMATION.name,
            TransactionStatus.CONFIRMED.name,
            TransactionStatus.NETWORK_REJECTED.name -> nunchukNativeSdk.updateTransaction(
                walletId = walletId,
                txId = transaction.transactionId.orEmpty(),
                newTxId = transaction.transactionId.orEmpty(),
                rawTx = transaction.hex.orEmpty(),
                rejectMsg = transaction.rejectMsg.orEmpty(),
            )
            TransactionStatus.READY_TO_BROADCAST.name -> nunchukNativeSdk.importPsbt(
                walletId,
                transaction.psbt.orEmpty()
            )
            else -> null
        }
    }
}

