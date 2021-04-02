package com.nunchuk.android.auth.components.changepass

internal sealed class ChangePasswordEvent {
    data class ShowEmailSentEvent(val email: String) : ChangePasswordEvent()
    object OldPasswordRequiredEvent : ChangePasswordEvent()
    object NewPasswordRequiredEvent : ChangePasswordEvent()
    object ConfirmPasswordRequiredEvent : ChangePasswordEvent()
    object ConfirmPasswordNotMatchedEvent : ChangePasswordEvent()
    object OldPasswordValidEvent : ChangePasswordEvent()
    object NewPasswordValidEvent : ChangePasswordEvent()
    object ConfirmPasswordValidEvent : ChangePasswordEvent()
    object ChangePasswordSuccessEvent : ChangePasswordEvent()
    data class ChangePasswordSuccessError(val errorMessage: String?) : ChangePasswordEvent()
}