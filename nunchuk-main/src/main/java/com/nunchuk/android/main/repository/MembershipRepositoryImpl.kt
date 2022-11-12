package com.nunchuk.android.main.repository

import com.nunchuk.android.api.key.MembershipApi
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.MemberSubscription
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.persistence.entity.MembershipStepEntity
import com.nunchuk.android.persistence.entity.toModel
import com.nunchuk.android.persistence.updateOrInsert
import com.nunchuk.android.repository.MembershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MembershipRepositoryImpl @Inject constructor(
    private val membershipStepDao: MembershipStepDao,
    private val accountManager: AccountManager,
    private val membershipApi: MembershipApi,
    private val nativeSdk: NunchukNativeSdk,
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
        membershipStepDao.deleteByMasterSignerId(masterSignerId)
    }

    override suspend fun getSubscription(): MemberSubscription {
        val result = membershipApi.getCurrentSubscription()
        if (result.isSuccess) {
            val data = result.data
            return MemberSubscription(data.subscriptionId, data.plan?.slug)
        } else {
            throw result.error
        }
    }

    override suspend fun restart(plan: MembershipPlan) {
        val steps = getSteps(plan).first()
        steps.filter { it.masterSignerId.isNotEmpty() }.forEach {
            nativeSdk.deleteMasterSigner(it.masterSignerId)
        }
        membershipStepDao.deleteStepByEmail(accountManager.getAccount().email)
    }
}