package com.nunchuk.android.auth.components.verify

internal sealed class VerifyNewDeviceEvent {
    object ProcessingEvent : VerifyNewDeviceEvent()
    object SignInSuccessEvent : VerifyNewDeviceEvent()
    data class SignInErrorEvent(val message: String) : VerifyNewDeviceEvent()
}