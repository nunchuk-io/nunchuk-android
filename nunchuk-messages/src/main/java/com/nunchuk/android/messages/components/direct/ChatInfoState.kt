package com.nunchuk.android.messages.components.direct

import com.nunchuk.android.model.Contact
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.Wallet

data class ChatInfoState(
    val contact: Contact? = null,
    val roomWallet: RoomWallet? = null,
    val wallet: Wallet? = null
)

sealed class ChatInfoEvent {
    object RoomNotFoundEvent : ChatInfoEvent()
    object CreateSharedWalletEvent : ChatInfoEvent()
    data class CreateTransactionEvent(
        val roomId: String,
        val walletId: String,
        val availableAmount: Double
    ) : ChatInfoEvent()
}