package com.nunchuk.android.transaction.send.amount

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.fromBTCToUSD
import com.nunchuk.android.core.util.fromUSDToBTC
import com.nunchuk.android.transaction.send.amount.InputAmountEvent.SwapCurrencyEvent
import javax.inject.Inject

internal class InputAmountViewModel @Inject constructor(
) : NunchukViewModel<InputAmountState, InputAmountEvent>() {

    private var availableAmount: Double = 0.0

    override val initialState = InputAmountState()

    fun init(availableAmount: Double) {
        updateState { initialState }
        this.availableAmount = availableAmount
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

    private fun String.toAmountValue() = try {
        toDouble()
    } catch (t: Exception) {
        0.0
    }

    fun handleAmountChanged(input: String) {
        val inputValue = input.toAmountValue()
        val currentState = getState()
        if (currentState.useBtc) {
            if (inputValue != currentState.amountBTC) {
                updateState { copy(amountBTC = inputValue, amountUSD = inputValue.fromBTCToUSD()) }
            }
        } else {
            if (inputValue != currentState.amountUSD) {
                updateState { copy(amountBTC = inputValue.fromUSDToBTC(), amountUSD = inputValue) }
            }
        }
    }

    fun handleContinueEvent() {
        val amount = getState().amountBTC
        if (amount <= 0 || amount > availableAmount) {
            event(InputAmountEvent.InsufficientFundsEvent)
        } else {
            event(InputAmountEvent.AcceptAmountEvent(amount))
        }
    }

}