package com.nunchuk.android.messages.components.group.action

import com.nunchuk.android.model.Contact

data class AddMembersState(
    val receipts: List<Contact> = ArrayList(),
    val suggestions: List<Contact> = ArrayList()
)

sealed class AddMembersEvent {
    object NoContactsEvent : AddMembersEvent()
    object AddMembersSuccessEvent : AddMembersEvent()
    data class AddMembersError(val message: String) : AddMembersEvent()
}
