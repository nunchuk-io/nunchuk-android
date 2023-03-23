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

package com.nunchuk.android.transaction.components.send.amount

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.domain.data.SAT
import com.nunchuk.android.core.util.*
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.SwapCurrencyEvent
import com.nunchuk.android.transaction.components.utils.privateNote
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class InputAmountViewModel @Inject constructor(
    private val parseBtcUriUseCase: ParseBtcUriUseCase
) : NunchukViewModel<InputAmountState, InputAmountEvent>() {

    private var availableAmount: Double = 0.0

    override val initialState = InputAmountState()

    fun init(availableAmount: Double) {
        updateState { initialState }
        this.availableAmount = availableAmount
    }

    fun parseBtcUri(content: String) {
        viewModelScope.launch {
            val result = parseBtcUriUseCase(content)
            if (result.isSuccess) {
                val btcUri = result.getOrThrow()
                if (btcUri.amount.value > 0) {
                    updateState {
                        copy(
                            address = btcUri.address,
                            privateNote = btcUri.privateNote,
                            amountBTC = btcUri.amount.pureBTC(),
                            useBtc = true
                        )
                    }
                } else {
                    updateState {
                        copy(
                            address = btcUri.address,
                        )
                    }
                }
                setEvent(InputAmountEvent.ParseBtcUriSuccess(result.getOrThrow()))
            } else {
                setEvent(InputAmountEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun switchCurrency() {
        updateState { copy(useBtc = !useBtc) }
        val currentState = getState()
        if (!currentState.useBtc) {
            event(SwapCurrencyEvent(currentState.amountUSD))
        } else {
            event(SwapCurrencyEvent(currentState.amountBTC))
        }
    }

    fun handleAmountChanged(input: String) {
        val inputValue = input.toNumericValue().toDouble()
        val currentState = getState()
        if (currentState.useBtc) {
            if (inputValue != currentState.amountBTC) {
                updateState {
                    copy(
                        amountBTC = if (CURRENT_DISPLAY_UNIT_TYPE == SAT) inputValue.fromSATtoBTC() else inputValue,
                        amountUSD = if (CURRENT_DISPLAY_UNIT_TYPE == SAT) inputValue.fromSATtoBTC().fromBTCToCurrency() else inputValue.fromBTCToCurrency()
                    )
                }
            }
        } else {
            if (inputValue != currentState.amountUSD) {
                updateState { copy(amountBTC = inputValue.fromCurrencyToBTC(), amountUSD = inputValue) }
            }
        }
    }

    fun getUseBTC() = getState().useBtc

    fun handleContinueEvent() {
        val amount = getState().amountBTC
        if (amount <= 0 || amount > availableAmount) {
            event(InputAmountEvent.InsufficientFundsEvent)
        } else {
            event(InputAmountEvent.AcceptAmountEvent(amount))
        }
    }

    fun getAddress(): String = getState().address

    fun getPrivateNote(): String = getState().privateNote

    fun getAmountBtc(): Double = getState().amountBTC
}