package com.nunchuk.android.contact.components.add

data class AddContactsState(val emails: List<EmailWithState>) {

    companion object {
        fun empty() = AddContactsState(ArrayList())
    }

}

sealed class AddContactsEvent {
    object InvalidEmailEvent : AddContactsEvent()
    object AllEmailValidEvent : AddContactsEvent()
    object AddContactSuccessEvent : AddContactsEvent()
    data class AddContactsErrorEvent(val message: String) : AddContactsEvent()
    data class LoadingEvent(val loading: Boolean = true) : AddContactsEvent()
}

data class EmailWithState(val email: String, val valid: Boolean = true)