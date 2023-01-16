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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
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
    applicationScope: CoroutineScope,
) : MembershipRepository {
    private val chain = ncDataStore.chain.stateIn(applicationScope, SharingStarted.Eagerly, Chain.MAIN)
    override fun getSteps(plan: MembershipPlan): Flow<List<MembershipStepInfo>> {
        return ncDataStore.chain.flatMapLatest { chain -> membershipStepDao.getSteps(accountManager.getAccount().chatId, chain, plan) }
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
                plan = info.plan,
                chain = chain.value
            )
        )
    }

    override suspend fun deleteStepBySignerId(masterSignerId: String) {
        membershipStepDao.deleteByMasterSignerId(accountManager.getAccount().chatId, chain.value, masterSignerId)
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
        membershipStepDao.deleteStepByEmail(chain.value, accountManager.getAccount().chatId)
    }

    override fun getLocalCurrentPlan(): Flow<MembershipPlan> = ncDataStore.membershipPlan

    override fun isRegisterAirgap(): Flow<Boolean> = ncDataStore.isRegisterAirgap
    override fun isSetupInheritance(): Flow<Boolean> = ncDataStore.isSetupInheritance

    override fun isRegisterColdcard(): Flow<Boolean> = ncDataStore.isRegisterColdCard
    override fun isHideUpsellBanner(): Flow<Boolean> = ncDataStore.isHideUpsellBanner

    override suspend fun setRegisterAirgap(value: Boolean) = ncDataStore.setRegisterAirgap(value)
    override suspend fun setSetupInheritance(value: Boolean) = ncDataStore.setSetupInheritance(value)

    override suspend fun setRegisterColdcard(value: Boolean) = ncDataStore.setRegisterColdcard(value)
    override suspend fun setHideUpsellBanner() = ncDataStore.setHideUpsellBanner()
}