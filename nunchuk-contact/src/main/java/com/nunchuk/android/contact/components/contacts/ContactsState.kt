package com.nunchuk.android.contact.components.contacts

import com.nunchuk.android.model.Contact

data class ContactsState(val contacts: List<Contact>, val pendingContacts: List<Contact>) {

    companion object {
        fun empty() = ContactsState(emptyList(), emptyList())
    }

}
