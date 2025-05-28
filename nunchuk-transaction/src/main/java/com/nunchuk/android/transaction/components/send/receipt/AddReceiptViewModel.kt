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
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.AcceptedAddressEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.AddressRequiredEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.InvalidAddressEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.ParseBtcUriEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.ShowError
import com.nunchuk.android.transaction.components.utils.privateNote
import com.nunchuk.android.type.WalletTemplate
import com.nunchuk.android.usecase.CheckAddressValidUseCase
import com.nunchuk.android.usecase.GetDefaultAntiFeeSnipingUseCase
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import com.nunchuk.android.usecase.wallet.GetUnusedWalletAddressUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AddReceiptViewModel @Inject constructor(
    private val checkAddressValidUseCase: CheckAddressValidUseCase,
    private val parseBtcUriUseCase: ParseBtcUriUseCase,
    private val getUnusedWalletAddressUseCase: GetUnusedWalletAddressUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val singleSignerMapper: SingleSignerMapper,
    private val getDefaultAntiFeeSnipingUseCase: GetDefaultAntiFeeSnipingUseCase,
) : NunchukViewModel<AddReceiptState, AddReceiptEvent>() {

    override val initialState = AddReceiptState()

    fun init(args: AddReceiptArgs) {
        updateState { initialState.copy(address = args.address, privateNote = args.privateNote) }
        if (args.walletId.isNotEmpty()) getWalletDetail(args.walletId)

        viewModelScope.launch {
            getDefaultAntiFeeSnipingUseCase(Unit)
                .collect { result ->
                    if (result.isSuccess) {
                        updateState { copy(antiFeeSniping = result.getOrThrow()) }
                    }
                }
        }
    }

    private fun getWalletDetail(walletId: String) {
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId).onSuccess { wallet ->
                val signers = wallet.signers.map { signer ->
                    singleSignerMapper(signer)
                }
                updateState {
                    copy(
                        addressType = wallet.addressType,
                        isValueKeySetDisable = wallet.walletTemplate == WalletTemplate.DISABLE_KEY_PATH,
                        signers = signers
                    )
                }
            }.onFailure {
                setEvent(ShowError(it.message.orUnknownError()))
            }
        }
    }

    fun parseBtcUri(content: String) {
        viewModelScope.launch {
            val result = parseBtcUriUseCase(content)
            if (result.isSuccess) {
                val btcUri = result.getOrThrow()
                updateState {
                    copy(
                        address = btcUri.address,
                        privateNote = btcUri.privateNote,
                        amount = btcUri.amount
                    )
                }
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
                    val result =
                        checkAddressValidUseCase(CheckAddressValidUseCase.Params(listOf(address)))
                    if (result.isSuccess && result.getOrThrow().isEmpty()) {
                        setEvent(
                            AcceptedAddressEvent(
                                address,
                                currentState.privateNote,
                                currentState.amount,
                                isCreateTransaction
                            )
                        )
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
    fun setEventHandled() {
        setEvent(AddReceiptEvent.NoOp)
    }
}