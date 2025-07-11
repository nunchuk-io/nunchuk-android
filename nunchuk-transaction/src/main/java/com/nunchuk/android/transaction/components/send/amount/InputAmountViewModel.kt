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

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.domain.data.SAT
import com.nunchuk.android.core.util.fromBTCToCurrency
import com.nunchuk.android.core.util.fromCurrencyToBTC
import com.nunchuk.android.core.util.fromSATtoBTC
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.toNumericValue
import com.nunchuk.android.model.BtcUri
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.SwapCurrencyEvent
import com.nunchuk.android.transaction.components.utils.privateNote
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class InputAmountViewModel @Inject constructor(
    private val parseBtcUriUseCase: ParseBtcUriUseCase,
    private val getAllCoinUseCase: GetAllCoinUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
) : NunchukViewModel<InputAmountState, InputAmountEvent>() {

    private var availableAmount: Double = 0.0
    private var availableAmountWithoutUnlocked: Double = 0.0
    private var hasLockedCoin: Boolean = false
    private var isFromSelectedCoin: Boolean = false

    override val initialState = InputAmountState()

    fun init(availableAmount: Double, walletId: String, isFromSelectedCoin: Boolean) {
        updateState { initialState }
        this.availableAmount = availableAmount
        this.isFromSelectedCoin = isFromSelectedCoin
        if (!isFromSelectedCoin) {
            checkLockedCoin(walletId)
        }
    }

    private fun checkLockedCoin(walletId: String) {
        viewModelScope.launch {
            setEvent(InputAmountEvent.Loading(true))
            getAllCoinUseCase(walletId).onSuccess {
                hasLockedCoin = it.any { coin -> coin.isLocked }
                availableAmountWithoutUnlocked =
                    it.sumOf { coin -> if (coin.isLocked) 0.0 else coin.amount.pureBTC() }
            }
            setEvent(InputAmountEvent.Loading(false))
        }
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

    fun updateBtcUri(btcUri: BtcUri) {
        updateState {
            copy(
                address = btcUri.address,
                privateNote = btcUri.privateNote,
                amountBTC = btcUri.amount.pureBTC(),
                amountUSD = btcUri.amount.pureBTC().fromBTCToCurrency(),
                useBtc = true
            )
        }
    }

    fun switchCurrency() {
        updateState { copy(useBtc = !useBtc) }
        val currentState = getState()
        if (!currentState.useBtc) {
            setEvent(SwapCurrencyEvent(currentState.amountUSD))
        } else {
            setEvent(SwapCurrencyEvent(currentState.amountBTC))
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
                        amountUSD = if (CURRENT_DISPLAY_UNIT_TYPE == SAT) inputValue.fromSATtoBTC()
                            .fromBTCToCurrency() else inputValue.fromBTCToCurrency()
                    )
                }
            }
        } else {
            if (inputValue != currentState.amountUSD) {
                updateState {
                    copy(
                        amountBTC = inputValue.fromCurrencyToBTC(),
                        amountUSD = inputValue
                    )
                }
            }
        }
    }

    fun getUseBTC() = getState().useBtc

    fun handleContinueEvent() {
        val amount = getState().amountBTC
        if (amount <= 0) {
            setEvent(InputAmountEvent.InvalidAmountEvent)
        } else if (amount > availableAmount) {
            setEvent(InputAmountEvent.InsufficientFundsEvent)
        } else if (hasLockedCoin && amount > availableAmountWithoutUnlocked && !isFromSelectedCoin) {
            setEvent(InputAmountEvent.InsufficientFundsLockedCoinEvent)
        } else {
            setEvent(InputAmountEvent.AcceptAmountEvent(amount))
        }
    }

    fun checkWallet() = viewModelScope.launch {
        getWalletsUseCase.execute().flowOn(Dispatchers.IO)
            .onException { setEvent(InputAmountEvent.ShowError(it.message.orUnknownError())) }
            .flowOn(Dispatchers.Main)
            .collect {
                setEvent(InputAmountEvent.CheckHasWallet(it.isNotEmpty()))
            }
    }

    fun getAddress(): String = getState().address

    fun getPrivateNote(): String = getState().privateNote

    fun getAmountBtc(): Double = getState().amountBTC

    fun isHasLockedCoin() = hasLockedCoin
}