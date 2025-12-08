/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.model

import com.nunchuk.android.type.WalletType

// Don't change the order of the step, we store ordinal to database
enum class MembershipStep {
    IRON_ADD_HARDWARE_KEY_1,
    IRON_ADD_HARDWARE_KEY_2,
    ADD_SEVER_KEY,
    SETUP_KEY_RECOVERY,
    CREATE_WALLET,
    PRIMARY_OWNER,
    HONEY_ADD_INHERITANCE_KEY,
    HONEY_ADD_HARDWARE_KEY_1,
    HONEY_ADD_HARDWARE_KEY_2,
    SETUP_INHERITANCE,
    BYZANTINE_ADD_INHERITANCE_KEY,
    BYZANTINE_ADD_HARDWARE_KEY_0,
    BYZANTINE_ADD_HARDWARE_KEY_1,
    BYZANTINE_ADD_HARDWARE_KEY_2,
    BYZANTINE_ADD_HARDWARE_KEY_3,
    BYZANTINE_ADD_HARDWARE_KEY_4,
    BYZANTINE_INVITE_MEMBER,
    BYZANTINE_ADD_INHERITANCE_KEY_1,
    HONEY_ADD_INHERITANCE_KEY_TIMELOCK,
    HONEY_ADD_HARDWARE_KEY_1_TIMELOCK,
    HONEY_ADD_HARDWARE_KEY_2_TIMELOCK,
    BYZANTINE_ADD_INHERITANCE_KEY_TIMELOCK,
    BYZANTINE_ADD_HARDWARE_KEY_0_TIMELOCK,
    BYZANTINE_ADD_HARDWARE_KEY_1_TIMELOCK,
    BYZANTINE_ADD_HARDWARE_KEY_2_TIMELOCK,
    BYZANTINE_ADD_HARDWARE_KEY_3_TIMELOCK,
    BYZANTINE_ADD_HARDWARE_KEY_4_TIMELOCK,
    BYZANTINE_ADD_INHERITANCE_KEY_1_TIMELOCK,
    TIMELOCK,
    SETUP_STARTED, // Placeholder step to indicate draft wallet exists even if no signers yet
}

val MembershipStep.isAddInheritanceKey: Boolean
    get() = this == MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY || this == MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1 || this == MembershipStep.HONEY_ADD_INHERITANCE_KEY
            || this == MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_TIMELOCK || this == MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1_TIMELOCK || this == MembershipStep.HONEY_ADD_INHERITANCE_KEY_TIMELOCK

val MembershipStep.isTimelockStep: Boolean
    get() = this == MembershipStep.TIMELOCK
            || this == MembershipStep.HONEY_ADD_INHERITANCE_KEY_TIMELOCK
            || this == MembershipStep.HONEY_ADD_HARDWARE_KEY_1_TIMELOCK
            || this == MembershipStep.HONEY_ADD_HARDWARE_KEY_2_TIMELOCK
            || this == MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_TIMELOCK
            || this == MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1_TIMELOCK
            || this == MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0_TIMELOCK
            || this == MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1_TIMELOCK
            || this == MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2_TIMELOCK
            || this == MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3_TIMELOCK
            || this == MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4_TIMELOCK

/**
 * @param requireInheritance is only applicable for walletType == WalletType.MINISCRIPT
 */
fun MembershipStep.toIndex(walletType: WalletType, requireInheritance: Boolean = false) = if (walletType == WalletType.MULTI_SIG) {
    when (this) {
        MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY,
        MembershipStep.HONEY_ADD_INHERITANCE_KEY,
        MembershipStep.IRON_ADD_HARDWARE_KEY_1,
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0 -> 0
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
        MembershipStep.IRON_ADD_HARDWARE_KEY_2,
        MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
        MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1 -> 1
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
        MembershipStep.HONEY_ADD_HARDWARE_KEY_2 -> 2
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3 -> 3
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4 -> 4
        else -> throw IllegalArgumentException()
    }
} else {
    if (requireInheritance) {
        when (this) {
            MembershipStep.HONEY_ADD_INHERITANCE_KEY_TIMELOCK, MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_TIMELOCK -> 0
            MembershipStep.HONEY_ADD_INHERITANCE_KEY, MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY -> 1
            MembershipStep.HONEY_ADD_HARDWARE_KEY_1_TIMELOCK, MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1_TIMELOCK -> 2
            MembershipStep.HONEY_ADD_HARDWARE_KEY_1, MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1 -> 3
            MembershipStep.HONEY_ADD_HARDWARE_KEY_2_TIMELOCK, MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0_TIMELOCK -> 4
            MembershipStep.HONEY_ADD_HARDWARE_KEY_2, MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0 -> 5
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1_TIMELOCK -> 6
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1 -> 7
            else -> throw IllegalArgumentException()
        }
    } else {
        when (this) {
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0_TIMELOCK -> 0
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0 -> 1
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1_TIMELOCK -> 2
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1 -> 3
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2_TIMELOCK -> 4
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2 -> 5
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3_TIMELOCK -> 6
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3 -> 7
            else -> throw IllegalArgumentException()
        }
    }

}

fun MembershipStep.toPairIndex(walletType: WalletType, requireInheritance: Boolean): List<Int> {
    val index = this.toIndex(walletType, requireInheritance)
    val pairStart = (index / 2) * 2
    return listOf(pairStart, pairStart + 1)
}