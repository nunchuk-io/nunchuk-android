package com.nunchuk.android.transaction.components.send.amount

sealed class InputAmountEvent {
    data class AcceptAmountEvent(val amount: Double) : InputAmountEvent()
    data class SwapCurrencyEvent(val amount: Double) : InputAmountEvent()
    object InsufficientFundsEvent : InputAmountEvent()
}

data class InputAmountState(
    val amountBTC: Double = 0.0,
    val amountUSD: Double = 0.0,
    val useBtc: Boolean = true
)