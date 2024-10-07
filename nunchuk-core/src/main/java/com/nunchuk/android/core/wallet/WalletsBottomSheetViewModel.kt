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

package com.nunchuk.android.core.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlansFlowUseCase
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.SavedAddress
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.isNonePlan
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.usecase.GetGroupsUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.membership.GetSavedAddressListLocalUseCase
import com.nunchuk.android.usecase.membership.GetSavedAddressListRemoteUseCase
import com.nunchuk.android.utils.ByzantineGroupUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WalletsBottomSheetViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getGroupsUseCase: GetGroupsUseCase,
    private val byzantineGroupUtils: ByzantineGroupUtils,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val getSavedAddressListLocalUseCase: GetSavedAddressListLocalUseCase,
    private val getSavedAddressListRemoteUseCase: GetSavedAddressListRemoteUseCase,
    private val getLocalMembershipPlansFlowUseCase: GetLocalMembershipPlansFlowUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(WalletsBottomSheetState())
    val state = _state.asStateFlow()

    private var exclusiveAddresses: List<String> = emptyList()

    fun init(
        configArgs: WalletComposeBottomSheet.ConfigArgs?,
        walletIds: List<String>,
        exclusiveWalletIds: List<String>,
        exclusiveAddresses: List<String>
    ) {
        this.exclusiveAddresses = exclusiveAddresses
        _state.update { it.copy(config = configArgs ?: WalletComposeBottomSheet.ConfigArgs()) }
        viewModelScope.launch {
            getGroupsUseCase(Unit)
                .collect { result ->
                    val groups = result.getOrDefault(emptyList())
                    val joinedGroups =
                        groups.filter { byzantineGroupUtils.isPendingAcceptInvite(it).not() }
                    val roles = groups.associateBy({ it.id },
                        { byzantineGroupUtils.getCurrentUserRole(it).toRole })
                    _state.update {
                        it.copy(joinedGroups = joinedGroups.associateBy { it.id }, roles = roles)
                    }
                    updateLockdownWalletsIds()
                }
        }
        viewModelScope.launch {
            getLocalMembershipPlansFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .collect { plans ->
                    if (plans.isNonePlan().not()) {
                        syncSavedAddresses()
                    }
                    _state.update { it.copy(isPremiumUser = plans.isNonePlan().not()) }
                }
        }
        if (configArgs?.isShowAddress() == true) getSavedAddresses()
        getWallets(exclusiveWalletIds, walletIds)
    }

    private fun syncSavedAddresses() {
        viewModelScope.launch {
            getSavedAddressListRemoteUseCase(Unit)
        }
    }

    private fun getWallets(exclusiveWalletIds: List<String>, walletIds: List<String>) {
        viewModelScope.launch {
            getWalletsUseCase.execute()
                .catch { Timber.e(it) }
                .collect { wallets ->
                    val filterWallets = wallets.filter { it.wallet.id !in exclusiveWalletIds }
                        .filter { if (walletIds.isEmpty()) true else it.wallet.id in walletIds }
                    _state.update { it.copy(wallets = filterWallets) }
                    composeWalletUiModels()
                }
        }
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect { wallets ->
                    val filterWallets = wallets.filter { it.localId !in exclusiveWalletIds }
                        .filter { if (walletIds.isEmpty()) true else it.localId in walletIds }
                    _state.update {
                        it.copy(
                            assistedWallets = filterWallets.associateBy { it.localId }
                        )
                    }
                    updateLockdownWalletsIds()
                }
        }
    }

    private fun updateLockdownWalletsIds() {
        val lockdownWalletIds = state.value.assistedWallets.values.filter {
            state.value.joinedGroups[it.groupId]?.isLocked == true
        }.map { it.localId }
        _state.update { it.copy(lockdownWalletIds = lockdownWalletIds.toSet()) }
        composeWalletUiModels()
    }

    fun getSavedAddresses() {
        viewModelScope.launch {
            getSavedAddressListLocalUseCase(Unit)
                .collect { result ->
                    _state.update { it.copy(savedAddresses = result.getOrNull()?.filter { it.address !in exclusiveAddresses }.orEmpty()) }
                }

        }
    }

    private fun composeWalletUiModels() {
        val wallets = state.value.wallets
        if (wallets.isEmpty()) return
        val uis = arrayListOf<WalletUiModel>()
        wallets.forEach { wallet ->
            val assistedWallet = state.value.assistedWallets[wallet.wallet.id]
            var group: ByzantineGroup? = null
            if (assistedWallet?.groupId.isNullOrEmpty().not()) {
                group = state.value.joinedGroups[assistedWallet?.groupId] ?: return@forEach // skip if group is not joined
            }
            val role = state.value.roles[assistedWallet?.groupId]
            val isLocked = state.value.lockdownWalletIds.contains(assistedWallet?.localId)
            val walletStatus = assistedWallet?.status ?: ""
            if (state.value.config.isShowDeactivatedWallets().not() && walletStatus == WalletStatus.REPLACED.name) return@forEach // skip if wallet is replaced
            uis.add(
                WalletUiModel(
                    wallet = wallet,
                    assistedWallet = assistedWallet,
                    isAssistedWallet = assistedWallet?.status == WalletStatus.ACTIVE.name,
                    group = group,
                    role = role ?: AssistedWalletRole.NONE,
                    isLocked = isLocked,
                    walletStatus = walletStatus
                )
            )
        }
        _state.update { it.copy(walletUiModels = uis) }
    }
}

data class WalletUiModel(
    val wallet: WalletExtended,
    val assistedWallet: AssistedWalletBrief?,
    val isAssistedWallet: Boolean,
    val group: ByzantineGroup?,
    val role: AssistedWalletRole,
    val isLocked: Boolean,
    val walletStatus: String
)

data class WalletsBottomSheetState(
    val wallets: List<WalletExtended> = emptyList(),
    val assistedWallets: Map<String, AssistedWalletBrief> = hashMapOf(),
    val lockdownWalletIds: Set<String> = emptySet(),
    val joinedGroups: Map<String, ByzantineGroup> = HashMap(),
    val savedAddresses: List<SavedAddress> = emptyList(),
    val roles: Map<String, AssistedWalletRole> = emptyMap(),
    val config: WalletComposeBottomSheet.ConfigArgs = WalletComposeBottomSheet.ConfigArgs(),
    val walletUiModels: List<WalletUiModel> = emptyList(),
    val isPremiumUser: Boolean = false,
)