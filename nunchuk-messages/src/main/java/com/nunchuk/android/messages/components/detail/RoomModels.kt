package com.nunchuk.android.messages.components.detail

data class Message(
    val sender: String,
    val content: String,
    var type: Int
)

enum class MessageType(val index: Int) {
    CHAT_MINE(0),
    CHAT_PARTNER(1)
}