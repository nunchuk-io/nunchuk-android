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
import com.nunchuk.android.core.data.model.ChangeEmailRequest
import com.nunchuk.android.core.data.model.ChangeEmailSignatureRequest
import com.nunchuk.android.core.data.model.ConfigSecurityQuestionPayload
import com.nunchuk.android.core.data.model.CreateSecurityQuestionRequest
import com.nunchuk.android.core.data.model.CreateServerKeysPayload
import com.nunchuk.android.core.data.model.CreateTimelockPayload
import com.nunchuk.android.core.data.model.DeleteAssistedWalletRequest
import com.nunchuk.android.core.data.model.EmptyRequest
import com.nunchuk.android.core.data.model.InheritanceByzantineRequestPlanning
import com.nunchuk.android.core.data.model.InheritanceCancelRequest
import com.nunchuk.android.core.data.model.InheritanceCheckRequest
import com.nunchuk.android.core.data.model.InheritanceClaimClaimRequest
import com.nunchuk.android.core.data.model.InheritanceClaimCreateTransactionRequest
import com.nunchuk.android.core.data.model.InheritanceClaimDownloadBackupRequest
import com.nunchuk.android.core.data.model.InheritanceClaimStatusRequest
import com.nunchuk.android.core.data.model.InitWalletConfigRequest
import com.nunchuk.android.core.data.model.LockdownUpdateRequest
import com.nunchuk.android.core.data.model.MarkRecoverStatusRequest
import com.nunchuk.android.core.data.model.QuestionsAndAnswerRequest
import com.nunchuk.android.core.data.model.RequestRecoverKeyRequest
import com.nunchuk.android.core.data.model.SecurityQuestionsUpdateRequest
import com.nunchuk.android.core.data.model.SyncTransactionRequest
import com.nunchuk.android.core.data.model.TapSignerPayload
import com.nunchuk.android.core.data.model.TimelockPayload
import com.nunchuk.android.core.data.model.UpdateKeyPayload
import com.nunchuk.android.core.data.model.UpdateWalletPayload
import com.nunchuk.android.core.data.model.byzantine.CreateDraftWalletRequest
import com.nunchuk.android.core.data.model.byzantine.CreateGroupRequest
import com.nunchuk.android.core.data.model.byzantine.CreateOrUpdateGroupChatRequest
import com.nunchuk.android.core.data.model.byzantine.EditGroupMemberRequest
import com.nunchuk.android.core.data.model.byzantine.SavedAddressRequest
import com.nunchuk.android.core.data.model.byzantine.WalletConfigDto
import com.nunchuk.android.core.data.model.byzantine.WalletConfigRequest
import com.nunchuk.android.core.data.model.byzantine.toDomainModel
import com.nunchuk.android.core.data.model.byzantine.toModel
import com.nunchuk.android.core.data.model.byzantine.toSavedAddress
import com.nunchuk.android.core.data.model.byzantine.toWalletType
import com.nunchuk.android.core.data.model.coin.CoinDataContent
import com.nunchuk.android.core.data.model.inheritance.BeneficiaryNotificationRequest
import com.nunchuk.android.core.data.model.inheritance.CreateUpdateInheritancePlanRequest
import com.nunchuk.android.core.data.model.inheritance.NotificationPreferencesRequest
import com.nunchuk.android.core.data.model.membership.ConfirmationCodeRequest
import com.nunchuk.android.core.data.model.membership.ConfirmationCodeVerifyRequest
import com.nunchuk.android.core.data.model.membership.CreateOrUpdateServerTransactionRequest
import com.nunchuk.android.core.data.model.membership.DesktopKeyRequest
import com.nunchuk.android.core.data.model.membership.GetWalletsResponse
import com.nunchuk.android.core.data.model.membership.HealthReminderRequest
import com.nunchuk.android.core.data.model.membership.KeyPolicyUpdateRequest
import com.nunchuk.android.core.data.model.membership.RequestSignatureTransactionRequest
import com.nunchuk.android.core.data.model.membership.ScheduleTransactionRequest
import com.nunchuk.android.core.data.model.membership.SignServerTransactionRequest
import com.nunchuk.android.core.data.model.membership.TransactionServerDto
import com.nunchuk.android.core.data.model.membership.UpdatePrimaryOwnerRequest
import com.nunchuk.android.core.data.model.membership.WalletDto
import com.nunchuk.android.core.data.model.membership.toDto
import com.nunchuk.android.core.data.model.membership.toEntity
import com.nunchuk.android.core.data.model.membership.toGroupKeyPolicy
import com.nunchuk.android.core.data.model.membership.toHealthReminder
import com.nunchuk.android.core.data.model.membership.toModel
import com.nunchuk.android.core.data.model.membership.toServerTransaction
import com.nunchuk.android.core.data.model.membership.toTransactionStatus
import com.nunchuk.android.core.data.model.membership.toWalletOption
import com.nunchuk.android.core.data.model.replacement.replacementsToModel
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.exception.RequestAddKeyCancelException
import com.nunchuk.android.core.exception.RequestAddSameKeyException
import com.nunchuk.android.core.gateway.SignerGateway
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.core.mapper.ServerSignerMapper
import com.nunchuk.android.core.mapper.toAlert
import com.nunchuk.android.core.mapper.toBackupKey
import com.nunchuk.android.core.mapper.toByzantineGroup
import com.nunchuk.android.core.mapper.toCalculateRequiredSignatures
import com.nunchuk.android.core.mapper.toCalculateRequiredSignaturesEx
import com.nunchuk.android.core.mapper.toDomain
import com.nunchuk.android.core.mapper.toGroupChat
import com.nunchuk.android.core.mapper.toGroupEntity
import com.nunchuk.android.core.mapper.toHistoryPeriod
import com.nunchuk.android.core.mapper.toInheritance
import com.nunchuk.android.core.mapper.toMemberRequest
import com.nunchuk.android.core.mapper.toPeriod
import com.nunchuk.android.core.mapper.toSavedAddress
import com.nunchuk.android.core.mapper.toSavedAddressEntity
import com.nunchuk.android.core.network.Data
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.toSignerTag
import com.nunchuk.android.core.util.ONE_HOUR_TO_SECONDS
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.core.util.toSignerType
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.BufferPeriodCountdown
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.CalculateRequiredSignaturesAction
import com.nunchuk.android.model.CalculateRequiredSignaturesExt
import com.nunchuk.android.model.CreateWalletResult
import com.nunchuk.android.model.DefaultPermissions
import com.nunchuk.android.model.FinalizeReplaceWalletResult
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.model.GroupStatus
import com.nunchuk.android.model.HealthCheckHistory
import com.nunchuk.android.model.HealthReminder
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
import com.nunchuk.android.model.SavedAddress
import com.nunchuk.android.model.SecurityQuestion
import com.nunchuk.android.model.ServerKeyExtra
import com.nunchuk.android.model.SeverWallet
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.SpendingPolicy
import com.nunchuk.android.model.SpendingTimeUnit
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TransactionAdditional
import com.nunchuk.android.model.UserWalletConfigsSetup
import com.nunchuk.android.model.VerifiedPKeyTokenRequest
import com.nunchuk.android.model.VerifiedPasswordTokenRequest
import com.nunchuk.android.model.VerifyFederatedTokenRequest
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.WalletConfig
import com.nunchuk.android.model.WalletConstraints
import com.nunchuk.android.model.WalletServer
import com.nunchuk.android.model.WalletServerSync
import com.nunchuk.android.model.WalletTimelock
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.inheritance.InheritanceNotificationSettings
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.model.isAllowSetupInheritance
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.model.membership.AssistedWalletBriefExt
import com.nunchuk.android.model.membership.GroupConfig
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.model.toIndex
import com.nunchuk.android.model.toMembershipPlan
import com.nunchuk.android.model.toPairIndex
import com.nunchuk.android.model.toVerifyType
import com.nunchuk.android.model.transaction.ExtendedTransaction
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.model.transaction.ServerTransactionType
import com.nunchuk.android.model.wallet.ReplaceWalletStatus
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.persistence.dao.AlertDao
import com.nunchuk.android.persistence.dao.AssistedWalletDao
import com.nunchuk.android.persistence.dao.GroupDao
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.persistence.dao.RequestAddKeyDao
import com.nunchuk.android.persistence.dao.SavedAddressDao
import com.nunchuk.android.persistence.entity.RequestAddKeyEntity
import com.nunchuk.android.persistence.entity.SavedAddressEntity
import com.nunchuk.android.repository.GroupWalletRepository
import com.nunchuk.android.repository.MembershipRepository
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.SERVER_KEY_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

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
    private val savedAddressDao: SavedAddressDao,
    private val groupWalletRepository: GroupWalletRepository,
    private val pushEventManager: PushEventManager,
    private val serverTransactionCache: LruCache<String, ServerTransaction>,
    private val syncer: ByzantineSyncer,
    private val serverSignerMapper: ServerSignerMapper,
    private val applicationScope: CoroutineScope,
    private val signerGateway: SignerGateway,
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
        val serverKeyId = data.key?.id ?: throw NullPointerException("Can not generate server key")
        val setServerKeyResponse = userWalletApiManager.walletApi.setServerKey(
            mapOf("server_key_id" to serverKeyId)
        )
        val key = data.key
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
                        xpub = key.xpub.orEmpty(),
                        userKeyFileName = ""
                    )
                ),
                plan = plan,
                groupId = ""
            )
        )
        if (setServerKeyResponse.isSuccess.not()) {
            throw setServerKeyResponse.error
        }
        return keyPolicy
    }

    override suspend fun getServerKey(xfp: String, derivationPath: String): KeyPolicy {
        val response = userWalletApiManager.walletApi.getServerKey(xfp, derivationPath)
        val policy =
            response.data.key?.policies ?: throw NullPointerException("Can not find key policy")
        val spendingLimit = policy.spendingLimit?.let {
            SpendingPolicy(
                limit = it.limit,
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
        return policy.toGroupKeyPolicy()
    }

    override suspend fun updateServerKeys(
        signatures: Map<String, String>,
        keyIdOrXfp: String,
        derivationPath: String,
        token: String,
        securityQuestionToken: String,
        body: String,
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
        val response = userWalletApiManager.walletApi.updateServerKeys(
            headers = headers,
            keyId = keyIdOrXfp,
            derivationPath = derivationPath,
            body = gson.fromJson(body, KeyPolicyUpdateRequest::class.java)
        )
        return response.data.dummyTransaction?.id.orEmpty()
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

    override suspend fun getServerWallets(): WalletServerSync {
        val result = userWalletApiManager.walletApi.getServerWallet()
        val assistedKeys = mutableSetOf<String>()
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
        val wallets = result.data.wallets.filter { it.status != WALLET_DELETED_STATUS }
        val deleteCount =
            assistedWalletDao.deleteAllPersonalWalletsExcept(wallets.map { it.localId.orEmpty() })
        if (wallets.isNotEmpty()) {
            updateOrInsertWallet(wallets = wallets)
        }
        ncDataStore.setAssistedKey(assistedKeys)
        result.data.wallets.forEach { planWalletCreated[it.slug.orEmpty()] = it.localId.orEmpty() }

        val claimingWallets = mutableListOf<WalletDto>()
        var page = 0
        val pageSize = 30
        runCatching {
            do {
                val response = userWalletApiManager.claimWalletApi.getClaimingWallets(
                    offset = page * pageSize,
                    limit = pageSize,
                    statuses = listOf("ACTIVE")
                )
                val wallets = response.data.wallets
                claimingWallets.addAll(wallets)
                page++
            } while (wallets.size == pageSize)
        }

        claimingWallets.forEach { walletServer ->
            Timber.d("Claiming wallet sync: ${walletServer.localId}")
            if (saveWalletToLib(walletServer, assistedKeys)) isNeedReload = true
        }
        ncDataStore.setClaimWallets(claimingWallets.map { it.localId.orEmpty() }.toSet())

        return WalletServerSync(
            keyPolicyMap = keyPolicyMap, isNeedReload = isNeedReload || deleteCount > 0
        )
    }

    private suspend fun updateOrInsertWallet(
        groupId: String? = null,
        wallets: List<WalletDto>
    ) {
        val existingWallets =
            assistedWalletDao.getAssistedWallets().associateBy { it.localId }
        val walletEntities = wallets.map { walletDto ->
            val existingWallet = existingWallets[walletDto.localId.orEmpty()]
            walletDto.toEntity().copy(
                groupId = groupId.orEmpty(),
                isSetupInheritance = existingWallet?.isSetupInheritance.orFalse(),
                registerColdcardCount = existingWallet?.registerColdcardCount ?: 0,
                registerAirgapCount = existingWallet?.registerAirgapCount ?: 0,
                ext = existingWallet?.ext,
                replaceSignerTypes = existingWallet?.replaceSignerTypes.orEmpty(),
            )
        }
        assistedWalletDao.updateOrInsert(walletEntities)
    }

    private suspend fun saveWalletToLib(
        walletServer: WalletDto, assistedKeys: MutableSet<String>,
    ): Boolean {
        coroutineContext.ensureActive()
        var isNeedReload = false
        val newSignerMap = hashMapOf<String, Boolean>()
        val isRemoveKey =
            walletServer.removeUnusedKeys && walletServer.status == WalletStatus.REPLACED.name
        if (nunchukNativeSdk.hasWallet(walletServer.localId.orEmpty()).not()) {
            isNeedReload = true
            if (!isRemoveKey) {
                walletServer.signerServerDtos?.forEach { signer ->
                    newSignerMap[signer.xfp.orEmpty()] =
                        !signerGateway.saveServerSignerIfNeed(signer)
                }
            }

            val wallet = nunchukNativeSdk.parseWalletDescriptor(walletServer.bsms.orEmpty()).apply {
                name = walletServer.name.orEmpty()
                description = walletServer.description.orEmpty()
                createDate = walletServer.createdTimeMilis / 1000
            }
            nunchukNativeSdk.createWallet2(wallet)
        } else if (!isRemoveKey) {
            walletServer.signerServerDtos?.forEach { signer ->
                signerGateway.saveServerSignerIfNeed(signer)
            }
        }
        val wallet = nunchukNativeSdk.getWallet(walletServer.localId.orEmpty())
        if (wallet.name != walletServer.name || wallet.description != walletServer.description) {
            nunchukNativeSdk.updateWallet(
                wallet.copy(
                    name = walletServer.name.orEmpty(),
                    description = walletServer.description.orEmpty(),
                )
            )
        }

        if (isRemoveKey) return isNeedReload
        walletServer.signerServerDtos?.forEach { signer ->
            val type = nunchukNativeSdk.signerTypeFromStr(signer.type.orEmpty())
            val tags = signer.tags.orEmpty().mapNotNull { it.toSignerTag() }
            if (type != SignerType.SERVER) {
                val localSigner = wallet.signers.find { it.masterFingerprint == signer.xfp }
                if (localSigner != null) {
                    assistedKeys.add(signer.xfp.orEmpty())
                    if (localSigner.hasMasterSigner) {
                        val masterSigner = nunchukNativeSdk.getMasterSigner(signer.xfp.orEmpty())
                        val isVisible =
                            if (newSignerMap[signer.xfp.orEmpty()] == true) signer.isVisible else masterSigner.isVisible || signer.isVisible
                        val newTags = tags.ifEmpty { masterSigner.tags }
                        val isChange =
                            masterSigner.name != signer.name || masterSigner.tags != newTags || masterSigner.isVisible != isVisible
                        if (isChange) {
                            isNeedReload = true
                            nunchukNativeSdk.updateMasterSigner(
                                masterSigner.copy(
                                    name = signer.name.orEmpty(),
                                    tags = newTags,
                                    isVisible = isVisible
                                )
                            )
                        }
                    } else {
                        val remoteSigner = runCatching {
                            nunchukNativeSdk.getRemoteSigner(
                                signer.xfp.orEmpty(), signer.derivationPath.orEmpty()
                            )
                        }.getOrNull()
                        if (remoteSigner != null) {
                            val isVisible =
                                if (newSignerMap[signer.xfp.orEmpty()] == true) signer.isVisible else remoteSigner.isVisible || signer.isVisible
                            val newTags = tags.ifEmpty { remoteSigner.tags }
                            val isChange =
                                remoteSigner.name != signer.name || remoteSigner.tags != newTags || remoteSigner.isVisible != isVisible
                            if (isChange) {
                                isNeedReload = true
                                nunchukNativeSdk.updateRemoteSigner(
                                    remoteSigner.copy(
                                        name = signer.name.orEmpty(),
                                        tags = newTags,
                                        isVisible = isVisible
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        return isNeedReload
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
        saveWalletToLib(wallet, mutableSetOf())
        return SeverWallet(wallet.id.orEmpty())
    }

    override suspend fun getWallet(walletId: String): WalletServer {
        val response = userWalletApiManager.walletApi.getWallet(walletId)
        val wallet = response.data.wallet ?: throw NullPointerException("Wallet empty")
        saveWalletToLib(wallet, mutableSetOf())
        return wallet.toModel()
    }

    override suspend fun getServerTransaction(
        groupId: String?, walletId: String, transactionId: String,
    ): ExtendedTransaction {
        val isClaimWallet = ncDataStore.claimWalletsFlow.first().contains(walletId)
        val transaction = runCatching {
            val response = if (!groupId.isNullOrEmpty()) {
                userWalletApiManager.groupWalletApi.getTransaction(groupId, walletId, transactionId)
            } else if (isClaimWallet) {
                userWalletApiManager.claimWalletApi.getClaimingWalletTransaction(walletId, transactionId)
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
        val token = response.data.token.token
        ncDataStore.setPasswordToken(token.orEmpty())
        return token
    }

    override suspend fun requestFederatedToken(targetAction: String) {
        val response = membershipApi.requestFederatedToken(
            targetAction
        )
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun verifyFederatedToken(targetAction: String, token: String): String? {
        val response = membershipApi.verifyFederatedToken(
            targetAction, VerifyFederatedTokenRequest(token)
        )
        if (response.isSuccess.not()) {
            throw response.error
        }
        ncDataStore.setPasswordToken(token)
        return token
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
        walletId: String, periodId: String, groupId: String?,
    ): CalculateRequiredSignatures {
        val response = if (groupId.isNullOrEmpty().not()) {
            userWalletApiManager.groupWalletApi.calculateRequiredSignaturesLockdown(
                LockdownUpdateRequest.Body(
                    walletId = walletId, periodId = periodId, groupId = groupId
                )
            )
        } else {
            userWalletApiManager.walletApi.calculateRequiredSignaturesLockdown(
                LockdownUpdateRequest.Body(
                    walletId = walletId, periodId = periodId
                )
            )
        }
        return response.data.result.toCalculateRequiredSignatures()
    }

    override suspend fun calculateRequiredSignaturesChangeEmail(newEmail: String): CalculateRequiredSignatures {
        val response = userWalletApiManager.walletApi.calculateRequiredSignaturesChangeEmail(
            ChangeEmailSignatureRequest(newEmail)
        )
        return response.data.result.toCalculateRequiredSignatures()
    }

    override suspend fun changeEmail(
        newEmail: String,
        verifyToken: String,
        securityQuestionToken: String,
        confirmCodeToken: String,
        confirmCodeNonce: String,
        draft: Boolean
    ): DummyTransactionPayload? {
        val headers = getHeaders(
            authorizations = emptyList(),
            verifyToken = verifyToken,
            securityQuestionToken = securityQuestionToken,
            confirmCodeToken = confirmCodeToken
        )
        val response = userWalletApiManager.walletApi.changeEmail(
            payload = ChangeEmailRequest(
                nonce = confirmCodeNonce,
                body = ChangeEmailRequest.Body(newEmail)
            ),
            headers = headers,
            draft = draft
        )
        if (response.isSuccess.not()) throw response.error
        return response.data.dummyTransaction?.toDomainModel()
    }

    private suspend fun markGroupWalletAsLocked(isLocked: Boolean, groupId: String) {
        val group = groupDao.getGroupById(groupId, accountManager.getAccount().chatId, chain.value)
        group?.let {
            groupDao.update(
                group.copy(
                    isLocked = isLocked
                )
            )
        }
    }

    override suspend fun generateInheritanceUserData(
        note: String,
        notificationEmails: List<String>,
        notifyToday: Boolean,
        activationTimeMilis: Long,
        bufferPeriodId: String?,
        walletId: String,
        groupId: String?,
        notificationPreferences: InheritanceNotificationSettings?,
        timezone: String,
    ): String {
        val body = CreateUpdateInheritancePlanRequest.Body(
            note = note,
            notifyToday = notifyToday,
            notificationEmails = notificationEmails,
            activationTimeMilis = activationTimeMilis,
            bufferPeriodId = bufferPeriodId,
            walletId = walletId,
            groupId = groupId,
            notificationPreferences = notificationPreferences?.let { prefs ->
                NotificationPreferencesRequest(
                    emailMeWalletConfig = prefs.emailMeWalletConfig,
                    beneficiaryNotifications = prefs.perEmailSettings.map { setting ->
                        BeneficiaryNotificationRequest(
                            email = setting.email,
                            notifyTimelockExpires = setting.notifyOnTimelockExpiry,
                            notifyWalletChanges = setting.notifyOnWalletChanges,
                            includeWalletConfig = setting.includeWalletConfiguration
                        )
                    }
                )
            },
            timezone = timezone
        )
        val nonce = getNonce()
        val request = CreateUpdateInheritancePlanRequest(
            nonce = nonce, body = body
        )
        return gson.toJson(request)
    }

    override suspend fun generateInheritanceClaimStatusUserData(
        magic: String,
        bsms: String?
    ): String {
        val body = InheritanceClaimStatusRequest.Body(
            magic = magic,
            bsms = bsms
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
        feeRate: String,
        amount: String,
        antiFeeSniping: Boolean,
        subtractFeeFromAmount: Boolean?,
        bsms: String?
    ): String {
        val amountVal = amount.toDoubleOrNull() ?: 0.0
        val body = InheritanceClaimCreateTransactionRequest.Body(
            magic = magic,
            bsms = bsms,
            address = address,
            feeRate = feeRate,
            amount = if (amountVal == 0.0) null else amount,
            antiFeeSniping = antiFeeSniping,
            subtractFeeFromAmount = subtractFeeFromAmount
        )
        val nonce = getNonce()
        val request = InheritanceClaimCreateTransactionRequest(
            nonce = nonce, body = body
        )
        return gson.toJson(request)
    }

    override suspend fun inheritanceClaimStatus(
        userData: String, masterFingerprints: List<String>, signatures: List<String>
    ): InheritanceAdditional {
        val headers = mutableMapOf<String, String>()
        masterFingerprints.forEachIndexed { index, masterFingerprint ->
            val signerToken =
                nunchukNativeSdk.createRequestToken(signatures[index], masterFingerprint)
            headers["$AUTHORIZATION_X-${index + 1}"] = signerToken
        }
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
        userData: String, masterFingerprints: List<String>, signatures: List<String>,
    ): TransactionAdditional {
        val headers = mutableMapOf<String, String>()
        masterFingerprints.forEachIndexed { index, masterFingerprint ->
            val signerToken =
                nunchukNativeSdk.createRequestToken(signatures[index], masterFingerprint)
            headers["$AUTHORIZATION_X-${index + 1}"] = signerToken
        }
        val request = gson.fromJson(userData, InheritanceClaimCreateTransactionRequest::class.java)
        val response =
            userWalletApiManager.walletApi.inheritanceClaimingCreateTransaction(headers, request)
        val transaction =
            response.data.transaction ?: throw NullPointerException("transaction from server null")
        return TransactionAdditional(
            psbt = transaction.psbt.orEmpty(),
            subAmount = response.data.subAmount ?: 0.0,
            fee = response.data.txFee ?: 0.0,
            feeRate = response.data.txFeeRate ?: 0.0,
            status = transaction.status.toTransactionStatus(),
            changePos = response.data.changePos ?: -1,
            subtractFeeFromAmount = transaction.subtractFeeFromAmount
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

    override suspend fun generateRequestPlanningInheritanceUserData(
        walletId: String,
        groupId: String,
    ): String {
        val body = InheritanceByzantineRequestPlanning.Body(
            walletId = walletId, groupId = groupId
        )
        val nonce = getNonce()
        val request = InheritanceByzantineRequestPlanning(
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
        action: CalculateRequiredSignaturesAction,
        groupId: String?,
        notificationPreferences: InheritanceNotificationSettings?,
        timezone: String,
    ): CalculateRequiredSignatures {
        val response =
            if (action == CalculateRequiredSignaturesAction.CANCEL || action == CalculateRequiredSignaturesAction.REQUEST_PLANNING) {
                userWalletApiManager.walletApi.calculateRequiredSignaturesInheritance(
                    CreateUpdateInheritancePlanRequest.Body(
                        walletId = walletId, groupId = groupId, timezone = timezone
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
                        groupId = groupId,
                        notificationPreferences = notificationPreferences?.let { prefs ->
                            NotificationPreferencesRequest(
                                emailMeWalletConfig = prefs.emailMeWalletConfig,
                                beneficiaryNotifications = prefs.perEmailSettings.map { settings ->
                                    BeneficiaryNotificationRequest(
                                        email = settings.email,
                                        notifyTimelockExpires = settings.notifyOnTimelockExpiry,
                                        notifyWalletChanges = settings.notifyOnWalletChanges,
                                        includeWalletConfig = settings.includeWalletConfiguration
                                    )
                                }
                            )
                        },
                        timezone = timezone
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
        draft: Boolean,
    ): String {
        val request = gson.fromJson(userData, CreateUpdateInheritancePlanRequest::class.java)
        val headers = getHeaders(
            authorizations = authorizations,
            verifyToken = verifyToken,
            securityQuestionToken = securityQuestionToken,
            confirmCodeToken = ""
        )
        val response = if (isUpdate) userWalletApiManager.walletApi.updateInheritance(
            headers, request, draft
        ) else userWalletApiManager.walletApi.createInheritance(headers, request, draft)
        if (response.isSuccess.not()) throw response.error
        if (request.body?.groupId == null) {
            val inheritance = response.data.inheritance
            inheritance?.walletLocalId?.also { walletLocalId ->
                updateAssistedWalletBriefExt(walletLocalId, inheritance.toInheritance())
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
        val headers = getHeaders(
            authorizations = authorizations,
            verifyToken = verifyToken,
            securityQuestionToken = securityQuestionToken,
            confirmCodeToken = ""
        )
        val response = userWalletApiManager.walletApi.inheritanceCancel(headers, request, draft)
        if (response.isSuccess && request.body?.groupId == null) {
            updateAssistedWalletBriefExt(walletId, null)
        }
        pushEventManager.push(PushEvent.InheritanceEvent(walletId = walletId, isCancelled = true))
        return response.data.dummyTransaction?.id.orEmpty()
    }

    override suspend fun requestPlanningInheritance(
        authorizations: List<String>,
        userData: String,
        walletId: String,
        groupId: String,
    ): String {
        val request = gson.fromJson(userData, InheritanceByzantineRequestPlanning::class.java)
        val headers = getHeaders(
            authorizations = authorizations,
            verifyToken = "",
            securityQuestionToken = "",
            confirmCodeToken = ""
        )
        val response =
            userWalletApiManager.walletApi.inheritanceRequestPlanning(headers, request, true)
        return response.data.dummyTransaction?.id.orEmpty()
    }

    override suspend fun inheritanceClaimDownloadBackup(
        magic: String,
        hashedBps: List<String>
    ): List<BackupKey> {
        val response = userWalletApiManager.walletApi.inheritanceClaimingDownloadBackups(
            InheritanceClaimDownloadBackupRequest(magic = magic, hashedBps = hashedBps)
        )
        if (response.isSuccess.not()) {
            throw response.error
        }
        return response.data.keys?.map { it.toBackupKey() }.orEmpty()
    }

    override suspend fun inheritanceClaimingClaim(
        magic: String, psbt: String, bsms: String?,
    ): TransactionAdditional {
        val response = userWalletApiManager.walletApi.inheritanceClaimingClaim(
            InheritanceClaimClaimRequest(magic = magic, psbt = psbt, bsms = bsms)
        )
        val transaction =
            response.data.transaction ?: throw NullPointerException("transaction from server null")
        return TransactionAdditional(
            psbt = transaction.psbt.orEmpty(),
            status = transaction.status.toTransactionStatus(),
        )
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
        draft: Boolean,
    ): String {
        var request = gson.fromJson(userData, SecurityQuestionsUpdateRequest::class.java)
        if (confirmCodeNonce.isNotEmpty()) {
            request = request.copy(nonce = confirmCodeNonce)
        }
        val headers = getHeaders(
            authorizations = authorizations,
            verifyToken = verifyToken,
            securityQuestionToken = securityQuestionToken,
            confirmCodeToken = confirmCodeToken
        )
        val response =
            userWalletApiManager.walletApi.securityQuestionsUpdate(headers, request, draft)
        if (response.isSuccess.not()) throw response.error
        return response.data.dummyTransaction?.id.orEmpty()
    }

    override suspend fun getNonce(): String {
        return userWalletApiManager.walletApi.getNonce().data.nonce?.nonce.orEmpty()
    }

    override suspend fun lockdownUpdate(
        authorizations: List<String>,
        verifyToken: String,
        userData: String,
        securityQuestionToken: String,
        confirmCodeToken: String,
        confirmCodeNonce: String,
    ) {
        var request = gson.fromJson(userData, LockdownUpdateRequest::class.java)
        if (confirmCodeNonce.isNotEmpty()) {
            request = request.copy(nonce = confirmCodeNonce)
        }
        val groupId = request.body?.groupId
        val response = if (groupId.isNullOrEmpty().not()) {
            userWalletApiManager.groupWalletApi.lockdownUpdate(
                getHeaders(
                    authorizations = authorizations,
                    verifyToken = verifyToken,
                    securityQuestionToken = securityQuestionToken,
                    confirmCodeToken = confirmCodeToken
                ), request
            )
        } else {
            userWalletApiManager.walletApi.lockdownUpdate(
                getHeaders(
                    authorizations = authorizations,
                    verifyToken = verifyToken,
                    securityQuestionToken = securityQuestionToken,
                    confirmCodeToken = confirmCodeToken
                ), request
            )
        }
        if (response.isSuccess.not()) throw response.error
        groupId?.let { markGroupWalletAsLocked(true, it) }
    }

    override suspend fun generateSecurityQuestionUserData(
        walletId: String, questions: List<QuestionsAndAnswer>,
    ): String {
        val questionsAndAnswerRequests = questions.map {
            QuestionsAndAnswerRequest(
                questionId = it.questionId, answer = it.answer, change = it.change
            )
        }
        val body =
            SecurityQuestionsUpdateRequest.Body(questionsAndAnswerRequests, walletId = walletId)
        val nonce = getNonce()
        val request = SecurityQuestionsUpdateRequest(
            nonce = nonce, body = body
        )
        return gson.toJson(request)
    }

    override suspend fun generateLockdownUserData(
        walletId: String,
        periodId: String,
        groupId: String?,
    ): String {
        val body =
            LockdownUpdateRequest.Body(
                periodId = periodId,
                walletId = walletId,
                groupId = if (groupId.isNullOrEmpty().not()) groupId else null
            )
        val nonce = getNonce()
        val request = LockdownUpdateRequest(
            nonce = nonce, body = body
        )
        return gson.toJson(request)
    }

    override suspend fun createServerTransaction(
        groupId: String?, walletId: String, psbt: String, note: String?,
    ) {
        val isClaimWallet = ncDataStore.claimWalletsFlow.first().contains(walletId)
        if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.createTransaction(
                groupId, walletId, CreateOrUpdateServerTransactionRequest(
                    note = note, psbt = psbt
                )
            )
        } else if (isClaimWallet) {
            userWalletApiManager.claimWalletApi.createClaimingWalletTransaction(
                localId = walletId,
                payload = CreateOrUpdateServerTransactionRequest(
                    psbt = psbt
                )
            )
        } else {
            userWalletApiManager.walletApi.createTransaction(
                walletId, CreateOrUpdateServerTransactionRequest(
                    note = note, psbt = psbt
                )
            )
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
        val isClaimWallet = ncDataStore.claimWalletsFlow.first().contains(walletId)
        val transaction = runCatching {
            val response = if (!groupId.isNullOrEmpty()) {
                userWalletApiManager.groupWalletApi.signServerTransaction(
                    groupId = groupId,
                    walletId = walletId,
                    transactionId = txId,
                    payload = SignServerTransactionRequest(psbt = psbt)
                )
            } else if (isClaimWallet) {
                userWalletApiManager.claimWalletApi.createClaimingWalletTransaction(
                    localId = walletId,
                    payload = CreateOrUpdateServerTransactionRequest(psbt = psbt)
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
        if (response.data.inheritance == null) throw IllegalStateException("Can not get inheritance")
        else {
            val inheritance = response.data.inheritance!!.toInheritance()
            inheritance.walletLocalId.also { walletLocalId ->
                updateAssistedWalletBriefExt(walletLocalId, inheritance)
            }
            return inheritance
        }
    }

    override suspend fun markSetupInheritance(walletId: String, isSetupInheritance: Boolean) {
        val entity = assistedWalletDao.getById(walletId) ?: return
        if (entity.isSetupInheritance != isSetupInheritance) {
            assistedWalletDao.updateOrInsert(entity.copy(isSetupInheritance = isSetupInheritance))
        }
    }

    private suspend fun updateAssistedWalletBriefExt(walletId: String, inheritance: Inheritance?) {
        val entity = assistedWalletDao.getById(walletId) ?: return
        val ext = entity.ext?.run {
            gson.fromJson(this, AssistedWalletBriefExt::class.java)
        } ?: AssistedWalletBriefExt()
        assistedWalletDao.updateOrInsert(
            entity.copy(
                ext = gson.toJson(
                    ext.copy(
                        inheritanceOwnerId = inheritance?.ownerId.orEmpty(),
                        isPlanningRequest = inheritance?.pendingRequests?.isNotEmpty() == true
                    )
                ),
                isSetupInheritance = inheritance != null
                        && inheritance.status != InheritanceStatus.PENDING_CREATION
                        && inheritance.status != InheritanceStatus.PENDING_APPROVAL,
            )
        )
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
            runCatching {
                nunchukNativeSdk.importPsbt(walletId, serverTransaction.psbt.orEmpty())
            }
            nunchukNativeSdk.updateTransaction(
                walletId,
                transactionId,
                serverTransaction.transactionId.orEmpty(),
                serverTransaction.hex.orEmpty(),
                serverTransaction.rejectMsg.orEmpty()
            ) to serverTransaction
        } else if (serverTransaction != null) {
            val libTx = try {
                nunchukNativeSdk.importPsbt(walletId, serverTransaction.psbt.orEmpty())
            } catch (e: Exception) {
                nunchukNativeSdk.getTransaction(walletId, transactionId)
            }
            updateReplaceTransactionIdIfNeed(walletId, libTx, serverTransaction)
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
            try {
                libTx to response.data.transaction
            } catch (e: Exception) {
                libTx to null
            }
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

    override suspend fun requestSignatureTransaction(
        groupId: String,
        walletId: String,
        transactionId: String,
        membershipId: String
    ) {
        val response = userWalletApiManager.groupWalletApi.requestSignatureTransaction(
            groupId = groupId,
            walletId = walletId,
            transactionId = transactionId,
            payload = RequestSignatureTransactionRequest(
                membershipId = membershipId
            )
        )
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun updatePrimaryOwner(
        groupId: String,
        walletId: String,
        primaryMembershipId: String
    ) {
        val response = userWalletApiManager.groupWalletApi.updatePrimaryOwner(
            groupId = groupId,
            walletId = walletId,
            payload = UpdatePrimaryOwnerRequest(
                primaryMembershipId = primaryMembershipId
            )
        )
        if (response.isSuccess.not()) {
            throw response.error
        }
        assistedWalletDao.getById(walletId)?.let {
            assistedWalletDao.updateOrInsert(it.copy(primaryMembershipId = primaryMembershipId))
        }
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
            nunchukNativeSdk.updateTransactionSchedule(walletId, transactionId, -1)
        }
        return response.data.transaction?.toServerTransaction()
            ?: throw NullPointerException("transaction from server null")
    }

    override suspend fun getLockdownPeriod(groupId: String?): List<Period> {
        val response =
            if (groupId.isNullOrEmpty()) userWalletApiManager.walletApi.getLockdownPeriod() else userWalletApiManager.groupWalletApi.getLockdownPeriod()
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
                Timber.d("${transition.transactionId} - $transition")
                if (transition.psbt.isNullOrEmpty().not()) {
                    val importTx = nunchukNativeSdk.importPsbt(walletId, transition.psbt)
                    if (transition.note.isNullOrEmpty().not() && importTx.memo != transition.note) {
                        nunchukNativeSdk.updateTransactionMemo(
                            walletId, importTx.txId, transition.note
                        )
                    }
                    updateScheduleTransactionIfNeed(
                        walletId, transition.transactionId.orEmpty(), transition
                    )

                    updateReplaceTransactionIdIfNeed(walletId, importTx, transition)

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
        } else if (transaction.type != ServerTransactionType.SCHEDULED) {
            nunchukNativeSdk.updateTransactionSchedule(walletId, transactionId, -1)
        }
    }

    private fun updateReplaceTransactionIdIfNeed(
        walletId: String, localTx: Transaction, serverTx: TransactionServerDto,
    ) {
        if (!serverTx.replaceTxId.isNullOrEmpty() && localTx.replacedTxid != serverTx.replaceTxId) {
            nunchukNativeSdk.replaceTransactionId(
                walletId, serverTx.transactionId.orEmpty(), serverTx.replaceTxId
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

    override suspend fun getGroupAssistedWalletConfig(): GroupConfig {
        val response = userWalletApiManager.walletApi.getGroupAssistedWalletConfig()
        val walletCounts = mutableMapOf<String, Int>()
        response.data.byzantine?.let {
            walletCounts["byzantine"] = it.remainingWalletCount
        }
        response.data.byzantinePro?.let {
            walletCounts["byzantine_pro"] = it.remainingWalletCount
        }
        response.data.honeyBadger?.let {
            walletCounts["honey_badger"] = it.remainingWalletCount
        }
        response.data.premier?.let {
            walletCounts["premier"] = it.remainingWalletCount
        }
        response.data.finney?.let {
            walletCounts["finney"] = it.remainingWalletCount
        }
        response.data.finneyPro?.let {
            walletCounts["finney_pro"] = it.remainingWalletCount
        }
        response.data.ironHand?.let {
            walletCounts["iron_hand"] = it.remainingWalletCount
        }
        response.data.honeyBadgerPlus?.let {
            walletCounts["honey_badger_plus"] = it.remainingWalletCount
        }
        response.data.honeyBadgerPremier?.let {
            walletCounts["honey_badger_premier"] = it.remainingWalletCount
        }

        return GroupConfig(
            walletsCount = walletCounts,
            personalOptions = response.data.personalWalletTypes.mapNotNull { it.toWalletOption() },
            groupOptions = response.data.groupWalletTypes.mapNotNull { it.toWalletOption() }
        )
    }

    override fun getAssistedWalletsLocal(): Flow<List<AssistedWalletBrief>> {
        return assistedWalletDao.getAssistedWalletsFlow().map { list ->
            list.map { wallet ->
                AssistedWalletBrief(
                    localId = wallet.localId,
                    plan = wallet.plan,
                    isSetupInheritance = wallet.isSetupInheritance,
                    registerAirgapCount = wallet.registerAirgapCount,
                    groupId = wallet.groupId,
                    primaryMembershipId = wallet.primaryMembershipId,
                    ext = wallet.ext?.run {
                        gson.fromJson(this, AssistedWalletBriefExt::class.java)
                    } ?: AssistedWalletBriefExt(),
                    alias = wallet.alias,
                    status = wallet.status,
                    replaceByWalletId = wallet.replaceByWalletId,
                    hideFiatCurrency = wallet.hideFiatCurrency,
                    walletType = wallet.walletType,
                )
            }
        }
    }

    override suspend fun clearLocalData() {
        assistedWalletDao.deleteAll()
        groupDao.deleteAll()
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

    override suspend fun syncDeletedTransaction(groupId: String?, walletId: String) {
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

    override suspend fun getPermissionGroupWallet(type: GroupWalletType): DefaultPermissions {
        val response = userWalletApiManager.groupWalletApi.getPermissionGroupWallet(
            n = type.n,
            m = type.m,
            allowInheritance = type.allowInheritance,
            requiredServerKey = type.requiredServerKey
        )
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

    override suspend fun updateServerKeyName(xfp: String, name: String, path: String) {
        val response =
            userWalletApiManager.walletApi.updateServerKey(xfp, UpdateKeyPayload(name), path)
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
                        xpub = key.xpub.orEmpty(),
                        userKeyFileName = "",
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

    private suspend fun getAllowInheritanceFromDraftWallet(groupId: String): Boolean {
        val response = if (groupId.isEmpty()) {
            userWalletApiManager.walletApi.getDraftWallet()
        } else {
            userWalletApiManager.groupWalletApi.getDraftWallet(groupId)
        }
        return response.data.draftWallet?.walletConfig?.allowInheritance ?: false
    }

    override suspend fun syncKey(
        groupId: String, step: MembershipStep, signer: SingleSigner, walletType: WalletType,
    ) {
        val allowInheritance = if (walletType == WalletType.MINISCRIPT) {
            getAllowInheritanceFromDraftWallet(groupId)
        } else {
            false
        }
        val index = step.toIndex(walletType, allowInheritance)
        val signerDto = serverSignerMapper(
            signer, step.isAddInheritanceKey
        ).copy(index = index)
        val response = if (groupId.isNotEmpty())
            userWalletApiManager.groupWalletApi.addKeyToServer(groupId, signerDto)
        else
            userWalletApiManager.walletApi.addKeyToServer(signerDto)
        if (response.isSuccess.not()) {
            throw response.error
        }
        pushEventManager.push(PushEvent.KeyAddedToGroup)
    }

    override suspend fun requestAddKey(
        groupId: String,
        step: MembershipStep,
        tags: List<SignerTag>,
        walletType: WalletType,
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
            val allowInheritance = if (walletType == WalletType.MINISCRIPT) {
                getAllowInheritanceFromDraftWallet(groupId)
            } else {
                false
            }
            val desktopKeyRequest = if (walletType == WalletType.MINISCRIPT) {
                DesktopKeyRequest(
                    tags = tags.map { it.name },
                    keyIndex = step.toIndex(walletType, allowInheritance),
                    keyIndices = step.toPairIndex(walletType, allowInheritance)
                )
            } else {
                DesktopKeyRequest(
                    tags = tags.map { it.name },
                    keyIndex = step.toIndex(walletType)
                )
            }
            val response =
                if (groupId.isNotEmpty()) userWalletApiManager.groupWalletApi.requestAddKey(
                    groupId,
                    desktopKeyRequest
                )
                else userWalletApiManager.walletApi.requestAddKey(desktopKeyRequest)
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
        val steps = membershipRepository.getSteps(plan, groupId).first()
        val signerFingerprints = steps.map { it.masterSignerId }.toSet()
        localRequests.forEach { localRequest ->
            val response = if (groupId.isNotEmpty()) {
                userWalletApiManager.groupWalletApi.getRequestAddKeyStatus(
                    groupId, localRequest.requestId
                )
            } else {
                userWalletApiManager.walletApi.getRequestAddKeyStatus(localRequest.requestId)
            }
            val request = response.data.request
            if (request?.status == "COMPLETED") {
                val isMiniscript = request.walletType == WalletType.MINISCRIPT.name
                val keysToProcess = if (isMiniscript) {
                    request.keys.orEmpty()
                } else {
                    request.key?.let { listOf(it) }.orEmpty()
                }

                if (keysToProcess.isEmpty()) {
                    // No keys to process, handle as before
                    if (request.key == null && request.keys.isNullOrEmpty()) {
                        requestAddKeyDao.delete(localRequest)
                        throw RequestAddKeyCancelException
                    }
                }

                keysToProcess.forEach { key ->
                    val type = nunchukNativeSdk.signerTypeFromStr(key.type.orEmpty())

                    val hasSigner = nunchukNativeSdk.hasSigner(
                        SingleSigner(
                            name = key.name.orEmpty(),
                            xpub = key.xpub.orEmpty(),
                            publicKey = key.pubkey.orEmpty(),
                            derivationPath = key.derivationPath.orEmpty(),
                            masterFingerprint = key.xfp.orEmpty(),
                        )
                    )
                    if (!hasSigner) {
                        nunchukNativeSdk.createSigner(
                            name = key.name.orEmpty(),
                            xpub = key.xpub.orEmpty(),
                            publicKey = key.pubkey.orEmpty(),
                            derivationPath = key.derivationPath.orEmpty(),
                            masterFingerprint = key.xfp.orEmpty(),
                            type = type,
                            tags = key.tags.orEmpty().mapNotNull { tag -> tag.toSignerTag() },
                            replace = false
                        )
                    }

                    if (!signerFingerprints.contains(key.xfp.orEmpty())) {
                        membershipRepository.saveStepInfo(
                            MembershipStepInfo(
                                step = localRequest.step,
                                masterSignerId = key.xfp.orEmpty(),
                                plan = plan,
                                verifyType = if (key.tags?.contains(SignerTag.INHERITANCE.name) == true) {
                                    key.userKey?.verificationType.toVerifyType(key.verificationType)
                                } else {
                                    VerifyType.APP_VERIFIED
                                },
                                extraData = gson.toJson(
                                    SignerExtra(
                                        derivationPath = key.derivationPath.orEmpty(),
                                        isAddNew = false,
                                        signerType = type,
                                        userKeyFileName = key.userKey?.fileName.orEmpty()
                                    )
                                ),
                                groupId = groupId
                            )
                        )
                    } else {
                        throw RequestAddSameKeyException
                    }
                }

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

    override suspend fun createDraftWalletTimelock(
        groupId: String?,
        timelockValue: Long,
        timezone: String,
        plan: MembershipPlan
    ) {
        val payload = CreateTimelockPayload(
            timelock = TimelockPayload(
                value = timelockValue,
                timezone = timezone
            )
        )
        val response = if (groupId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.createDraftWalletTimelock(payload)
        } else {
            userWalletApiManager.groupWalletApi.createGroupDraftWalletTimelock(groupId, payload)
        }
        if (response.isSuccess.not()) {
            throw response.error
        }
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
        walletType: String?
    ): ByzantineGroup {
        val response = userWalletApiManager.groupWalletApi.createGroup(
            CreateGroupRequest(
                walletConfig = WalletConfigRequest(
                    allowInheritance,
                    m,
                    n,
                    requiredServerKey
                ),
                setupPreference = setupPreference,
                members = members.map { it.toMemberRequest() },
                walletType = walletType
            )
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

    override fun getGroups(): Flow<List<ByzantineGroup>> = groupDao.getGroupsFlow()
        .map { group ->
            val groups = group.map { serverGroup ->
                serverGroup.toByzantineGroup()
            }
            groups
        }

    override suspend fun getGroupsRemote(): List<ByzantineGroup> {
        return syncer.syncGroups() ?: emptyList()
    }

    override suspend fun syncGroupWallets(): Boolean {
        val response = userWalletApiManager.groupWalletApi.getGroups()
        val groupAssistedKeys = mutableSetOf<String>()
        val groups = response.data.groups.orEmpty()
        val groupIds = groups.asSequence().map { it.id.orEmpty() }.toSet()
        if (groups.isNotEmpty()) {
            groups.forEach {
                when (it.status) {
                    GroupStatus.PENDING_WALLET.name -> {
                        groupWalletRepository.syncDraftWallet(it.id.orEmpty())
                    }

                    GroupStatus.ACTIVE.name -> {
                        syncGroupWallet(it.id.orEmpty(), groupAssistedKeys)
                    }
                }
            }
        }
        val isDeletedWallet = deleteAssistedWallets(groupIds)
        ncDataStore.setGroupAssistedKey(groupAssistedKeys)
        syncer.syncGroups(groups)

        return groups.isNotEmpty() || isDeletedWallet
    }

    override suspend fun hasGroupWallets(): Boolean {
        val response = userWalletApiManager.groupWalletApi.getGroups()
        val groups = response.data.groups.orEmpty()
        return groups.isNotEmpty()
    }

    private suspend fun deleteAssistedWallets(groupIds: Set<String>): Boolean {
        val localGroupWallets =
            assistedWalletDao.getAssistedWallets().filter { it.groupId.isNotEmpty() }
        val deleteGroupIds = localGroupWallets.map { it.groupId }
            .filter { groupId -> groupIds.isEmpty() || groupIds.contains(groupId).not() }
        assistedWalletDao.deletes(localGroupWallets.filter { deleteGroupIds.contains(it.groupId) })

        localGroupWallets.filter { deleteGroupIds.contains(it.groupId) }
            .map { it.localId }.forEach { localId ->
                nunchukNativeSdk.deleteWallet(localId)
            }
        return deleteGroupIds.isNotEmpty()
    }

    override fun getGroup(groupId: String): Flow<ByzantineGroup> {
        return groupDao.getById(
            groupId,
            chatId = accountManager.getAccount().chatId,
            chain = chain.value
        ).map { group ->
            group.toByzantineGroup()
        }
    }

    override suspend fun getLocalGroup(groupId: String): ByzantineGroup? {
        return groupDao.getGroupById(
            groupId,
            chatId = accountManager.getAccount().chatId,
            chain = chain.value
        )?.toByzantineGroup()
    }

    override suspend fun getGroupRemote(groupId: String): ByzantineGroup {
        return syncer.syncGroup(groupId) ?: throw NullPointerException("Can not get group")
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
        val response =
            userWalletApiManager.groupWalletApi.editGroupMember(
                groupId,
                getHeaders(authorizations, verifyToken, securityQuestionToken, confirmCodeToken),
                request
            )
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

    override suspend fun createGroupWallet(
        groupId: String,
        name: String,
        primaryMembershipId: String?,
        sendBsmsEmail: Boolean
    ): CreateWalletResult {
        val response =
            userWalletApiManager.groupWalletApi.createGroupWallet(
                groupId,
                CreateDraftWalletRequest(
                    name = name,
                    primaryMembershipId = primaryMembershipId,
                    sendBsmsEmail = sendBsmsEmail
                )
            )
        val wallet = response.data.wallet ?: throw NullPointerException("Wallet empty")
        saveWalletToLib(wallet, mutableSetOf())
        assistedWalletDao.insert(
            wallet.toEntity().copy(
                groupId = groupId,
                primaryMembershipId = primaryMembershipId.orEmpty()
            )
        )
        membershipStepDao.deleteStepByGroupId(groupId)
        val createdWallet = nunchukNativeSdk.getWallet(wallet.localId.orEmpty())
        return CreateWalletResult(
            wallet = createdWallet,
            requiresRegistration = wallet.requiresRegistration == true
        )
    }

    override suspend fun createPersonalWallet(
        name: String,
        sendBsmsEmail: Boolean
    ): CreateWalletResult {
        val response = userWalletApiManager.walletApi.createWalletFromDraft(
            CreateDraftWalletRequest(
                name = name,
                sendBsmsEmail = sendBsmsEmail
            )
        )
        val wallet = response.data.wallet ?: throw NullPointerException("Wallet empty")
        val chatId = accountManager.getAccount().chatId
        saveWalletToLib(wallet, mutableSetOf())
        assistedWalletDao.insert(wallet.toEntity())
        membershipStepDao.deleteStepByChatId(chain.value, chatId)
        requestAddKeyDao.deleteRequests(chatId, chain.value)
        val createdWallet = nunchukNativeSdk.getWallet(wallet.localId.orEmpty())
        return CreateWalletResult(
            wallet = createdWallet,
            requiresRegistration = wallet.requiresRegistration == true
        )
    }

    override suspend fun syncGroupWallet(
        groupId: String,
        groupAssistedKeys: MutableSet<String>,
    ): WalletServer {
        val response = userWalletApiManager.groupWalletApi.getGroupWallet(groupId)
        val wallet = response.data.wallet ?: throw NullPointerException("Wallet empty")
        val slug = wallet.slug.toMembershipPlan()
        saveWalletToLib(wallet, groupAssistedKeys)
        membershipStepDao.deleteStepByGroupId(groupId)
        requestAddKeyDao.deleteRequests(groupId)
        updateOrInsertWallet(groupId, listOf(wallet))
        if (slug.isAllowSetupInheritance()) {
            applicationScope.launch { // This allows it to run in the background without blocking the return of wallet.toModel()
                runCatching { getInheritance(wallet.localId.orEmpty(), groupId = groupId) }
            }
        }
        return wallet.toModel()
    }

    override fun getAlerts(groupId: String?, walletId: String?): Flow<List<Alert>> {
        return alertDao.getAlertsFlow(
            groupId = groupId.orEmpty(),
            walletId = walletId.orEmpty(),
            chain.value
        ).map { alerts ->
            alerts.map { alert ->
                alert.toAlert()
            }
        }
    }

    override suspend fun getAlertsRemote(groupId: String?, walletId: String?): List<Alert> {
        return syncer.syncAlerts(groupId = groupId, walletId = walletId) ?: emptyList()
    }

    override suspend fun markAlertAsRead(groupId: String?, walletId: String?, alertId: String) {
        if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.markAlertAsRead(groupId, alertId)
        } else if (!walletId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.markAlertAsRead(walletId, alertId)
        } else {
            userWalletApiManager.walletApi.markDraftWalletAlertAsRead(alertId)
        }
    }

    override suspend fun dismissAlert(groupId: String?, walletId: String?, alertId: String) {
        val response = if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.dismissAlert(groupId, alertId)
        } else if (!walletId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.dismissAlert(walletId, alertId)
        } else {
            userWalletApiManager.walletApi.dismissDraftWalletAlert(alertId)
        }
        if (response.isSuccess.not()) throw response.error
    }

    override suspend fun getAlertTotal(groupId: String?, walletId: String?): Int {
        val response = if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.getAlertTotal(groupId)
        } else if (!walletId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.getAlertTotal(walletId)
        } else {
            userWalletApiManager.walletApi.getDraftWalletAlertTotal()
        }
        return response.data.total
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
                groupId = groupId,
                CreateOrUpdateGroupChatRequest(historyPeriodId = historyPeriodId, roomId = roomId)
            )
        }
        if (response.isSuccess.not() || response.data.chat == null) {
            throw response.error
        }
        return response.data.chat!!.toGroupChat()
    }

    override suspend fun getGroupChatByGroupId(groupId: String): GroupChat {
        val response = userWalletApiManager.groupWalletApi.getGroupChat(groupId)
        if (response.isSuccess.not() || response.data.chat == null) {
            throw response.error
        }
        return response.data.chat!!.toGroupChat()
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
        var body: Any? = null
        runCatching {
            body = when (action) {
                TargetAction.EDIT_GROUP_MEMBERS.name -> gson.fromJson(
                    userData,
                    EditGroupMemberRequest.Body::class.java
                )

                TargetAction.UPDATE_SECURITY_QUESTIONS.name -> {
                    val request = gson.fromJson(
                        userData,
                        SecurityQuestionsUpdateRequest::class.java
                    )
                    request.body
                }

                TargetAction.EMERGENCY_LOCKDOWN.name -> {
                    val request = gson.fromJson(
                        userData,
                        LockdownUpdateRequest::class.java
                    )
                    request.body
                }

                TargetAction.DOWNLOAD_KEY_BACKUP.name -> {
                    EmptyRequest()
                }

                TargetAction.CHANGE_EMAIL.name -> {
                    gson.fromJson(
                        userData,
                        ChangeEmailRequest.Body::class.java
                    )
                }

                else -> null
            }
        }
        if (body == null) throw IllegalStateException("Can not request confirmation code")
        val request = ConfirmationCodeRequest(nonce = nonce, body = body)
        val response = userWalletApiManager.walletApi.requestConfirmationCode(
            action = action, payload = request
        )
        if (response.isSuccess.not()) {
            throw response.error
        }
        return Pair(nonce, response.data.codeId.orEmpty())
    }

    override suspend fun verifyConfirmationCode(codeId: String, code: String): String {
        val response = userWalletApiManager.walletApi.verifyConfirmationCode(
            codeId, ConfirmationCodeVerifyRequest(code = code)
        )
        if (response.isSuccess.not()) {
            throw response.error
        }
        return response.data.token.orEmpty()
    }

    override suspend fun syncDeletedWallet(): Boolean {
        val results = mutableListOf<Result<Boolean>>()
        runCatching {
            val deletedWalletResponse = userWalletApiManager.groupWalletApi.getDeletedGroupWallets()
            results.addAll(deletedWalletResponse.data.walletLocalIds.map { localId ->
                runCatching {
                    nunchukNativeSdk.deleteWallet(localId)
                }
            })
        }

        val pageSize = 30
        val statuses = listOf("DELETED")
        
        runCatching {
            results.addAll(
                syncDeletedWalletsWithPagination(
                    fetchWallets = { offset, limit, statusList ->
                        userWalletApiManager.groupWalletApi.getWallets(offset, limit, statusList)
                    },
                    statuses = statuses,
                    pageSize = pageSize
                )
            )
        }

        runCatching {
            results.addAll(
                syncDeletedWalletsWithPagination(
                    fetchWallets = { offset, limit, statusList ->
                        userWalletApiManager.walletApi.getWallets(offset, limit, statusList)
                    },
                    statuses = statuses,
                    pageSize = pageSize
                )
            )
        }

        runCatching {
            results.addAll(
                syncDeletedWalletsWithPagination(
                    fetchWallets = { offset, limit, statusList ->
                        userWalletApiManager.claimWalletApi.getClaimingWallets(
                            offset = offset,
                            limit = limit,
                            statuses = statusList
                        )
                    },
                    statuses = statuses,
                    pageSize = pageSize
                )
            )
        }

        return results.any { it.isSuccess }
    }

    private suspend fun syncDeletedWalletsWithPagination(
        fetchWallets: suspend (Int, Int, List<String>) -> Data<GetWalletsResponse>,
        statuses: List<String>,
        pageSize: Int
    ): List<Result<Boolean>> {
        val results = mutableListOf<Result<Boolean>>()
        var page = 0
        do {
            val response = fetchWallets(page * pageSize, pageSize, statuses)
            val wallets = response.data.wallets
            results.addAll(wallets.map { wallet ->
                runCatching {
                    nunchukNativeSdk.deleteWallet(wallet.localId.orEmpty())
                }
            })
            page++
        } while (wallets.size == pageSize)
        return results
    }

    override suspend fun deleteKeyForReplacedWallet(groupId: String, walletId: String) {
        val localWallets = nunchukNativeSdk.getWallets()
        val response = if (groupId.isNotEmpty()) {
            userWalletApiManager.groupWalletApi.getGroupWallet(groupId = groupId)
        } else {
            userWalletApiManager.walletApi.getWallet(walletId)
        }
        val walletServer = response.data.wallet ?: throw NullPointerException("Can not get wallet")
        val isRemoveKey = walletServer.removeUnusedKeys
        if (isRemoveKey) {
            val localWallet = localWallets.find { it.id == walletServer.localId }
            localWallet?.signers?.forEach { signer ->
                if (!signerExistence(signer, localWallets)) {
                    if (signer.hasMasterSigner) {
                        nunchukNativeSdk.deleteMasterSigner(signer.masterFingerprint)
                    } else {
                        nunchukNativeSdk.deleteRemoteSigner(
                            signer.masterFingerprint,
                            signer.derivationPath
                        )
                    }
                }
            }
        }
    }

    private fun signerExistence(
        signer: SingleSigner,
        wallets: List<Wallet>
    ): Boolean {
        return wallets.any {
            it.signers.any anyLast@{ singleSigner ->
                if (singleSigner.hasMasterSigner) {
                    return@anyLast singleSigner.masterFingerprint == signer.masterFingerprint
                }
                return@anyLast singleSigner.masterFingerprint == signer.masterFingerprint && singleSigner.derivationPath == signer.derivationPath
            }
        }
    }

    override suspend fun deleteKey(xfp: String) {
        userWalletApiManager.walletApi.deleteKey(xfp)
    }

    override suspend fun healthCheckHistory(xfp: String): List<HealthCheckHistory> {
        val response = userWalletApiManager.walletApi.healthCheckHistory(xfp)
        return response.data.history?.map {
            HealthCheckHistory(
                id = it.id.orEmpty(),
                type = it.type.orEmpty(),
                createdTimeMillis = it.createdTimeMillis.orDefault(0),
                walletId = it.payload?.walletId.orEmpty(),
                walletLocalId = it.payload?.walletLocalId.orEmpty(),
                dummyTransactionId = it.payload?.dummyTransactionId.orEmpty(),
                transactionId = it.payload?.transactionId.orEmpty(),
            )
        }.orEmpty()
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

    override suspend fun calculateRequiredSignaturesRecoverKey(xfp: String): CalculateRequiredSignaturesExt {
        val response = userWalletApiManager.walletApi.calculateRequiredSignaturesRecoverKey(xfp)
        return response.data.toCalculateRequiredSignaturesEx()
    }

    override suspend fun requestRecoverKey(
        authorizations: List<String>,
        verifyToken: String,
        securityQuestionToken: String,
        confirmCodeToken: String,
        confirmCodeNonce: String,
        xfp: String
    ) {
        val request = RequestRecoverKeyRequest(nonce = confirmCodeNonce)
        val headers = getHeaders(
            authorizations = authorizations,
            verifyToken = verifyToken,
            securityQuestionToken = securityQuestionToken,
            confirmCodeToken = confirmCodeToken
        )
        val response =
            userWalletApiManager.walletApi.requestRecoverKey(headers, id = xfp, payload = request)
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun recoverKey(
        xfp: String,
        verifyToken: String,
        securityQuestionToken: String
    ): BackupKey {
        val headersMap = getHeaders(
            authorizations = emptyList(),
            verifyToken = verifyToken,
            securityQuestionToken = securityQuestionToken,
            confirmCodeToken = ""
        )
        val response = userWalletApiManager.walletApi.recoverKey(id = xfp, headers = headersMap)
        val key = response.data.key ?: throw NullPointerException("Can not get key")
        return key.toBackupKey()
    }

    override suspend fun markKeyAsRecovered(xfp: String, status: String) {
        val response =
            userWalletApiManager.walletApi.markRecoverStatus(xfp, MarkRecoverStatusRequest(status))
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun denyInheritanceRequestPlanning(
        requestId: String,
        groupId: String,
        walletId: String
    ) {
        val response = userWalletApiManager.walletApi.denyInheritanceRequestPlanning(
            requestId = requestId,
            query = mapOf("group_id" to groupId, "wallet" to walletId)
        )
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun approveInheritanceRequestPlanning(
        requestId: String,
        groupId: String,
        walletId: String
    ) {
        val response = userWalletApiManager.walletApi.approveInheritanceRequestPlanning(
            requestId = requestId,
            query = mapOf("group_id" to groupId, "wallet" to walletId)
        )
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun replaceTransaction(
        groupId: String?,
        walletId: String,
        transactionId: String,
        newTxPsbt: String
    ) {
        val isClaimWallet = ncDataStore.claimWalletsFlow.first().contains(walletId)
        val response = if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.replaceTransaction(
                groupId = groupId,
                walletId = walletId,
                transactionId = transactionId,
                payload = CreateOrUpdateServerTransactionRequest(psbt = newTxPsbt)
            )
        } else if (isClaimWallet) {
            userWalletApiManager.claimWalletApi.replaceClaimingWalletTransaction(
                localId = walletId,
                transactionId = transactionId,
                payload = CreateOrUpdateServerTransactionRequest(psbt = newTxPsbt)
            )
        } else {
            userWalletApiManager.walletApi.replaceTransaction(
                walletId = walletId,
                transactionId = transactionId,
                payload = CreateOrUpdateServerTransactionRequest(psbt = newTxPsbt)
            )
        }

        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun getUserWalletsServer(): List<WalletServer> {
        val result = userWalletApiManager.walletApi.getServerWallet()
        return result.data.wallets.map { it.toModel() }
    }

    override suspend fun getGroupWalletsServer(): List<WalletServer> {
        val response = userWalletApiManager.groupWalletApi.getWallets(0, 30, listOf("ACTIVE"))
        val wallets = response.data.wallets
        return wallets.map { it.toModel() }
    }

    override suspend fun updateKeyType(localSigner: SingleSigner, serverSigner: SignerServer) {
        val tapSigner = if (localSigner.type == SignerType.NFC) {
            val tapSignerStatus =
                nunchukNativeSdk.getTapSignerStatusFromMasterSigner(localSigner.masterSignerId)
            TapSignerPayload(
                cardId = tapSignerStatus.ident,
                version = tapSignerStatus.version,
                birthHeight = tapSignerStatus.birthHeight,
                isTestNet = tapSignerStatus.isTestNet,
                isInheritance = localSigner.tags.find { it == SignerTag.INHERITANCE } != null,
            )
        } else {
            null
        }
        val response = userWalletApiManager.walletApi.updateServerKey(
            xfp = localSigner.masterFingerprint,
            payload = UpdateKeyPayload(
                name = localSigner.name,
                type = localSigner.type.name,
                tags = localSigner.tags.map { it.name },
                tapSignerPayload = tapSigner
            ),
            derivationPath = ""
        )
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun getSavedAddressesRemote(): List<SavedAddress> {
        val chatId = accountManager.getAccount().chatId
        var index = 0
        val remoteList = mutableListOf<SavedAddress>()
        runCatching {
            while (true) {
                val response = userWalletApiManager.walletApi.getSavedAddressList(index)
                if (response.isSuccess.not()) return emptyList()
                val savedAddressList = response.data.addresses.orEmpty().map { it.toSavedAddress() }
                remoteList.addAll(savedAddressList)
                if (response.data.addresses.orEmpty().size < TRANSACTION_PAGE_COUNT) break
                index += TRANSACTION_PAGE_COUNT
            }
        }.onFailure {
            return emptyList()
        }
        val localAddresses =
            savedAddressDao.getSavedAddressList(chatId = chatId, chain = chain.value)
        val remoteAddresses = remoteList.map { it.address }
        val deleteAddresses = localAddresses.filter {
            it.address !in remoteAddresses
        }
        savedAddressDao.updateData(
            updateOrInsertList = remoteList.map {
                it.toSavedAddressEntity(
                    chain = chain.value,
                    chatId = chatId
                )
            },
            deleteList = deleteAddresses
        )
        return remoteList
    }

    override fun getSavedAddressesLocal(): Flow<List<SavedAddress>> {
        return savedAddressDao.getSavedAddressFlow(
            chain = chain.value,
            chatId = accountManager.getAccount().chatId
        )
            .map { savedAddresses ->
                savedAddresses.map { it.toSavedAddress() }
            }
    }

    override suspend fun addOrUpdateSavedAddress(
        address: String,
        label: String,
        isPremiumUser: Boolean
    ) {
        if (isPremiumUser) {
            val response = userWalletApiManager.walletApi.addOrUpdateSavedAddress(
                SavedAddressRequest(address = address, label = label)
            )
            if (response.isSuccess.not()) {
                throw response.error
            }
        }

        savedAddressDao.updateOrInsert(
            SavedAddressEntity(
                address = address,
                label = label,
                chain = chain.value,
                chatId = accountManager.getAccount().chatId
            )
        )
        return
    }

    override suspend fun deleteSavedAddress(address: String, isPremiumUser: Boolean) {
        if (isPremiumUser) {
            val response = userWalletApiManager.walletApi.deleteSavedAddress(address)
            if (response.isSuccess.not()) {
                throw response.error
            }
        }
        val entity = savedAddressDao.getByAddress(
            address,
            chain = chain.value,
            chatId = accountManager.getAccount().chatId
        )
        entity?.let {
            savedAddressDao.delete(entity)
        }
    }

    override suspend fun getHealthReminders(
        groupId: String?,
        walletId: String
    ): List<HealthReminder> {
        val response = if (groupId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.getHealthReminders(walletId)
        } else {
            userWalletApiManager.groupWalletApi.getHealthReminders(groupId, walletId)
        }
        return response.data.reminders.orEmpty().map { it.toHealthReminder() }
    }

    override suspend fun addOrUpdateHealthReminder(
        groupId: String?,
        walletId: String,
        xfps: List<String>,
        frequency: String,
        startDateMillis: Long
    ) {
        val response = if (groupId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.addOrUpdateHealthReminder(
                walletId,
                HealthReminderRequest(
                    xfps = xfps,
                    frequency = frequency,
                    startDateMillis = startDateMillis
                )
            )
        } else {
            userWalletApiManager.groupWalletApi.updateHealthReminder(
                groupId,
                walletId,
                HealthReminderRequest(
                    xfps = xfps,
                    frequency = frequency,
                    startDateMillis = startDateMillis
                )
            )
        }
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun deleteHealthReminder(
        groupId: String?,
        walletId: String,
        xfps: List<String>
    ) {
        val response = if (groupId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.deleteHealthReminder(
                walletId,
                xfps = xfps
            )
        } else {
            userWalletApiManager.groupWalletApi.deleteHealthReminder(
                groupId,
                walletId,
                xfps = xfps
            )
        }
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun skipHealthReminder(groupId: String?, walletId: String, xfp: String) {
        val response = if (groupId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.skipHealthReminder(
                walletId,
                xfp
            )
        } else {
            userWalletApiManager.groupWalletApi.skipHealthReminder(
                groupId,
                walletId,
                xfp
            )
        }
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun replaceKeyStatus(
        groupId: String?,
        walletId: String
    ): ReplaceWalletStatus {
        val response = if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.getReplaceWalletStatus(groupId, walletId)
        } else {
            userWalletApiManager.walletApi.getReplaceWalletStatus(walletId)
        }
        val status = response.data.status ?: return ReplaceWalletStatus()
        val replaceSignerTypes = status.signers.map { it.replaceBy.type.toSignerType() }
        assistedWalletDao.getById(walletId)?.let {
            assistedWalletDao.updateOrInsert(it.copy(replaceSignerTypes = replaceSignerTypes))
        }
        status.signers.map {
            signerGateway.saveServerSignerIfNeed(it.replaceBy)
            it.replacements.forEach { replacement ->
                signerGateway.saveServerSignerIfNeed(replacement)
            }
        }

        return ReplaceWalletStatus(
            pendingReplaceXfps = status.pendingReplaceXfps,
            signers = status.signers.associate { it.xfp to it.replaceBy.toModel() },
            replacements = status.signers.associate { it.xfp to it.replacementsToModel() },
            timelock = status.timelock?.let {
                WalletTimelock(
                    timelockValue = it.value,
                    timezone = it.timezone ?: ""
                )
            }
        )
    }

    override suspend fun finalizeReplaceWallet(
        groupId: String?,
        walletId: String
    ): FinalizeReplaceWalletResult {
        val verifyToken = ncDataStore.passwordToken.first()
        val response = if (groupId.isNullOrEmpty()) {
            userWalletApiManager.walletApi.finalizeReplaceWallet(
                verifyToken = verifyToken,
                walletId = walletId,
            )
        } else {
            userWalletApiManager.groupWalletApi.finalizeReplaceWallet(
                verifyToken = verifyToken,
                groupId = groupId,
                walletId = walletId,
            )
        }

        val wallet = response.data.wallet ?: throw NullPointerException("Wallet empty")
        saveWalletToLib(wallet, mutableSetOf())
        assistedWalletDao.insert(wallet.toEntity())
        val createdWallet = nunchukNativeSdk.getWallet(wallet.localId.orEmpty())
        return FinalizeReplaceWalletResult(
            wallet = createdWallet,
            requiresRegistration = wallet.requiresRegistration == true
        )
    }

    override suspend fun initWallet(
        walletConfig: WalletConfig,
        walletType: WalletType?
    ) {
        val response = userWalletApiManager.walletApi.initDraftWallet(
            InitWalletConfigRequest(
                walletConfig = WalletConfigDto(
                    m = walletConfig.m,
                    n = walletConfig.n,
                    requiredServerKey = walletConfig.requiredServerKey,
                    allowInheritance = walletConfig.allowInheritance
                ),
                walletType = walletType?.name
            )
        )
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun removeKeyReplacement(groupId: String?, walletId: String, xfp: String) {
        val verifyToken = ncDataStore.passwordToken.first()
        val response = if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.removeKeyReplacement(
                verifyToken = verifyToken,
                groupId = groupId,
                walletId = walletId,
                xfp = xfp
            )
        } else {
            userWalletApiManager.walletApi.removeKeyReplacement(
                verifyToken = verifyToken,
                walletId = walletId,
                xfp = xfp
            )
        }

        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    override suspend fun getUserWalletConfigsSetup(): UserWalletConfigsSetup {
        val response = userWalletApiManager.walletApi.getUserWalletConfigsSetup()
        if (response.isSuccess.not()) {
            throw response.error
        }
        return response.data.toDomain()
    }

    override suspend fun changeTimelockType(
        groupId: String?,
        walletId: String
    ): DraftWallet {
        val response = if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.changeTimelockType(groupId, walletId)
        } else {
            userWalletApiManager.walletApi.changeTimelockType(walletId)
        }
        if (response.isSuccess.not()) {
            throw response.error
        }
        val draftWallet =
            response.data.draftWallet ?: throw NullPointerException("draftWallet null")
        return DraftWallet(
            groupId = draftWallet.groupId,
            config = draftWallet.walletConfig.toModel(),
            isMasterSecurityQuestionSet = draftWallet.isMasterSecurityQuestionSet,
            signers = draftWallet.signers.map { it.toModel() },
            walletType = draftWallet.walletType.toWalletType(),
            timelock = draftWallet.timelock?.value ?: 0L,
            replaceWallet = draftWallet.replaceWallet.toModel()
        )
    }

    override suspend fun replaceTimelock(
        groupId: String?,
        walletId: String,
        timelockValue: Long,
        timezone: String
    ) {
        val verifyToken = ncDataStore.passwordToken.first()
        val payload = CreateTimelockPayload(
            timelock = TimelockPayload(
                value = timelockValue,
                timezone = timezone
            )
        )
        val response = if (!groupId.isNullOrEmpty()) {
            userWalletApiManager.groupWalletApi.replaceTimelock(
                verifyToken = verifyToken,
                groupId = groupId,
                walletId = walletId,
                payload = payload
            )
        } else {
            userWalletApiManager.walletApi.replaceTimelock(
                verifyToken = verifyToken,
                walletId = walletId,
                payload = payload
            )
        }
        if (response.isSuccess.not()) {
            throw response.error
        }
    }

    private fun getHeaders(
        authorizations: List<String>,
        verifyToken: String,
        securityQuestionToken: String,
        confirmCodeToken: String,
    ): MutableMap<String, String> {
        val headers = mutableMapOf<String, String>()
        authorizations.forEachIndexed { index, value ->
            headers["$AUTHORIZATION_X-${index + 1}"] = value
        }
        headers[VERIFY_TOKEN] = verifyToken
        headers[SECURITY_QUESTION_TOKEN] = securityQuestionToken
        headers[CONFIRMATION_TOKEN] = confirmCodeToken
        return headers
    }

    companion object {
        private const val WALLET_DELETED_STATUS = "DELETED"
        private const val VERIFY_TOKEN = "Verify-token"
        private const val SECURITY_QUESTION_TOKEN = "Security-Question-token"
        internal const val AUTHORIZATION_X = "AuthorizationX"
        internal const val CONFIRMATION_TOKEN = "Confirmation-token"
    }
}

