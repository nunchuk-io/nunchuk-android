package com.nunchuk.android.messages.contact

data class AddContactsState(val emails: List<String>) {

    companion object {
        fun empty() = AddContactsState(ArrayList())
    }

}

sealed class AddContactsEvent {
    object InvalidEmailEvent : AddContactsEvent()
    object AllEmailValidEvent : AddContactsEvent()
}