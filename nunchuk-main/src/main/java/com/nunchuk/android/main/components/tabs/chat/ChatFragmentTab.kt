package com.nunchuk.android.main.components.tabs.chat

private const val POSITION_MESSAGES = 0
private const val POSITION_CONTACTS = 1

enum class ChatFragmentTab(val position: Int) {
    MESSAGES(POSITION_MESSAGES),
    CONTACTS(POSITION_CONTACTS)
}
