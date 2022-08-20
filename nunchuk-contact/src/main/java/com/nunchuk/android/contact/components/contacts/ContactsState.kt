package com.nunchuk.android.contact.components.contacts

import com.nunchuk.android.model.Contact

data class ContactsState(
    val contacts: List<Contact>,
    val pendingContacts: List<Contact>,
    val receivedContactRequestCount: Int
) {

    companion object {
        fun empty() = ContactsState(
            contacts = emptyList(),
            pendingContacts = emptyList(),
            receivedContactRequestCount = 0
        )
    }

}
