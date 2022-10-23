package com.nunchuk.android.model

data class Contact(
    val id: String,
    val name: String,
    val email: String,
    val gender: String,
    val avatar: String,
    val status: String,
    val chatId: String,
    val loginType: String,
    val username: String
) {

    companion object {
        const val PRIMARY_KEY = "PRIMARY_KEY"
        const val EMAIL = "EMAIL"
        const val UNKNOWN = "UNKNOWN"
    }

    fun isLoginInPrimaryKey() = loginType == PRIMARY_KEY
}

data class SentContact(val contact: Contact)

data class ReceiveContact(val contact: Contact)