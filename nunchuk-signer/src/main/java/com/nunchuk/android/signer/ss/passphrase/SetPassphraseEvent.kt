package com.nunchuk.android.signer.ss.passphrase

sealed class SetPassphraseEvent {
    object PassPhraseRequiredEvent : SetPassphraseEvent()
    object ConfirmPassPhraseRequiredEvent : SetPassphraseEvent()
    object ConfirmPassPhraseNotMatchedEvent : SetPassphraseEvent()
    data class PassphraseCompletedEvent(val skip: Boolean) : SetPassphraseEvent()
}

data class SetPassphraseState(
    val passphrase: String = "",
    val confirmPassphrase: String = ""
)