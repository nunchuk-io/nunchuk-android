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

package com.nunchuk.android.main.repository

import com.google.gson.Gson
import com.nunchuk.android.api.key.MembershipApi
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.persistence.NCSharePreferences
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.MemberSubscription
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SignerExtra
import com.nunchuk.android.model.toMembershipPlan
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.persistence.dao.AssistedWalletDao
import com.nunchuk.android.persistence.dao.GroupDao
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.persistence.entity.MembershipStepEntity
import com.nunchuk.android.persistence.entity.toModel
import com.nunchuk.android.repository.MembershipRepository
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.SignerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

class MembershipRepositoryImpl @Inject constructor(
    private val membershipStepDao: MembershipStepDao,
    private val accountManager: AccountManager,
    private val membershipApi: MembershipApi,
    private val nativeSdk: NunchukNativeSdk,
    private val gson: Gson,
    private val ncDataStore: NcDataStore,
    private val ncSharePreferences: NCSharePreferences,
    private val assistedWalletDao: AssistedWalletDao,
    private val groupDao: GroupDao,
    applicationScope: CoroutineScope,
) : MembershipRepository {
    private val chain =
        ncDataStore.chain.stateIn(applicationScope, SharingStarted.Eagerly, Chain.MAIN)

    override fun getSteps(plan: MembershipPlan, groupId: String): Flow<List<MembershipStepInfo>> {
        return ncDataStore.chain.flatMapLatest { chain ->
            if (groupId.isNotEmpty()) {
                membershipStepDao.getSteps(
                    accountManager.getAccount().chatId,
                    chain,
                    groupId
                )
            } else {
                membershipStepDao.getSteps(
                    accountManager.getAccount().chatId,
                    chain,
                    plan
                )
            }
        }.map {
            it.map { entity -> entity.toModel() }
        }
    }

    override suspend fun saveStepInfo(info: MembershipStepInfo) {
        membershipStepDao.updateOrInsert(
            listOf(
                MembershipStepEntity(
                    chatId = accountManager.getAccount().chatId,
                    step = info.step,
                    masterSignerId = info.masterSignerId,
                    verifyType = info.verifyType,
                    id = info.id,
                    extraJson = info.extraData,
                    keyIdInServer = info.keyIdInServer,
                    plan = info.plan,
                    chain = chain.value,
                    groupId = info.groupId
                )
            )
        )
    }

    override suspend fun deleteStepBySignerId(masterSignerId: String) {
        membershipStepDao.deleteByMasterSignerId(
            accountManager.getAccount().chatId,
            chain.value,
            masterSignerId
        )
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
                        || (data.status == "ACTIVE" && Calendar.getInstance().timeInMillis <= data.graceValidUntilUtcMillis))
            ) {
                data.plan?.slug.toMembershipPlan()
            } else if (chain == Chain.MAIN) {
                MembershipPlan.NONE
            } else {
                data.plan?.slug.toMembershipPlan()
            }
            ncDataStore.setMembershipPlan(plan)
            return MemberSubscription(data.subscriptionId, data.plan?.slug, plan)
        } else {
            throw result.error
        }
    }

    override suspend fun restart(plan: MembershipPlan, groupId: String) {
        val steps = getSteps(plan, groupId).firstOrNull().orEmpty()
        steps.filter { it.masterSignerId.isNotEmpty() }.forEach {
            runCatching {
                gson.fromJson(it.extraData, SignerExtra::class.java)
                    ?.takeIf { extra -> extra.isAddNew }
                    ?.let { extra ->
                        if (extra.signerType == SignerType.NFC) {
                            nativeSdk.deleteMasterSigner(it.masterSignerId)
                        } else {
                            nativeSdk.deleteRemoteSigner(it.masterSignerId, extra.derivationPath)
                        }
                    }
            }
        }
        if (groupId.isNotEmpty()) {
            membershipStepDao.deleteStepByGroupId(groupId)
        } else {
            membershipStepDao.deleteStepByChatId(chain.value, accountManager.getAccount().chatId)
        }
    }

    override fun getLocalCurrentPlan(): Flow<MembershipPlan> = ncDataStore.membershipPlan

    override fun isHideUpsellBanner(): Flow<Boolean> = ncDataStore.isHideUpsellBanner

    override suspend fun setRegisterAirgap(walletId: String, value: Int) {
        val entity = assistedWalletDao.getById(walletId) ?: return
        assistedWalletDao.update(entity.copy(registerAirgapCount = entity.registerAirgapCount + value))
    }

    override suspend fun setHideUpsellBanner() = ncDataStore.setHideUpsellBanner()

    override suspend fun isViewPendingWallet(groupId: String): Boolean {
        return groupDao.getGroupById(
            groupId,
            accountManager.getAccount().chatId,
            chain.value
        )?.isViewPendingWallet ?: false
    }

    override suspend fun setViewPendingWallet(groupId: String) {
        groupDao.getGroupById(
            groupId,
            accountManager.getAccount().chatId,
            chain.value
        )?.let {
            groupDao.updateOrInsert(it.copy(isViewPendingWallet = true))
        }
    }
}