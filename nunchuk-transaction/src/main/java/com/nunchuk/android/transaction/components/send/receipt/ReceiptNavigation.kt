package com.nunchuk.android.transaction.components.send.receipt

import kotlinx.serialization.Serializable

@Serializable
sealed class ReceiptNavigation

@Serializable
data object Main : ReceiptNavigation()
@Serializable
data object Batch : ReceiptNavigation()
@Serializable
data object TaprootFeeSelection : ReceiptNavigation()
@Serializable
data object ChooseSigningPath : ReceiptNavigation()
@Serializable
data object TimelockNotice : ReceiptNavigation()
@Serializable
data class ChooseSigningPolicy(
    val isSelectingModeEnabled: Boolean,
)