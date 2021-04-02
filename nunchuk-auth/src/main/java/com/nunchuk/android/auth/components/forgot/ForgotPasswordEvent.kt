package com.nunchuk.android.auth.components.forgot

internal sealed class ForgotPasswordEvent {
    object EmailRequiredEvent : ForgotPasswordEvent()
    object EmailInvalidEvent : ForgotPasswordEvent()
    object EmailValidEvent : ForgotPasswordEvent()
    data class ForgotPasswordSuccessEvent(val email: String) : ForgotPasswordEvent()
    data class ForgotPasswordErrorEvent(val errorMessage: String?) : ForgotPasswordEvent()
}