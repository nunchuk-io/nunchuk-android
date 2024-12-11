package com.nunchuk.android.model.signer

import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType

data class SupportedSigner(
    val type: SignerType,
    val tag: SignerTag?
)