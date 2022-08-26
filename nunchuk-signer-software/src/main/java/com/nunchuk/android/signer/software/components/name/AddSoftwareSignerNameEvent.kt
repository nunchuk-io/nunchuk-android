package com.nunchuk.android.signer.software.components.name

sealed class AddSoftwareSignerNameEvent {
    data class SignerNameInputCompletedEvent(val signerName: String) : AddSoftwareSignerNameEvent()
    object SignerNameRequiredEvent : AddSoftwareSignerNameEvent()
    data class ImportPrimaryKeyErrorEvent(val message: String) : AddSoftwareSignerNameEvent()
    data class LoadingEvent(val loading: Boolean) : AddSoftwareSignerNameEvent()
    data class InitFailure(val message: String) : AddSoftwareSignerNameEvent()
    data class GetTurnOnNotificationSuccess(val isTurnOn: Boolean) : AddSoftwareSignerNameEvent()
}

data class AddSoftwareSignerNameState(
    val args: AddSoftwareSignerNameArgs? = null,
    val signerName: String = ""
)