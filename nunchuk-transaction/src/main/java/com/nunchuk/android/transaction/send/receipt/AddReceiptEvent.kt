package com.nunchuk.android.transaction.send.receipt

sealed class AddReceiptEvent {
    object InvalidAddressEvent : AddReceiptEvent()
    object AddressRequiredEvent : AddReceiptEvent()
    data class AcceptedAddressEvent(val address: String, val privateNote: String) : AddReceiptEvent()
}

data class AddReceiptState(val address: String = "", val privateNote: String = "")
