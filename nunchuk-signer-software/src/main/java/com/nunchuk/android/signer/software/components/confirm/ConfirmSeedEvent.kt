package com.nunchuk.android.signer.software.components.confirm

sealed class ConfirmSeedEvent {
    object ConfirmSeedCompletedEvent : ConfirmSeedEvent()
    object SelectedIncorrectWordEvent : ConfirmSeedEvent()
}

data class ConfirmSeedState(val groups: List<PhraseWordGroup> = ArrayList())
