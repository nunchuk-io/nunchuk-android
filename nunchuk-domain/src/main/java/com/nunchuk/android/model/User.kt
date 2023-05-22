package com.nunchuk.android.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val gender: String,
    val avatar: String,
    val status: String,
    val chatId: String,
    val loginType: String,
    val username: String?
)