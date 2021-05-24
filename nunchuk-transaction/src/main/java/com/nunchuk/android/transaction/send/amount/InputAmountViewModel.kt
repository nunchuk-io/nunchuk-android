package com.nunchuk.android.transaction.send.amount

import com.nunchuk.android.arch.vm.NunchukViewModel
import javax.inject.Inject

internal class InputAmountViewModel @Inject constructor(
) : NunchukViewModel<InputAmountState, InputAmountEvent>() {

    override val initialState = InputAmountState()

}