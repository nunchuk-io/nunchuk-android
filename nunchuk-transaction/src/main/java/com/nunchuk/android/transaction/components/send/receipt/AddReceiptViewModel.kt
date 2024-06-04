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

package com.nunchuk.android.transaction.components.send.receipt

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.*
import com.nunchuk.android.transaction.components.utils.privateNote
import com.nunchuk.android.usecase.CheckAddressValidUseCase
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import com.nunchuk.android.usecase.wallet.GetUnusedWalletAddressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AddReceiptViewModel @Inject constructor(
    private val checkAddressValidUseCase: CheckAddressValidUseCase,
    private val parseBtcUriUseCase: ParseBtcUriUseCase,
    private val getUnusedWalletAddressUseCase: GetUnusedWalletAddressUseCase
) : NunchukViewModel<AddReceiptState, AddReceiptEvent>() {

    override val initialState = AddReceiptState()

    fun init(address: String, privateNote: String) {
        updateState { initialState.copy(address = address, privateNote = privateNote) }
    }

    fun parseBtcUri(content: String) {
        viewModelScope.launch {
            val result = parseBtcUriUseCase(content)
            if (result.isSuccess) {
                val btcUri = result.getOrThrow()
                updateState { copy(address = btcUri.address, privateNote = btcUri.privateNote, amount = btcUri.amount) }
                setEvent(ParseBtcUriEvent)
            } else {
                setEvent(ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun handleContinueEvent(isCreateTransaction: Boolean) {
        viewModelScope.launch {
            val currentState = getState()
            val address = currentState.address
            when {
                address.isEmpty() -> event(AddressRequiredEvent)
                else -> {
                    val result = checkAddressValidUseCase(CheckAddressValidUseCase.Params(listOf(address)))
                    if (result.isSuccess && result.getOrThrow().isEmpty()) {
                        setEvent(AcceptedAddressEvent(address, currentState.privateNote, currentState.amount, isCreateTransaction))
                    } else {
                        setEvent(InvalidAddressEvent)
                    }
                }
            }
        }
    }

    fun updateAddress(address: String) {
        updateState { copy(address = address) }
    }

    fun getFirstUnusedAddress(walletId: String) {
        viewModelScope.launch {
            getUnusedWalletAddressUseCase(walletId).onSuccess { addresses ->
                updateState { copy(address = addresses.first()) }
            }.onFailure {
                setEvent(ShowError(it.message.orUnknownError()))
            }
        }
    }

    fun handleReceiptChanged(address: String) {
        updateState { copy(address = address) }
    }

    fun handlePrivateNoteChanged(privateNote: String) {
        updateState { copy(privateNote = privateNote) }
    }

    fun getAddReceiptState() = getState()
}