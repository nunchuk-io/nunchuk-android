package com.nunchuk.android.transaction.components.send.amount

import com.nunchuk.android.model.BtcUri

sealed class InputAmountEvent {
    data class AcceptAmountEvent(val amount: Double) : InputAmountEvent()
    data class SwapCurrencyEvent(val amount: Double) : InputAmountEvent()
    data class ParseBtcUriSuccess(val btcUri: BtcUri) : InputAmountEvent()
    data class ShowError(val message: String) : InputAmountEvent()
    object InsufficientFundsEvent : InputAmountEvent()
}

data class InputAmountState(
    val amountBTC: Double = 0.0,
    val amountUSD: Double = 0.0,
    val useBtc: Boolean = true,
    val address: String = "",
    val privateNote: String = "",
)