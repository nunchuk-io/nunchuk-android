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
                        amountUSD = if (CURRENT_DISPLAY_UNIT_TYPE == SAT) inputValue.fromSATtoBTC().fromBTCToUSD() else inputValue.fromBTCToUSD()
                    )
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

    fun getAddress(): String = getState().address

    fun getPrivateNote(): String = getState().privateNote

    fun getAmountBtc(): Double = getState().amountBTC
}