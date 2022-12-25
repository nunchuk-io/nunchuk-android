package com.nunchuk.android.main.components.tabs.services.keyrecovery.intro

import com.nunchuk.android.core.signer.SignerModel

sealed class KeyRecoveryIntroEvent {
    data class Loading(val isLoading: Boolean) : KeyRecoveryIntroEvent()
    data class GetTapSignerSuccess(val signers: List<SignerModel>) : KeyRecoveryIntroEvent()
}

data class KeyRecoveryIntroState(val tapSigners: List<SignerModel> = emptyList())