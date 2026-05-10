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

package com.nunchuk.android.transaction.components.send.amount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.toNumericValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
internal class InputStablecoinAmountViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(InputStablecoinAmountState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<InputStablecoinAmountEvent>()
    val event = _event.asSharedFlow()

    fun selectToken(token: StablecoinToken) {
        if (_state.value.selectedToken == token) return
        _state.update {
            it.copy(
                selectedToken = token,
                // Reset input when switching tokens — balances differ per token.
                inputText = "",
                amountToken = 0.0,
                amountUsd = 0.0,
                useToken = true,
            )
        }
    }

    fun switchCurrency() {
        val current = _state.value
        val newUseToken = !current.useToken
        val displayAmount = if (newUseToken) current.amountToken else current.amountUsd
        _state.update {
            it.copy(
                useToken = newUseToken,
                inputText = formatRawInput(displayAmount),
            )
        }
    }

    fun setInputAmount(amount: Double) {
        val text = formatRawInput(amount)
        _state.update { it.copy(inputText = text) }
        handleAmountChanged(text)
    }

    fun handleAmountChanged(input: String) {
        val current = _state.value
        val inputValue = input.toNumericValue().toDouble()
        if (input != current.inputText) {
            _state.update { it.copy(inputText = input) }
        }
        // TODO(stablecoin): use real fx rates per token. Stubbed 1:1 for USDT, ~$74,500 for LBTC.
        val tokenToUsd = when (current.selectedToken) {
            StablecoinToken.USDT -> 1.0
            StablecoinToken.LBTC -> 74_500.0
        }
        if (current.useToken) {
            if (inputValue != current.amountToken) {
                _state.update {
                    it.copy(
                        amountToken = inputValue,
                        amountUsd = inputValue * tokenToUsd,
                    )
                }
            }
        } else {
            if (inputValue != current.amountUsd) {
                _state.update {
                    it.copy(
                        amountToken = if (tokenToUsd == 0.0) 0.0 else inputValue / tokenToUsd,
                        amountUsd = inputValue,
                    )
                }
            }
        }
    }

    fun sendAll() {
        val current = _state.value
        val balance = balanceForToken(current.selectedToken)
        val amount = if (current.useToken) {
            balance
        } else {
            current.amountUsd.let {
                when (current.selectedToken) {
                    StablecoinToken.USDT -> current.usdtBalanceUsd
                    StablecoinToken.LBTC -> current.lbtcBalanceUsd
                }
            }
        }
        setInputAmount(amount)
    }

    fun handleContinueEvent() {
        val current = _state.value
        val amount = current.amountToken
        val balance = balanceForToken(current.selectedToken)
        val event = when {
            amount <= 0 -> InputStablecoinAmountEvent.InvalidAmountEvent
            amount > balance -> InputStablecoinAmountEvent.InsufficientFundsEvent
            else -> InputStablecoinAmountEvent.AcceptAmountEvent(amount, current.selectedToken)
        }
        viewModelScope.launch { _event.emit(event) }
    }

    fun getAmountToken(): Double = _state.value.amountToken

    fun getSelectedToken(): StablecoinToken = _state.value.selectedToken

    private fun balanceForToken(token: StablecoinToken): Double = when (token) {
        StablecoinToken.USDT -> _state.value.usdtBalance
        StablecoinToken.LBTC -> _state.value.lbtcBalance
    }

    private fun formatRawInput(amount: Double): String {
        if (amount <= 0.0) return ""
        return rawDecimalFormat.format(amount)
    }

    private val rawDecimalFormat = DecimalFormat(
        "0.########",
        DecimalFormatSymbols(Locale.US),
    ).apply {
        isGroupingUsed = false
    }
}
