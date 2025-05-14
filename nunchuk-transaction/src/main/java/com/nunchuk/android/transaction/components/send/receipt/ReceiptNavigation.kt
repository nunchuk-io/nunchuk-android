package com.nunchuk.android.transaction.components.send.receipt

import kotlinx.serialization.Serializable

@Serializable
sealed class ReceiptNavigation {
    @Serializable
    data object Main : ReceiptNavigation()
    @Serializable
    data object TaprootFeeSelection : ReceiptNavigation()
}