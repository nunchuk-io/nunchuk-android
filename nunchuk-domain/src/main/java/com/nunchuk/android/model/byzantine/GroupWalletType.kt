package com.nunchuk.android.model.byzantine

import androidx.annotation.Keep

@Keep
enum class GroupWalletType(val m: Int, val n: Int, val allowInheritance: Boolean) {
    TWO_OF_FOUR_MULTISIG(2, 4, true), TWO_OF_THREE(2, 3, false), THREE_OF_FIVE(3, 5, false), TWO_OF_FOUR_MULTISIG_NO_INHERITANCE(2, 4, false)
}

fun String.toGroupWalletType(): GroupWalletType? {
    return when(this) {
        "W2OF4_INHERITANCE" -> GroupWalletType.TWO_OF_FOUR_MULTISIG
        "W2OF3" -> GroupWalletType.TWO_OF_THREE
        "W3OF5" -> GroupWalletType.THREE_OF_FIVE
        "W2OF4_PLATFORM_KEY" -> GroupWalletType.TWO_OF_FOUR_MULTISIG_NO_INHERITANCE
        else -> null
    }
}