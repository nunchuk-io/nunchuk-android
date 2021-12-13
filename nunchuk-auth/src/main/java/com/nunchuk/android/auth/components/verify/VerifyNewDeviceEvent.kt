package com.nunchuk.android.auth.components.verify

internal sealed class VerifyNewDeviceEvent {
    object ProcessingEvent : VerifyNewDeviceEvent()
    data class SignInSuccessEvent(val token: String, val encryptedDeviceId: String) : VerifyNewDeviceEvent()
    data class SignInErrorEvent(val message: String) : VerifyNewDeviceEvent()
}