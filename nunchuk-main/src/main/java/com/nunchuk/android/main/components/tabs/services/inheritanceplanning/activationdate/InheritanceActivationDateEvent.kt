package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.activationdate

sealed class InheritanceActivationDateEvent {
    data class ContinueClick(val date: Long) : InheritanceActivationDateEvent()
}

data class InheritanceActivationDateState(val date: Long = 0L, val displayDate: String = "")