package com.nunchuk.android.transaction.components.send.receipt

import com.nunchuk.android.model.Amount

sealed class AddReceiptEvent {
    object InvalidAddressEvent : AddReceiptEvent()
    object AddressRequiredEvent : AddReceiptEvent()
    data class ShowError(val message: String) : AddReceiptEvent()
    data class AcceptedAddressEvent(val address: String, val privateNote: String, val amount: Amount, val isCreateTransaction: Boolean) : AddReceiptEvent()
}

data class AddReceiptState(val address: String = "", val privateNote: String = "", val amount: Amount = Amount())
