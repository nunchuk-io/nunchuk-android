package com.nunchuk.android.main.membership.model

import androidx.annotation.StringRes
import com.nunchuk.android.main.R
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.WalletConfig
import com.nunchuk.android.model.byzantine.GroupWalletType


@get:StringRes
val GroupWalletType.title: Int
    get() {
        return when (this) {
            GroupWalletType.TWO_OF_FOUR_MULTISIG -> R.string.nc_2_of_4_multisig_title
            GroupWalletType.TWO_OF_THREE -> R.string.nc_2_of_3_multisig_title
            GroupWalletType.THREE_OF_FIVE -> R.string.nc_3_of_5_multisig_title
            GroupWalletType.TWO_OF_FOUR_MULTISIG_NO_INHERITANCE -> R.string.nc_2_of_4_multisig_without_inheritance_planning
            GroupWalletType.THREE_OF_FIVE_INHERITANCE -> R.string.nc_3_of_5_multisig_with_inheritance_planning
            GroupWalletType.THREE_OF_FIVE_PLATFORM_KEY -> R.string.nc_3_of_5_multisig_without_inheritance_planning
            GroupWalletType.TWO_OF_THREE_PLATFORM_KEY -> R.string.nc_2_of_3_multisig_without_inheritance_planning
        }
    }

fun GroupWalletType.shortName(plan: MembershipPlan): Int =
    when {
        this == GroupWalletType.TWO_OF_FOUR_MULTISIG -> R.string.nc_pro
        this == GroupWalletType.THREE_OF_FIVE_INHERITANCE && plan == MembershipPlan.BYZANTINE_PRO -> R.string.nc_pro
        this == GroupWalletType.THREE_OF_FIVE_PLATFORM_KEY && plan == MembershipPlan.BYZANTINE_PRO -> R.string.nc_pro

        this == GroupWalletType.TWO_OF_FOUR_MULTISIG_NO_INHERITANCE -> R.string.nc_premier
        this == GroupWalletType.THREE_OF_FIVE_INHERITANCE && plan == MembershipPlan.BYZANTINE_PREMIER -> R.string.nc_premier
        this == GroupWalletType.THREE_OF_FIVE_PLATFORM_KEY && plan == MembershipPlan.BYZANTINE_PREMIER -> R.string.nc_premier
        this == GroupWalletType.THREE_OF_FIVE && plan == MembershipPlan.FINNEY -> R.string.nc_finney_cap
        this == GroupWalletType.THREE_OF_FIVE && plan == MembershipPlan.FINNEY_PRO -> R.string.nc_finney_pro_cap
        else -> R.string.nc_standard
    }

@get:StringRes
val GroupWalletType.desc: Int
    get() {
        return when (this) {
            GroupWalletType.TWO_OF_FOUR_MULTISIG -> R.string.nc_2_of_4_multisig_desc
            GroupWalletType.TWO_OF_THREE -> R.string.nc_2_of_3_multisig_desc
            GroupWalletType.THREE_OF_FIVE -> R.string.nc_3_of_5_multisig_desc
            GroupWalletType.TWO_OF_FOUR_MULTISIG_NO_INHERITANCE -> R.string.nc_2_of_4_no_inheritance_desc
            GroupWalletType.THREE_OF_FIVE_INHERITANCE -> R.string.nc_3_of_5_multisig_with_inheritance_planning_desc
            GroupWalletType.THREE_OF_FIVE_PLATFORM_KEY -> R.string.nc_3_of_5_multisig_without_inheritance_planning_desc
            GroupWalletType.TWO_OF_THREE_PLATFORM_KEY -> R.string.nc_3_of_5_multisig_without_inheritance_planning_desc
        }
    }

fun String.toGroupWalletType(): GroupWalletType? {
    return GroupWalletType.entries.find { this == it.name }
}

fun WalletConfig.toGroupWalletType(): GroupWalletType? {
    return GroupWalletType.entries
        .find { this.m == it.m && this.n == it.n && this.allowInheritance == it.allowInheritance && this.requiredServerKey == it.requiredServerKey }
}

fun GroupWalletType.toSteps(isPersonalWallet: Boolean = false, isOnChain: Boolean = false): List<MembershipStep> = when (this) {
    GroupWalletType.TWO_OF_FOUR_MULTISIG -> if (isPersonalWallet) {
        listOf(
            MembershipStep.HONEY_ADD_INHERITANCE_KEY,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_2,
            MembershipStep.ADD_SEVER_KEY,
        ).toMutableList().apply {
            if (isOnChain) {
                add(MembershipStep.TIMELOCK)
            }
        }
    } else {
        listOf(
            MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
            MembershipStep.ADD_SEVER_KEY,
        )
    }

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

    GroupWalletType.TWO_OF_FOUR_MULTISIG_NO_INHERITANCE -> listOf(
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
        MembershipStep.ADD_SEVER_KEY,
    )

    GroupWalletType.THREE_OF_FIVE_INHERITANCE -> listOf(
        MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY,
        MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3,
        MembershipStep.ADD_SEVER_KEY,
    )

    GroupWalletType.THREE_OF_FIVE_PLATFORM_KEY -> listOf(
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3,
        MembershipStep.ADD_SEVER_KEY,
    )

    GroupWalletType.TWO_OF_THREE_PLATFORM_KEY -> listOf(
        MembershipStep.IRON_ADD_HARDWARE_KEY_1,
        MembershipStep.IRON_ADD_HARDWARE_KEY_2,
        MembershipStep.ADD_SEVER_KEY,
    )
}