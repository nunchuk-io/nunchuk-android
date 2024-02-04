package com.nunchuk.android.core.repository

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.data.model.byzantine.DraftWalletDto
import com.nunchuk.android.core.data.model.byzantine.HealthCheckRequest
import com.nunchuk.android.core.data.model.byzantine.toDomainModel
import com.nunchuk.android.core.data.model.byzantine.toModel
import com.nunchuk.android.core.data.model.membership.SignerServerDto
import com.nunchuk.android.core.data.model.membership.WalletAliasRequest
import com.nunchuk.android.core.data.model.membership.toModel
import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.core.mapper.toKeyHealthStatus
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.core.signer.toSignerTag
import com.nunchuk.android.core.util.gson
import com.nunchuk.android.core.util.toSignerType
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.ServerKeyExtra
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.byzantine.DraftWallet
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.byzantine.KeyHealthStatus
import com.nunchuk.android.model.byzantine.UserAlias
import com.nunchuk.android.model.toVerifyType
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.persistence.dao.AssistedWalletDao
import com.nunchuk.android.persistence.dao.KeyHealthStatusDao
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.repository.GroupWalletRepository
import com.nunchuk.android.repository.MembershipRepository
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal class GroupWalletRepositoryImpl @Inject constructor(
    private val userWalletApiManager: UserWalletApiManager,
    private val accountManager: AccountManager,
    private val membershipRepository: MembershipRepository,
    private val membershipStepDao: MembershipStepDao,
    private val keyHealthStatusDao: KeyHealthStatusDao,
    ncDataStore: NcDataStore,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val byzantineSyncer: ByzantineSyncer,
    applicationScope: CoroutineScope,
    private val assistedWalletDao: AssistedWalletDao
) : GroupWalletRepository {
    private val chain =
        ncDataStore.chain.stateIn(applicationScope, SharingStarted.Eagerly, Chain.MAIN)

    override suspend fun syncGroupDraftWallet(groupId: String): DraftWallet {
        val response = userWalletApiManager.groupWalletApi.getDraftWallet(groupId)
        val draftWallet =
            response.data.draftWallet ?: throw NullPointerException("draftWallet null")
        return handleDraftWallet(draftWallet, groupId)
    }

    private suspend fun handleDraftWallet(
        draftWallet: DraftWalletDto,
        groupId: String,
    ): DraftWallet {
        val chatId = accountManager.getAccount().chatId
        val newSigner = mutableMapOf<String, Boolean>()
        draftWallet.signers.forEach { key ->
            val signerType = key.type.toSignerType()
            newSigner[key.xfp.orEmpty()] = !saveServerSignerIfNeed(key)
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
                    0 -> if (draftWallet.walletConfig?.allowInheritance == true) MembershipStep.BYZANTINE_ADD_TAP_SIGNER else MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0
                    1 -> if (draftWallet.walletConfig?.n == GroupWalletType.THREE_OF_FIVE_INHERITANCE.n && draftWallet.walletConfig.allowInheritance) MembershipStep.BYZANTINE_ADD_TAP_SIGNER_1 else MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1
                    2 -> MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2
                    3 -> MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3
                    4 -> MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4
                    else -> throw IllegalArgumentException()
                }
                val info = membershipStepDao.getStep(chatId, chain.value, step, groupId)
                val verifyType =
                    if (signerType == SignerType.NFC) key.tapsignerKey?.verificationType.toVerifyType() else VerifyType.APP_VERIFIED
                if (info == null || info.masterSignerId != key.xfp || info.verifyType != verifyType) {
                    membershipRepository.saveStepInfo(
                        MembershipStepInfo(
                            id = info?.id ?: 0L,
                            step = step,
                            masterSignerId = key.xfp.orEmpty(),
                            plan = MembershipPlan.BYZANTINE,
                            verifyType = verifyType,
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
        }
        handleUpdateServerSigners(draftWallet.signers, newSigner)
        return DraftWallet(
            config = draftWallet.walletConfig.toModel(),
            isMasterSecurityQuestionSet = draftWallet.isMasterSecurityQuestionSet,
            signers = draftWallet.signers.map { it.toModel() }
        )
    }

    private fun handleUpdateServerSigners(
        signers: List<SignerServerDto>,
        newSigner: Map<String, Boolean>,
    ) {
        val masterSigners = nunchukNativeSdk.getMasterSigners().associateBy { it.id }
        signers.forEach { signer ->
            val type = nunchukNativeSdk.signerTypeFromStr(signer.type.orEmpty())
            val tags = signer.tags.orEmpty().mapNotNull { it.toSignerTag() }
            if (type != SignerType.SERVER) {
                if (masterSigners.containsKey(signer.xfp.orEmpty())) {
                    val masterSigner = nunchukNativeSdk.getMasterSigner(signer.xfp.orEmpty())
                    val isVisible =
                        if (newSigner[signer.xfp.orEmpty()] == true) signer.isVisible else masterSigner.isVisible || signer.isVisible
                    val isChange =
                        masterSigner.name != signer.name || masterSigner.tags != tags || masterSigner.isVisible != isVisible
                    if (isChange) {
                        nunchukNativeSdk.updateMasterSigner(
                            masterSigner.copy(
                                name = signer.name.orEmpty(),
                                tags = tags,
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
                            if (newSigner[signer.xfp.orEmpty()] == true) signer.isVisible else remoteSigner.isVisible || signer.isVisible
                        val isChange =
                            remoteSigner.name != signer.name || remoteSigner.tags != tags || remoteSigner.isVisible != isVisible
                        if (isChange) {
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
        }
    }

    private fun saveServerSignerIfNeed(signer: SignerServerDto): Boolean {
        val hasSigner = nunchukNativeSdk.hasSigner(
            SingleSigner(
                name = signer.name.orEmpty(),
                xpub = signer.xpub.orEmpty(),
                publicKey = signer.pubkey.orEmpty(),
                derivationPath = signer.derivationPath.orEmpty(),
                masterFingerprint = signer.xfp.orEmpty(),
            )
        )
        if (hasSigner) return true
        val tapsigner = signer.tapsigner
        if (tapsigner != null) {
            nunchukNativeSdk.addTapSigner(
                cardId = tapsigner.cardId,
                name = signer.name.orEmpty(),
                xfp = signer.xfp.orEmpty(),
                version = tapsigner.version.orEmpty(),
                brithHeight = tapsigner.birthHeight,
                isTestNet = tapsigner.isTestnet
            )
        } else {
            val type = nunchukNativeSdk.signerTypeFromStr(signer.type.orEmpty())
            nunchukNativeSdk.createSigner(name = signer.name.orEmpty(),
                xpub = signer.xpub.orEmpty(),
                publicKey = signer.pubkey.orEmpty(),
                derivationPath = signer.derivationPath.orEmpty(),
                masterFingerprint = signer.xfp.orEmpty(),
                type = type,
                tags = signer.tags.orEmpty().mapNotNull { tag -> tag.toSignerTag() })
        }
        return false
    }

    override fun getWalletHealthStatus(
        groupId: String,
        walletId: String,
    ): Flow<List<KeyHealthStatus>> {
        return keyHealthStatusDao.getKeysFlow(
            groupId,
            walletId,
            accountManager.getAccount().chatId,
            chain.value
        ).map {
            it.map { entity -> entity.toKeyHealthStatus() }
        }
    }

    override suspend fun getWalletHealthStatusRemote(
        groupId: String,
        walletId: String,
    ): List<KeyHealthStatus> {
        return byzantineSyncer.syncKeyHealthStatus(groupId, walletId) ?: emptyList()
    }


    override suspend fun requestHealthCheck(groupId: String, walletId: String, xfp: String) {
        val response = userWalletApiManager.groupWalletApi.requestHealthCheck(
            groupId = groupId,
            walletId = walletId,
            xfp = xfp
        )

        if (response.isSuccess.not()) throw response.error
    }

    override suspend fun healthCheck(
        groupId: String,
        walletId: String,
        xfp: String,
        draft: Boolean,
    ): DummyTransactionPayload {
        val response = userWalletApiManager.groupWalletApi.healthCheck(
            groupId = groupId,
            walletId = walletId,
            xfp = xfp,
            draft = draft,
            request = HealthCheckRequest(nonce = getNonce())
        )
        return response.data.dummyTransaction?.toDomainModel()
            ?: throw NullPointerException("dummyTransaction null")
    }

    override suspend fun getWalletAliases(groupId: String, walletId: String): List<UserAlias> {
        val response = userWalletApiManager.groupWalletApi.getWalletAlias(
            groupId = groupId,
            walletId = walletId
        )
        return response.data.walletAlias.map {
            UserAlias(
                membershipId = it.membershipId.orEmpty(),
                alias = it.alias.orEmpty()
            )
        }
    }

    override suspend fun setWalletAlias(groupId: String, walletId: String, alias: String) {
        val response = userWalletApiManager.groupWalletApi.setWalletAlias(
            groupId = groupId,
            walletId = walletId,
            request = WalletAliasRequest(alias = alias)
        )
        if (response.isSuccess.not()) throw response.error
        assistedWalletDao.getByGroupId(groupId)?.let {
            assistedWalletDao.update(it.copy(alias = alias))
        }
    }

    override suspend fun deleteWalletAlias(groupId: String, walletId: String) {
        val response = userWalletApiManager.groupWalletApi.deleteWalletAlias(
            groupId = groupId,
            walletId = walletId
        )
        if (response.isSuccess.not()) throw response.error
        assistedWalletDao.getByGroupId(groupId)?.let {
            assistedWalletDao.update(it.copy(alias = ""))
        }
    }

    private suspend fun getNonce(): String {
        return userWalletApiManager.walletApi.getNonce().data.nonce?.nonce.orEmpty()
    }
}