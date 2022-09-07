package com.nunchuk.android.signer.mk4.name

import com.nunchuk.android.model.SingleSigner

sealed class AddNameMk4ViewEvent {
    data class Loading(val isLoading: Boolean) : AddNameMk4ViewEvent()
    data class CreateMk4SignerSuccess(val signer: SingleSigner) : AddNameMk4ViewEvent()
    data class ShowError(val message: String) : AddNameMk4ViewEvent()
}