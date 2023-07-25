/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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
import com.google.gson.reflect.TypeToken
import com.nunchuk.android.api.key.MembershipApi
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.data.api.TRANSACTION_PAGE_COUNT
import com.nunchuk.android.core.data.model.CalculateRequiredSignaturesSecurityQuestionPayload
import com.nunchuk.android.core.data.model.ConfigSecurityQuestionPayload
import com.nunchuk.android.core.data.model.CreateSecurityQuestionRequest
import com.nunchuk.android.core.data.model.CreateServerKeysPayload
import com.nunchuk.android.core.data.model.CreateUpdateInheritancePlanRequest
import com.nunchuk.android.core.data.model.DeleteAssistedWalletRequest
import com.nunchuk.android.core.data.model.InheritanceCancelRequest
import com.nunchuk.android.core.data.model.InheritanceCheckRequest
import com.nunchuk.android.core.data.model.InheritanceClaimClaimRequest
import com.nunchuk.android.core.data.model.InheritanceClaimCreateTransactionRequest
import com.nunchuk.android.core.data.model.InheritanceClaimDownloadBackupRequest
import com.nunchuk.android.core.data.model.InheritanceClaimStatusRequest
import com.nunchuk.android.core.data.model.LockdownUpdateRequest
import com.nunchuk.android.core.data.model.QuestionsAndAnswerRequest
import com.nunchuk.android.core.data.model.QuestionsAndAnswerRequestBody
import com.nunchuk.android.core.data.model.SecurityQuestionsUpdateRequest
import com.nunchuk.android.core.data.model.SyncTransactionRequest
import com.nunchuk.android.core.data.model.UpdateKeyPayload
import com.nunchuk.android.core.data.model.UpdateWalletPayload
import com.nunchuk.android.core.data.model.byzantine.CreateGroupRequest
import com.nunchuk.android.core.data.model.byzantine.CreateOrUpdateGroupChatRequest
import com.nunchuk.android.core.data.model.byzantine.EditGroupMemberRequest
import com.nunchuk.android.core.data.model.byzantine.GroupResponse
import com.nunchuk.android.core.data.model.byzantine.MemberRequest
import com.nunchuk.android.core.data.model.byzantine.WalletConfigRequest
import com.nunchuk.android.core.data.model.byzantine.toModel
import com.nunchuk.android.core.data.model.coin.CoinDataContent
import com.nunchuk.android.core.data.model.membership.CreateOrUpdateServerTransactionRequest
import com.nunchuk.android.core.data.model.membership.CreateWalletRequest
import com.nunchuk.android.core.data.model.membership.DesktopKeyRequest
import com.nunchuk.android.core.data.model.membership.KeyPolicyUpdateRequest
import com.nunchuk.android.core.data.model.membership.ScheduleTransactionRequest
import com.nunchuk.android.core.data.model.membership.SignServerTransactionRequest
import com.nunchuk.android.core.data.model.membership.SignerServerDto
import com.nunchuk.android.core.data.model.membership.TapSignerDto
import com.nunchuk.android.core.data.model.membership.TransactionServerDto
import com.nunchuk.android.core.data.model.membership.WalletDto
import com.nunchuk.android.core.data.model.membership.toDto
import com.nunchuk.android.core.data.model.membership.toExternalModel
import com.nunchuk.android.core.data.model.membership.toModel
import com.nunchuk.android.core.data.model.membership.toServerTransaction
import com.nunchuk.android.core.exception.RequestAddKeyCancelException
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.core.mapper.toAlert
import com.nunchuk.android.core.mapper.toBackupKey
import com.nunchuk.android.core.mapper.toByzantineGroup
import com.nunchuk.android.core.mapper.toCalculateRequiredSignatures
import com.nunchuk.android.core.mapper.toGroupChat
import com.nunchuk.android.core.mapper.toHistoryPeriod
import com.nunchuk.android.core.mapper.toInheritance
import com.nunchuk.android.core.mapper.toPeriod
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.signer.toSignerTag
import com.nunchuk.android.core.util.ONE_HOUR_TO_SECONDS
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.core.util.toSignerType
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.BufferPeriodCountdown
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineGroupBrief
import com.nunchuk.android.model.ByzantineMemberBrief
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.DefaultPermissions
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.model.InheritanceCheck
import com.nunchuk.android.model.InheritanceStatus
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.QuestionsAndAnswer
import com.nunchuk.android.model.SecurityQuestion
import com.nunchuk.android.model.ServerKeyExtra
import com.nunchuk.android.model.SeverWallet
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.SpendingPolicy
import com.nunchuk.android.model.SpendingTimeUnit
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TransactionAdditional
import com.nunchuk.android.model.VerifiedPKeyTokenRequest
import com.nunchuk.android.model.VerifiedPasswordTokenRequest
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.WalletConstraints
import com.nunchuk.android.model.WalletServerSync
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.model.membership.AssistedWalletConfig
import com.nunchuk.android.model.membership.GroupConfig
import com.nunchuk.android.model.toIndex
import com.nunchuk.android.model.toMembershipPlan
import com.nunchuk.android.model.toVerifyType
import com.nunchuk.android.model.transaction.ExtendedTransaction
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.model.transaction.ServerTransactionType
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.persistence.dao.AssistedWalletDao
import com.nunchuk.android.persistence.dao.GroupDao
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.persistence.dao.RequestAddKeyDao
import com.nunchuk.android.persistence.entity.AssistedWalletEntity
import com.nunchuk.android.persistence.entity.GroupEntity
import com.nunchuk.android.persistence.entity.RequestAddKeyEntity
import com.nunchuk.android.repository.MembershipRepository
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.utils.SERVER_KEY_NAME
import com.nunchuk.android.utils.isServerMasterSigner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
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
    private val requestAddKeyDao: RequestAddKeyDao,
    private val groupDao: GroupDao,
    applicationScope: CoroutineScope,
) : PremiumWalletRepository {
    private val chain =
        ncDataStore.chain.stateIn(applicationScope, SharingStarted.Eagerly, Chain.MAIN)

    override suspend fun getSecurityQuestions(): List<SecurityQuestion> {
        val questions = userWalletApiManager.walletApi.getSecurityQuestion().data.questions.map {
            SecurityQuestion(
                id = it.id, question = it.question, isAnswer = it.isAnswer ?: false
            )
        }
        ncDataStore.setSetupSecurityQuestion(questions.any { it.isAnswer })
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
            ncDataStore.setSetupSecurityQuestion(true)
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
                plan = plan,
                groupId = ""
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
                currencyUnit = it.currency,
                timeUnit = runCatching { SpendingTimeUnit.valueOf(it.interval) }.getOrElse { SpendingTimeUnit.DAILY })
        }
        return KeyPolicy(
            autoBroadcastTransaction = policy.autoBroadcastTransaction,
            signingDelayInSeconds = policy.signingDelaySeconds,
            spendingPolicy = spendingLimit
        )
    }

    override suspend fun getGroupServerKey(groupId: String, xfp: String): GroupKeyPolicy {
        val response = userWalletApiManager.groupWalletApi.getGroupServerKey(groupId, xfp)
        val policy =
            response.data.key?.policies ?: throw NullPointerException("Can not find key policy")
        return policy.toExternalModel()
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

    override suspend fun updateGroupServerKeys(
        signatures: Map<String, String>,
        groupId: String,
        keyIdOrXfp: String,
        token: String,
        securityQuestionToken: String,
        body: String
    ): String {
        val headers = mutableMapOf(VERIFY_TOKEN to token)
        if (securityQuestionToken.isNotEmpty()) {
            headers[SECURITY_QUESTION_TOKEN] = securityQuestionToken
        }
        signatures.map { (masterFingerprint, signature) ->
            nunchukNativeSdk.createRequestToken(signature, masterFingerprint)
        }.forEachIndexed { index, signerToken ->
            headers["$AUTHORIZATION_X-${index + 1}"] = signerToken
        }
        val response = userWalletApiManager.groupWalletApi.updateGroupServerKeys(
            headers = headers,
            keyId = keyIdOrXfp,
            groupId = groupId,
            body = gson.fromJson(body, KeyPolicyUpdateRequest::class.java)
        )
        return response.data.dummyTransaction?.id.orEmpty()
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
        val chatId = accountManager.getAccount().chatId
        val inheritanceTapSigner = membershipStepDao.getStep(
            chatId, chain.value, MembershipStep.HONEY_ADD_TAP_SIGNER
        )
        val signers = wallet.signers.map {
            mapToServerSignerDto(it, inheritanceTapSigner?.masterSignerId == it.masterFingerprint)
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
        val serverWallet = response.data.wallet ?: throw NullPointerException("Wallet empty")
        if (response.isSuccess) {
            assistedWalletDao.insert(
                AssistedWalletEntity(
                    localId = serverWallet.localId.orEmpty(),
                    plan = plan,
                    id = serverWallet.id?.toLongOrNull() ?: 0L
                )
            )
            membershipStepDao.deleteStepByChatId(chain.value, chatId)
            requestAddKeyDao.deleteRequests(chatId, chain.value)
        }
        return SeverWallet(serverWallet.id.orEmpty())
    }

    private fun mapToServerSignerDto(
        signer: SingleSigner, isInheritanceKey: Boolean
    ) = if (signer.type == SignerType.NFC) {
        val status = nunchukNativeSdk.getTapSignerStatusFromMasterSigner(signer.masterSignerId)
        SignerServerDto(
            name = signer.name,
            xfp = signer.masterFingerprint,
            derivationPath = signer.derivationPath,
            xpub = signer.xpub,
            pubkey = signer.publicKey,
            type = SignerType.NFC.name,
            tapsigner = TapSignerDto(
                cardId = status.ident.orEmpty(),
                version = status.version.orEmpty(),
                birthHeight = status.birthHeight,
                isTestnet = status.isTestNet,
                isInheritance = isInheritanceKey
            ),
            tags = if (isInheritanceKey) listOf(
                SignerTag.INHERITANCE.name
            ) else null
        )
    } else {
        SignerServerDto(name = signer.name,
            xfp = signer.masterFingerprint,
            derivationPath = signer.derivationPath,
            xpub = signer.xpub,
            pubkey = signer.publicKey,
            type = signer.type.name,
            tags = signer.tags.map { it.name })
    }

    override suspend fun getServerWallet(): WalletServerSync {
        val result = userWalletApiManager.walletApi.getServerWallet()
        val assistedKeys = mutableSetOf<String>()
        val partition = result.data.wallets.partition { it.status == WALLET_ACTIVE_STATUS }
        var deleteCount = 0
        if (partition.second.isNotEmpty()) {
            partition.second.filter { it.status == WALLET_DELETED_STATUS }
                .forEach { runCatching { nunchukNativeSdk.deleteWallet(it.localId.orEmpty()) } }
            deleteCount =
                assistedWalletDao.deleteBatch(partition.second.map { it.localId.orEmpty() })
        }
        if (partition.first.isNotEmpty()) {
            assistedWalletDao.insert(partition.first.map { wallet ->
                AssistedWalletEntity(
                    localId = wallet.localId.orEmpty(),
                    plan = wallet.slug.toMembershipPlan(),
                    id = wallet.id?.toLongOrNull() ?: 0L
                )
            })
        }
        var isNeedReload = false

        val keyPolicyMap = hashMapOf<String, KeyPolicy>()
        result.data.wallets.filter {
            it.localId.isNullOrEmpty().not() && it.status != WALLET_DELETED_STATUS
        }.forEach { walletServer ->
            keyPolicyMap[walletServer.localId.orEmpty()] = KeyPolicy(
                walletServer.serverKeyDto?.policies?.autoBroadcastTransaction ?: false,
                (walletServer.serverKeyDto?.policies?.signingDelaySeconds
                    ?: 0) / ONE_HOUR_TO_SECONDS
            )
            if (saveWalletToLib(walletServer, assistedKeys)) isNeedReload = true
        }
        val planWalletCreated = hashMapOf<String, String>()
        ncDataStore.setAssistedKey(assistedKeys)
        result.data.wallets.forEach { planWalletCreated[it.slug.orEmpty()] = it.localId.orEmpty() }
        return WalletServerSync(
            keyPolicyMap = keyPolicyMap,
            isNeedReload = isNeedReload || deleteCount > 0
        )
    }

    private fun saveWalletToLib(
        walletServer: WalletDto, assistedKeys: MutableSet<String>
    ): Boolean {
        var isNeedReload = false
        if (nunchukNativeSdk.hasWallet(walletServer.localId.orEmpty()).not()) {
            isNeedReload = true
            walletServer.signerServerDtos.forEach { signer ->
                saveServerSignerIfNeed(signer)
            }

            val wallet = nunchukNativeSdk.parseWalletDescriptor(walletServer.bsms.orEmpty()).apply {
                name = walletServer.name.orEmpty()
                description = walletServer.description.orEmpty()
            }
            nunchukNativeSdk.createWallet2(wallet)
        }

        walletServer.signerServerDtos.forEach { signer ->
            val type = nunchukNativeSdk.signerTypeFromStr(signer.type.orEmpty())
            val tags = signer.tags.orEmpty().mapNotNull { it.toSignerTag() }
            if (type != SignerType.SERVER) {
                assistedKeys.add(signer.xfp.orEmpty())
                if (type.isServerMasterSigner) {
                    val masterSigner = nunchukNativeSdk.getMasterSigner(signer.xfp.orEmpty())
                    val isChange = masterSigner.name != signer.name || masterSigner.tags != tags
                    if (isChange) {
                        isNeedReload = true
                        nunchukNativeSdk.updateMasterSigner(
                            masterSigner.copy(
                                name = signer.name.orEmpty(), tags = tags
                            )
                        )
                    }
                } else {
                    val remoteSigner = nunchukNativeSdk.getRemoteSigner(
                        signer.xfp.orEmpty(), signer.derivationPath.orEmpty()
                    )
                    val isChange = remoteSigner.name != signer.name || remoteSigner.tags != tags
                    if (isChange) {
                        isNeedReload = true
                        nunchukNativeSdk.updateRemoteSigner(
                            remoteSigner.copy(
                                name = signer.name.orEmpty(), tags = tags
                            )
                        )
                    }
                }
            }
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
        return isNeedReload
    }

    private fun saveServerSignerIfNeed(signer: SignerServerDto) {
        val tapsigner = signer.tapsigner
        if (tapsigner != null) {
            nunchukNativeSdk.addTapSigner(
                cardId = tapsigner.cardId,
                name = signer.name.orEmpty(),
                xfp = signer.xfp.orEmpty(),
                version = tapsigner.version,
                brithHeight = tapsigner.birthHeight,
                isTestNet = tapsigner.isTestnet
            )
        } else {
            val type = nunchukNativeSdk.signerTypeFromStr(signer.type.orEmpty())
            if (nunchukNativeSdk.hasSigner(
                    SingleSigner(
                        name = signer.name.orEmpty(),
                        xpub = signer.xpub.orEmpty(),
                        publicKey = signer.pubkey.orEmpty(),
                        derivationPath = signer.derivationPath.orEmpty(),
                        masterFingerprint = signer.xfp.orEmpty(),
                    )
                ).not()
            ) {
                nunchukNativeSdk.createSigner(name = signer.name.orEmpty(),
                    xpub = signer.xpub.orEmpty(),
                    publicKey = signer.pubkey.orEmpty(),
                    derivationPath = signer.derivationPath.orEmpty(),
                    masterFingerprint = signer.xfp.orEmpty(),
                    type = type,
                    tags = signer.tags.orEmpty().mapNotNull { tag -> tag.toSignerTag() })
            }
        }
    }


    override suspend fun updateServerWallet(walletLocalId: String, name: String): SeverWallet {
        val response = userWalletApiManager.walletApi.updateWallet(
            walletLocalId, UpdateWalletPayload(name = name)
        )
        val wallet = response.data.wallet ?: throw NullPointerException("Wallet empty")
        return SeverWallet(wallet.id.orEmpty())
    }

    override suspend fun updateServerKey(xfp: String, name: String): Boolean {
        return userWalletApiManager.walletApi.updateKeyName(xfp, UpdateKeyPayload(name)).isSuccess
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
        return response.data.toBackupKey()
    }

    override suspend fun verifiedPasswordToken(targetAction: String, password: String): String? {
        val response = membershipApi.verifiedPasswordToken(
            targetAction, VerifiedPasswordTokenRequest(password)
        )
        return response.data.token.token
    }

    override suspend fun verifiedPKeyToken(
        targetAction: String, address: String, signature: String
    ): String? {
        val response = membershipApi.verifiedPKeyToken(
            targetAction, VerifiedPKeyTokenRequest(address = address, signature = signature)
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
        return response.data.result.toCalculateRequiredSignatures()
    }

    override suspend fun calculateRequiredSignaturesUpdateKeyPolicy(
        xfp: String, walletId: String, keyPolicy: KeyPolicy
    ): CalculateRequiredSignatures {
        val response = userWalletApiManager.walletApi.calculateRequiredSignaturesUpdateServerKey(
            xfp, CreateServerKeysPayload(
                walletId = walletId, keyPoliciesDtoPayload = keyPolicy.toDto(), name = null
            )
        )
        return response.data.result.toCalculateRequiredSignatures()
    }

    override suspend fun calculateRequiredSignaturesUpdateGroupKeyPolicy(
        xfp: String,
        walletId: String,
        groupId: String,
        keyPolicy: GroupKeyPolicy
    ): CalculateRequiredSignatures {
        val response =
            userWalletApiManager.groupWalletApi.calculateRequiredSignaturesUpdateGroupServerKey(
                groupId = groupId,
                id = xfp,
                payload = CreateServerKeysPayload(
                    walletId = walletId, keyPoliciesDtoPayload = keyPolicy.toDto(), name = null
                ),
            )
        return response.data.result.toCalculateRequiredSignatures()
    }

    override suspend fun calculateRequiredSignaturesLockdown(
        walletId: String, periodId: String
    ): CalculateRequiredSignatures {
        val response = userWalletApiManager.walletApi.calculateRequiredSignaturesLockdown(
            LockdownUpdateRequest.Body(
                walletId = walletId, periodId = periodId
            )
        )
        return response.data.result.toCalculateRequiredSignatures()
    }

    override suspend fun generateInheritanceUserData(
        note: String,
        notificationEmails: List<String>,
        notifyToday: Boolean,
        activationTimeMilis: Long,
        bufferPeriodId: String?,
        walletId: String,
        groupId: String?
    ): String {
        val body = CreateUpdateInheritancePlanRequest.Body(
            note = note,
            notifyToday = notifyToday,
            notificationEmails = notificationEmails,
            activationTimeMilis = activationTimeMilis,
            bufferPeriodId = bufferPeriodId,
            walletId = walletId,
            groupId = groupId
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
        magic: String, address: String, feeRate: String
    ): String {
        val body = InheritanceClaimCreateTransactionRequest.Body(
            magic = magic, address = address, feeRate = feeRate
        )
        val nonce = getNonce()
        val request = InheritanceClaimCreateTransactionRequest(
            nonce = nonce, body = body
        )
        return gson.toJson(request)
    }

    override suspend fun inheritanceClaimStatus(
        userData: String, masterFingerprint: String, signature: String
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
        userData: String, masterFingerprint: String, signature: String
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
        return response.data.result.toCalculateRequiredSignatures()
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
        return response.data.toBackupKey()
    }

    override suspend fun inheritanceClaimingClaim(
        magic: String, psbt: String
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
        walletId: String, psbt: String, note: String?
    ) {
        val response = userWalletApiManager.walletApi.createTransaction(
            walletId, CreateOrUpdateServerTransactionRequest(
                note = note, psbt = psbt
            )
        )
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun updateServerTransaction(
        walletId: String,
        txId: String,
        note: String?,
    ) {
        val response = userWalletApiManager.walletApi.updateTransaction(
            walletId, txId, CreateOrUpdateServerTransactionRequest(
                note = note,
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

    override suspend fun getInheritance(walletId: String, groupId: String?): Inheritance {
        val response = userWalletApiManager.walletApi.getInheritance(walletId, groupId)
        if (response.data.inheritance == null) throw NullPointerException("Can not get inheritance")
        else return response.data.inheritance!!.toInheritance().also {
            markSetupInheritance(walletId, it.status != InheritanceStatus.PENDING_CREATION)
        }
    }

    private suspend fun markSetupInheritance(walletId: String, isSetupInheritance: Boolean) {
        val entity = assistedWalletDao.getById(walletId) ?: return
        if (entity.isSetupInheritance != isSetupInheritance) {
            assistedWalletDao.updateOrInsert(entity.copy(isSetupInheritance = isSetupInheritance))
        }
    }

    private suspend fun handleServerTransaction(
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
            val libTx = nunchukNativeSdk.importPsbt(walletId, transaction.psbt.orEmpty())
            if (libTx.psbt != transaction.psbt) {
                userWalletApiManager.walletApi.syncTransaction(
                    walletId, transactionId, SyncTransactionRequest(psbt = libTx.psbt)
                )
            }
            libTx
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

    override suspend fun generateUpdateGroupServerKey(
        walletId: String,
        keyPolicy: GroupKeyPolicy
    ): String {
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
        walletId: String, transactionId: String
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
            val response = userWalletApiManager.walletApi.getTransactionsToSync(walletId, index)
            if (response.isSuccess.not()) throw response.error
            response.data.transactions.forEach { transition ->
                if (transition.psbt.isNullOrEmpty().not()) {
                    val importTx = nunchukNativeSdk.importPsbt(walletId, transition.psbt.orEmpty())
                    if (transition.note.isNullOrEmpty().not() && importTx.memo != transition.note) {
                        nunchukNativeSdk.updateTransactionMemo(
                            walletId, importTx.txId, transition.note.orEmpty()
                        )
                    }
                    updateScheduleTransactionIfNeed(
                        walletId, transition.transactionId.orEmpty(), transition
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
        walletId: String, transactionId: String, transaction: TransactionServerDto
    ) {
        if (transaction.type == ServerTransactionType.SCHEDULED && transaction.broadCastTimeMillis > System.currentTimeMillis()) {
            nunchukNativeSdk.updateTransactionSchedule(
                walletId, transactionId, transaction.broadCastTimeMillis / 1000
            )
        }
    }

    override suspend fun calculateRequiredSignaturesDeleteAssistedWallet(walletId: String): CalculateRequiredSignatures {
        val response =
            userWalletApiManager.walletApi.calculateRequiredSignaturesDeleteAssistedWallet(walletId)
        return response.data.result.toCalculateRequiredSignatures()
    }

    override suspend fun deleteAssistedWallet(
        authorizations: List<String>,
        verifyToken: String,
        securityQuestionToken: String,
        walletId: String
    ) {
        val nonce = getNonce()
        val request = DeleteAssistedWalletRequest(nonce = nonce)
        val headers = mutableMapOf<String, String>()
        authorizations.forEachIndexed { index, value ->
            headers["$AUTHORIZATION_X-${index + 1}"] = value
        }
        headers[VERIFY_TOKEN] = verifyToken
        headers[SECURITY_QUESTION_TOKEN] = securityQuestionToken
        userWalletApiManager.walletApi.deleteAssistedWallet(walletId, headers, request)
        assistedWalletDao.deleteBatch(listOf(walletId))
    }

    override suspend fun getAssistedWalletConfig(): AssistedWalletConfig {
        val response = userWalletApiManager.walletApi.getAssistedWalletConfig()
        if (response.data.remainingWalletCount == 0) {
            val chatId = accountManager.getAccount().chatId
            membershipStepDao.deleteStepByChatId(chain.value, chatId)
            requestAddKeyDao.deleteRequests(chatId, chain.value)
        }
        return AssistedWalletConfig(
            totalAllowedWallet = response.data.totalAllowedWallet,
            activeWalletCount = response.data.activeWalletCount,
            remainingWalletCount = response.data.remainingWalletCount
        )
    }

    override suspend fun getGroupAssistedWalletConfig(): GroupConfig {
        val response = userWalletApiManager.walletApi.getGroupAssistedWalletConfig()
        return GroupConfig(
            totalAllowedGroup = response.data.totalAllowedGroup,
            activeGroupCount = response.data.activeGroupCount,
            remainingGroupCount = response.data.remainingGroupCount
        )
    }

    override fun getAssistedWalletsLocal(): Flow<List<AssistedWalletBrief>> {
        return assistedWalletDao.getAssistedWallets().map { list ->
            list.map { wallet ->
                AssistedWalletBrief(
                    localId = wallet.localId,
                    plan = wallet.plan,
                    isSetupInheritance = wallet.isSetupInheritance,
                    isRegisterAirgap = wallet.isRegisterAirgap,
                    isRegisterColdcard = wallet.isRegisterColdcard,
                    groupId = wallet.groupId
                )
            }
        }
    }

    override suspend fun clearLocalData() {
        assistedWalletDao.deleteAll()
    }

    override suspend fun reuseKeyWallet(walletId: String, plan: MembershipPlan) {
        val wallet = nunchukNativeSdk.getWallet(walletId)
        val verifyMap = coroutineScope {
            wallet.signers.filter { it.type == SignerType.NFC }.map { it.masterFingerprint }
                .map { key ->
                    async {
                        userWalletApiManager.walletApi.getKey(key)
                    }
                }.awaitAll().associateBy { it.data.keyXfp }
                .mapValues { it.value.data.verificationType }
        }
        var inheritanceKey: SingleSigner? = null
        if (plan == MembershipPlan.HONEY_BADGER) {
            inheritanceKey = wallet.signers.find {
                it.type == SignerType.NFC && nunchukNativeSdk.getMasterSigner(it.masterFingerprint).tags.contains(
                    SignerTag.INHERITANCE
                )
            } ?: throw NullPointerException("Can not find inheritance key")
            membershipRepository.saveStepInfo(
                MembershipStepInfo(
                    step = MembershipStep.HONEY_ADD_TAP_SIGNER,
                    masterSignerId = inheritanceKey.masterFingerprint,
                    plan = plan,
                    verifyType = verifyMap[inheritanceKey.masterFingerprint].toVerifyType(),
                    extraData = gson.toJson(
                        SignerExtra(
                            derivationPath = inheritanceKey.derivationPath,
                            isAddNew = false,
                            signerType = inheritanceKey.type
                        )
                    ),
                    groupId = ""
                )
            )
        }

        val steps = if (plan == MembershipPlan.IRON_HAND) {
            listOf(MembershipStep.IRON_ADD_HARDWARE_KEY_1, MembershipStep.IRON_ADD_HARDWARE_KEY_2)
        } else {
            listOf(MembershipStep.HONEY_ADD_HARDWARE_KEY_1, MembershipStep.HONEY_ADD_HARDWARE_KEY_2)
        }
        wallet.signers.filter { it.type != SignerType.SERVER && it != inheritanceKey }
            .forEachIndexed { index, singleSigner ->
                membershipRepository.saveStepInfo(
                    MembershipStepInfo(
                        step = steps[index],
                        masterSignerId = singleSigner.masterFingerprint,
                        plan = plan,
                        verifyType = if (singleSigner.type == SignerType.NFC) verifyMap[singleSigner.masterFingerprint].toVerifyType() else VerifyType.APP_VERIFIED,
                        extraData = gson.toJson(
                            SignerExtra(
                                derivationPath = singleSigner.derivationPath,
                                isAddNew = false,
                                signerType = singleSigner.type
                            )
                        ),
                        groupId = ""
                    )
                )
            }
    }

    override suspend fun getCoinControlData(walletId: String): String {
        return userWalletApiManager.walletApi.getCoinControlData(walletId).data.data.orEmpty()
    }

    override suspend fun uploadCoinControlData(walletId: String, data: String) {
        userWalletApiManager.walletApi.uploadCoinControlData(walletId, CoinDataContent(data))
    }

    override suspend fun clearTransactionEmergencyLockdown(walletId: String) {
        (0 until Int.MAX_VALUE step TRANSACTION_PAGE_COUNT).forEach { index ->
            val response = userWalletApiManager.walletApi.getTransactionsToDelete(walletId, index)
            if (response.isSuccess.not()) throw response.error
            response.data.transactions.forEach { transition ->
                transition.transactionId?.let {
                    nunchukNativeSdk.deleteTransaction(
                        walletId = walletId, txId = transition.transactionId
                    )
                }
            }
            if (response.data.transactions.size < TRANSACTION_PAGE_COUNT) return
        }
    }

    override suspend fun getPermissionGroupWallet(): DefaultPermissions {
        val response = userWalletApiManager.groupWalletApi.getPermissionGroupWallet()
        if (response.isSuccess.not()) {
            throw response.error
        }
        val permissionsResponse = response.data.defaultPermissions ?: hashMapOf()
        val permissions = permissionsResponse.map {
            it.key to it.value.filter { permission -> permission.hidden == false }.map { data ->
                DefaultPermissions.Data(
                    alternativeNames = data.alternativeNames ?: hashMapOf(),
                    hidden = data.hidden.orFalse(),
                    name = data.name.orEmpty(),
                    slug = data.slug.orEmpty(),
                )
            }
        }.toMap()
        return DefaultPermissions(permissions = permissions)
    }

    override fun assistedKeys(): Flow<Set<String>> {
        return ncDataStore.assistedKeys.combine(ncDataStore.groupAssistedKeys) { keys, groupKeys ->
            keys + groupKeys
        }
    }

    override suspend fun updateServerKeyName(xfp: String, name: String) {
        val response = userWalletApiManager.walletApi.updateKeyName(xfp, UpdateKeyPayload(name))
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun syncGroupDraftWallet(groupId: String): DraftWallet {
        val response = userWalletApiManager.groupWalletApi.getDraftWallet(groupId)
        val draftWallet =
            response.data.draftWallet ?: throw NullPointerException("draftWallet null")
        val chatId = accountManager.getAccount().chatId
        draftWallet.signers.forEach { key ->
            val signerType = key.type.toSignerType()
            if (signerType == SignerType.SERVER) {
                if (membershipStepDao.getStep(
                        chatId, chain.value, MembershipStep.ADD_SEVER_KEY, groupId
                    ) == null
                ) {
                    membershipRepository.saveStepInfo(
                        MembershipStepInfo(
                            step = MembershipStep.ADD_SEVER_KEY,
                            verifyType = VerifyType.APP_VERIFIED,
                            extraData = gson.toJson(
                                ServerKeyExtra(
                                    name = key.name.orEmpty(),
                                    xfp = key.xfp.orEmpty(),
                                    derivationPath = key.derivationPath.orEmpty(),
                                    xpub = key.xpub.orEmpty()
                                )
                            ),
                            plan = MembershipPlan.BYZANTINE,
                            keyIdInServer = draftWallet.serverKeyId.orEmpty(),
                            groupId = groupId
                        )
                    )
                }
            } else {
                val step = when (key.index) {
                    0 -> if (draftWallet.walletConfig?.n == 4) MembershipStep.BYZANTINE_ADD_TAP_SIGNER else MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0
                    1 -> MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1
                    2 -> MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2
                    3 -> MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3
                    4 -> MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4
                    else -> throw IllegalArgumentException()
                }
                val info = membershipStepDao.getStep(chatId, chain.value, step, groupId)
                if (info == null || info.masterSignerId != key.xfp) {
                    membershipRepository.saveStepInfo(
                        MembershipStepInfo(
                            step = step,
                            masterSignerId = key.xfp.orEmpty(),
                            plan = MembershipPlan.BYZANTINE,
                            verifyType = if (signerType == SignerType.NFC) key.tapsignerKey?.verificationType.toVerifyType() else VerifyType.APP_VERIFIED,
                            extraData = gson.toJson(
                                SignerExtra(
                                    derivationPath = key.derivationPath.orEmpty(),
                                    isAddNew = false,
                                    signerType = signerType
                                )
                            ),
                            groupId = groupId
                        )
                    )
                }
            }
            saveServerSignerIfNeed(key)
        }
        return DraftWallet(
            draftWallet.walletConfig.toModel(),
            draftWallet.signers.map { it.toModel() }
        )
    }

    override suspend fun createGroupServerKey(
        groupId: String, name: String, groupKeyPolicy: GroupKeyPolicy
    ) {
        val response = userWalletApiManager.groupWalletApi.createGroupServerKey(
            groupId,
            CreateServerKeysPayload(keyPoliciesDtoPayload = groupKeyPolicy.toDto(), name = name)
        )
        val serverKeyId =
            response.data.key?.id ?: throw NullPointerException("Can not generate server key")
        val setServerKeyResponse = userWalletApiManager.groupWalletApi.setGroupServerKey(
            groupId, mapOf("server_key_id" to serverKeyId)
        )
        val key = response.data.key ?: throw NullPointerException("Response from server empty")
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
                plan = MembershipPlan.BYZANTINE,
                groupId = groupId
            )
        )
        if (setServerKeyResponse.isSuccess.not()) {
            throw setServerKeyResponse.error
        }
    }

    override suspend fun syncKeyToGroup(
        groupId: String, step: MembershipStep, signer: SingleSigner
    ) {
        val index = step.toIndex()
        val signerDto = mapToServerSignerDto(
            signer, step == MembershipStep.BYZANTINE_ADD_TAP_SIGNER
        ).copy(index = index)
        val response = userWalletApiManager.groupWalletApi.addKeyToServer(groupId, signerDto)
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun requestAddKey(
        groupId: String,
        step: MembershipStep,
        tags: List<SignerTag>
    ): String {
        val chatId = accountManager.getAccount().chatId
        var localRequest =
            requestAddKeyDao.getRequest(chatId, chain.value, step, tags.joinToString(), groupId)
        if (localRequest != null) {
            val response =
                if (groupId.isNotEmpty())
                    userWalletApiManager.groupWalletApi.getRequestAddKeyStatus(
                        groupId,
                        localRequest.requestId
                    )
                else
                    userWalletApiManager.walletApi.getRequestAddKeyStatus(localRequest.requestId)
            if (response.data.request == null) {
                requestAddKeyDao.delete(localRequest)
                localRequest = null
            }
        }
        return if (localRequest == null) {
            val response = if (groupId.isNotEmpty())
                userWalletApiManager.groupWalletApi.requestAddKey(
                    groupId,
                    DesktopKeyRequest(tags.map { it.name }, keyIndex = step.toIndex())
                )
            else
                userWalletApiManager.walletApi.requestAddKey(
                    DesktopKeyRequest(
                        tags.map { it.name },
                        keyIndex = step.toIndex()
                    )
                )
            val requestId = response.data.request?.id.orEmpty()
            requestAddKeyDao.insert(
                RequestAddKeyEntity(
                    requestId = requestId,
                    chain = chain.value,
                    chatId = chatId,
                    step = step,
                    tag = tags.joinToString()
                )
            )
            requestId
        } else {
            userWalletApiManager.walletApi.pushRequestAddKey(localRequest.requestId)
            localRequest.requestId
        }
    }

    override suspend fun checkKeyAdded(
        plan: MembershipPlan,
        groupId: String,
        requestId: String?
    ): Boolean {
        val chatId = accountManager.getAccount().chatId
        val localRequests = if (requestId == null) {
            requestAddKeyDao.getRequests(chatId, chain.value, groupId)
        } else {
            requestAddKeyDao.getRequest(requestId)?.let { listOf(it) }.orEmpty()
        }

        localRequests.forEach { localRequest ->
            val response = if (groupId.isNotEmpty()) {
                userWalletApiManager.groupWalletApi.getRequestAddKeyStatus(
                    groupId,
                    localRequest.requestId
                )
            } else {
                userWalletApiManager.walletApi.getRequestAddKeyStatus(localRequest.requestId)
            }
            val request = response.data.request
            val key = request?.key
            if (request?.status == "COMPLETED" && key != null) {
                val type = nunchukNativeSdk.signerTypeFromStr(key.type.orEmpty())
                nunchukNativeSdk.createSigner(
                    name = key.name.orEmpty(),
                    xpub = key.xpub.orEmpty(),
                    publicKey = key.pubkey.orEmpty(),
                    derivationPath = key.derivationPath.orEmpty(),
                    masterFingerprint = key.xfp.orEmpty(),
                    type = type,
                    tags = key.tags.orEmpty().mapNotNull { tag -> tag.toSignerTag() }
                )
                membershipRepository.saveStepInfo(
                    MembershipStepInfo(
                        step = localRequest.step,
                        masterSignerId = key.xfp.orEmpty(),
                        plan = plan,
                        verifyType = VerifyType.APP_VERIFIED,
                        extraData = gson.toJson(
                            SignerExtra(
                                derivationPath = key.derivationPath.orEmpty(),
                                isAddNew = false,
                                signerType = type
                            )
                        ),
                        groupId = ""
                    )
                )
                requestAddKeyDao.delete(localRequest)
                if (requestId != null) return true
            } else if (request == null) {
                requestAddKeyDao.delete(localRequest)
                throw RequestAddKeyCancelException
            }
        }

        return false
    }

    override suspend fun deleteDraftWallet() {
        userWalletApiManager.walletApi.deleteDraftWallet()
        requestAddKeyDao.deleteRequests(accountManager.getAccount().chatId, chain.value)
    }

    override suspend fun cancelRequestIdIfNeed(groupId: String, step: MembershipStep) {
        val account = accountManager.getAccount()
        val entity = requestAddKeyDao.getRequest(account.chatId, chain.value, step, groupId)
        if (entity != null) {
            if (groupId.isNotEmpty()) {
                userWalletApiManager.groupWalletApi.cancelRequestAddKey(groupId, entity.requestId)
            } else {
                userWalletApiManager.walletApi.cancelRequestAddKey(entity.requestId)
            }
            requestAddKeyDao.delete(entity)
        }
    }

    override suspend fun createGroup(
        m: Int,
        n: Int,
        requiredServerKey: Boolean,
        allowInheritance: Boolean,
        setupPreference: String,
        members: List<AssistedMember>
    ): ByzantineGroup {
        val response = userWalletApiManager.groupWalletApi.createGroup(
            CreateGroupRequest(
                walletConfig = WalletConfigRequest(allowInheritance, m, n, requiredServerKey),
                setupPreference = setupPreference,
                members = members.map {
                    MemberRequest(
                        emailOrUsername = it.email, permissions = emptyList(), role = it.role
                    )
                })
        )
        val groupResponse = response.data.data ?: throw NullPointerException("Can not create group")
        val group = groupResponse.toByzantineGroup()
        groupDao.updateOrInsert(
            groupResponse.toGroupEntity(accountManager.getAccount().chatId)
        )
        return group
    }

    override suspend fun getWalletConstraints(): WalletConstraints {
        val response = userWalletApiManager.groupWalletApi.getGroupWalletsConstraints()
        return WalletConstraints(maximumKeyholder = response.data.data?.maximumKeyholder ?: 0)
    }

    override fun getGroupBriefs(): Flow<List<ByzantineGroupBrief>> =
        groupDao.getGroups(chatId = accountManager.getAccount().chatId).map {
            val groupBriefs = it.map { group ->
                val type = object : TypeToken<List<ByzantineMemberBrief>>() {}.type
                val members = gson.fromJson<List<ByzantineMemberBrief>>(group.members, type)
                ByzantineGroupBrief(
                    groupId = group.groupId,
                    status = group.status,
                    members = members,
                    createdTimeMillis = group.createdTimeMillis
                )
            }
            groupBriefs
        }

    override fun getGroupBriefById(groupId: String): Flow<ByzantineGroupBrief> {
        return groupDao.getById(groupId, chatId = accountManager.getAccount().chatId).map { group ->
            val type = object : TypeToken<List<ByzantineMemberBrief>>() {}.type
            val members = gson.fromJson<List<ByzantineMemberBrief>>(group.members, type)
            ByzantineGroupBrief(
                groupId = group.groupId,
                status = group.status,
                members = members,
                createdTimeMillis = group.createdTimeMillis
            )
        }
    }

    override suspend fun syncGroupWallets(): Boolean {
        val response = userWalletApiManager.groupWalletApi.getGroups()
        var shouldReload = false
        val groupAssistedKeys = mutableSetOf<String>()
        val groups = response.data.groups.orEmpty()
        if (groups.isNotEmpty()) {
            syncGroup(groups)
            groups.forEach {
                if (it.status == "PENDING_WALLET") {
                    syncGroupDraftWallet(it.id.orEmpty())
                } else if (it.status == "ACTIVE") {
                    if (syncGroupWallet(it.id.orEmpty(), groupAssistedKeys)) shouldReload = true
                }
            }
        }

        ncDataStore.setGroupAssistedKey(groupAssistedKeys)
        return shouldReload
    }

    private suspend fun syncGroup(groups: List<GroupResponse>) {
        val groupLocals =
            groupDao.getGroups(accountManager.getAccount().chatId).firstOrNull() ?: emptyList()
        val allGroupIds = groupLocals.map { it.groupId }.toHashSet()
        val addGroupIds = HashSet<String>()
        val chatId = accountManager.getAccount().chatId
        groupDao.updateOrInsert(groups.filter { it.id.isNullOrEmpty().not() }.map { group ->
            addGroupIds.add(group.id!!)
            group.toGroupEntity(chatId)
        }.toList())
        allGroupIds.removeAll(addGroupIds)
        if (allGroupIds.isNotEmpty()) {
            groupDao.deleteGroups(allGroupIds.toList(), chatId = chatId)
        }
    }

    private fun GroupResponse.toGroupEntity(chatId: String): GroupEntity {
        val memberBrief = members.orEmpty().map {
            ByzantineMemberBrief(
                emailOrUsername = it.emailOrUsername.orEmpty(),
                role = it.role.orEmpty(),
                inviterUserId = it.inviterUserId.orEmpty(),
                status = it.status.orEmpty(),
                avatar = it.user?.avatar,
                name = it.user?.name,
                email = it.user?.email,
                userId = it.user?.id
            )
        }
       groupDao.getGroupById(id.orEmpty(), chatId)?.let {
            return it.copy(
                groupId = id.orEmpty(),
                chatId = chatId,
                status = status.orEmpty(),
                createdTimeMillis = createdTimeMillis ?: 0,
                members = gson.toJson(memberBrief)
            )
        }
        return GroupEntity(
            groupId = id.orEmpty(),
            chatId = chatId,
            status = status.orEmpty(),
            createdTimeMillis = createdTimeMillis ?: 0,
            members = gson.toJson(memberBrief)
        )
    }

    override suspend fun getGroup(groupId: String): ByzantineGroup {
        val response = userWalletApiManager.groupWalletApi.getGroup(groupId)
        return response.data.data?.toByzantineGroup()
            ?: throw NullPointerException("Can not get group")
    }

    override suspend fun deleteGroupWallet(groupId: String) {
        userWalletApiManager.groupWalletApi.deleteDraftWallet(groupId)
        requestAddKeyDao.deleteRequests(groupId)
    }

    override suspend fun deleteGroup(groupId: String) {
        groupDao.deleteGroups(listOf(groupId), chatId = accountManager.getAccount().chatId)
    }

    override suspend fun generateEditGroupMemberUserData(
        members: List<AssistedMember>
    ): String {
        val body = EditGroupMemberRequest.Body(members = members.map {
            MemberRequest(
                emailOrUsername = it.email, permissions = emptyList(), role = it.role
            )
        })
        return gson.toJson(body)
    }

    override suspend fun calculateRequiredSignaturesEditGroupMember(
        groupId: String, members: List<AssistedMember>
    ): CalculateRequiredSignatures {
        val response = userWalletApiManager.groupWalletApi.calculateRequiredSignaturesEditMember(
            groupId,
            EditGroupMemberRequest.Body(members = members.map {
                MemberRequest(
                    emailOrUsername = it.email, permissions = emptyList(), role = it.role
                )
            })
        )
        return response.data.result.toCalculateRequiredSignatures()
    }

    override suspend fun editGroupMember(
        groupId: String,
        authorizations: List<String>,
        verifyToken: String,
        members: List<AssistedMember>,
        securityQuestionToken: String
    ): ByzantineGroup {
        val nonce = getNonce()
        val request = EditGroupMemberRequest(
            nonce = nonce, body = EditGroupMemberRequest.Body(members = members.map {
                MemberRequest(
                    emailOrUsername = it.email, permissions = emptyList(), role = it.role
                )
            })
        )
        val headers = mutableMapOf<String, String>()
        authorizations.forEachIndexed { index, value ->
            headers["$AUTHORIZATION_X-${index + 1}"] = value
        }
        headers[VERIFY_TOKEN] = verifyToken
        headers[SECURITY_QUESTION_TOKEN] = securityQuestionToken
        val response =
            userWalletApiManager.groupWalletApi.editGroupMember(groupId, headers, request)
        return response.data.data?.toByzantineGroup()
            ?: throw NullPointerException("Can not get group")
    }

    override suspend fun groupMemberAcceptRequest(groupId: String) {
        val response = userWalletApiManager.groupWalletApi.groupMemberAcceptRequest(groupId)
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun groupMemberDenyRequest(groupId: String) {
        val response = userWalletApiManager.groupWalletApi.groupMemberDenyRequest(groupId)
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun createGroupWallet(groupId: String, name: String): Wallet {
        val response =
            userWalletApiManager.groupWalletApi.createGroupWallet(groupId, mapOf("name" to name))
        val wallet = response.data.wallet ?: throw NullPointerException("Wallet empty")
        saveWalletToLib(wallet, mutableSetOf())
        assistedWalletDao.insert(
            AssistedWalletEntity(
                localId = wallet.localId.orEmpty(),
                plan = wallet.slug.toMembershipPlan(),
                id = wallet.id?.toLongOrNull() ?: 0L,
                groupId = groupId
            )
        )
        membershipStepDao.deleteStepByChatId(chain.value, accountManager.getAccount().chatId)
        return nunchukNativeSdk.getWallet(wallet.localId.orEmpty())
    }

    override suspend fun syncGroupWallet(
        groupId: String,
        groupAssistedKeys: MutableSet<String>
    ): Boolean {
        val response = userWalletApiManager.groupWalletApi.getGroupWallet(groupId)
        val wallet = response.data.wallet ?: throw NullPointerException("Wallet empty")
        membershipStepDao.deleteStepByGroupId(groupId)
        requestAddKeyDao.deleteRequests(groupId)
        assistedWalletDao.insert(
            AssistedWalletEntity(
                localId = wallet.localId.orEmpty(),
                plan = wallet.slug.toMembershipPlan(),
                id = wallet.id?.toLongOrNull() ?: 0L,
                groupId = groupId
            )
        )
        return saveWalletToLib(wallet, groupAssistedKeys)
    }

    override suspend fun getAlerts(groupId: String): List<Alert> {
        val alerts = arrayListOf<Alert>()
        (0 until Int.MAX_VALUE step TRANSACTION_PAGE_COUNT).forEach { index ->
            val response = userWalletApiManager.groupWalletApi.getAlerts(groupId, offset = index)
            if (response.isSuccess.not()) throw response.error
            val alertList = response.data.alerts.orEmpty().map { it.toAlert() }
            alerts.addAll(alertList)
            if (response.data.alerts.orEmpty().size < TRANSACTION_PAGE_COUNT) return alerts
        }
        return alerts
    }

    override suspend fun createOrUpdateGroupChat(
        groupId: String,
        historyPeriodId: String?
    ): GroupChat {
        val response = if (historyPeriodId == null) {
            userWalletApiManager.groupWalletApi.createGroupChat(
                groupId = groupId,
                CreateOrUpdateGroupChatRequest()
            )
        } else {
            userWalletApiManager.groupWalletApi.updateGroupChat(
                groupId = groupId,
                CreateOrUpdateGroupChatRequest(historyPeriodId)
            )
        }
        return response.data.chat.toGroupChat()
    }

    override suspend fun getGroupChat(groupId: String): GroupChat {
        val response = userWalletApiManager.groupWalletApi.getGroupChat(groupId)
        return response.data.chat.toGroupChat()
    }

    override suspend fun deleteGroupChat(groupId: String) {
        userWalletApiManager.groupWalletApi.deleteGroupChat(groupId)
    }

    override suspend fun getHistoryPeriod(): List<HistoryPeriod> {
        val response = userWalletApiManager.groupWalletApi.getHistoryPeriods()
        return response.data.periods.orEmpty().map { it.toHistoryPeriod() }
    }

    companion object {
        private const val WALLET_ACTIVE_STATUS = "ACTIVE"
        private const val WALLET_DELETED_STATUS = "DELETED"
        private const val VERIFY_TOKEN = "Verify-token"
        private const val SECURITY_QUESTION_TOKEN = "Security-Question-token"
        internal const val AUTHORIZATION_X = "AuthorizationX"
    }
}

