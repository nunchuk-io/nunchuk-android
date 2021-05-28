package com.nunchuk.android.transaction.send.fee

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.transaction.send.fee.EstimatedFeeEvent.EstimatedFeeCompletedEvent
import com.nunchuk.android.transaction.send.fee.EstimatedFeeEvent.EstimatedFeeErrorEvent
import com.nunchuk.android.usecase.EstimateFeeUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class EstimatedFeeViewModel @Inject constructor(
    private val estimateFeeUseCase: EstimateFeeUseCase
) : NunchukViewModel<EstimatedFeeState, EstimatedFeeEvent>() {

    override val initialState = EstimatedFeeState()

    init {
        viewModelScope.launch {
            when (val result = estimateFeeUseCase.execute()) {
                is Success -> updateState { copy(estimatedFee = result.data) }
                is Error -> event(EstimatedFeeErrorEvent(result.exception.message.orEmpty()))
            }
        }
    }

    fun handleCustomizeFeeSwitch(checked: Boolean) {
        if (checked) {
            updateState { copy(customizeFeeDetails = true) }
        } else {
            updateState { copy(customizeFeeDetails = false, manualFeeDetails = false, manualFeeRate = 0, subtractFeeFromSendMoney = false) }
        }
    }

    fun handleSubtractFeeSwitch(checked: Boolean) {
        updateState { copy(subtractFeeFromSendMoney = checked) }
    }

    fun handleManualFeeSwitch(checked: Boolean) {
        updateState { copy(manualFeeDetails = checked) }
    }

    fun handleContinueEvent() {
        getState().apply {
            event(
                EstimatedFeeCompletedEvent(
                    estimatedFee = estimatedFee.pureBTC(),
                    subtractFeeFromSendMoney = subtractFeeFromSendMoney,
                    manualFeeRate = manualFeeRate
                )
            )
        }
    }

}