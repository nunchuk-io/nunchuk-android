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

package com.nunchuk.android.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.usecase.coin.SyncCoinControlData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncInfoViewModel @Inject constructor(
    getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val syncCoinControlData: SyncCoinControlData,
    private val assistedWalletManager: AssistedWalletManager
) : ViewModel() {
    private val assistedWallets = getAssistedWalletsFlowUseCase(Unit)
        .map { it.getOrElse { emptyList() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            while (true) {
                delay(300_000L)
                syncCoin()
            }
        }
    }

    fun init() {
        viewModelScope.launch {
            delay(1000L)
            syncCoin()
        }
    }

    private fun syncCoin() {
        viewModelScope.launch {
            assistedWallets.value.forEach {
                syncCoinControlData(
                    SyncCoinControlData.Param(
                        assistedWalletManager.getGroupId(it.localId), it.localId
                    )
                )
            }
        }
    }
}