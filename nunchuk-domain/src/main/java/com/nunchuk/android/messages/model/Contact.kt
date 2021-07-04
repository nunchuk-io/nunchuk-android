package com.nunchuk.android.messages.model

data class Contact(
    val id: String,
    val name: String,
    val email: String,
    val gender: String,
    val avatar: String,
    val status: String,
    val chatId: String
)