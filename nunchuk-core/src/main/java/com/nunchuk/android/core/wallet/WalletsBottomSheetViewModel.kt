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
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.SavedAddress
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.usecase.GetGroupsUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.membership.GetSavedAddressListUseCase
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
    private val getSavedAddressListUseCase: GetSavedAddressListUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(WalletsBottomSheetState())
    val state = _state.asStateFlow()

    private var exclusiveAddresses: List<String> = emptyList()

    fun init(
        isShowAddress: Boolean,
        assistedWalletIds: List<String>,
        exclusiveWalletIds: List<String>,
        exclusiveAddresses: List<String>
    ) {
        this.exclusiveAddresses = exclusiveAddresses
        _state.update { it.copy(isShowAddress = isShowAddress) }
        viewModelScope.launch {
            getGroupsUseCase(Unit)
                .collect { result ->
                    val groups = result.getOrDefault(emptyList())
                    val joinedGroups =
                        groups.filter { byzantineGroupUtils.isPendingAcceptInvite(it).not() }
                    _state.update {
                        it.copy(joinedGroups = joinedGroups.associateBy { it.id })
                    }
                    updateLockdownWalletsIds()
                }
        }
        if (isShowAddress) getSavedAddresses()
        getWallets(exclusiveWalletIds, assistedWalletIds)
    }

    private fun getWallets(exclusiveWalletIds: List<String>, assistedWalletIds: List<String>) {
        viewModelScope.launch {
            getWalletsUseCase.execute()
                .catch { Timber.e(it) }
                .collect { wallets ->
                    val filterWallets = wallets.filter { it.wallet.id !in exclusiveWalletIds }
                        .filter { if (assistedWalletIds.isEmpty()) true else it.wallet.id in assistedWalletIds }
                    _state.update { it.copy(wallets = filterWallets) }
                }
        }
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect { wallets ->
                    val assistedWallets = wallets.filter { it.localId !in exclusiveWalletIds }
                        .filter { if (assistedWalletIds.isEmpty()) true else it.localId in assistedWalletIds }
                    _state.update {
                        it.copy(
                            assistedWalletIds = assistedWallets.map { it.localId },
                            assistedWallets = assistedWallets
                        )
                    }
                    updateLockdownWalletsIds()
                }
        }
    }

    private fun updateLockdownWalletsIds() {
        val lockdownWalletIds = state.value.assistedWallets.filter {
            state.value.joinedGroups[it.groupId]?.isLocked == true
        }.map { it.localId }
        _state.update { it.copy(lockdownWalletIds = lockdownWalletIds.toSet()) }
    }

    fun getSavedAddresses() {
        viewModelScope.launch {
            getSavedAddressListUseCase(Unit)
                .onSuccess { addresses ->
                    _state.update { it.copy(savedAddresses = addresses.filter { it.address !in exclusiveAddresses } ) }
                }
        }
    }
}

data class WalletsBottomSheetState(
    val wallets: List<WalletExtended> = emptyList(),
    val assistedWallets: List<AssistedWalletBrief> = emptyList(),
    val assistedWalletIds: List<String> = emptyList(),
    val lockdownWalletIds: Set<String> = emptySet(),
    val joinedGroups: Map<String, ByzantineGroup> = HashMap(),
    val savedAddresses: List<SavedAddress> = emptyList(),
    val isShowAddress: Boolean = false
)