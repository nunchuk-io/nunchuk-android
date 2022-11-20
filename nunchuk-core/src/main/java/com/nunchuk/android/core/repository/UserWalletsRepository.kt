package com.nunchuk.android.core.repository

import com.google.gson.Gson
import com.nunchuk.android.core.data.api.UserWalletsApi
import com.nunchuk.android.core.data.model.*
import com.nunchuk.android.core.data.model.membership.*
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
        plan: MembershipPlan,
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
                    isVerify = true,
                    plan = plan
                )
            )
        } else {
            throw result.error
        }
    }

    override suspend fun createServerKeys(
        name: String,
        keyPolicy: KeyPolicy,
        plan: MembershipPlan
    ): KeyPolicy {
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
                    ServerKeyExtra(
                        name = key.name.orEmpty(),
                        xfp = key.xfp.orEmpty(),
                        derivationPath = key.derivationPath.orEmpty(),
                        xpub = key.xpub.orEmpty()
                    )
                ),
                plan = plan
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

    override suspend fun createServerWallet(
        wallet: Wallet,
        serverKeyId: String,
        plan: MembershipPlan
    ): SeverWallet {
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
                    isVerify = true,
                    plan = plan
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
        val planWalletCreated = mutableSetOf<String>()
        result.data.wallets.forEach { planWalletCreated.add(it.slug.orEmpty()) }
        return WalletServerSync(
            planWalletCreated = planWalletCreated,
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
        val transaction = response.data.transaction
        if (transaction != null && transaction.status != PENDING_SIGNING_STATUS) {
            return nunchukNativeSdk.importPsbt(walletId, transaction.psbt.orEmpty())
        }
        return null
    }

    override suspend fun createServerTransaction(
        walletId: String,
        psbt: String,
        note: String?
    ): Transaction? {
        val response = userWalletsApi.createTransaction(
            walletId, CreateServerTransactionRequest(
                note = note, psbt = psbt
            )
        )
        val transaction = response.data.transaction
        if (transaction != null && transaction.status != PENDING_SIGNING_STATUS) {
            return nunchukNativeSdk.importPsbt(walletId, transaction.psbt.orEmpty())
        }
        return null
    }

    override suspend fun signServerTransaction(
        walletId: String,
        txId: String,
        psbt: String
    ): Transaction? {
        val response = userWalletsApi.signServerTransaction(
            walletId, txId, SignServerTransactionRequest(psbt = psbt)
        )
        val transaction = response.data.transaction
        if (transaction != null && transaction.status != PENDING_SIGNING_STATUS) {
            val newTransaction = nunchukNativeSdk.importPsbt(walletId, transaction.psbt.orEmpty())
            return if (transaction.status == PENDING_CONFIRMATION_STATUS) {
                newTransaction.copy(status = TransactionStatus.PENDING_CONFIRMATION)
            } else {
                newTransaction
            }
        }
        return null
    }

    override suspend fun deleteServerTransaction(walletId: String, transactionId: String) {
        val response = userWalletsApi.deleteTransaction(walletId, transactionId)
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    companion object {
        private const val PENDING_SIGNING_STATUS = "PENDING_SIGNING"
        private const val PENDING_CONFIRMATION_STATUS = "PENDING_CONFIRMATION"
    }
}

