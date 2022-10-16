package com.nunchuk.android.signer.software.components.primarykey.passphrase

import com.nunchuk.android.core.domain.CheckPassphrasePrimaryKeyUseCase

sealed class PKeyEnterPassphraseEvent {
    data class LoadingEvent(val loading: Boolean) : PKeyEnterPassphraseEvent()
    data class CheckPassphraseSuccess(val result: CheckPassphrasePrimaryKeyUseCase.Result) : PKeyEnterPassphraseEvent()
    object CheckPassphraseError : PKeyEnterPassphraseEvent()
}

data class PKeyEnterPassphraseState(
    val args: PKeyEnterPassphraseArgs? = null,
    val passphrase: String = ""
)