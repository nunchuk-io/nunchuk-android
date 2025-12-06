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
import com.nunchuk.android.core.domain.membership.GetClaimWalletsFlowUseCase
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.usecase.GetGroupsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal class AssistedWalletManagerImpl @Inject constructor(
    getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    applicationScope: CoroutineScope,
    getGroupsUseCase: GetGroupsUseCase,
    getClaimWalletsFlowUseCase: GetClaimWalletsFlowUseCase,
) : AssistedWalletManager {

    private val _assistedGroups = getGroupsUseCase(Unit).map {
        it.getOrElse { emptyList() }.associateBy { it.id }
    }.stateIn(
        applicationScope,
        SharingStarted.Eagerly,
        emptyMap()
    )

    private val _assistedWalletBrief =
        getAssistedWalletsFlowUseCase(Unit).map { wallets ->
            wallets.getOrElse { emptyList() }.associateBy { it.localId }
        }.stateIn(applicationScope, SharingStarted.Eagerly, emptyMap())

    private val _claimableWallets = getClaimWalletsFlowUseCase(Unit).map {
        it.getOrThrow().toSet()
    }.stateIn(applicationScope, SharingStarted.Eagerly, emptySet())

    override fun isActiveAssistedWallet(walletId: String): Boolean {
        return _assistedWalletBrief.value[walletId]?.status == WalletStatus.ACTIVE.name
    }

    override fun getGroupId(walletId: String): String? {
        return _assistedWalletBrief.value[walletId]?.groupId
    }

    override fun isInactiveAssistedWallet(walletId: String): Boolean {
        return _assistedWalletBrief.value[walletId]?.status?.let { status -> status != WalletStatus.ACTIVE.name && status != WalletStatus.LOCKED.name } != false
    }

    override fun getWalletAlias(walletId: String): String {
        return _assistedWalletBrief.value[walletId]?.alias.orEmpty()
    }

    override fun getWalletPlan(walletId: String): MembershipPlan {
        return _assistedWalletBrief.value[walletId]?.plan ?: MembershipPlan.NONE
    }

    override fun getBriefWallet(walletId: String): AssistedWalletBrief? {
        return _assistedWalletBrief.value[walletId]
    }

    override fun getGroup(groupId: String) = _assistedGroups.value[groupId]

    override fun isGroupAssistedWallet(groupId: String?): Boolean {
        return !groupId.isNullOrEmpty() && _assistedGroups.value[groupId] != null
    }

    override fun isSyncableWallet(walletId: String): Boolean {
        return _assistedWalletBrief.value[walletId]?.status == WalletStatus.ACTIVE.name || _claimableWallets.value.contains(walletId)
    }
}