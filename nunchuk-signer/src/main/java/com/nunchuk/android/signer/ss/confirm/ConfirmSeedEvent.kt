package com.nunchuk.android.signer.ss.confirm

sealed class ConfirmSeedEvent {
    object ConfirmSeedCompletedEvent : ConfirmSeedEvent()
    object SelectedIncorrectWordEvent : ConfirmSeedEvent()
}

data class ConfirmSeedState(val groups: List<PhraseWordGroup> = ArrayList())
