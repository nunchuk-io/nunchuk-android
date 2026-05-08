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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ParseWalletDescriptorUseCase
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.domain.data.SAT
import com.nunchuk.android.core.util.fromBTCToCurrency
import com.nunchuk.android.core.util.fromBTCtoSAT
import com.nunchuk.android.core.util.fromCurrencyToBTC
import com.nunchuk.android.core.util.fromSATtoBTC
import com.nunchuk.android.core.util.getCurrencyLocale
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.toNumericValue
import com.nunchuk.android.model.BtcUri
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.SwapCurrencyEvent
import com.nunchuk.android.transaction.components.utils.privateNote
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
internal class InputAmountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val parseBtcUriUseCase: ParseBtcUriUseCase,
    private val getAllCoinUseCase: GetAllCoinUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val parseWalletDescriptorUseCase: ParseWalletDescriptorUseCase,
) : ViewModel() {

    private val args: InputAmountArgs = InputAmountArgs.fromSavedStateHandle(savedStateHandle)
    private val availableAmount: Double = args.availableAmount
    private val isFromSelectedCoin: Boolean = args.inputs.isNotEmpty()
    private var availableAmountWithoutUnlocked: Double = 0.0
    private var hasLockedCoin: Boolean = false

    private val _state = MutableStateFlow(InputAmountState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<InputAmountEvent>()
    val event = _event.asSharedFlow()

    init {
        if (!isFromSelectedCoin) {
            checkLockedCoin(args.walletId)
        }
        args.btcUri?.let(::applyBtcUri)
    }

    private fun applyBtcUri(btcUri: BtcUri) {
        val amountBtc = btcUri.amount.pureBTC()
        _state.update {
            it.copy(
                address = btcUri.address,
                privateNote = btcUri.privateNote,
                amountBTC = amountBtc,
                amountUSD = amountBtc.fromBTCToCurrency(),
                useBtc = true,
                inputText = formatRawInput(amountBtc, useBtc = true),
            )
        }
    }

    private fun checkLockedCoin(walletId: String) {
        viewModelScope.launch {
            _event.emit(InputAmountEvent.Loading(true))
            getAllCoinUseCase(walletId).onSuccess { coins ->
                hasLockedCoin = coins.any { it.isLocked }
                availableAmountWithoutUnlocked =
                    coins.sumOf { coin -> if (coin.isLocked) 0.0 else coin.amount.pureBTC() }
            }
            _event.emit(InputAmountEvent.Loading(false))
        }
    }

    fun parseBtcUri(content: String) {
        viewModelScope.launch {
            parseBtcUriUseCase(content)
                .onSuccess { btcUri ->
                    if (btcUri.amount.value > 0) {
                        val amountBtc = btcUri.amount.pureBTC()
                        _state.update {
                            it.copy(
                                address = btcUri.address,
                                privateNote = btcUri.privateNote,
                                amountBTC = amountBtc,
                                amountUSD = amountBtc.fromBTCToCurrency(),
                                useBtc = true,
                                inputText = formatRawInput(amountBtc, useBtc = true),
                            )
                        }
                    } else {
                        _state.update { it.copy(address = btcUri.address) }
                    }
                    _event.emit(InputAmountEvent.ParseBtcUriSuccess(btcUri))
                }
                .onFailure {
                    _event.emit(InputAmountEvent.ShowError(it.message.orUnknownError()))
                }
        }
    }

    fun switchCurrency() {
        val current = _state.value
        val newUseBtc = !current.useBtc
        val displayAmount = if (newUseBtc) current.amountBTC else current.amountUSD
        _state.update {
            it.copy(
                useBtc = newUseBtc,
                inputText = formatRawInput(displayAmount, useBtc = newUseBtc),
            )
        }
        viewModelScope.launch { _event.emit(SwapCurrencyEvent(displayAmount)) }
    }

    fun setInputAmount(amount: Double) {
        val useBtc = _state.value.useBtc
        val text = formatRawInput(amount, useBtc = useBtc)
        _state.update { it.copy(inputText = text) }
        handleAmountChanged(text)
    }

    fun handleAmountChanged(input: String) {
        val current = _state.value
        val locale = if (current.useBtc) Locale.US else getCurrencyLocale()
        val inputValue = input.toNumericValue(locale).toDouble()
        if (input != current.inputText) {
            _state.update { it.copy(inputText = input) }
        }
        if (current.useBtc) {
            if (inputValue != current.amountBTC) {
                _state.update {
                    it.copy(
                        amountBTC = if (CURRENT_DISPLAY_UNIT_TYPE == SAT) inputValue.fromSATtoBTC() else inputValue,
                        amountUSD = if (CURRENT_DISPLAY_UNIT_TYPE == SAT) {
                            inputValue.fromSATtoBTC().fromBTCToCurrency()
                        } else {
                            inputValue.fromBTCToCurrency()
                        },
                    )
                }
            }
        } else {
            if (inputValue != current.amountUSD) {
                _state.update {
                    it.copy(
                        amountBTC = inputValue.fromCurrencyToBTC(),
                        amountUSD = inputValue,
                    )
                }
            }
        }
    }

    private fun formatRawInput(amount: Double, useBtc: Boolean): String {
        if (amount <= 0.0) return ""
        return if (useBtc && CURRENT_DISPLAY_UNIT_TYPE == SAT) {
            amount.fromBTCtoSAT().toLong().toString()
        } else {
            rawDecimalFormat.format(amount)
        }
    }

    private val rawDecimalFormat = DecimalFormat(
        "0.########",
        DecimalFormatSymbols(Locale.US),
    ).apply {
        isGroupingUsed = false
    }

    fun getUseBTC() = _state.value.useBtc

    fun handleContinueEvent() {
        val amount = _state.value.amountBTC
        val event = when {
            amount <= 0 -> InputAmountEvent.InvalidAmountEvent
            amount > availableAmount -> InputAmountEvent.InsufficientFundsEvent
            hasLockedCoin && amount > availableAmountWithoutUnlocked && !isFromSelectedCoin ->
                InputAmountEvent.InsufficientFundsLockedCoinEvent
            else -> InputAmountEvent.AcceptAmountEvent(amount)
        }
        viewModelScope.launch { _event.emit(event) }
    }

    fun checkWallet(bsms: String?) = viewModelScope.launch {
        val claimWallet = bsms?.let { bsmsValue ->
            parseWalletDescriptorUseCase(bsmsValue).getOrDefault(Wallet())
        }
        val claimWalletId = claimWallet?.id
        getWalletsUseCase.execute()
            .flowOn(Dispatchers.IO)
            .onException { _event.emit(InputAmountEvent.ShowError(it.message.orUnknownError())) }
            .flowOn(Dispatchers.Main)
            .collect { wallets ->
                val oldWalletIds = wallets.map { wallet -> wallet.wallet.id }
                val hasWallet = if (!claimWalletId.isNullOrEmpty()) {
                    oldWalletIds.any { it != claimWalletId }
                } else {
                    oldWalletIds.isNotEmpty()
                }
                _event.emit(InputAmountEvent.CheckHasWallet(hasWallet))
            }
    }

    fun getAddress(): String = _state.value.address

    fun getPrivateNote(): String = _state.value.privateNote

    fun getAmountBtc(): Double = _state.value.amountBTC

    fun isHasLockedCoin() = hasLockedCoin
}
