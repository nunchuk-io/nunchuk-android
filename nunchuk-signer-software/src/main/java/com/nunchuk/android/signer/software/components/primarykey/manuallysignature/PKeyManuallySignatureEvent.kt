package com.nunchuk.android.signer.software.components.primarykey.manuallysignature

sealed class PKeyManuallySignatureEvent {
    data class LoadingEvent(val loading: Boolean) : PKeyManuallySignatureEvent()
    data class ProcessFailure(val message: String) : PKeyManuallySignatureEvent()
    object SignInSuccess : PKeyManuallySignatureEvent()
    data class GetTurnOnNotificationSuccess(val isTurnOn: Boolean) : PKeyManuallySignatureEvent()
}

data class PKeyManuallySignatureState(
    val username: String? = null,
    val signature: String? = null,
    val challengeMessage: String = "",
    val staySignedIn: Boolean = false
)