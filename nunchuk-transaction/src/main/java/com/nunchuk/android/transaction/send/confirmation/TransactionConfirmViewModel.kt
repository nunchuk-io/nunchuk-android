package com.nunchuk.android.transaction.send.confirmation

import com.nunchuk.android.arch.vm.NunchukViewModel
import javax.inject.Inject

internal class TransactionConfirmViewModel @Inject constructor(
) : NunchukViewModel<TransactionConfirmState, TransactionConfirmEvent>() {

    override val initialState = TransactionConfirmState()

}