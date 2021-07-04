package com.nunchuk.android.main.components.tabs.chat.contacts

import com.nunchuk.android.messages.model.Contact

data class ContactsState(val contacts: List<Contact>) {

    companion object {
        fun empty() = ContactsState(emptyList())
    }

}

sealed class ContactsEvent
