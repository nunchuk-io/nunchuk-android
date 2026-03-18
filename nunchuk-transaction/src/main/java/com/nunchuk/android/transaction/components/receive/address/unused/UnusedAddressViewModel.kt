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

package com.nunchuk.android.transaction.components.receive.address.unused

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.usecase.GetAddressPathUseCase
import com.nunchuk.android.usecase.GetAddressesUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.MarkAddressAsUsedUseCase
import com.nunchuk.android.usecase.NewAddressUseCase
import com.nunchuk.android.usecase.wallet.GetAddressIndexUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class UnusedAddressViewModel @Inject constructor(
    private val addressesUseCase: GetAddressesUseCase,
    private val newAddressUseCase: NewAddressUseCase,
    private val getAddressPathUseCase: GetAddressPathUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val getAddressIndexUseCase: GetAddressIndexUseCase,
    private val markAddressAsUsedUseCase: MarkAddressAsUsedUseCase,
    private val pushEventManager: PushEventManager,
) : ViewModel() {

    private val _state = MutableStateFlow(UnusedAddressState())
    val state: StateFlow<UnusedAddressState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<UnusedAddressEvent>()
    val event: SharedFlow<UnusedAddressEvent> = _event.asSharedFlow()

    private lateinit var walletId: String

    fun init(walletId: String) {
        this.walletId = walletId
        getUnusedAddresses()
        getWallet()
    }

    private fun getWallet() {
        viewModelScope.launch {
            getWalletUseCase.execute(walletId)
                .onException { }
                .collect { _state.update { state -> state.copy(wallet = it.wallet) } }
        }
    }

    fun getAddressPath(address: String) {
        viewModelScope.launch {
            getAddressPathUseCase(GetAddressPathUseCase.Params(walletId, address))
                .onSuccess {
                    _event.emit(UnusedAddressEvent.GetAddressPathSuccessEvent(it))
                }
        }
    }

    fun isSingleSignWallet(): Boolean {
        val wallet = _state.value.wallet
        val requireSigns = wallet.totalRequireSigns
        val totalSigns = wallet.signers.size
        return requireSigns == 1 && totalSigns == 1
    }

    private fun getUnusedAddresses() {
        viewModelScope.launch {
            addressesUseCase.execute(walletId = walletId)
                .onException { _state.update { it.copy(addresses = emptyList()) } }
                .collect { onSuccess(it) }
        }
    }

    fun markAddressAsUsed(address: String) {
        viewModelScope.launch {
            markAddressAsUsedUseCase(MarkAddressAsUsedUseCase.Params(walletId, address))
                .onSuccess {
                    _state.update { state ->
                        state.copy(addresses = state.addresses.filter { it != address })
                    }
                    _event.emit(UnusedAddressEvent.MarkAddressAsUsedSuccessEvent(address))
                    pushEventManager.push(PushEvent.ReloadUsedAddress(address))
                }.onFailure { }
        }
    }

    private fun onSuccess(addresses: List<String>) {
        _state.update { it.copy(addresses = addresses) }
        if (addresses.isEmpty()) {
            generateAddress()
        }
    }

    fun generateAddress() {
        viewModelScope.launch {
            newAddressUseCase.execute(walletId = walletId)
                .onException { _event.emit(UnusedAddressEvent.GenerateAddressErrorEvent(it.message.orEmpty())) }
                .collect { getUnusedAddresses() }
        }
    }

    suspend fun getAddressIndex(address: String): Int {
        return getAddressIndexUseCase(GetAddressIndexUseCase.Params(walletId, address)).getOrThrow()
    }
}
