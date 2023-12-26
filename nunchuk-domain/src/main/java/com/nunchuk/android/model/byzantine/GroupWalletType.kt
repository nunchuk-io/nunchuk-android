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
    THREE_OF_FIVE_INHERITANCE(3, 5, true, true),
    THREE_OF_FIVE_PLATFORM_KEY(3, 5, false, true),
    TWO_OF_FOUR_MULTISIG_NO_INHERITANCE(2, 4, false, true)
}

fun String.toGroupWalletType(): GroupWalletType? {
    return when (this) {
        "W2OF4_INHERITANCE", GroupWalletType.TWO_OF_FOUR_MULTISIG.name -> GroupWalletType.TWO_OF_FOUR_MULTISIG
        "W2OF3", GroupWalletType.TWO_OF_THREE.name -> GroupWalletType.TWO_OF_THREE
        "W3OF5", GroupWalletType.THREE_OF_FIVE.name -> GroupWalletType.THREE_OF_FIVE
        "W2OF4_PLATFORM_KEY", GroupWalletType.TWO_OF_FOUR_MULTISIG_NO_INHERITANCE.name -> GroupWalletType.TWO_OF_FOUR_MULTISIG_NO_INHERITANCE
        "W3OF5_INHERITANCE", GroupWalletType.THREE_OF_FIVE_INHERITANCE.name -> GroupWalletType.THREE_OF_FIVE_INHERITANCE
        "W3OF5_PLATFORM_KEY", GroupWalletType.THREE_OF_FIVE_PLATFORM_KEY.name -> GroupWalletType.THREE_OF_FIVE_PLATFORM_KEY
        else -> null
    }
}

fun GroupWalletType.isAllowInheritance(): Boolean {
    return GroupWalletType.entries.find { it == this }?.allowInheritance ?: false
}
