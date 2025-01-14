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

fun WalletConfigType.getMN(): Pair<Int, Int> {
    return when (this) {
        WalletConfigType.TOW_OF_THREE -> Pair(2, 3)
        WalletConfigType.THREE_OF_FIVE -> Pair(3, 5)
        WalletConfigType.CUSTOM -> Pair(0, 0)
    }
}

fun getWalletConfigTypeBy(n: Int, m: Int): WalletConfigType {
    if (n == 3 && m == 2) {
        return WalletConfigType.TOW_OF_THREE
    } else if (n == 5 && m == 3) {
        return WalletConfigType.THREE_OF_FIVE
    }
    return WalletConfigType.CUSTOM
}