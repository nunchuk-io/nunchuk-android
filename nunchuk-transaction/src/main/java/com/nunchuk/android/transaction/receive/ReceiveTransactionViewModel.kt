package com.nunchuk.android.transaction.receive

import com.nunchuk.android.arch.vm.NunchukViewModel
import javax.inject.Inject

internal class ReceiveTransactionViewModel @Inject constructor(
) : NunchukViewModel<ReceiveTransactionState, ReceiveTransactionEvent>() {

    override val initialState = ReceiveTransactionState()

    fun init() {
        updateState { initialState }
    }

}