package com.nunchuk.android.main.repository

import com.google.gson.Gson
import com.nunchuk.android.api.key.MembershipApi
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.model.MemberSubscription
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.persistence.entity.MembershipStepEntity
import com.nunchuk.android.persistence.entity.toModel
import com.nunchuk.android.persistence.updateOrInsert
import com.nunchuk.android.repository.MembershipRepository
import com.nunchuk.android.type.SignerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MembershipRepositoryImpl @Inject constructor(
    private val membershipStepDao: MembershipStepDao,
    private val accountManager: AccountManager,
    private val membershipApi: MembershipApi,
    private val nativeSdk: NunchukNativeSdk,
    private val gson: Gson,
    private val ncDataStore: NcDataStore,
) : MembershipRepository {
    override fun getSteps(plan: MembershipPlan): Flow<List<MembershipStepInfo>> {
        return membershipStepDao.getSteps(accountManager.getAccount().email, plan)
            .map {
                it.map { entity -> entity.toModel() }
            }
    }

    override suspend fun saveStepInfo(info: MembershipStepInfo) {
        membershipStepDao.updateOrInsert(
            MembershipStepEntity(
                email = accountManager.getAccount().email,
                step = info.step,
                masterSignerId = info.masterSignerId,
                isVerify = info.isVerify,
                id = info.id,
                extraJson = info.extraData,
                keyIdInServer = info.keyIdInServer,
                plan = info.plan
            )
        )
    }

    override suspend fun deleteStepBySignerId(masterSignerId: String) {
        membershipStepDao.deleteByMasterSignerId(accountManager.getAccount().email, masterSignerId)
    }

    override suspend fun getSubscription(): MemberSubscription {
        val result = membershipApi.getCurrentSubscription()
        if (result.isSuccess) {
            val data = result.data
            val plan = if (data.plan?.slug == IRON_HAND_PLAN) {
                MembershipPlan.IRON_HAND
            } else if (data.plan?.slug == HONEY_BADGER_PLAN) {
                MembershipPlan.HONEY_BADGER
            } else {
                MembershipPlan.NONE
            }
            ncDataStore.setMembershipPlan(plan)
            return MemberSubscription(data.subscriptionId, data.plan?.slug)
        } else {
            throw result.error
        }
    }

    override suspend fun restart(plan: MembershipPlan) {
        val steps = getSteps(plan).first()
        steps.filter { it.masterSignerId.isNotEmpty() }.forEach {
            runCatching { gson.fromJson(it.extraData, SignerExtra::class.java) }
                .getOrNull()
                ?.takeIf { extra -> extra.isAddNew }
                ?.let { extra ->
                    if (extra.signerType == SignerType.NFC) {
                        nativeSdk.deleteMasterSigner(it.masterSignerId)
                    } else {
                        nativeSdk.deleteRemoteSigner(it.masterSignerId, extra.derivationPath)
                    }
                }
        }
        membershipStepDao.deleteStepByEmail(accountManager.getAccount().email)
    }


    companion object {
        private const val IRON_HAND_PLAN = "iron_hand"
        private const val HONEY_BADGER_PLAN = "honey_badger"
    }
}