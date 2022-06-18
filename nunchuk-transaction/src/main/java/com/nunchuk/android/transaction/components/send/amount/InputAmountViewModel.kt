package com.nunchuk.android.transaction.components.send.amount

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.entities.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.entities.SAT
import com.nunchuk.android.core.util.fromBTCToSAT
import com.nunchuk.android.core.util.fromBTCToUSD
import com.nunchuk.android.core.util.fromUSDToBTC
import com.nunchuk.android.core.util.toNumericValue
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.SwapCurrencyEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
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

    fun handleAmountChanged(input: String) {
        val inputValue = input.toNumericValue().toDouble()
        val currentState = getState()
        if (currentState.useBtc) {
            if (inputValue != currentState.amountBTC) {
                updateState { copy(
                    amountBTC = if(CURRENT_DISPLAY_UNIT_TYPE == SAT) inputValue.fromBTCToSAT()  else inputValue,
                    amountUSD = if(CURRENT_DISPLAY_UNIT_TYPE == SAT) inputValue.fromBTCToSAT().fromBTCToUSD()  else inputValue.fromBTCToUSD() )
                }
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