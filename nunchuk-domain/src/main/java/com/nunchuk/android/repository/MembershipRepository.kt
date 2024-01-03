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

package com.nunchuk.android.repository

import com.nunchuk.android.model.MemberSubscription
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStepInfo
import kotlinx.coroutines.flow.Flow

interface MembershipRepository {
    fun getSteps(plan: MembershipPlan, groupId: String): Flow<List<MembershipStepInfo>>
    suspend fun saveStepInfo(info: MembershipStepInfo)
    suspend fun deleteStepBySignerId(masterSignerId: String)
    suspend fun getSubscription() : MemberSubscription
    suspend fun restart(plan: MembershipPlan, groupId: String)
    fun getLocalCurrentPlan(): Flow<MembershipPlan>
    fun isHideUpsellBanner(): Flow<Boolean>
    suspend fun setRegisterAirgap(walletId: String, value: Int)
    suspend fun setHideUpsellBanner()
    suspend fun setViewPendingWallet(groupId: String)
    suspend fun isViewPendingWallet(groupId: String) : Boolean
}