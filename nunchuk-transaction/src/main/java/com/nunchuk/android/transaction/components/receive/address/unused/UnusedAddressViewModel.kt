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

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.transaction.components.receive.address.unused.UnusedAddressEvent.GenerateAddressErrorEvent
import com.nunchuk.android.usecase.GetAddressPathUseCase
import com.nunchuk.android.usecase.GetAddressesUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.NewAddressUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class UnusedAddressViewModel @Inject constructor(
    private val addressesUseCase: GetAddressesUseCase,
    private val newAddressUseCase: NewAddressUseCase,
    private val getAddressPathUseCase: GetAddressPathUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    ) : NunchukViewModel<UnusedAddressState, UnusedAddressEvent>() {

    private lateinit var walletId: String

    override val initialState = UnusedAddressState()

    fun init(walletId: String) {
        this.walletId = walletId
        getUnusedAddresses()
        getWallet()
    }

    private fun getWallet() {
        viewModelScope.launch {
            getWalletUseCase.execute(walletId)
                .onException { }
                .collect { updateState { copy(wallet = it.wallet) } }
        }
    }

    fun getAddressPath(address: String) {
        viewModelScope.launch {
            getAddressPathUseCase(GetAddressPathUseCase.Params(walletId, address))
                .onSuccess {
                    setEvent(UnusedAddressEvent.GetAddressPathSuccessEvent(it))
                }
        }
    }

    fun isSingleSignWallet(): Boolean {
        val requireSigns = getState().wallet.totalRequireSigns
        val totalSigns = getState().wallet.signers.size
        return requireSigns == 1 && totalSigns == 1
    }

    private fun getUnusedAddresses() {
        viewModelScope.launch {
            addressesUseCase.execute(walletId = walletId)
                .onException { onError() }
                .collect { onSuccess(it) }
        }

    }

    private fun onError() {
        updateState { copy(addresses = emptyList()) }
    }

    private fun onSuccess(addresses: List<String>) {
        updateState { copy(addresses = addresses) }
        if (addresses.isEmpty()) {
            generateAddress()
        }
    }

    fun generateAddress() {
        viewModelScope.launch {
            newAddressUseCase.execute(walletId = walletId)
                .onException { event(GenerateAddressErrorEvent(it.message.orEmpty())) }
                .collect { getUnusedAddresses() }
        }
    }

}