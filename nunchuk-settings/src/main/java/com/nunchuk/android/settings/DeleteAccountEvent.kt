package com.nunchuk.android.settings

data class DeleteAccountState(val email: String)

sealed class DeleteAccountEvent {
    object Loading : DeleteAccountEvent()
    object ConfirmDeleteSuccess : DeleteAccountEvent()
    data class ConfirmDeleteError(val message: String) : DeleteAccountEvent()
}
