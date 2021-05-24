package com.nunchuk.android.transaction.send.receipt

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.transaction.send.confirmation.TransactionConfirmEvent
import com.nunchuk.android.transaction.send.confirmation.TransactionConfirmState
import javax.inject.Inject

internal class AddReceiptViewModel @Inject constructor(
) : NunchukViewModel<TransactionConfirmState, TransactionConfirmEvent>() {

    override val initialState = TransactionConfirmState()

}