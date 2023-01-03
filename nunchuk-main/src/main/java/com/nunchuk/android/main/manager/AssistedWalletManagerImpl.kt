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

package com.nunchuk.android.main.manager

import com.nunchuk.android.core.domain.GetAssistedWalletIdFlowUseCase
import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.MembershipPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal class AssistedWalletManagerImpl @Inject constructor(
    getAssistedWalletIdFlowUseCase: GetAssistedWalletIdFlowUseCase,
    ncDataStore: NcDataStore,
    applicationScope: CoroutineScope,
) : AssistedWalletManager {
    private val _assistedWalletId = getAssistedWalletIdFlowUseCase(Unit)
        .map { it.getOrNull().orEmpty() }
        .stateIn(applicationScope, SharingStarted.Eagerly, "")

    private val plan = ncDataStore.membershipPlan.stateIn(
        applicationScope,
        SharingStarted.Eagerly,
        MembershipPlan.NONE
    )

    override val assistedWalletId: Flow<String> = _assistedWalletId

    override fun isActiveAssistedWallet(walletId: String): Boolean {
        return _assistedWalletId.value == walletId && plan.value != MembershipPlan.NONE
    }

    override fun isInactiveAssistedWallet(walletId: String): Boolean {
        return _assistedWalletId.value == walletId && plan.value == MembershipPlan.NONE
    }
}