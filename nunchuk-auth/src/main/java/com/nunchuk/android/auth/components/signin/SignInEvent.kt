package com.nunchuk.android.auth.components.signin

import com.nunchuk.android.core.network.ErrorDetail
import com.nunchuk.android.model.PrimaryKey

internal sealed class SignInEvent {
    object EmailRequiredEvent : SignInEvent()
    object EmailValidEvent : SignInEvent()
    object EmailInvalidEvent : SignInEvent()
    object PasswordRequiredEvent : SignInEvent()
    object PasswordValidEvent : SignInEvent()
    object ProcessingEvent : SignInEvent()
    data class SignInSuccessEvent(val token: String, val deviceId: String) : SignInEvent()
    data class SignInErrorEvent(val code: Int? = null, val message: String? = null, val errorDetail: ErrorDetail? = null) : SignInEvent()
    data class CheckPrimaryKeyAccountEvent(val accounts: ArrayList<PrimaryKey>) : SignInEvent()
}