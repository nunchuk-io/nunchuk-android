package com.nunchuk.android.model.byzantine

import androidx.annotation.Keep

@Keep
enum class GroupWalletType(
    val m: Int,
    val n: Int,
    val allowInheritance: Boolean,
    val requiredServerKey: Boolean,
) {
    TWO_OF_FOUR_MULTISIG(2, 4, true, true),
    TWO_OF_THREE(2, 3, false, false),
    THREE_OF_FIVE(3, 5, false, false),
    TWO_OF_FOUR_MULTISIG_NO_INHERITANCE(2, 4, false, true)
}

fun GroupWalletType.isPremier(): Boolean {
    return this == GroupWalletType.TWO_OF_FOUR_MULTISIG_NO_INHERITANCE
}

fun String.toGroupWalletType(): GroupWalletType? {
    return when (this) {
        "W2OF4_INHERITANCE", GroupWalletType.TWO_OF_FOUR_MULTISIG.name -> GroupWalletType.TWO_OF_FOUR_MULTISIG
        "W2OF3", GroupWalletType.TWO_OF_THREE.name -> GroupWalletType.TWO_OF_THREE
        "W3OF5", GroupWalletType.THREE_OF_FIVE.name -> GroupWalletType.THREE_OF_FIVE
        "W2OF4_PLATFORM_KEY", GroupWalletType.TWO_OF_FOUR_MULTISIG_NO_INHERITANCE.name -> GroupWalletType.TWO_OF_FOUR_MULTISIG_NO_INHERITANCE
        else -> null
    }
}