package com.nunchuk.android.messages.model

data class Message(
    val contactName: String,
    val lastMessage: String,
    val time: String,
    val messageCount: Int = 0
)
