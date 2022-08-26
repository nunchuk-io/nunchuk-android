package com.nunchuk.android.signer.software.components.passphrase

import com.nunchuk.android.model.MasterSigner

sealed class SetPassphraseEvent {
    data class LoadingEvent(val loading: Boolean) : SetPassphraseEvent()
    object PassPhraseRequiredEvent : SetPassphraseEvent()
    object PassPhraseValidEvent : SetPassphraseEvent()
    object ConfirmPassPhraseRequiredEvent : SetPassphraseEvent()
    object ConfirmPassPhraseNotMatchedEvent : SetPassphraseEvent()
    data class CreateSoftwareSignerCompletedEvent(
        val masterSigner: MasterSigner? = null,
        val skipPassphrase: Boolean
    ) : SetPassphraseEvent()

    data class CreateSoftwareSignerErrorEvent(val message: String) : SetPassphraseEvent()
    data class CreateWalletSuccessEvent(val walletId: String) : SetPassphraseEvent()
    data class CreateWalletErrorEvent(val message: String) : SetPassphraseEvent()
}

data class SetPassphraseState(
    val passphrase: String = "",
    val confirmPassphrase: String = ""
)