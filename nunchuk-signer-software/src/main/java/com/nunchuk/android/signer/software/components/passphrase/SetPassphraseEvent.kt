package com.nunchuk.android.signer.software.components.passphrase

import com.nunchuk.android.model.MasterSigner

sealed class SetPassphraseEvent {
    data class LoadingEvent(val loading: Boolean) : SetPassphraseEvent()
    object PassPhraseRequiredEvent : SetPassphraseEvent()
    object PassPhraseValidEvent : SetPassphraseEvent()
    object ConfirmPassPhraseRequiredEvent : SetPassphraseEvent()
    object ConfirmPassPhraseNotMatchedEvent : SetPassphraseEvent()
    data class CreateSoftwareSignerCompletedEvent(
        val masterSigner: MasterSigner,
        val skipPassphrase: Boolean
    ) : SetPassphraseEvent()

    data class CreateSoftwareSignerErrorEvent(val message: String) : SetPassphraseEvent()
}

data class SetPassphraseState(
    val passphrase: String = "",
    val confirmPassphrase: String = ""
)