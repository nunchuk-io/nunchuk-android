package com.nunchuk.android.main.components.signer

sealed class AddSignerEvent {
    data class AddSignerSuccessEvent(
        val signerName: String,
        val signerSpec: String
    ) : AddSignerEvent()

    object SignerExistedEvent : AddSignerEvent()

    object InvalidSignerSpecEvent : AddSignerEvent()
}
