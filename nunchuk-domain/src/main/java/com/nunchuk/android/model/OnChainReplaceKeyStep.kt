/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2025 Nunchuk                                              *
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

/**
 * Steps for Replace Key flow on On-Chain Timelock wallets.
 * Order matters for indexing utilities below.
 */
enum class OnChainReplaceKeyStep {
    INHERITANCE_KEY,
    INHERITANCE_KEY_TIMELOCK,
    INHERITANCE_KEY_1,
    INHERITANCE_KEY_1_TIMELOCK,
    HARDWARE_KEY,
    HARDWARE_KEY_TIMELOCK,
    HARDWARE_KEY_1,
    HARDWARE_KEY_1_TIMELOCK,
    SERVER_KEY,
    TIMELOCK
}

/**
 * Mirrors helpers in MembershipStep.
 */
val OnChainReplaceKeyStep.isAddInheritanceKey: Boolean
    get() = when (this) {
        OnChainReplaceKeyStep.INHERITANCE_KEY,
        OnChainReplaceKeyStep.INHERITANCE_KEY_1,
        OnChainReplaceKeyStep.INHERITANCE_KEY_TIMELOCK,
        OnChainReplaceKeyStep.INHERITANCE_KEY_1_TIMELOCK -> true

        else -> false
    }

val OnChainReplaceKeyStep.isTimelockStep: Boolean
    get() = when (this) {
        OnChainReplaceKeyStep.INHERITANCE_KEY_TIMELOCK,
        OnChainReplaceKeyStep.INHERITANCE_KEY_1_TIMELOCK,
        OnChainReplaceKeyStep.HARDWARE_KEY_TIMELOCK,
        OnChainReplaceKeyStep.HARDWARE_KEY_1_TIMELOCK -> true

        else -> false
    }

/**
 * Index of the step based on the enum declaration order.
 * This mirrors MembershipStep.toIndex but without WalletType branching for replace flow.
 */
fun OnChainReplaceKeyStep.toIndex(isGroupWallet: Boolean): Int {
    if (isGroupWallet) {
        return when (this) {
            OnChainReplaceKeyStep.INHERITANCE_KEY_TIMELOCK -> 0
            OnChainReplaceKeyStep.INHERITANCE_KEY -> 1
            OnChainReplaceKeyStep.INHERITANCE_KEY_1_TIMELOCK -> 2
            OnChainReplaceKeyStep.INHERITANCE_KEY_1 -> 3
            OnChainReplaceKeyStep.HARDWARE_KEY_TIMELOCK -> 4
            OnChainReplaceKeyStep.HARDWARE_KEY -> 5
            OnChainReplaceKeyStep.HARDWARE_KEY_1_TIMELOCK -> 6
            OnChainReplaceKeyStep.HARDWARE_KEY_1 -> 7
            else -> throw IllegalArgumentException()
        }
    }

    return when (this) {
        OnChainReplaceKeyStep.INHERITANCE_KEY_TIMELOCK -> 0
        OnChainReplaceKeyStep.INHERITANCE_KEY -> 1
        OnChainReplaceKeyStep.HARDWARE_KEY_TIMELOCK -> 2
        OnChainReplaceKeyStep.HARDWARE_KEY -> 3
        OnChainReplaceKeyStep.HARDWARE_KEY_1_TIMELOCK -> 4
        OnChainReplaceKeyStep.HARDWARE_KEY_1 -> 5
        else -> throw IllegalArgumentException()
    }
}

/**
 * Returns the timelock counterpart if present.
 */
fun OnChainReplaceKeyStep.getTimelockStep(): OnChainReplaceKeyStep? = when (this) {
    OnChainReplaceKeyStep.INHERITANCE_KEY -> OnChainReplaceKeyStep.INHERITANCE_KEY_TIMELOCK
    OnChainReplaceKeyStep.INHERITANCE_KEY_1 -> OnChainReplaceKeyStep.INHERITANCE_KEY_1_TIMELOCK
    OnChainReplaceKeyStep.HARDWARE_KEY -> OnChainReplaceKeyStep.HARDWARE_KEY_TIMELOCK
    OnChainReplaceKeyStep.HARDWARE_KEY_1 -> OnChainReplaceKeyStep.HARDWARE_KEY_1_TIMELOCK
    else -> null
}

/**
 * Convert an [Int] index to an [OnChainReplaceKeyStep].
 * Returns null if the integer does not map to a valid step.
 */
fun Int.toOnChainReplaceKeyStep(isGroupWallet: Boolean): OnChainReplaceKeyStep {
    if (isGroupWallet) {
        return when (this) {
            0 -> OnChainReplaceKeyStep.INHERITANCE_KEY_TIMELOCK
            1 -> OnChainReplaceKeyStep.INHERITANCE_KEY
            2 -> OnChainReplaceKeyStep.INHERITANCE_KEY_1_TIMELOCK
            3 -> OnChainReplaceKeyStep.INHERITANCE_KEY_1
            4 -> OnChainReplaceKeyStep.HARDWARE_KEY_TIMELOCK
            5 -> OnChainReplaceKeyStep.HARDWARE_KEY
            6 -> OnChainReplaceKeyStep.HARDWARE_KEY_1_TIMELOCK
            7 -> OnChainReplaceKeyStep.HARDWARE_KEY_1
            else -> OnChainReplaceKeyStep.SERVER_KEY
        }

    } else {
        return when (this) {
            0 -> OnChainReplaceKeyStep.INHERITANCE_KEY_TIMELOCK
            1 -> OnChainReplaceKeyStep.INHERITANCE_KEY
            2 -> OnChainReplaceKeyStep.HARDWARE_KEY_TIMELOCK
            3 -> OnChainReplaceKeyStep.HARDWARE_KEY
            4 -> OnChainReplaceKeyStep.HARDWARE_KEY_1_TIMELOCK
            5 -> OnChainReplaceKeyStep.HARDWARE_KEY_1
            else -> OnChainReplaceKeyStep.SERVER_KEY
        }
    }

}

/**
 * Returns the list of replace key steps based on whether it's a group wallet or not.
 */
fun getReplaceKeySteps(isGroupWallet: Boolean): List<OnChainReplaceKeyStep> {
    return if (isGroupWallet) {
        listOf(
            OnChainReplaceKeyStep.INHERITANCE_KEY,
            OnChainReplaceKeyStep.INHERITANCE_KEY_TIMELOCK,
            OnChainReplaceKeyStep.INHERITANCE_KEY_1,
            OnChainReplaceKeyStep.INHERITANCE_KEY_1_TIMELOCK,
            OnChainReplaceKeyStep.HARDWARE_KEY,
            OnChainReplaceKeyStep.HARDWARE_KEY_TIMELOCK,
            OnChainReplaceKeyStep.HARDWARE_KEY_1,
            OnChainReplaceKeyStep.HARDWARE_KEY_1_TIMELOCK,
            OnChainReplaceKeyStep.SERVER_KEY,
            OnChainReplaceKeyStep.TIMELOCK,
        )
    } else {
        listOf(
            OnChainReplaceKeyStep.INHERITANCE_KEY,
            OnChainReplaceKeyStep.INHERITANCE_KEY_TIMELOCK,
            OnChainReplaceKeyStep.HARDWARE_KEY,
            OnChainReplaceKeyStep.HARDWARE_KEY_TIMELOCK,
            OnChainReplaceKeyStep.HARDWARE_KEY_1,
            OnChainReplaceKeyStep.HARDWARE_KEY_1_TIMELOCK,
            OnChainReplaceKeyStep.SERVER_KEY,
            OnChainReplaceKeyStep.TIMELOCK,
        )
    }
}




