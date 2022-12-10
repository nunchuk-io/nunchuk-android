package com.nunchuk.android.core.repository

import com.google.gson.Gson
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.data.api.UserWalletsApi
import com.nunchuk.android.core.data.model.*
import com.nunchuk.android.core.data.model.membership.*
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.util.ONE_HOUR_TO_SECONDS
import com.nunchuk.android.model.*
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.repository.MembershipRepository
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.utils.SERVER_KEY_NAME
import javax.inject.Inject


internal class PremiumWalletRepositoryImpl @Inject constructor(
    private val userWalletsApi: UserWalletsApi,
    private val membershipRepository: MembershipRepository,
    private val gson: Gson,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val ncDataStore: NcDataStore,
    private val membershipStepDao: MembershipStepDao,
    private val accountManager: AccountManager,
) : PremiumWalletRepository {

    override suspend fun getSecurityQuestions(verifyToken: String?): List<SecurityQuestion> {
        val questions = userWalletsApi.getSecurityQuestion(verifyToken).data.questions.map {
            SecurityQuestion(
                id = it.id, question = it.question, isAnswer = it.isAnswer ?: false
            )
        }
        return questions
    }

    override suspend fun verifySecurityQuestions(questions: List<QuestionsAndAnswer>): String {
        return userWalletsApi.verifySecurityQuestion(
            ConfigSecurityQuestionPayload(
                questionsAndAnswerRequests = questions.map {
                    QuestionsAndAnswerRequest(
                        questionId = it.questionId, answer = it.answer
                    )
                })
        ).data.token?.token.orEmpty()
    }

    override suspend fun configSecurityQuestions(
        questions: List<QuestionsAndAnswer>,
        plan: MembershipPlan,
    ) {
        val result = userWalletsApi.configSecurityQuestion(
            ConfigSecurityQuestionPayload(
                questionsAndAnswerRequests = questions.map {
                    QuestionsAndAnswerRequest(
                        questionId = it.questionId, answer = it.answer
                    )
                })
        )
        if (result.isSuccess) {
            membershipRepository.saveStepInfo(
                MembershipStepInfo(
                    step = MembershipStep.SETUP_KEY_RECOVERY,
                    verifyType = VerifyType.APP_VERIFIED,
                    plan = plan
                )
            )
        } else {
            throw result.error
        }
    }

    override suspend fun createServerKeys(
        name: String, keyPolicy: KeyPolicy, plan: MembershipPlan
    ): KeyPolicy {
        val data = userWalletsApi.createServerKey(
            CreateServerKeysPayload(
                name = name, keyPoliciesDtoPayload = keyPolicy.toDto()
            )
        ).data
        val key = data.key ?: throw NullPointerException("Response from server empty")
        membershipRepository.saveStepInfo(
            MembershipStepInfo(
                step = MembershipStep.ADD_SEVER_KEY,
                verifyType = VerifyType.APP_VERIFIED,
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
        val spendingLimit = policy.spendingLimit?.let {
            SpendingPolicy(
                limit = it.limit,
                currencyUnit = runCatching { SpendingCurrencyUnit.valueOf(it.currency) }.getOrElse { SpendingCurrencyUnit.USD },
                timeUnit = runCatching { SpendingTimeUnit.valueOf(it.interval) }.getOrElse { SpendingTimeUnit.DAILY }
            )
        }
        return KeyPolicy(
            autoBroadcastTransaction = policy.autoBroadcastTransaction,
            signingDelayInSeconds = policy.signingDelaySeconds,
            spendingPolicy = spendingLimit
        )
    }

    override suspend fun updateServerKeys(
        signatures: Map<String, String>,
        keyIdOrXfp: String,
        token: String,
        securityQuestionToken: String,
        body: String,
    ): KeyPolicy {
        val headers = mutableMapOf("Verify-token" to token)
        if (securityQuestionToken.isNotEmpty()) {
            headers["Security-Question-token"] = securityQuestionToken
        }
        signatures.map { (masterFingerprint, signature) ->
            nunchukNativeSdk.createRequestToken(signature, masterFingerprint)
        }.forEachIndexed { index, signerToken ->
            headers["AuthorizationX-${index + 1}"] = signerToken
        }
        val response = userWalletsApi.updateServerKeys(
            headers,
            keyIdOrXfp,
            gson.fromJson(body, KeyPolicyUpdateRequest::class.java)
        )
        val serverPolicy =
            response.data.key?.policies ?: throw NullPointerException("Can not find key policy")
        return KeyPolicy(
            autoBroadcastTransaction = serverPolicy.autoBroadcastTransaction,
            signingDelayInSeconds = serverPolicy.signingDelaySeconds
        )
    }

    override suspend fun createSecurityQuestion(question: String): SecurityQuestion {
        val response =
            userWalletsApi.createSecurityQuestion(CreateSecurityQuestionRequest(question)).data.question
        return SecurityQuestion(
            id = response.id, question = response.question, isAnswer = response.isAnswer ?: true
        )
    }

    override suspend fun createServerWallet(
        wallet: Wallet, serverKeyId: String, plan: MembershipPlan
    ): SeverWallet {
        val entity = membershipStepDao.getStep(
            accountManager.getAccount().chatId,
            MembershipStep.HONEY_ADD_TAP_SIGNER
        )
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
                        isTestnet = status.isTestNet,
                        isInheritance = entity?.masterSignerId == it.masterSignerId
                    )
                )
            } else {
                SignerServerDto(
                    name = it.name,
                    xfp = it.masterFingerprint,
                    derivationPath = it.derivationPath,
                    xpub = it.xpub,
                    pubkey = it.publicKey,
                    type = it.type.name,
                )
            }
        }
        val bsms = nunchukNativeSdk.exportWalletToBsms(wallet)
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
                    verifyType = VerifyType.APP_VERIFIED,
                    plan = plan
                )
            )
            ncDataStore.setAssistedWalletId(response.data.wallet.localId.orEmpty())
            ncDataStore.setAssistedWalletPlan(plan.name.lowercase())
        }
        return SeverWallet(response.data.wallet.id.orEmpty())
    }

    override suspend fun getServerWallet(): WalletServerSync {
        val result = userWalletsApi.getServerWallet()

        result.data.wallets.find { it.status == WALLET_ACTIVE_STATUS }?.let {
            ncDataStore.setAssistedWalletId(it.localId.orEmpty())
            ncDataStore.setAssistedWalletPlan(it.slug.orEmpty())
        }
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
        val planWalletCreated = hashMapOf<String, String>()
        result.data.wallets.forEach { planWalletCreated[it.slug.orEmpty()] = it.localId.orEmpty() }
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
        walletId: String, transactionId: String
    ): Transaction? {
        val response = userWalletsApi.getTransaction(walletId, transactionId)
        val transaction = response.data.transaction
        return handleServerTransaction(walletId, transactionId, transaction)
    }

    override suspend fun downloadBackup(
        id: String, questions: List<QuestionsAndAnswer>, verifyToken: String
    ): BackupKey {
        val configSecurityQuestionPayload =
            ConfigSecurityQuestionPayload(questionsAndAnswerRequests = questions.map {
                QuestionsAndAnswerRequest(
                    questionId = it.questionId, answer = it.answer
                )
            })
        val response = userWalletsApi.downloadBackup(verifyToken, id, configSecurityQuestionPayload)
        return BackupKey(
            keyId = response.data.keyId,
            keyCheckSum = response.data.keyCheckSum,
            keyBackUpBase64 = response.data.keyBackUpBase64,
            keyChecksumAlgorithm = response.data.keyChecksumAlgorithm.orEmpty(),
            keyName = response.data.keyName.orEmpty()
        )
    }

    override suspend fun verifiedPasswordToken(targetAction: String, password: String): String? {
        val response = userWalletsApi.verifiedPasswordToken(
            targetAction, VerifiedPasswordTokenRequest(password)
        )
        return response.data.token.token
    }

    override suspend fun calculateRequiredSignaturesSecurityQuestions(
        walletId: String, questions: List<QuestionsAndAnswer>
    ): CalculateRequiredSignatures {
        val request = CalculateRequiredSignaturesSecurityQuestionPayload(walletId = walletId,
            questionsAndAnswerRequests = questions.map {
                QuestionsAndAnswerRequest(
                    questionId = it.questionId, answer = it.answer
                )
            })
        val response = userWalletsApi.calculateRequiredSignaturesSecurityQuestions(request)
        return CalculateRequiredSignatures(
            type = response.data.result?.type.orEmpty(),
            requiredSignatures = response.data.result?.requiredSignatures ?: 0
        )
    }

    override suspend fun calculateRequiredSignaturesUpdateKeyPolicy(
        xfp: String,
        walletId: String,
        keyPolicy: KeyPolicy
    ): CalculateRequiredSignatures {
        val response = userWalletsApi.calculateRequiredSignaturesUpdateServerKey(
            xfp,
            CreateServerKeysPayload(
                walletId = walletId,
                keyPoliciesDtoPayload = keyPolicy.toDto(),
                name = null
            )
        )
        return CalculateRequiredSignatures(
            type = response.data.result?.type.orEmpty(),
            requiredSignatures = response.data.result?.requiredSignatures ?: 0
        )
    }

    override suspend fun securityQuestionsUpdate(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
    ) {
        val request = gson.fromJson(userData, SecurityQuestionsUpdateRequest::class.java)
        val headers = mutableMapOf<String, String>()
        authorizations.forEachIndexed { index, value ->
            headers["AuthorizationX-${index + 1}"] = value
        }
        headers["Verify-token"] = verifyToken
        return userWalletsApi.securityQuestionsUpdate(headers, request)
    }

    override suspend fun getNonce(): String {
        return userWalletsApi.getNonce().data.nonce?.nonce.orEmpty()
    }

    override suspend fun generateSecurityQuestionUserData(
        walletId: String,
        questions: List<QuestionsAndAnswer>
    ): String {
        val questionsAndAnswerRequests = questions.map {
            QuestionsAndAnswerRequest(
                questionId = it.questionId,
                answer = it.answer
            )
        }
        val body = QuestionsAndAnswerRequestBody(questionsAndAnswerRequests, walletId = walletId)
        val nonce = getNonce()
        val request = SecurityQuestionsUpdateRequest(
            nonce = nonce,
            body = body
        )
        return gson.toJson(request)
    }

    override suspend fun createServerTransaction(
        walletId: String,
        psbt: String,
        note: String?,
        txId: String
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
        walletId: String, txId: String, psbt: String
    ): Transaction? {
        val response = userWalletsApi.signServerTransaction(
            walletId, txId, SignServerTransactionRequest(psbt = psbt)
        )
        val transaction = response.data.transaction
        return handleServerTransaction(walletId, txId, transaction)
    }

    override suspend fun deleteServerTransaction(walletId: String, transactionId: String) {
        val response = userWalletsApi.deleteTransaction(walletId, transactionId)
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun getInheritance(walletId: String): Inheritance {
        val response = userWalletsApi.getInheritance(walletId)
        val inheritanceDto =
            response.data.inheritance ?: throw NullPointerException("Can not get inheritance")
        val status = when (inheritanceDto.status) {
            "ACTIVE" -> InheritanceStatus.ACTIVE
            "CLAIMED" -> InheritanceStatus.CLAIMED
            else -> InheritanceStatus.PENDING_CREATION
        }
        return Inheritance(
            walletId = inheritanceDto.walletId.orEmpty(),
            walletLocalId = inheritanceDto.walletLocalId.orEmpty(),
            magic = inheritanceDto.magic.orEmpty(),
            note = inheritanceDto.note.orEmpty(),
            notificationEmails = inheritanceDto.notificationEmails,
            status = status,
            activationTimeMilis = inheritanceDto.activationTimeMilis,
            createdTimeMilis = inheritanceDto.createdTimeMilis,
            lastModifiedTimeMilis = inheritanceDto.lastModifiedTimeMilis,
        )
    }

    private fun handleServerTransaction(
        walletId: String,
        transactionId: String,
        transaction: TransactionServer?
    ): Transaction? {
        transaction ?: return null
        return if (transaction.status == TransactionStatus.PENDING_CONFIRMATION.name
            || transaction.status == TransactionStatus.CONFIRMED.name
            || transaction.status == TransactionStatus.NETWORK_REJECTED.name
        ) {
            nunchukNativeSdk.importPsbt(walletId, transaction.psbt.orEmpty())
            nunchukNativeSdk.updateTransaction(
                walletId,
                transactionId,
                transaction.transactionId.orEmpty(),
                transaction.hex.orEmpty(),
                transaction.rejectMsg.orEmpty()
            )
        } else {
            nunchukNativeSdk.importPsbt(walletId, transaction.psbt.orEmpty())
        }
    }

    override suspend fun generateUpdateServerKey(walletId: String, keyPolicy: KeyPolicy): String {
        val body = CreateServerKeysPayload(
            walletId = walletId,
            keyPoliciesDtoPayload = keyPolicy.toDto(),
            name = SERVER_KEY_NAME
        )
        val nonce = getNonce()
        val request = KeyPolicyUpdateRequest(
            nonce = nonce,
            body = body
        )
        return gson.toJson(request)
    }

    override suspend fun scheduleTransaction(
        walletId: String,
        transactionId: String,
        scheduleTime: Long
    ): Long {
        val transaction = nunchukNativeSdk.getTransaction(walletId, transactionId)
        val response = userWalletsApi.scheduleTransaction(
            walletId,
            transactionId,
            ScheduleTransactionRequest(scheduleTime, transaction.psbt)
        )
        return response.data.transaction?.broadCastTimeMillis ?: 0L
    }

    companion object {
        private const val WALLET_ACTIVE_STATUS = "ACTIVE"
    }
}

