package com.nunchuk.android.utils

import com.nunchuk.android.type.SignerType

val SignerType.isServerMasterSigner: Boolean
    get() = this == SignerType.NFC
