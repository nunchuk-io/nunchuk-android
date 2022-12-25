package com.nunchuk.android.main.repository

import com.google.gson.Gson
import com.nunchuk.android.api.key.MembershipApi
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.persistence.NCSharePreferences
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.model.*
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.persistence.entity.MembershipStepEntity
import com.nunchuk.android.persistence.entity.toModel
import com.nunchuk.android.persistence.updateOrInsert
import com.nunchuk.android.repository.MembershipRepository
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject

class MembershipRepositoryImpl @Inject constructor(
    private val membershipStepDao: MembershipStepDao,
    private val accountManager: AccountManager,
    private val membershipApi: MembershipApi,
    private val nativeSdk: NunchukNativeSdk,
    private val gson: Gson,
    private val ncDataStore: NcDataStore,
    private val ncSharePreferences: NCSharePreferences,
) : MembershipRepository {
    override fun getSteps(plan: MembershipPlan): Flow<List<MembershipStepInfo>> {
        return membershipStepDao.getSteps(accountManager.getAccount().chatId, plan)
            .map {
                it.map { entity -> entity.toModel() }
            }
    }

    override suspend fun saveStepInfo(info: MembershipStepInfo) {
        membershipStepDao.updateOrInsert(
            MembershipStepEntity(
                chatId = accountManager.getAccount().chatId,
                step = info.step,
                masterSignerId = info.masterSignerId,
                verifyType = info.verifyType,
                id = info.id,
                extraJson = info.extraData,
                keyIdInServer = info.keyIdInServer,
                plan = info.plan
            )
        )
    }

    override suspend fun deleteStepBySignerId(masterSignerId: String) {
        membershipStepDao.deleteByMasterSignerId(accountManager.getAccount().chatId, masterSignerId)
    }

    override suspend fun getSubscription(): MemberSubscription {
        val chain = gson.fromJson(
            ncSharePreferences.appSettings,
            AppSettings::class.java
        )?.chain ?: Chain.MAIN
        var result = membershipApi.getCurrentSubscription()
        if (result.isSuccess.not() && chain != Chain.MAIN) {
            result = membershipApi.getTestnetCurrentSubscription()
        }
        if (result.isSuccess) {
            val data = result.data
            val plan = if (chain == Chain.MAIN &&
                (data.status == "PENDING"
                        || (data.status == "ACTIVE" && Calendar.getInstance().timeInMillis < data.validUntilUtcMillis))
            ) {
                data.plan?.slug.toMembershipPlan()
            } else {
                if (chain == Chain.MAIN) MembershipPlan.NONE else data.plan?.slug.toMembershipPlan()
            }
            ncDataStore.setMembershipPlan(plan)
            return MemberSubscription(data.subscriptionId, data.plan?.slug, plan)
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
        membershipStepDao.deleteStepByEmail(accountManager.getAccount().chatId)
    }

    override fun getLocalCurrentPlan(): Flow<MembershipPlan> = ncDataStore.membershipPlan

    override fun isRegisterAirgap(): Flow<Boolean> = ncDataStore.isRegisterAirgap
    override fun isSetupInheritance(): Flow<Boolean> = ncDataStore.isSetupInheritance

    override fun isRegisterColdcard(): Flow<Boolean> = ncDataStore.isRegisterColdCard

    override suspend fun setRegisterAirgap(value: Boolean) = ncDataStore.setRegisterAirgap(value)
    override suspend fun setSetupInheritance(value: Boolean) = ncDataStore.setSetupInheritance(value)

    override suspend fun setRegisterColdcard(value: Boolean) = ncDataStore.setRegisterColdcard(value)
}