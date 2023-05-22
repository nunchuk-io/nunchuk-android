package com.nunchuk.android.main.membership.model

import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.nunchuk.android.main.R

@Keep
enum class GroupWalletType {
    TWO_OF_THREE, TWO_OF_FOUR_MULTISIG, THREE_OF_FIVE
}

@get:StringRes
val GroupWalletType.title: Int
    get() {
        return when (this) {
            GroupWalletType.TWO_OF_FOUR_MULTISIG -> R.string.nc_2_of_4_multisig_title
            GroupWalletType.TWO_OF_THREE -> R.string.nc_2_of_3_multisig_title
            GroupWalletType.THREE_OF_FIVE -> R.string.nc_3_of_5_multisig_title
        }
    }

@get:StringRes
val GroupWalletType.desc: Int
    get() {
        return when (this) {
            GroupWalletType.TWO_OF_FOUR_MULTISIG -> R.string.nc_2_of_4_multisig_desc
            GroupWalletType.TWO_OF_THREE -> R.string.nc_2_of_3_multisig_desc
            GroupWalletType.THREE_OF_FIVE -> R.string.nc_3_of_5_multisig_desc
        }
    }