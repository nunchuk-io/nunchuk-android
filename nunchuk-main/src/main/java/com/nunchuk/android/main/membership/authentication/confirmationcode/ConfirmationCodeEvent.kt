package com.nunchuk.android.main.membership.authentication.confirmationcode

sealed class ConfirmChangeEvent {
    data class Loading(val loading: Boolean) : ConfirmChangeEvent()
    data class Error(val message: String) : ConfirmChangeEvent()
}

data class ConfirmChangeState(
    val code: String = "",
)