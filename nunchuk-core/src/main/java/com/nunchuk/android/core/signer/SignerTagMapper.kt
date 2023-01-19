package com.nunchuk.android.core.signer

import com.nunchuk.android.type.SignerTag

fun String?.toSignerTag() : SignerTag? {
   return SignerTag.values().find { it.name == this }
}