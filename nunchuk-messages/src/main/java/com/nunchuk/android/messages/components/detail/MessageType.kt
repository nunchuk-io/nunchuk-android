package com.nunchuk.android.messages.components.detail

enum class MessageType(val index: Int) {
    TYPE_CHAT_MINE(0),
    TYPE_CHAT_PARTNER(1),
    TYPE_NOTIFICATION(2),
    TYPE_DATE(3),
    TYPE_NUNCHUK_WALLET_CARD(4),
    TYPE_NUNCHUK_WALLET_NOTIFICATION(5),
    TYPE_NUNCHUK_TRANSACTION_CARD(6),
    TYPE_NUNCHUK_TRANSACTION_NOTIFICATION(7)
}