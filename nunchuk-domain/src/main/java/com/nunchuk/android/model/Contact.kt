package com.nunchuk.android.model

data class Contact(
    val id: String,
    val name: String,
    val email: String,
    val gender: String,
    val avatar: String,
    val status: String,
    val chatId: String
)

data class SentContact(val contact: Contact)

data class ReceiveContact(val contact: Contact)