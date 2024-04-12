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

package com.nunchuk.android.main.manager

import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.MembershipPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal class AssistedWalletManagerImpl @Inject constructor(
    getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    ncDataStore: NcDataStore,
    applicationScope: CoroutineScope,
) : AssistedWalletManager {
    private val plan = ncDataStore.localMembershipPlan.stateIn(
        applicationScope,
        SharingStarted.Eagerly,
        MembershipPlan.NONE
    )

    private val _assistedWalletBrief =
        getAssistedWalletsFlowUseCase(Unit).map { wallets ->
            wallets.getOrElse { emptyList() }.associateBy { it.localId }
        }.stateIn(applicationScope, SharingStarted.Eagerly, emptyMap())

    override fun isActiveAssistedWallet(walletId: String): Boolean {
        return _assistedWalletBrief.value[walletId] != null
                && (plan.value != MembershipPlan.NONE || !getGroupId(walletId).isNullOrEmpty())
    }

    override fun getGroupId(walletId: String): String? {
        return _assistedWalletBrief.value[walletId]?.groupId
    }

    override fun isInactiveAssistedWallet(walletId: String): Boolean {
        return _assistedWalletBrief.value[walletId] != null
                && (plan.value == MembershipPlan.NONE && getGroupId(walletId).isNullOrEmpty())
    }

    override fun getWalletAlias(walletId: String): String {
        return _assistedWalletBrief.value[walletId]?.alias.orEmpty()
    }

    override fun getWalletPlan(walletId: String): MembershipPlan {
        return _assistedWalletBrief.value[walletId]?.plan ?: MembershipPlan.NONE
    }
}