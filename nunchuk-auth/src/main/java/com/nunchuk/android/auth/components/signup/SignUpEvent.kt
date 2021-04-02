package com.nunchuk.android.auth.components.signup

internal sealed class SignUpEvent {
    object NameRequiredEvent : SignUpEvent()
    object EmailRequiredEvent : SignUpEvent()
    object NameInvalidEvent : SignUpEvent()
    object EmailInvalidEvent : SignUpEvent()
    object NameValidEvent : SignUpEvent()
    object EmailValidEvent : SignUpEvent()
    object LoadingEvent : SignUpEvent()
    data class SignUpErrorEvent(val errorMessage: String?) : SignUpEvent()
    data class AccountExistedEvent(val errorMessage: String?) : SignUpEvent()
    object SignUpSuccessEvent : SignUpEvent()
}