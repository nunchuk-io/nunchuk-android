package com.nunchuk.android.transaction.send.fee

import com.nunchuk.android.arch.vm.NunchukViewModel
import javax.inject.Inject

internal class EstimatedFeeViewModel @Inject constructor(
) : NunchukViewModel<EstimatedFeeState, EstimatedFeeEvent>() {

    override val initialState = EstimatedFeeState()

}