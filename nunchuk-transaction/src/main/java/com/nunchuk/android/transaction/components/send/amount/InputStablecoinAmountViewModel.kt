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
import com.nunchuk.android.core.util.BTC_CURRENCY_EXCHANGE_RATE
import com.nunchuk.android.core.util.USDT_CURRENCY_EXCHANGE_RATE
import com.nunchuk.android.core.util.fromBTCToCurrency
import com.nunchuk.android.core.util.fromUsdtToCurrency
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.toNumericValue
import com.nunchuk.android.model.defaultRate
import com.nunchuk.android.usecase.EstimateLiquidFeeUseCase
import com.nunchuk.android.usecase.GetLbtcAssetIdUseCase
import com.nunchuk.android.usecase.GetUsdtAssetIdUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.IsLiquidAddressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
internal class InputStablecoinAmountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getWalletUseCase: GetWalletUseCase,
    private val estimateLiquidFeeUseCase: EstimateLiquidFeeUseCase,
    private val getUsdtAssetIdUseCase: GetUsdtAssetIdUseCase,
    private val getLbtcAssetIdUseCase: GetLbtcAssetIdUseCase,
    private val isLiquidAddressUseCase: IsLiquidAddressUseCase,
) : ViewModel() {

    private val args = InputAmountArgs.fromSavedStateHandle(savedStateHandle)

    private val _state = MutableStateFlow(InputStablecoinAmountState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<InputStablecoinAmountEvent>()
    val event = _event.asSharedFlow()

    init {
        loadWalletBalances()
        loadLiquidFee()
    }

    private fun loadWalletBalances() {
        if (args.walletId.isEmpty()) return
        viewModelScope.launch {
            getWalletUseCase.execute(args.walletId)
                .flowOn(Dispatchers.IO)
                .collect { extended ->
                    val wallet = extended.wallet
                    val usdt = wallet.usdtBalance.pureBTC()
                    val lbtc = wallet.lbtcBalance.pureBTC()
                    _state.update {
                        it.copy(
                            usdtBalance = usdt,
                            usdtBalanceUsd = usdt.fromUsdtToCurrency(),
                            lbtcBalance = lbtc,
                            lbtcBalanceUsd = lbtc.fromBTCToCurrency(),
                        )
                    }
                }
        }
    }

    private fun loadLiquidFee() {
        viewModelScope.launch {
            estimateLiquidFeeUseCase(Unit).onSuccess { rates ->
                // Fee rate is sat/kvB. Liquid simple 1-in/2-out confidential tx ≈ 1.5 kvB.
                val rateSatPerKvB = rates.defaultRate
                val estimatedSizeKvB = LIQUID_TX_ESTIMATED_KVB
                val feeSat = rateSatPerKvB * estimatedSizeKvB
                val feeLbtc = feeSat / SAT_PER_BTC
                _state.update {
                    it.copy(
                        networkFeeLbtc = feeLbtc,
                        networkFeeUsd = feeLbtc.fromBTCToCurrency(),
                    )
                }
            }
        }
    }

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
                isSendAll = false,
            )
        }
    }

    fun parseLiquidAddress(content: String) {
        viewModelScope.launch {
            val address = content.trim()
            val isValid = isLiquidAddressUseCase(address).getOrDefault(false)
            if (isValid) {
                _state.update { it.copy(address = address) }
                _event.emit(InputStablecoinAmountEvent.AddressScanned(address))
            } else {
                _event.emit(InputStablecoinAmountEvent.InvalidAddressEvent)
            }
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
            // The user edited the amount manually, so it's no longer a "send all".
            _state.update { it.copy(inputText = input, isSendAll = false) }
        }
        val tokenToFiat = when (current.selectedToken) {
            StablecoinToken.USDT -> USDT_CURRENCY_EXCHANGE_RATE
            StablecoinToken.LBTC -> BTC_CURRENCY_EXCHANGE_RATE
        }
        if (current.useToken) {
            if (inputValue != current.amountToken) {
                _state.update {
                    it.copy(
                        amountToken = inputValue,
                        amountUsd = inputValue * tokenToFiat,
                    )
                }
            }
        } else {
            if (inputValue != current.amountUsd) {
                _state.update {
                    it.copy(
                        amountToken = if (tokenToFiat == 0.0) 0.0 else inputValue / tokenToFiat,
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
        // setInputAmount() runs handleAmountChanged() which clears isSendAll, so flag
        // it afterwards. The fee is paid in LBTC, so a send-all only needs the fee
        // subtracted from the output when the sent asset is LBTC itself.
        setInputAmount(amount)
        _state.update { it.copy(isSendAll = true) }
    }

    fun handleContinueEvent() {
        viewModelScope.launch {
            val current = _state.value
            val inputAmount = current.amountToken
            val balance = balanceForToken(current.selectedToken)
            // Tapping "Send all" or typing an amount that reaches the balance are both
            // send-all: the whole balance leaves the wallet, so there's no room for fee.
            // Use >= (with a sub-satoshi tolerance) so a typed full balance still counts
            // even if it rounds a hair above the stored balance.
            val isSendAll = current.isSendAll || inputAmount >= balance - SEND_ALL_EPSILON
            // Clamp a send-all to the exact balance; the lib subtracts the fee from it.
            val amount = if (isSendAll) balance else inputAmount
            val event = when {
                inputAmount <= 0 -> InputStablecoinAmountEvent.InvalidAmountEvent
                inputAmount > balance + SEND_ALL_EPSILON -> InputStablecoinAmountEvent.InsufficientFundsEvent
                else -> InputStablecoinAmountEvent.AcceptAmountEvent(
                    amount = amount,
                    token = current.selectedToken,
                    tokenAssetId = assetIdFor(current.selectedToken),
                    // Only the fee asset (LBTC) output can absorb the network fee; a
                    // USDT send-all still pays the fee from the separate LBTC balance.
                    subtractFeeFromAmount = isSendAll &&
                            current.selectedToken == StablecoinToken.LBTC,
                    // Carry a scanned Liquid address (if any) to prefill the recipient screen.
                    address = current.address,
                )
            }
            _event.emit(event)
        }
    }

    private suspend fun assetIdFor(token: StablecoinToken): String = when (token) {
        StablecoinToken.USDT -> getUsdtAssetIdUseCase(Unit).getOrDefault("")
        StablecoinToken.LBTC -> getLbtcAssetIdUseCase(Unit).getOrDefault("")
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

    companion object {
        // Liquid confidential 1-in/2-out p2wpkh tx is ~1.5 kvB after rangeproof discount.
        private const val LIQUID_TX_ESTIMATED_KVB = 1.5
        private const val SAT_PER_BTC = 100_000_000.0

        // Half a satoshi: tighter than the smallest representable unit (1e-8) so a typed
        // full balance counts as send-all without false positives from rounding.
        private const val SEND_ALL_EPSILON = 0.5 / SAT_PER_BTC
    }
}
