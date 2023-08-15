package com.nunchuk.android.core.repository

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.data.model.byzantine.DraftWalletDto
import com.nunchuk.android.core.data.model.byzantine.ReuseFromGroupRequest
import com.nunchuk.android.core.data.model.byzantine.toModel
import com.nunchuk.android.core.data.model.membership.SignerServerDto
import com.nunchuk.android.core.data.model.membership.toModel
import com.nunchuk.android.core.manager.UserWalletApiManager
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
import com.nunchuk.android.model.byzantine.SimilarGroup
import com.nunchuk.android.model.toVerifyType
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.repository.GroupWalletRepository
import com.nunchuk.android.repository.MembershipRepository
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal class GroupWalletRepositoryImpl @Inject constructor(
    private val userWalletApiManager: UserWalletApiManager,
    private val accountManager: AccountManager,
    private val membershipRepository: MembershipRepository,
    private val membershipStepDao: MembershipStepDao,
    private val ncDataStore: NcDataStore,
    private val nunchukNativeSdk: NunchukNativeSdk,
    applicationScope: CoroutineScope,
): GroupWalletRepository {
    private val chain =
        ncDataStore.chain.stateIn(applicationScope, SharingStarted.Eagerly, Chain.MAIN)
    override suspend fun findSimilarGroup(groupId: String): List<SimilarGroup> {
        return userWalletApiManager.groupWalletApi.findSimilar(groupId).data.similar.map {
            SimilarGroup(
                walletLocalId = it.walletLocalId.orEmpty(),
                groupId = it.groupId.orEmpty(),
            )
        }.toList()
    }

    override suspend fun reuseGroupWallet(groupId: String, fromGroupId: String): DraftWallet {
        val response = userWalletApiManager.groupWalletApi.reuseFromGroup(groupId, ReuseFromGroupRequest(fromGroupId))
        val draftWallet =
            response.data.draftWallet ?: throw NullPointerException("draftWallet null")
        return handleDraftWallet(draftWallet, groupId)
    }

    override suspend fun syncGroupDraftWallet(groupId: String): DraftWallet {
        val response = userWalletApiManager.groupWalletApi.getDraftWallet(groupId)
        val draftWallet =
            response.data.draftWallet ?: throw NullPointerException("draftWallet null")
        return handleDraftWallet(draftWallet, groupId)
    }

    private suspend fun handleDraftWallet(
        draftWallet: DraftWalletDto,
        groupId: String
    ): DraftWallet {
        val chatId = accountManager.getAccount().chatId
        draftWallet.signers.forEach { key ->
            val signerType = key.type.toSignerType()
            saveServerSignerIfNeed(key)
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
                val verifyType =
                    if (signerType == SignerType.NFC) key.tapsignerKey?.verificationType.toVerifyType() else VerifyType.APP_VERIFIED
                if (info == null || info.masterSignerId != key.xfp || info.verifyType != verifyType) {
                    membershipRepository.saveStepInfo(
                        MembershipStepInfo(
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
        // TODO Hai should remove local key if sync failed
        return DraftWallet(
            config = draftWallet.walletConfig.toModel(),
            isMasterSecurityQuestionSet = draftWallet.isMasterSecurityQuestionSet,
            signers = draftWallet.signers.map { it.toModel() }
        )
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
}