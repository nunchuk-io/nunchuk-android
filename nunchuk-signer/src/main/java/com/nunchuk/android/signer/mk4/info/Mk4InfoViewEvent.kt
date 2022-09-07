package com.nunchuk.android.signer.mk4.info

import com.nunchuk.android.model.SingleSigner

sealed class Mk4InfoViewEvent {
    data class Loading(val isLoading: Boolean) : Mk4InfoViewEvent()
    data class LoadMk4SignersSuccess(val signers: List<SingleSigner>) : Mk4InfoViewEvent()
    data class ShowError(val message: String) : Mk4InfoViewEvent()
}