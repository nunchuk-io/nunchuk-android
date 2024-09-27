package com.nunchuk.android.signer.signer

import com.nunchuk.android.core.signer.SignerModel

data class SignerUiState(
    val signers: List<SignerModel> = emptyList(),
)