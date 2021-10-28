package com.nunchuk.android.signer.software.components.passphrase

sealed class SetPassphraseEvent {
    data class LoadingEvent(val loading: Boolean) : SetPassphraseEvent()
    object PassPhraseRequiredEvent : SetPassphraseEvent()
    object PassPhraseValidEvent : SetPassphraseEvent()
    object ConfirmPassPhraseRequiredEvent : SetPassphraseEvent()
    object ConfirmPassPhraseNotMatchedEvent : SetPassphraseEvent()
    data class CreateSoftwareSignerCompletedEvent(
        val id: String,
        val name: String,
        val skipPassphrase: Boolean
    ) : SetPassphraseEvent()

    data class CreateSoftwareSignerErrorEvent(val message: String) : SetPassphraseEvent()
}

data class SetPassphraseState(
    val passphrase: String = "",
    val confirmPassphrase: String = ""
)