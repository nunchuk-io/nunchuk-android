package com.nunchuk.android.model.byzantine

import androidx.annotation.Keep

@Keep
enum class GroupWalletType(val m: Int, val n: Int, val isPro: Boolean) {
    TWO_OF_FOUR_MULTISIG(2, 4, true), TWO_OF_THREE(2, 3, false), THREE_OF_FIVE(3, 5, false)
}