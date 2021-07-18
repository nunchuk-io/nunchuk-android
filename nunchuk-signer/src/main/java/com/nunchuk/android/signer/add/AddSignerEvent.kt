package com.nunchuk.android.signer.add

sealed class AddSignerEvent {
    data class AddSignerSuccessEvent(val id: String, val name: String) : AddSignerEvent()
    data class AddSignerErrorEvent(val message: String) : AddSignerEvent()
    object InvalidSignerSpecEvent : AddSignerEvent()
    object SignerNameRequiredEvent : AddSignerEvent()
    object LoadingEvent : AddSignerEvent()
}
