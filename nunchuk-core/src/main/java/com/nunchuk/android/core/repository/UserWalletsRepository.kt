/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.core.repository

import com.google.gson.Gson
import com.nunchuk.android.api.key.MembershipApi
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.data.api.TRANSACTION_PAGE_COUNT
import com.nunchuk.android.core.data.model.*
import com.nunchuk.android.core.data.model.membership.*
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.signer.toSignerTag
import com.nunchuk.android.core.util.ONE_HOUR_TO_SECONDS
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.model.*
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.model.transaction.ExtendedTransaction
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.model.transaction.ServerTransactionType
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.persistence.dao.AssistedWalletDao
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.persistence.entity.AssistedWalletEntity
import com.nunchuk.android.repository.MembershipRepository
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.utils.SERVER_KEY_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal class PremiumWalletRepositoryImpl @Inject constructor(
    private val userWalletApiManager: UserWalletApiManager,
    private val membershipRepository: MembershipRepository,
    private val gson: Gson,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val ncDataStore: NcDataStore,
    private val membershipStepDao: MembershipStepDao,
    private val accountManager: AccountManager,
    private val membershipApi: MembershipApi,
    private val assistedWalletDao: AssistedWalletDao,
    applicationScope: CoroutineScope,
) : PremiumWalletRepository {
    private val chain =
        ncDataStore.chain.stateIn(applicationScope, SharingStarted.Eagerly, Chain.MAIN)

    override suspend fun getSecurityQuestions(verifyToken: String?): List<SecurityQuestion> {
        val questions =
            userWalletApiManager.walletApi.getSecurityQuestion(verifyToken).data.questions.map {
                SecurityQuestion(
                    id = it.id, question = it.question, isAnswer = it.isAnswer ?: false
                )
            }
        return questions
    }

    override suspend fun verifySecurityQuestions(questions: List<QuestionsAndAnswer>): String {
        return userWalletApiManager.walletApi.verifySecurityQuestion(
            ConfigSecurityQuestionPayload(questionsAndAnswerRequests = questions.map {
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
        val result = userWalletApiManager.walletApi.configSecurityQuestion(
            ConfigSecurityQuestionPayload(questionsAndAnswerRequests = questions.map {
                QuestionsAndAnswerRequest(
                    questionId = it.questionId, answer = it.answer, change = it.change
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
        val data = userWalletApiManager.walletApi.createServerKey(
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
        val response = userWalletApiManager.walletApi.getServerKey(xfp)
        val policy =
            response.data.key?.policies ?: throw NullPointerException("Can not find key policy")
        val spendingLimit = policy.spendingLimit?.let {
            SpendingPolicy(limit = it.limit,
                currencyUnit = runCatching { SpendingCurrencyUnit.valueOf(it.currency) }.getOrElse { SpendingCurrencyUnit.USD },
                timeUnit = runCatching { SpendingTimeUnit.valueOf(it.interval) }.getOrElse { SpendingTimeUnit.DAILY })
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
        val headers = mutableMapOf(VERIFY_TOKEN to token)
        if (securityQuestionToken.isNotEmpty()) {
            headers[SECURITY_QUESTION_TOKEN] = securityQuestionToken
        }
        signatures.map { (masterFingerprint, signature) ->
            nunchukNativeSdk.createRequestToken(signature, masterFingerprint)
        }.forEachIndexed { index, signerToken ->
            headers["$AUTHORIZATION_X-${index + 1}"] = signerToken
        }
        val response = userWalletApiManager.walletApi.updateServerKeys(
            headers, keyIdOrXfp, gson.fromJson(body, KeyPolicyUpdateRequest::class.java)
        )
        val serverPolicy =
            response.data.key?.policies ?: throw NullPointerException("Can not find key policy")
        return KeyPolicy(
            autoBroadcastTransaction = serverPolicy.autoBroadcastTransaction,
            signingDelayInSeconds = serverPolicy.signingDelaySeconds
        )
    }

    override suspend fun createSecurityQuestion(question: String): SecurityQuestion {
        val response = userWalletApiManager.walletApi.createSecurityQuestion(
            CreateSecurityQuestionRequest(
                question
            )
        ).data.question
        return SecurityQuestion(
            id = response.id, question = response.question, isAnswer = response.isAnswer ?: true
        )
    }

    override suspend fun createServerWallet(
        wallet: Wallet, serverKeyId: String, plan: MembershipPlan
    ): SeverWallet {
        val inheritanceTapSigner = membershipStepDao.getStep(
            accountManager.getAccount().chatId, chain.value, MembershipStep.HONEY_ADD_TAP_SIGNER
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
                        isInheritance = inheritanceTapSigner?.masterSignerId == it.masterSignerId
                    ),
                    tags = if (inheritanceTapSigner?.masterSignerId == it.masterSignerId) listOf(
                        SignerTag.INHERITANCE.name
                    ) else null
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
        val response = userWalletApiManager.walletApi.createWallet(request)
        if (response.isSuccess) {
            assistedWalletDao.insert(
                AssistedWalletEntity(
                    localId = response.data.wallet.localId.orEmpty(), plan = plan
                )
            )
            membershipStepDao.deleteStepByChatId(chain.value, accountManager.getAccount().chatId)
        }
        return SeverWallet(response.data.wallet.id.orEmpty())
    }

    override suspend fun getServerWallet(): WalletServerSync {
        val result = userWalletApiManager.walletApi.getServerWallet()
        if (result.data.hasWalletCreated) {
            val partition = result.data.wallets.partition { it.status == WALLET_ACTIVE_STATUS }
            if (partition.second.isNotEmpty()) {
                assistedWalletDao.deleteBatch(partition.second.map { it.localId.orEmpty() })
            }
            if (partition.first.isNotEmpty()) {
                assistedWalletDao.insert(
                    partition.first.map { wallet ->
                        AssistedWalletEntity(
                            wallet.localId.orEmpty(),
                            wallet.slug.toMembershipPlan()
                        )
                    }
                )
            }
        }
        var isNeedReload = false

        val keyPolicyMap = hashMapOf<String, KeyPolicy>()
        result.data.wallets.filter { it.localId.isNullOrEmpty().not() }.forEach { walletServer ->
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
            }

            val wallet = nunchukNativeSdk.getWallet(walletServer.localId.orEmpty())
            if (wallet.name != walletServer.name || wallet.description != walletServer.description) {
                nunchukNativeSdk.updateWallet(
                    wallet.copy(
                        name = walletServer.name.orEmpty(),
                        description = walletServer.description.orEmpty()
                    )
                )
            }

            val signers = HashMap<String, SingleSigner>(wallet.signers.size)
            wallet.signers.associateByTo(signers) { it.masterFingerprint }

            walletServer.signerServerDtos.filter { it.tags.isNullOrEmpty().not() }
                .forEach { serverSigner ->
                    signers[serverSigner.xfp]?.let { localSigner ->
                        if (localSigner.hasMasterSigner) {
                            val masterSigner =
                                nunchukNativeSdk.getMasterSigner(localSigner.masterSignerId)
                            val tags = serverSigner.tags.orEmpty().mapNotNull { it.toSignerTag() }
                            if (tags.isNotEmpty()) {
                                masterSigner.tags = tags
                                nunchukNativeSdk.updateMasterSigner(masterSigner)
                            }
                        }
                    }
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
        val response = userWalletApiManager.walletApi.updateWallet(
            walletLocalId, UpdateWalletPayload(name = name)
        )
        return SeverWallet(response.data.wallet.id.orEmpty())
    }

    override suspend fun getServerTransaction(
        walletId: String, transactionId: String
    ): ExtendedTransaction {
        val response = userWalletApiManager.walletApi.getTransaction(walletId, transactionId)
        val transaction =
            response.data.transaction ?: throw NullPointerException("Transaction from server null")
        updateScheduleTransactionIfNeed(walletId, transactionId, transaction)
        return ExtendedTransaction(
            transaction = handleServerTransaction(
                walletId, transactionId, transaction
            ),
            serverTransaction = response.data.transaction?.toServerTransaction(),
        )
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
        val response = userWalletApiManager.walletApi.downloadBackup(
            verifyToken, id, configSecurityQuestionPayload
        )
        return BackupKey(
            keyId = response.data.keyId,
            keyCheckSum = response.data.keyCheckSum,
            keyBackUpBase64 = response.data.keyBackUpBase64,
            keyChecksumAlgorithm = response.data.keyChecksumAlgorithm.orEmpty(),
            keyName = response.data.keyName.orEmpty(),
            keyXfp = response.data.keyXfp.orEmpty(),
            cardId = response.data.cardId.orEmpty(),
            verificationType = response.data.verificationType.orEmpty(),
            verifiedTimeMilis = response.data.verifiedTimeMilis ?: 0L
        )
    }

    override suspend fun verifiedPasswordToken(targetAction: String, password: String): String? {
        val response = membershipApi.verifiedPasswordToken(
            targetAction, VerifiedPasswordTokenRequest(password)
        )
        return response.data.token.token
    }

    override suspend fun calculateRequiredSignaturesSecurityQuestions(
        walletId: String, questions: List<QuestionsAndAnswer>
    ): CalculateRequiredSignatures {
        val request = CalculateRequiredSignaturesSecurityQuestionPayload(
            walletId = walletId,
            questionsAndAnswerRequests = questions.map {
                QuestionsAndAnswerRequest(
                    questionId = it.questionId, answer = it.answer
                )
            })
        val response =
            userWalletApiManager.walletApi.calculateRequiredSignaturesSecurityQuestions(request)
        return CalculateRequiredSignatures(
            type = response.data.result?.type.orEmpty(),
            requiredSignatures = response.data.result?.requiredSignatures ?: 0
        )
    }

    override suspend fun calculateRequiredSignaturesUpdateKeyPolicy(
        xfp: String, walletId: String, keyPolicy: KeyPolicy
    ): CalculateRequiredSignatures {
        val response = userWalletApiManager.walletApi.calculateRequiredSignaturesUpdateServerKey(
            xfp, CreateServerKeysPayload(
                walletId = walletId, keyPoliciesDtoPayload = keyPolicy.toDto(), name = null
            )
        )
        return CalculateRequiredSignatures(
            type = response.data.result?.type.orEmpty(),
            requiredSignatures = response.data.result?.requiredSignatures ?: 0
        )
    }

    override suspend fun calculateRequiredSignaturesLockdown(
        walletId: String, periodId: String
    ): CalculateRequiredSignatures {
        val response = userWalletApiManager.walletApi.calculateRequiredSignaturesLockdown(
            LockdownUpdateRequest.Body(
                walletId = walletId, periodId = periodId
            )
        )
        return CalculateRequiredSignatures(
            type = response.data.result?.type.orEmpty(),
            requiredSignatures = response.data.result?.requiredSignatures ?: 0
        )
    }

    override suspend fun generateInheritanceUserData(
        note: String,
        notificationEmails: List<String>,
        notifyToday: Boolean,
        activationTimeMilis: Long,
        bufferPeriodId: String?,
        walletId: String
    ): String {
        val body = CreateUpdateInheritancePlanRequest.Body(
            note = note,
            notifyToday = notifyToday,
            notificationEmails = notificationEmails,
            activationTimeMilis = activationTimeMilis,
            bufferPeriodId = bufferPeriodId,
            walletId = walletId
        )
        val nonce = getNonce()
        val request = CreateUpdateInheritancePlanRequest(
            nonce = nonce, body = body
        )
        return gson.toJson(request)
    }

    override suspend fun generateInheritanceClaimStatusUserData(magic: String): String {
        val body = InheritanceClaimStatusRequest.Body(
            magic = magic
        )
        val nonce = getNonce()
        val request = InheritanceClaimStatusRequest(
            nonce = nonce, body = body
        )
        return gson.toJson(request)
    }

    override suspend fun generateInheritanceClaimCreateTransactionUserData(
        magic: String,
        address: String,
        feeRate: String
    ): String {
        val body = InheritanceClaimCreateTransactionRequest.Body(
            magic = magic,
            address = address,
            feeRate = feeRate
        )
        val nonce = getNonce()
        val request = InheritanceClaimCreateTransactionRequest(
            nonce = nonce, body = body
        )
        return gson.toJson(request)
    }

    override suspend fun inheritanceClaimStatus(
        userData: String,
        masterFingerprint: String,
        signature: String
    ): InheritanceAdditional {
        val headers = mutableMapOf<String, String>()
        val signerToken = nunchukNativeSdk.createRequestToken(signature, masterFingerprint)
        headers["$AUTHORIZATION_X-1"] = signerToken
        val request = gson.fromJson(userData, InheritanceClaimStatusRequest::class.java)
        val response =
            userWalletApiManager.walletApi.inheritanceClaimingStatus(headers, request).data

        val bufferPeriodCountdown = if (response.bufferPeriodCountdown == null) {
            null
        } else {
            BufferPeriodCountdown(
                activationTimeMilis = response.bufferPeriodCountdown.activationTimeMilis ?: 0,
                bufferInterval = response.bufferPeriodCountdown.bufferInterval.orEmpty(),
                bufferIntervalCount = response.bufferPeriodCountdown.bufferIntervalCount ?: 0,
                remainingCount = response.bufferPeriodCountdown.remainingCount ?: 0,
                remainingDisplayName = response.bufferPeriodCountdown.remainingDisplayName.orEmpty()
            )
        }

        return InheritanceAdditional(
            inheritance = response.inheritance?.toInheritance(),
            balance = response.balance ?: 0.0,
            bufferPeriodCountdown = bufferPeriodCountdown
        )
    }

    override suspend fun inheritanceClaimCreateTransaction(
        userData: String,
        masterFingerprint: String,
        signature: String
    ): TransactionAdditional {
        val headers = mutableMapOf<String, String>()
        val signerToken = nunchukNativeSdk.createRequestToken(signature, masterFingerprint)
        headers["$AUTHORIZATION_X-1"] = signerToken
        val request = gson.fromJson(userData, InheritanceClaimCreateTransactionRequest::class.java)
        val response =
            userWalletApiManager.walletApi.inheritanceClaimingCreateTransaction(headers, request)
        val transaction =
            response.data.transaction ?: throw NullPointerException("transaction from server null")
        return TransactionAdditional(
            psbt = transaction.psbt.orEmpty(),
            subAmount = response.data.subAmount ?: 0.0,
            fee = response.data.txFee ?: 0.0,
            feeRate = response.data.txFeeRate ?: 0.0
        )
    }

    override suspend fun generateCancelInheritanceUserData(walletId: String): String {
        val body = InheritanceCancelRequest.Body(
            walletId = walletId
        )
        val nonce = getNonce()
        val request = InheritanceCancelRequest(
            nonce = nonce, body = body
        )
        return gson.toJson(request)
    }

    override suspend fun calculateRequiredSignaturesInheritance(
        note: String,
        notificationEmails: List<String>,
        notifyToday: Boolean,
        activationTimeMilis: Long,
        walletId: String,
        bufferPeriodId: String?,
        isCancelInheritance: Boolean
    ): CalculateRequiredSignatures {
        val response = if (isCancelInheritance) {
            userWalletApiManager.walletApi.calculateRequiredSignaturesInheritance(
                CreateUpdateInheritancePlanRequest.Body(
                    walletId = walletId
                )
            )
        } else {
            userWalletApiManager.walletApi.calculateRequiredSignaturesInheritance(
                CreateUpdateInheritancePlanRequest.Body(
                    walletId = walletId,
                    note = note,
                    notificationEmails = notificationEmails,
                    notifyToday = notifyToday,
                    activationTimeMilis = activationTimeMilis,
                    bufferPeriodId = bufferPeriodId
                )
            )
        }
        return CalculateRequiredSignatures(
            type = response.data.result?.type.orEmpty(),
            requiredSignatures = response.data.result?.requiredSignatures ?: 0
        )
    }

    override suspend fun createOrUpdateInheritance(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String,
        isUpdate: Boolean,
        plan: MembershipPlan
    ): Inheritance {
        val request = gson.fromJson(userData, CreateUpdateInheritancePlanRequest::class.java)
        val headers = mutableMapOf<String, String>()
        authorizations.forEachIndexed { index, value ->
            headers["$AUTHORIZATION_X-${index + 1}"] = value
        }
        headers[VERIFY_TOKEN] = verifyToken
        headers[SECURITY_QUESTION_TOKEN] = securityQuestionToken
        val response = if (isUpdate) userWalletApiManager.walletApi.updateInheritance(
            headers, request
        ) else userWalletApiManager.walletApi.createInheritance(headers, request)
        if (response.data.inheritance == null) throw NullPointerException("Can not get inheritance")
        else return response.data.inheritance!!.toInheritance().also {
            markSetupInheritance(it.walletLocalId, true)
        }
    }

    override suspend fun cancelInheritance(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String,
        walletId: String
    ) {
        val request = gson.fromJson(userData, InheritanceCancelRequest::class.java)
        val headers = mutableMapOf<String, String>()
        authorizations.forEachIndexed { index, value ->
            headers["$AUTHORIZATION_X-${index + 1}"] = value
        }
        headers[VERIFY_TOKEN] = verifyToken
        headers[SECURITY_QUESTION_TOKEN] = securityQuestionToken
        val response = userWalletApiManager.walletApi.inheritanceCancel(headers, request)
        if (response.isSuccess) {
            markSetupInheritance(walletId, false)
        }
    }

    override suspend fun inheritanceClaimDownloadBackup(magic: String): BackupKey {
        val response = userWalletApiManager.walletApi.inheritanceClaimingDownloadBackup(
            InheritanceClaimDownloadBackupRequest(magic = magic)
        )
        return BackupKey(
            keyId = response.data.keyId,
            keyCheckSum = response.data.keyCheckSum,
            keyBackUpBase64 = response.data.keyBackUpBase64,
            keyChecksumAlgorithm = response.data.keyChecksumAlgorithm.orEmpty(),
            keyName = response.data.keyName.orEmpty(),
            keyXfp = response.data.keyXfp.orEmpty(),
            cardId = response.data.cardId.orEmpty(),
            verificationType = response.data.verificationType.orEmpty(),
            verifiedTimeMilis = response.data.verifiedTimeMilis ?: 0L
        )
    }

    override suspend fun inheritanceClaimingClaim(
        magic: String,
        psbt: String
    ): TransactionAdditional {
        val response = userWalletApiManager.walletApi.inheritanceClaimingClaim(
            InheritanceClaimClaimRequest(magic = magic, psbt = psbt)
        )
        val transaction =
            response.data.transaction ?: throw NullPointerException("transaction from server null")
        return TransactionAdditional(psbt = transaction.psbt.orEmpty())
    }

    override suspend fun inheritanceCheck(): InheritanceCheck {
        val request = InheritanceCheckRequest(environment = "PRODUCTION")
        val response = userWalletApiManager.walletApi.inheritanceCheck(request)
        return InheritanceCheck(
            isValid = response.data.isValid.orFalse(),
            isPaid = response.data.isPaid.orFalse(),
            isExpired = response.data.isExpired.orFalse()
        )
    }

    override suspend fun securityQuestionsUpdate(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String
    ) {
        val request = gson.fromJson(userData, SecurityQuestionsUpdateRequest::class.java)
        val headers = mutableMapOf<String, String>()
        authorizations.forEachIndexed { index, value ->
            headers["$AUTHORIZATION_X-${index + 1}"] = value
        }
        headers[VERIFY_TOKEN] = verifyToken
        headers[SECURITY_QUESTION_TOKEN] = securityQuestionToken
        return userWalletApiManager.walletApi.securityQuestionsUpdate(headers, request)
    }

    override suspend fun getNonce(): String {
        return userWalletApiManager.walletApi.getNonce().data.nonce?.nonce.orEmpty()
    }

    override suspend fun lockdownUpdate(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String
    ) {
        val request = gson.fromJson(userData, LockdownUpdateRequest::class.java)
        val headers = mutableMapOf<String, String>()
        authorizations.forEachIndexed { index, value ->
            headers["$AUTHORIZATION_X-${index + 1}"] = value
        }
        headers[VERIFY_TOKEN] = verifyToken
        headers[SECURITY_QUESTION_TOKEN] = securityQuestionToken
        return userWalletApiManager.walletApi.lockdownUpdate(headers, request)
    }

    override suspend fun generateSecurityQuestionUserData(
        walletId: String, questions: List<QuestionsAndAnswer>
    ): String {
        val questionsAndAnswerRequests = questions.map {
            QuestionsAndAnswerRequest(
                questionId = it.questionId, answer = it.answer, change = it.change
            )
        }
        val body = QuestionsAndAnswerRequestBody(questionsAndAnswerRequests, walletId = walletId)
        val nonce = getNonce()
        val request = SecurityQuestionsUpdateRequest(
            nonce = nonce, body = body
        )
        return gson.toJson(request)
    }

    override suspend fun generateLockdownUserData(walletId: String, periodId: String): String {
        val body = LockdownUpdateRequest.Body(periodId = periodId, walletId = walletId)
        val nonce = getNonce()
        val request = LockdownUpdateRequest(
            nonce = nonce, body = body
        )
        return gson.toJson(request)
    }

    override suspend fun createServerTransaction(
        walletId: String, psbt: String, note: String?, txId: String
    ) {
        val response = userWalletApiManager.walletApi.createTransaction(
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
    ): ExtendedTransaction {
        val response = userWalletApiManager.walletApi.signServerTransaction(
            walletId, txId, SignServerTransactionRequest(psbt = psbt)
        )
        val transaction =
            response.data.transaction ?: throw NullPointerException("transaction from server null")
        return ExtendedTransaction(
            transaction = handleServerTransaction(
                walletId, txId, transaction
            ),
            serverTransaction = response.data.transaction?.toServerTransaction(),
        )
    }

    override suspend fun deleteServerTransaction(walletId: String, transactionId: String) {
        val response = userWalletApiManager.walletApi.deleteTransaction(walletId, transactionId)
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun getInheritance(walletId: String): Inheritance {
        val response = userWalletApiManager.walletApi.getInheritance(walletId)
        if (response.data.inheritance == null) throw NullPointerException("Can not get inheritance")
        else return response.data.inheritance!!.toInheritance().also {
            markSetupInheritance(walletId, it.status != InheritanceStatus.PENDING_CREATION)
        }
    }

    private suspend fun markSetupInheritance(walletId: String, isSetupInheritance: Boolean) {
        val entity = assistedWalletDao.getById(walletId)
        if (entity.isSetupInheritance.not()) {
            assistedWalletDao.updateOrInsert(entity.copy(isSetupInheritance = isSetupInheritance))
        }
    }

    private fun handleServerTransaction(
        walletId: String, transactionId: String, transaction: TransactionServerDto
    ): Transaction {
        return if (transaction.status == TransactionStatus.PENDING_CONFIRMATION.name || transaction.status == TransactionStatus.CONFIRMED.name || transaction.status == TransactionStatus.NETWORK_REJECTED.name) {
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
            walletId = walletId, keyPoliciesDtoPayload = keyPolicy.toDto(), name = SERVER_KEY_NAME
        )
        val nonce = getNonce()
        val request = KeyPolicyUpdateRequest(
            nonce = nonce, body = body
        )
        return gson.toJson(request)
    }

    override suspend fun scheduleTransaction(
        walletId: String, transactionId: String, scheduleTime: Long
    ): ServerTransaction {
        val transaction = nunchukNativeSdk.getTransaction(walletId, transactionId)
        val response = userWalletApiManager.walletApi.scheduleTransaction(
            walletId, transactionId, ScheduleTransactionRequest(scheduleTime, transaction.psbt)
        )
        val serverTransaction = response.data.transaction
            ?: throw NullPointerException("Schedule transaction does not return server transaction")
        updateScheduleTransactionIfNeed(walletId, transactionId, serverTransaction)
        return serverTransaction.toServerTransaction()
    }

    override suspend fun deleteScheduleTransaction(
        walletId: String,
        transactionId: String
    ): ServerTransaction {
        val response = userWalletApiManager.walletApi.deleteScheduleTransaction(
            walletId, transactionId,
        )
        if (response.isSuccess) {
            nunchukNativeSdk.updateTransactionSchedule(walletId, transactionId, 0)
        }
        return response.data.transaction?.toServerTransaction()
            ?: throw NullPointerException("transaction from server null")
    }

    override suspend fun getLockdownPeriod(): List<Period> {
        val response = userWalletApiManager.walletApi.getLockdownPeriod()
        return response.data.periods?.map {
            Period(
                id = it.id.orEmpty(),
                interval = it.interval.orEmpty(),
                intervalCount = it.intervalCount.orDefault(0),
                enabled = it.enabled.orFalse(),
                displayName = it.displayName.orEmpty(),
                isRecommended = false
            )
        }.orEmpty()
    }

    override suspend fun syncTransaction(walletId: String) {
        (0 until Int.MAX_VALUE step TRANSACTION_PAGE_COUNT).forEach { index ->
            val response = userWalletApiManager.walletApi.getTransactions(walletId, index)
            if (response.isSuccess.not()) throw response.error
            response.data.transactions.forEach { transition ->
                if (transition.psbt.isNullOrEmpty().not()) {
                    nunchukNativeSdk.importPsbt(walletId, transition.psbt.orEmpty())
                    updateScheduleTransactionIfNeed(
                        walletId,
                        transition.transactionId.orEmpty(),
                        transition
                    )
                }
            }
            if (response.data.transactions.size < TRANSACTION_PAGE_COUNT) return
        }
    }

    override suspend fun getInheritanceBufferPeriod(): List<Period> {
        val response = userWalletApiManager.walletApi.getInheritanceBufferPeriod()
        return response.data.periods?.map { it.toPeriod() }.orEmpty()
    }

    private fun updateScheduleTransactionIfNeed(
        walletId: String,
        transactionId: String,
        transaction: TransactionServerDto
    ) {
        if (transaction.type == ServerTransactionType.SCHEDULED && transaction.broadCastTimeMillis > System.currentTimeMillis()) {
            nunchukNativeSdk.updateTransactionSchedule(
                walletId,
                transactionId,
                transaction.broadCastTimeMillis / 1000
            )
        }
    }

    override fun getAssistedWalletsLocal(): Flow<List<AssistedWalletBrief>> {
        return assistedWalletDao.getAssistedWallets().map { list ->
            list.map { wallet ->
                AssistedWalletBrief(
                    wallet.localId, wallet.plan, wallet.isSetupInheritance
                )
            }
        }
    }

    override suspend fun clearLocalData() {
        assistedWalletDao.deleteAll()
    }

    override suspend fun reuseKeyWallet(walletId: String) {
        val wallet = nunchukNativeSdk.getWallet(walletId)
        val inheritanceKey = wallet.signers.find {
            it.type == SignerType.NFC
                    && nunchukNativeSdk.getMasterSigner(it.masterFingerprint).tags.contains(
                SignerTag.INHERITANCE
            )
        } ?: throw NullPointerException("Can not find inheritance key")
        val verifyMap = coroutineScope {
            wallet.signers.filter { it.type == SignerType.NFC }
                .map { it.masterFingerprint }.map { key ->
                async {
                    userWalletApiManager.walletApi.getKey(key)
                }
            }.awaitAll().associateBy { it.data.keyXfp }.mapValues { it.value.data.verificationType }
        }
        membershipRepository.saveStepInfo(
            MembershipStepInfo(
                step = MembershipStep.HONEY_ADD_TAP_SIGNER,
                masterSignerId = inheritanceKey.masterFingerprint,
                plan = MembershipPlan.HONEY_BADGER,
                verifyType = verifyMap[inheritanceKey.masterFingerprint].toVerifyType(),
                extraData = gson.toJson(
                    SignerExtra(
                        derivationPath = inheritanceKey.derivationPath,
                        isAddNew = false,
                        signerType = inheritanceKey.type
                    )
                )
            )
        )

        val steps =
            listOf(MembershipStep.HONEY_ADD_HARDWARE_KEY_1, MembershipStep.HONEY_ADD_HARDWARE_KEY_2)
        wallet.signers.filter { it.type != SignerType.SERVER && it != inheritanceKey }
            .forEachIndexed { index, singleSigner ->
                membershipRepository.saveStepInfo(
                    MembershipStepInfo(
                        step = steps[index],
                        masterSignerId = singleSigner.masterFingerprint,
                        plan = MembershipPlan.HONEY_BADGER,
                        verifyType = verifyMap[singleSigner.masterFingerprint].toVerifyType(),
                        extraData = gson.toJson(
                            SignerExtra(
                                derivationPath = singleSigner.derivationPath,
                                isAddNew = false,
                                signerType = singleSigner.type
                            )
                        )
                    )
                )
            }
    }

    private fun InheritanceDto.toInheritance(): Inheritance {
        val status = when (this.status) {
            "ACTIVE" -> InheritanceStatus.ACTIVE
            "CLAIMED" -> InheritanceStatus.CLAIMED
            else -> InheritanceStatus.PENDING_CREATION
        }
        return Inheritance(
            walletId = walletId.orEmpty(), walletLocalId = walletLocalId.orEmpty(),
            magic = magic.orEmpty(),
            note = note.orEmpty(),
            notificationEmails = notificationEmails.orEmpty(),
            status = status,
            activationTimeMilis = activationTimeMilis ?: 0,
            createdTimeMilis = createdTimeMilis ?: 0,
            lastModifiedTimeMilis = lastModifiedTimeMilis ?: 0,
            bufferPeriod = bufferPeriod?.toPeriod()
        )
    }

    private fun PeriodResponse.Data.toPeriod() = Period(
        id = id.orEmpty(),
        interval = interval.orEmpty(),
        intervalCount = intervalCount.orDefault(0),
        enabled = enabled.orFalse(),
        displayName = displayName.orEmpty(),
        isRecommended = isRecommended.orFalse()
    )

    companion object {
        private const val WALLET_ACTIVE_STATUS = "ACTIVE"
        private const val VERIFY_TOKEN = "Verify-token"
        private const val SECURITY_QUESTION_TOKEN = "Security-Question-token"
        private const val AUTHORIZATION_X = "AuthorizationX"
    }
}

