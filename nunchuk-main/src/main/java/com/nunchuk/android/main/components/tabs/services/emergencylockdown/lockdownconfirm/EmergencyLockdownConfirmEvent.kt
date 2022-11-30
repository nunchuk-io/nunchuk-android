package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownconfirm

import com.nunchuk.android.core.signer.SignerModel

sealed class LockdownConfirmEvent {
    data class Loading(val isLoading: Boolean) : LockdownConfirmEvent()
    object ContinueClick : LockdownConfirmEvent()
}

data class LockdownConfirmState(
    val signers: List<SignerModel> = emptyList()
)