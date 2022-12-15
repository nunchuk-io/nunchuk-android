package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref

import com.nunchuk.android.contact.components.add.EmailWithState

sealed class InheritanceNotifyPrefEvent {
    data class ContinueClick(val emails: List<String>, val isNotify: Boolean) :
        InheritanceNotifyPrefEvent()

    object InvalidEmailEvent : InheritanceNotifyPrefEvent()
    object AllEmailValidEvent : InheritanceNotifyPrefEvent()
}

data class InheritanceNotifyPrefState(
    val emails: List<EmailWithState> = emptyList(),
    val isNotify: Boolean = false
)