package com.nunchuk.android.wallet.personal.components.add

import androidx.annotation.Keep

@Keep
enum class WalletConfigType {
    TOW_OF_THREE,
    THREE_OF_FIVE,
    CUSTOM
}

fun WalletConfigType.toOptionName(): String {
    return when (this) {
        WalletConfigType.TOW_OF_THREE -> "2/3 multisig"
        WalletConfigType.THREE_OF_FIVE -> "3/5 multisig"
        WalletConfigType.CUSTOM -> "Customize"
    }
}