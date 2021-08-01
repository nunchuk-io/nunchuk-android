package com.nunchuk.android.signer.components.add

sealed class AddSignerEvent {
    data class AddSignerSuccessEvent(val id: String, val name: String) : AddSignerEvent()
    data class AddSignerErrorEvent(val message: String) : AddSignerEvent()
    object InvalidSignerSpecEvent : AddSignerEvent()
    object SignerNameRequiredEvent : AddSignerEvent()
    object LoadingEvent : AddSignerEvent()
}
