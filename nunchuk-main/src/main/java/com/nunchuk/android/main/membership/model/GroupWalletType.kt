package com.nunchuk.android.main.membership.model

import androidx.annotation.StringRes
import com.nunchuk.android.main.R
import com.nunchuk.android.model.ByzantineWalletConfig
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.byzantine.GroupWalletType


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

fun String.toGroupWalletType(): GroupWalletType? {
    return GroupWalletType.values().find { this == it.name }
}

fun ByzantineWalletConfig.toGroupWalletType(): GroupWalletType? {
    return GroupWalletType.values().find { this.m == it.m && this.n == it.n }
}

fun GroupWalletType.toSteps() : List<MembershipStep> = when(this) {
    GroupWalletType.TWO_OF_FOUR_MULTISIG -> listOf(
        MembershipStep.BYZANTINE_ADD_TAP_SIGNER,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
        MembershipStep.ADD_SEVER_KEY,
    )
    GroupWalletType.TWO_OF_THREE -> listOf(
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
    )
    GroupWalletType.THREE_OF_FIVE -> listOf(
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4,
    )
}