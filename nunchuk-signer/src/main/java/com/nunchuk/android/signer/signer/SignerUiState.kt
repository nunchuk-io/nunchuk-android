package com.nunchuk.android.signer.signer

import com.nunchuk.android.core.signer.SignerModel

data class SignerUiState(
    val signers: List<SignerModel>? = null, // init with null to trick as a loading state
)