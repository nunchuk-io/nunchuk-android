package com.nunchuk.android.auth.components.signin

internal sealed class SignInEvent {
    object EmailRequiredEvent : SignInEvent()
    object EmailValidEvent : SignInEvent()
    object EmailInvalidEvent : SignInEvent()
    object PasswordRequiredEvent : SignInEvent()
    object PasswordValidEvent : SignInEvent()
    object ProcessingEvent : SignInEvent()
    object SignInSuccessEvent : SignInEvent()
    data class SignInErrorEvent(val message: String?) : SignInEvent()
}