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

import android.util.LruCache
import com.google.gson.Gson
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
import com.nunchuk.android.core.data.model.byzantine.WalletConfigRequest
import com.nunchuk.android.core.data.model.byzantine.toModel
import com.nunchuk.android.core.data.model.coin.CoinDataContent
import com.nunchuk.android.core.data.model.membership.ConfirmationCodeRequest
import com.nunchuk.android.core.data.model.membership.ConfirmationCodeVerifyRequest
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
import com.nunchuk.android.core.data.model.membership.toServerTransaction
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.exception.RequestAddKeyCancelException
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.core.mapper.toAlert
import com.nunchuk.android.core.mapper.toBackupKey
import com.nunchuk.android.core.mapper.toByzantineGroup
import com.nunchuk.android.core.mapper.toCalculateRequiredSignatures
import com.nunchuk.android.core.mapper.toGroupChat
import com.nunchuk.android.core.mapper.toGroupEntity
import com.nunchuk.android.core.mapper.toHistoryPeriod
import com.nunchuk.android.core.mapper.toInheritance
import com.nunchuk.android.core.mapper.toMemberRequest
import com.nunchuk.android.core.mapper.toPeriod
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.toSignerTag
import com.nunchuk.android.core.util.ONE_HOUR_TO_SECONDS
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.BufferPeriodCountdown
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.DefaultPermissions
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.model.GroupStatus
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
import com.nunchuk.android.persistence.dao.AlertDao
import com.nunchuk.android.persistence.dao.AssistedWalletDao
import com.nunchuk.android.persistence.dao.GroupDao
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.persistence.dao.RequestAddKeyDao
import com.nunchuk.android.persistence.entity.AssistedWalletEntity
import com.nunchuk.android.persistence.entity.RequestAddKeyEntity
import com.nunchuk.android.repository.GroupWalletRepository
import com.nunchuk.android.repository.MembershipRepository
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.util.LoadingOptions
import com.nunchuk.android.utils.SERVER_KEY_NAME
import com.nunchuk.android.utils.isServerMasterSigner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
    private val alertDao: AlertDao,
    private val groupWalletRepository: GroupWalletRepository,
    private val pushEventManager: PushEventManager,
    private val serverTransactionCache: LruCache<String, ServerTransaction>,
    private val syncer: ByzantineSyncer,
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
        name: String, keyPolicy: KeyPolicy, plan: MembershipPlan,
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

    override suspend fun getServerKey(xfp: String, derivationPath: String): KeyPolicy {
        val response = userWalletApiManager.walletApi.getServerKey(xfp, derivationPath)
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

    override suspend fun getGroupServerKey(
        groupId: String,
        xfp: String,
        derivationPath: String,
    ): GroupKeyPolicy {
        val response =
            userWalletApiManager.groupWalletApi.getGroupServerKey(groupId, xfp, derivationPath)
        val policy =
            response.data.key?.policies ?: throw NullPointerException("Can not find key policy")
        return policy.toExternalModel()
    }

    override suspend fun updateServerKeys(
        signatures: Map<String, String>,
        keyIdOrXfp: String,
        derivationPath: String,
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
            headers = headers,
            keyId = keyIdOrXfp,
            derivationPath = derivationPath,
            body = gson.fromJson(body, KeyPolicyUpdateRequest::class.java)
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
        derivationPath: String,
        token: String,
        securityQuestionToken: String,
        body: String,
        draft: Boolean,
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
            derivationPath = derivationPath,
            groupId = groupId,
            body = gson.fromJson(body, KeyPolicyUpdateRequest::class.java),
            draft = draft
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
        wallet: Wallet, serverKeyId: String, plan: MembershipPlan,
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
        signer: SingleSigner, isInheritanceKey: Boolean,
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
            keyPolicyMap = keyPolicyMap, isNeedReload = isNeedReload || deleteCount > 0
        )
    }

    private fun saveWalletToLib(
        walletServer: WalletDto, assistedKeys: MutableSet<String>,
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
                    val isVisible = masterSigner.isVisible || signer.isVisible
                    val isChange =
                        masterSigner.name != signer.name || masterSigner.tags != tags || masterSigner.isVisible != isVisible
                    if (isChange) {
                        isNeedReload = true
                        nunchukNativeSdk.updateMasterSigner(
                            masterSigner.copy(
                                name = signer.name.orEmpty(),
                                tags = tags,
                                isVisible = isVisible
                            )
                        )
                    }
                } else {
                    val remoteSigner = nunchukNativeSdk.getRemoteSigner(
                        signer.xfp.orEmpty(), signer.derivationPath.orEmpty()
                    )
                    val isVisible = remoteSigner.isVisible || signer.isVisible
                    val isChange =
                        remoteSigner.name != signer.name || remoteSigner.tags != tags || remoteSigner.isVisible != isVisible
                    if (isChange) {
                        isNeedReload = true
                        nunchukNativeSdk.updateRemoteSigner(
                            remoteSigner.copy(
                                name = signer.name.orEmpty(),
                                tags = tags,
                                isVisible = isVisible
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


    override suspend fun updateServerWallet(
        walletLocalId: String,
        name: String,
        groupId: String?,
    ): SeverWallet {
        val response = if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.updateWallet(
                groupId, walletLocalId, UpdateWalletPayload(name = name)
            )
        } else {
            userWalletApiManager.walletApi.updateWallet(
                walletLocalId, UpdateWalletPayload(name = name)
            )
        }
        val wallet = response.data.wallet ?: throw NullPointerException("Wallet empty")
        return SeverWallet(wallet.id.orEmpty())
    }

    override suspend fun updateServerKey(xfp: String, name: String): Boolean {
        return userWalletApiManager.walletApi.updateKeyName(xfp, UpdateKeyPayload(name)).isSuccess
    }

    override suspend fun getServerTransaction(
        groupId: String?, walletId: String, transactionId: String,
    ): ExtendedTransaction {
        val transaction = runCatching {
            val response = if (!groupId.isNullOrEmpty()) {
                userWalletApiManager.groupWalletApi.getTransaction(groupId, walletId, transactionId)
            } else {
                userWalletApiManager.walletApi.getTransaction(walletId, transactionId)
            }
            response.data.transaction
        }.getOrNull()
        transaction?.let {
            updateScheduleTransactionIfNeed(walletId, transactionId, transaction)
        }
        val result = handleServerTransaction(groupId, walletId, transactionId, transaction)
        return ExtendedTransaction(
            transaction = result.first,
            serverTransaction = result.second?.toServerTransaction(),
        )
    }

    override suspend fun getOnlyServerTransaction(
        groupId: String?,
        walletId: String,
        transactionId: String,
    ): ServerTransaction {
        val response = if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.getTransaction(groupId, walletId, transactionId)
        } else {
            userWalletApiManager.walletApi.getTransaction(walletId, transactionId)
        }
        return response.data.transaction?.toServerTransaction()
            ?: throw NullPointerException("Transaction empty")
    }

    override suspend fun downloadBackup(
        id: String, questions: List<QuestionsAndAnswer>, verifyToken: String,
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
        targetAction: String, address: String, signature: String,
    ): String? {
        val response = membershipApi.verifiedPKeyToken(
            targetAction, VerifiedPKeyTokenRequest(address = address, signature = signature)
        )
        return response.data.token.token
    }

    override suspend fun calculateRequiredSignaturesSecurityQuestions(
        walletId: String, questions: List<QuestionsAndAnswer>,
    ): CalculateRequiredSignatures {
        val request = CalculateRequiredSignaturesSecurityQuestionPayload(walletId = walletId,
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
        xfp: String,
        derivationPath: String,
        walletId: String,
        keyPolicy: KeyPolicy,
    ): CalculateRequiredSignatures {
        val response = userWalletApiManager.walletApi.calculateRequiredSignaturesUpdateServerKey(
            id = xfp,
            derivationPath = derivationPath,
            CreateServerKeysPayload(
                walletId = walletId, keyPoliciesDtoPayload = keyPolicy.toDto(), name = null
            ),
        )
        return response.data.result.toCalculateRequiredSignatures()
    }

    override suspend fun calculateRequiredSignaturesUpdateGroupKeyPolicy(
        xfp: String,
        derivationPath: String,
        walletId: String,
        groupId: String,
        keyPolicy: GroupKeyPolicy,
    ): CalculateRequiredSignatures {
        val response =
            userWalletApiManager.groupWalletApi.calculateRequiredSignaturesUpdateGroupServerKey(
                groupId = groupId,
                id = xfp,
                derivationPath = derivationPath,
                payload = CreateServerKeysPayload(
                    walletId = walletId, keyPoliciesDtoPayload = keyPolicy.toDto(), name = null
                ),
            )
        return response.data.result.toCalculateRequiredSignatures()
    }

    override suspend fun calculateRequiredSignaturesLockdown(
        walletId: String, periodId: String,
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
        groupId: String?,
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
        magic: String, address: String, feeRate: String,
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
        userData: String, masterFingerprint: String, signature: String,
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
        userData: String, masterFingerprint: String, signature: String,
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

    override suspend fun generateCancelInheritanceUserData(
        walletId: String,
        groupId: String?,
    ): String {
        val body = InheritanceCancelRequest.Body(
            walletId = walletId, groupId = groupId
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
        isCancelInheritance: Boolean,
        groupId: String?,
    ): CalculateRequiredSignatures {
        val response = if (isCancelInheritance) {
            userWalletApiManager.walletApi.calculateRequiredSignaturesInheritance(
                CreateUpdateInheritancePlanRequest.Body(
                    walletId = walletId, groupId = groupId
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
                    bufferPeriodId = bufferPeriodId,
                    groupId = groupId
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
        plan: MembershipPlan,
        draft: Boolean,
    ): String {
        val request = gson.fromJson(userData, CreateUpdateInheritancePlanRequest::class.java)
        val headers = mutableMapOf<String, String>()
        authorizations.forEachIndexed { index, value ->
            headers["$AUTHORIZATION_X-${index + 1}"] = value
        }
        headers[VERIFY_TOKEN] = verifyToken
        headers[SECURITY_QUESTION_TOKEN] = securityQuestionToken
        val response = if (isUpdate) userWalletApiManager.walletApi.updateInheritance(
            headers, request, draft
        ) else userWalletApiManager.walletApi.createInheritance(headers, request, draft)
        if (response.isSuccess.not()) throw response.error
        if (request.body?.groupId == null) {
            response.data.inheritance?.walletLocalId?.also {
                markSetupInheritance(it, true)
            }
        }
        return response.data.dummyTransaction?.id.orEmpty()
    }

    override suspend fun cancelInheritance(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String,
        walletId: String,
        draft: Boolean,
    ): String {
        val request = gson.fromJson(userData, InheritanceCancelRequest::class.java)
        val headers = mutableMapOf<String, String>()
        authorizations.forEachIndexed { index, value ->
            headers["$AUTHORIZATION_X-${index + 1}"] = value
        }
        headers[VERIFY_TOKEN] = verifyToken
        headers[SECURITY_QUESTION_TOKEN] = securityQuestionToken
        val response = userWalletApiManager.walletApi.inheritanceCancel(headers, request, draft)
        if (response.isSuccess && request.body?.groupId == null) {
            markSetupInheritance(walletId, false)
        }
        return response.data.dummyTransaction?.id.orEmpty()
    }

    override suspend fun inheritanceClaimDownloadBackup(magic: String): BackupKey {
        val response = userWalletApiManager.walletApi.inheritanceClaimingDownloadBackup(
            InheritanceClaimDownloadBackupRequest(magic = magic)
        )
        return response.data.toBackupKey()
    }

    override suspend fun inheritanceClaimingClaim(
        magic: String, psbt: String,
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
        securityQuestionToken: String,
        confirmCodeToken: String,
        confirmCodeNonce: String,
    ) {
        var request = gson.fromJson(userData, SecurityQuestionsUpdateRequest::class.java)
        if (confirmCodeNonce.isNotEmpty()) {
            request = request.copy(nonce = confirmCodeNonce)
        }
        val headers = mutableMapOf<String, String>()
        authorizations.forEachIndexed { index, value ->
            headers["$AUTHORIZATION_X-${index + 1}"] = value
        }
        headers[VERIFY_TOKEN] = verifyToken
        headers[SECURITY_QUESTION_TOKEN] = securityQuestionToken
        headers[CONFIRMATION_TOKEN] = confirmCodeToken
        return userWalletApiManager.walletApi.securityQuestionsUpdate(headers, request)
    }

    override suspend fun getNonce(): String {
        return userWalletApiManager.walletApi.getNonce().data.nonce?.nonce.orEmpty()
    }

    override suspend fun lockdownUpdate(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String,
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
        walletId: String, questions: List<QuestionsAndAnswer>,
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
        groupId: String?, walletId: String, psbt: String, note: String?,
    ) {
        val response = if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.createTransaction(
                groupId, walletId, CreateOrUpdateServerTransactionRequest(
                    note = note, psbt = psbt
                )
            )
        } else {
            userWalletApiManager.walletApi.createTransaction(
                walletId, CreateOrUpdateServerTransactionRequest(
                    note = note, psbt = psbt
                )
            )
        }
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun updateServerTransaction(
        groupId: String?,
        walletId: String,
        txId: String,
        note: String?,
    ) {
        val response = if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.updateTransaction(
                groupId, walletId, txId, CreateOrUpdateServerTransactionRequest(
                    note = note
                )
            )
        } else {
            userWalletApiManager.walletApi.updateTransaction(
                walletId, txId, CreateOrUpdateServerTransactionRequest(
                    note = note
                )
            )
        }
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun signServerTransaction(
        groupId: String?, walletId: String, txId: String, psbt: String,
    ): ExtendedTransaction {
        val transaction = runCatching {
            val response = if (!groupId.isNullOrEmpty()) {
                userWalletApiManager.groupWalletApi.signServerTransaction(
                    groupId = groupId,
                    walletId = walletId,
                    transactionId = txId,
                    payload = SignServerTransactionRequest(psbt = psbt)
                )
            } else {
                userWalletApiManager.walletApi.signServerTransaction(
                    walletId = walletId,
                    transactionId = txId,
                    payload = SignServerTransactionRequest(psbt = psbt)
                )
            }
            response.data.transaction
        }.getOrNull()
        val result = handleServerTransaction(groupId, walletId, txId, transaction)
        return ExtendedTransaction(
            transaction = result.first,
            serverTransaction = result.second?.toServerTransaction(),
        )
    }

    override suspend fun deleteServerTransaction(
        groupId: String?,
        walletId: String,
        transactionId: String,
    ) {
        val response = if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.deleteTransaction(groupId, walletId, transactionId)
        } else {
            userWalletApiManager.walletApi.deleteTransaction(walletId, transactionId)
        }
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

    override suspend fun markSetupInheritance(walletId: String, isSetupInheritance: Boolean) {
        val entity = assistedWalletDao.getById(walletId) ?: return
        if (entity.isSetupInheritance != isSetupInheritance) {
            assistedWalletDao.updateOrInsert(entity.copy(isSetupInheritance = isSetupInheritance))
        }
    }

    private suspend fun handleServerTransaction(
        groupId: String?,
        walletId: String,
        transactionId: String,
        serverTransaction: TransactionServerDto?,
    ): Pair<Transaction, TransactionServerDto?> {
        return if (serverTransaction?.status == TransactionStatus.PENDING_CONFIRMATION.name
            || serverTransaction?.status == TransactionStatus.CONFIRMED.name
            || serverTransaction?.status == TransactionStatus.NETWORK_REJECTED.name
        ) {
            nunchukNativeSdk.importPsbt(walletId, serverTransaction.psbt.orEmpty())
            nunchukNativeSdk.updateTransaction(
                walletId,
                transactionId,
                serverTransaction.transactionId.orEmpty(),
                serverTransaction.hex.orEmpty(),
                serverTransaction.rejectMsg.orEmpty()
            ) to serverTransaction
        } else if (serverTransaction != null) {
            val libTx = nunchukNativeSdk.importPsbt(walletId, serverTransaction.psbt.orEmpty())
            if (libTx.psbt != serverTransaction.psbt) {
                val response = if (!groupId.isNullOrEmpty()) {
                    userWalletApiManager.groupWalletApi.syncTransaction(
                        groupId, walletId, transactionId, SyncTransactionRequest(psbt = libTx.psbt)
                    )
                } else {
                    userWalletApiManager.walletApi.syncTransaction(
                        walletId, transactionId, SyncTransactionRequest(psbt = libTx.psbt)
                    )
                }
                libTx to response.data.transaction
            } else {
                libTx to serverTransaction
            }
        } else {
            // sync local transaction to server
            val libTx = nunchukNativeSdk.getTransaction(walletId, transactionId)
            val response = if (!groupId.isNullOrEmpty()) {
                userWalletApiManager.groupWalletApi.syncTransaction(
                    groupId, walletId, transactionId, SyncTransactionRequest(psbt = libTx.psbt)
                )
            } else {
                userWalletApiManager.walletApi.syncTransaction(
                    walletId, transactionId, SyncTransactionRequest(psbt = libTx.psbt)
                )
            }
            libTx to response.data.transaction
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
        keyPolicy: GroupKeyPolicy,
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
        groupId: String?, walletId: String, transactionId: String, scheduleTime: Long,
    ): ServerTransaction {
        val transaction = nunchukNativeSdk.getTransaction(walletId, transactionId)
        val response = if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.scheduleTransaction(
                groupId = groupId,
                walletId = walletId,
                transactionId = transactionId,
                payload = ScheduleTransactionRequest(
                    scheduleTime = scheduleTime, psbt = transaction.psbt
                )
            )
        } else {
            userWalletApiManager.walletApi.scheduleTransaction(
                walletId = walletId,
                transactionId = transactionId,
                payload = ScheduleTransactionRequest(
                    scheduleTime = scheduleTime, psbt = transaction.psbt
                )
            )
        }
        val serverTransaction = response.data.transaction
            ?: throw NullPointerException("Schedule transaction does not return server transaction")
        updateScheduleTransactionIfNeed(walletId, transactionId, serverTransaction)
        return serverTransaction.toServerTransaction()
    }

    override suspend fun deleteScheduleTransaction(
        groupId: String?, walletId: String, transactionId: String,
    ): ServerTransaction {
        val response = if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.deleteScheduleTransaction(
                groupId = groupId, walletId = walletId, transactionId = transactionId
            )
        } else {
            userWalletApiManager.walletApi.deleteScheduleTransaction(
                walletId = walletId, transactionId = transactionId
            )
        }
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

    override suspend fun syncTransaction(groupId: String?, walletId: String) {
        (0 until Int.MAX_VALUE step TRANSACTION_PAGE_COUNT).forEach { index ->
            val response = if (!groupId.isNullOrEmpty()) {
                userWalletApiManager.groupWalletApi.getTransactionsToSync(groupId, walletId, index)
            } else {
                userWalletApiManager.walletApi.getTransactionsToSync(walletId, index)
            }
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
                    if (transition.type == ServerTransactionType.SCHEDULED) {
                        serverTransactionCache.put(
                            transition.transactionId.orEmpty(),
                            transition.toServerTransaction()
                        )
                    }
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
        walletId: String, transactionId: String, transaction: TransactionServerDto,
    ) {
        if (transaction.type == ServerTransactionType.SCHEDULED && transaction.broadCastTimeMillis > System.currentTimeMillis()) {
            nunchukNativeSdk.updateTransactionSchedule(
                walletId, transactionId, transaction.broadCastTimeMillis / 1000
            )
        }
    }

    override suspend fun calculateRequiredSignaturesDeleteAssistedWallet(
        walletId: String,
        groupId: String?,
    ): CalculateRequiredSignatures {
        val response =
            if (groupId.isNullOrEmpty()) userWalletApiManager.walletApi.calculateRequiredSignaturesDeleteAssistedWallet(
                walletId
            )
            else userWalletApiManager.groupWalletApi.calculateRequiredSignaturesDeleteAssistedWallet(
                groupId = groupId, walletId = walletId
            )
        return response.data.result.toCalculateRequiredSignatures()
    }

    override suspend fun deleteAssistedWallet(
        authorizations: List<String>,
        verifyToken: String,
        securityQuestionToken: String,
        walletId: String,
        groupId: String?,
    ) {
        val nonce = getNonce()
        val request = DeleteAssistedWalletRequest(nonce = nonce)
        val headers = mutableMapOf<String, String>()
        authorizations.forEachIndexed { index, value ->
            headers["$AUTHORIZATION_X-${index + 1}"] = value
        }
        headers[VERIFY_TOKEN] = verifyToken
        headers[SECURITY_QUESTION_TOKEN] = securityQuestionToken
        if (groupId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.deleteAssistedWallet(
                walletId = walletId, headers = headers, payload = request
            )
        } else {
            userWalletApiManager.groupWalletApi.deleteAssistedWallet(
                groupId = groupId, walletId = walletId, headers = headers, payload = request
            )
        }
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
            remainingByzantineWallet = response.data.byzantine?.remainingWalletCount ?: 0,
            remainingByzantineProWallet = response.data.byzantinePro?.remainingWalletCount ?: 0,
            remainingHoneyBadgerWallet = response.data.honeyBadger?.remainingWalletCount ?: 0,
        )
    }

    override fun getAssistedWalletsLocal(): Flow<List<AssistedWalletBrief>> {
        return assistedWalletDao.getAssistedWallets().map { list ->
            list.map { wallet ->
                AssistedWalletBrief(
                    localId = wallet.localId,
                    plan = wallet.plan,
                    isSetupInheritance = wallet.isSetupInheritance,
                    registerAirgapCount = wallet.registerAirgapCount,
                    registerColdcardCount = wallet.registerColdcardCount,
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
            wallet.signers.filter { it.type == SignerType.NFC }.map { key ->
                async {
                    userWalletApiManager.walletApi.getKey(
                        key.masterFingerprint, key.derivationPath
                    )
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

    override suspend fun getCoinControlData(groupId: String?, walletId: String): String {
        return if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.getCoinControlData(
                groupId, walletId
            ).data.data.orEmpty()
        } else {
            userWalletApiManager.walletApi.getCoinControlData(walletId).data.data.orEmpty()
        }
    }

    override suspend fun uploadCoinControlData(groupId: String?, walletId: String, data: String) {
        if (!groupId.isNullOrEmpty()) userWalletApiManager.groupWalletApi.uploadCoinControlData(
            groupId, walletId, CoinDataContent(data)
        )
        else userWalletApiManager.walletApi.uploadCoinControlData(walletId, CoinDataContent(data))
    }

    override suspend fun clearTransactionEmergencyLockdown(groupId: String?, walletId: String) {
        (0 until Int.MAX_VALUE step TRANSACTION_PAGE_COUNT).forEach { index ->
            val response = if (!groupId.isNullOrEmpty()) {
                userWalletApiManager.groupWalletApi.getTransactionsToDelete(
                    groupId,
                    walletId,
                    index
                )
            } else {
                userWalletApiManager.walletApi.getTransactionsToDelete(walletId, index)
            }
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

    override suspend fun createGroupServerKey(
        groupId: String, name: String, groupKeyPolicy: GroupKeyPolicy,
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
        groupId: String, step: MembershipStep, signer: SingleSigner,
    ) {
        val index = step.toIndex()
        val signerDto = mapToServerSignerDto(
            signer, step == MembershipStep.BYZANTINE_ADD_TAP_SIGNER
        ).copy(index = index)
        val response = userWalletApiManager.groupWalletApi.addKeyToServer(groupId, signerDto)
        if (response.isSuccess.not()) {
            throw response.error
        }
        pushEventManager.push(PushEvent.KeyAddedToGroup)
    }

    override suspend fun requestAddKey(
        groupId: String,
        step: MembershipStep,
        tags: List<SignerTag>,
    ): String {
        val chatId = accountManager.getAccount().chatId
        var localRequest =
            requestAddKeyDao.getRequest(chatId, chain.value, step, tags.joinToString(), groupId)
        if (localRequest != null) {
            val response =
                if (groupId.isNotEmpty()) userWalletApiManager.groupWalletApi.getRequestAddKeyStatus(
                    groupId, localRequest.requestId
                )
                else userWalletApiManager.walletApi.getRequestAddKeyStatus(localRequest.requestId)
            if (response.data.request == null) {
                requestAddKeyDao.delete(localRequest)
                localRequest = null
            }
        }
        return if (localRequest == null) {
            val response =
                if (groupId.isNotEmpty()) userWalletApiManager.groupWalletApi.requestAddKey(
                    groupId, DesktopKeyRequest(tags.map { it.name }, keyIndex = step.toIndex())
                )
                else userWalletApiManager.walletApi.requestAddKey(
                    DesktopKeyRequest(
                        tags.map { it.name }, keyIndex = step.toIndex()
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
        requestId: String?,
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
                    groupId, localRequest.requestId
                )
            } else {
                userWalletApiManager.walletApi.getRequestAddKeyStatus(localRequest.requestId)
            }
            val request = response.data.request
            val key = request?.key
            if (request?.status == "COMPLETED" && key != null) {
                val type = nunchukNativeSdk.signerTypeFromStr(key.type.orEmpty())
                nunchukNativeSdk.createSigner(name = key.name.orEmpty(),
                    xpub = key.xpub.orEmpty(),
                    publicKey = key.pubkey.orEmpty(),
                    derivationPath = key.derivationPath.orEmpty(),
                    masterFingerprint = key.xfp.orEmpty(),
                    type = type,
                    tags = key.tags.orEmpty().mapNotNull { tag -> tag.toSignerTag() })
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
                        groupId = groupId
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
        val response = userWalletApiManager.walletApi.deleteDraftWallet()
        if (response.isSuccess.not()) {
            throw response.error
        }
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
        members: List<AssistedMember>,
    ): ByzantineGroup {
        val response = userWalletApiManager.groupWalletApi.createGroup(
            CreateGroupRequest(walletConfig = WalletConfigRequest(
                allowInheritance,
                m,
                n,
                requiredServerKey
            ),
                setupPreference = setupPreference,
                members = members.map { it.toMemberRequest() })
        )
        val groupResponse = response.data.data ?: throw NullPointerException("Can not create group")
        val group = groupResponse.toByzantineGroup()
        groupDao.updateOrInsert(
            groupResponse.toGroupEntity(accountManager.getAccount().chatId, chain.value, groupDao)
        )
        return group
    }

    override suspend fun getWalletConstraints(): List<WalletConstraints> {
        val response = userWalletApiManager.groupWalletApi.getGroupWalletsConstraints()
        if (response.isSuccess.not()) {
            throw response.error
        }
        return response.data.constraints?.map {
            WalletConstraints(
                maximumKeyholder = it.maximumKeyholder.orDefault(0),
                walletConfig = it.walletConfig.toModel()
            )
        } ?: emptyList()
    }

    override fun getGroups(loadingOptions: LoadingOptions): Flow<List<ByzantineGroup>> =
        chain.flatMapLatest {
            when (loadingOptions) {
                LoadingOptions.OFFLINE -> {
                    groupDao.getGroups(chatId = accountManager.getAccount().chatId, it)
                        .map { group ->
                            val groups = group.map { group ->
                                group.toByzantineGroup()
                            }
                            groups
                        }
                }

                LoadingOptions.REMOTE -> {
                    return@flatMapLatest flow {
                        syncer.syncGroups()?.let {
                            emit(it)
                        }
                    }
                }
            }
        }

    override suspend fun syncGroupWallets(): Boolean {
        val response = userWalletApiManager.groupWalletApi.getGroups()
        val groupAssistedKeys = mutableSetOf<String>()
        val groups = response.data.groups.orEmpty()
        if (groups.isNotEmpty()) {
            groups.forEach {
                when (it.status) {
                    GroupStatus.PENDING_WALLET.name -> {
                        groupWalletRepository.syncGroupDraftWallet(it.id.orEmpty())
                    }

                    GroupStatus.ACTIVE.name -> {
                        syncGroupWallet(it.id.orEmpty(), groupAssistedKeys)
                    }
                }
            }
        }
        ncDataStore.setGroupAssistedKey(groupAssistedKeys)
        syncer.syncGroups()

        return groups.isNotEmpty()
    }

    override fun getGroup(groupId: String, loadingOption: LoadingOptions): Flow<ByzantineGroup> {
        return when (loadingOption) {
            LoadingOptions.OFFLINE -> {
                groupDao.getById(
                    groupId,
                    chatId = accountManager.getAccount().chatId,
                    chain = chain.value
                ).map { group ->
                    group.toByzantineGroup()
                }
            }

            LoadingOptions.REMOTE -> {
                return flow {
                    syncer.syncGroup(groupId)?.let {
                        emit(it)
                    }
                }
            }
        }
    }

    override suspend fun deleteGroupWallet(groupId: String) {
        val response = userWalletApiManager.groupWalletApi.deleteDraftWallet(groupId)
        if (response.isSuccess.not()) {
            throw response.error
        }
        requestAddKeyDao.deleteRequests(groupId)
    }

    override suspend fun deleteGroup(groupId: String) {
        groupDao.deleteGroups(listOf(groupId), chatId = accountManager.getAccount().chatId)
    }

    override suspend fun updateGroupStatus(groupId: String, status: String) {
        val group =
            groupDao.getGroupById(groupId, chatId = accountManager.getAccount().chatId, chain.value)
                ?: return
        groupDao.update(group.copy(status = status))
    }

    override suspend fun generateEditGroupMemberUserData(
        members: List<AssistedMember>,
    ): String {
        val body = EditGroupMemberRequest.Body(members = members.map { it.toMemberRequest() })
        return gson.toJson(body)
    }

    override suspend fun calculateRequiredSignaturesEditGroupMember(
        groupId: String, members: List<AssistedMember>,
    ): CalculateRequiredSignatures {
        val response = userWalletApiManager.groupWalletApi.calculateRequiredSignaturesEditMember(
            groupId, EditGroupMemberRequest.Body(members = members.map { it.toMemberRequest() })
        )
        return response.data.result.toCalculateRequiredSignatures()
    }

    override suspend fun editGroupMember(
        groupId: String,
        authorizations: List<String>,
        verifyToken: String,
        members: List<AssistedMember>,
        securityQuestionToken: String,
        confirmCodeToken: String,
        confirmCodeNonce: String,
    ): ByzantineGroup {
        val request = EditGroupMemberRequest(
            nonce = confirmCodeNonce,
            body = EditGroupMemberRequest.Body(members = members.map { it.toMemberRequest() })
        )
        val headers = mutableMapOf<String, String>()
        authorizations.forEachIndexed { index, value ->
            headers["$AUTHORIZATION_X-${index + 1}"] = value
        }
        headers[VERIFY_TOKEN] = verifyToken
        headers[SECURITY_QUESTION_TOKEN] = securityQuestionToken
        headers[CONFIRMATION_TOKEN] = confirmCodeToken
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
        membershipStepDao.deleteStepByGroupId(groupId)
        return nunchukNativeSdk.getWallet(wallet.localId.orEmpty())
    }

    override suspend fun syncGroupWallet(
        groupId: String,
        groupAssistedKeys: MutableSet<String>,
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

    override fun getAlerts(groupId: String, loadingOption: LoadingOptions): Flow<List<Alert>> {
        return when (loadingOption) {
            LoadingOptions.OFFLINE -> {
                alertDao.getAlerts(groupId, chatId = accountManager.getAccount().chatId, chain.value)
                    .map { alerts ->
                        alerts.map { alert ->
                            alert.toAlert()
                        }
                    }
            }

            LoadingOptions.REMOTE -> {
                return flow {
                    val syncerList = syncer.syncAlerts(groupId)
                    if (syncerList != null) {
                        emit(syncerList)
                    }
                }
            }
        }
    }

    override suspend fun markAlertAsRead(groupId: String, alertId: String) {
        userWalletApiManager.groupWalletApi.markAlertAsRead(groupId, alertId)
    }

    override suspend fun dismissAlert(groupId: String, alertId: String) {
        val response = userWalletApiManager.groupWalletApi.dismissAlert(groupId, alertId)
        if (response.isSuccess.not()) throw response.error
    }

    override suspend fun getAlertTotal(groupId: String): Int {
        val response = userWalletApiManager.groupWalletApi.getAlertTotal(groupId)
        return response.data.total ?: 0
    }

    override suspend fun createOrUpdateGroupChat(
        roomId: String,
        groupId: String,
        historyPeriodId: String?,
    ): GroupChat {
        val response = if (historyPeriodId == null) {
            userWalletApiManager.groupWalletApi.createGroupChat(
                groupId = groupId, CreateOrUpdateGroupChatRequest(roomId = roomId)
            )
        } else {
            userWalletApiManager.groupWalletApi.updateGroupChat(
                groupId = groupId, CreateOrUpdateGroupChatRequest(historyPeriodId = historyPeriodId, roomId = roomId)
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

    override suspend fun requestConfirmationCode(
        action: String,
        userData: String,
    ): Pair<String, String> {
        val nonce = getNonce()
        val body = when (action) {
            TargetAction.EDIT_GROUP_MEMBERS.name -> gson.fromJson(
                userData,
                EditGroupMemberRequest.Body::class.java
            )

            TargetAction.UPDATE_SECURITY_QUESTIONS.name -> gson.fromJson(
                userData,
                QuestionsAndAnswerRequestBody::class.java
            )

            else -> throw IllegalArgumentException("Unsupported action")
        }
        val request = ConfirmationCodeRequest(nonce = nonce, body = body)
        val response = userWalletApiManager.walletApi.requestConfirmationCode(
            action = action, payload = request
        )
        return Pair(nonce, response.data.codeId.orEmpty())
    }

    override suspend fun verifyConfirmationCode(codeId: String, code: String): String {
        val response = userWalletApiManager.walletApi.verifyConfirmationCode(
            codeId, ConfirmationCodeVerifyRequest(code = code)
        )
        return response.data.token.orEmpty()
    }

    override suspend fun syncDeletedWallet(): Boolean {
        val response = userWalletApiManager.groupWalletApi.getWallets(0, 30)
        val wallets = response.data.wallets
        val results = wallets.map {
            runCatching {
                nunchukNativeSdk.deleteWallet(it.localId.orEmpty())
            }
        }
        return results.any { it.isSuccess }
    }

    override suspend fun deleteKey(xfp: String) {
        val response = userWalletApiManager.walletApi.deleteKey(xfp)
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun syncConfirmedTransactionNotes(groupId: String?, walletId: String) {
        (0 until Int.MAX_VALUE step TRANSACTION_PAGE_COUNT).forEach { index ->
            val response = if (!groupId.isNullOrEmpty()) {
                userWalletApiManager.groupWalletApi.getConfirmedAndRejectedTransactions(
                    groupId,
                    walletId,
                    index
                )
            } else {
                userWalletApiManager.walletApi.getConfirmedAndRejectedTransactions(walletId, index)
            }
            if (response.isSuccess.not()) throw response.error
            response.data.notes.forEach { transition ->
                if (!transition.note.isNullOrEmpty()) {
                    nunchukNativeSdk.updateTransactionMemo(
                        walletId, transition.transactionId.orEmpty(), transition.note
                    )
                }
            }
            if (response.data.notes.size < TRANSACTION_PAGE_COUNT) return
        }
    }

    companion object {
        private const val WALLET_ACTIVE_STATUS = "ACTIVE"
        private const val WALLET_DELETED_STATUS = "DELETED"
        private const val VERIFY_TOKEN = "Verify-token"
        private const val SECURITY_QUESTION_TOKEN = "Security-Question-token"
        internal const val AUTHORIZATION_X = "AuthorizationX"
        internal const val CONFIRMATION_TOKEN = "Confirmation-token"
    }
}

