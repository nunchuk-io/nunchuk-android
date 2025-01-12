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

fun getWalletConfigTypeBy(n: Int, m: Int): WalletConfigType {
    if (n == 2 && m == 3) {
        return WalletConfigType.TOW_OF_THREE
    } else if (n == 3 && m == 5) {
        return WalletConfigType.THREE_OF_FIVE
    }
    return WalletConfigType.CUSTOM
}