package com.nunchuk.android.signer.components.details.message

import com.nunchuk.android.model.SignedMessage

data class SignMessageUiState(
    val defaultPath: String = "",
    val signedMessage: SignedMessage? = null,
)