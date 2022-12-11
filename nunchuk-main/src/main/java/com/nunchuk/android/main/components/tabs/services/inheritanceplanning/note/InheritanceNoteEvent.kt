package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.note

sealed class InheritanceNoteEvent {
    data class ContinueClick(val note: String) : InheritanceNoteEvent()
}

data class InheritanceNoteState(val note: String = "")