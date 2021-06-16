package com.nunchuk.android.messages.model

data class Message(val contactName: String, val lastMessage: String, val time: String, val messageCount: Int = 0)

object MessageProvider {

    fun messages() = listOf(
        Message(
            contactName = "Khoa Pham",
            lastMessage = "Khoa: Last message should be at this size powe...",
            time = "1d ago",
            messageCount = 0
        ),
        Message(
            contactName = "Khoa Pham, Hugo Nguyen, Ace Le, Chi Nguyen, Hung...",
            lastMessage = "Khoa: Last message should be at this size powe...",
            time = "Mar 7",
            messageCount = 10
        ),
        Message(
            contactName = "Bruce Bee",
            lastMessage = "Bruce: Welcome to Nunchuk! My name...",
            time = "1m",
            messageCount = 10
        ),
    )

}