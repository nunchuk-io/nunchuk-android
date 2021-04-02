package com.nunchuk.android.auth.components.recover

internal sealed class RecoverPasswordEvent {
    object OldPasswordRequiredEvent : RecoverPasswordEvent()
    object NewPasswordRequiredEvent : RecoverPasswordEvent()
    object ConfirmPasswordRequiredEvent : RecoverPasswordEvent()
    object ConfirmPasswordNotMatchedEvent : RecoverPasswordEvent()
    object OldPasswordValidEvent : RecoverPasswordEvent()
    object NewPasswordValidEvent : RecoverPasswordEvent()
    object ConfirmPasswordValidEvent : RecoverPasswordEvent()
    object RecoverPasswordSuccessEvent : RecoverPasswordEvent()
    data class RecoverPasswordErrorEvent(val errorMessage: String?) : RecoverPasswordEvent()
}