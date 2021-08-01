package com.nunchuk.android.signer.components.ss.confirm

sealed class ConfirmSeedEvent {
    object ConfirmSeedCompletedEvent : ConfirmSeedEvent()
    object SelectedIncorrectWordEvent : ConfirmSeedEvent()
}

data class ConfirmSeedState(val groups: List<PhraseWordGroup> = ArrayList())
