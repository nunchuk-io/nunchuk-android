package com.nunchuk.android.transaction.send.fee

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
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
        updateState { copy(customizeFeeDetails = checked) }
    }

}