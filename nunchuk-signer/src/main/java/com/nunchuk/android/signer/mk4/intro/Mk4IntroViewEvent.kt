package com.nunchuk.android.signer.mk4.intro

import com.nunchuk.android.model.SingleSigner

sealed class Mk4IntroViewEvent {
    data class Loading(val isLoading: Boolean) : Mk4IntroViewEvent()
    data class LoadMk4SignersSuccess(val signers: List<SingleSigner>) : Mk4IntroViewEvent()
    data class ShowError(val message: String) : Mk4IntroViewEvent()
    object OnContinueClicked : Mk4IntroViewEvent()
}