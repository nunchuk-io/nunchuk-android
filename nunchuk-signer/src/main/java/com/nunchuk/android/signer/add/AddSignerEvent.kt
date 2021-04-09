package com.nunchuk.android.signer.add

sealed class AddSignerEvent {
    data class AddSignerSuccessEvent(
        val signerName: String,
        val signerSpec: String
    ) : AddSignerEvent()

    data class AddSignerErrorEvent(val message: String) : AddSignerEvent()

    object InvalidSignerSpecEvent : AddSignerEvent()

    object SignerNameRequiredEvent : AddSignerEvent()
}
