package com.nunchuk.android.signer.components.add

import com.nunchuk.android.model.SingleSigner

sealed class AddSignerEvent {
    data class AddSignerSuccessEvent(val singleSigner: SingleSigner) : AddSignerEvent()
    data class ParseKeystoneSignerSuccess(val signerSpec: String) : AddSignerEvent()
    data class AddSignerErrorEvent(val message: String) : AddSignerEvent()
    object InvalidSignerSpecEvent : AddSignerEvent()
    object SignerNameRequiredEvent : AddSignerEvent()
    object LoadingEvent : AddSignerEvent()
}
