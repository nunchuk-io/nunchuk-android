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

package com.nunchuk.android.transaction.components.receive.address.used

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.model.Amount
import com.nunchuk.android.transaction.components.receive.address.UsedAddressModel
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.GetAddressAssetsUseCase
import com.nunchuk.android.usecase.GetAddressBalanceUseCase
import com.nunchuk.android.usecase.GetAddressesUseCase
import com.nunchuk.android.usecase.GetLiquidAssetIdsUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class UsedAddressViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val getAddressesUseCase: GetAddressesUseCase,
    private val getAddressBalanceUseCase: GetAddressBalanceUseCase,
    private val getAddressAssetsUseCase: GetAddressAssetsUseCase,
    private val getLiquidAssetIdsUseCase: GetLiquidAssetIdsUseCase,
    private val pushEventManager: PushEventManager,
) : ViewModel() {

    private val _state = MutableStateFlow(UsedAddressState())
    val state: StateFlow<UsedAddressState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<UsedAddressEvent>()
    val event: SharedFlow<UsedAddressEvent> = _event.asSharedFlow()

    private lateinit var walletId: String

    init {
        viewModelScope.launch {
            pushEventManager.event.collect { event ->
                when (event) {
                    is PushEvent.ReloadUsedAddress -> getUsedAddresses()
                    else -> {}
                }
            }
        }
    }

    fun init(walletId: String) {
        this.walletId = walletId
        viewModelScope.launch {
            val wallet = runCatching {
                getWalletUseCase.execute(walletId).first().wallet
            }.getOrNull()
            if (wallet != null) {
                _state.update { it.copy(walletType = wallet.walletType) }
            }
            if (_state.value.walletType == WalletType.LIQUID) {
                getLiquidAssetIdsUseCase(Unit).onSuccess { ids ->
                    _state.update {
                        it.copy(usdtAssetId = ids.usdtAssetId, lbtcAssetId = ids.lbtcAssetId)
                    }
                }
            }
            getUsedAddresses()
        }
    }

    private fun getUsedAddresses() {
        viewModelScope.launch {
            getAddressesUseCase.execute(walletId = walletId, used = true)
                .onException { _event.emit(UsedAddressEvent.GetUsedAddressErrorEvent(it.message.orEmpty())) }
                .collect { addresses ->
                    if (_state.value.walletType == WalletType.LIQUID) {
                        getAddressAssets(addresses)
                    } else {
                        getAddressBalance(addresses)
                    }
                }
        }
    }

    private fun getAddressBalance(addresses: List<String>) {
        viewModelScope.launch {
            val addressModels = addresses.map { address ->
                getAddressBalanceUseCase(GetAddressBalanceUseCase.Param(walletId, address))
                    .getOrElse { Amount.ZER0 }
                    .let { balance -> UsedAddressModel(address, balance) }
            }
            _state.update { it.copy(addresses = addressModels) }
        }
    }

    private fun getAddressAssets(addresses: List<String>) {
        viewModelScope.launch {
            val addressModels = addresses.map { address ->
                val assets = getAddressAssetsUseCase(
                    GetAddressAssetsUseCase.Param(walletId, address)
                ).getOrDefault(emptyMap())
                UsedAddressModel(address = address, assets = assets)
            }
            _state.update { it.copy(addresses = addressModels) }
        }
    }
}
