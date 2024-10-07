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

// Don't change the order of the step, we store ordinal to database
enum class MembershipStep {
    IRON_ADD_HARDWARE_KEY_1,
    IRON_ADD_HARDWARE_KEY_2,
    ADD_SEVER_KEY,
    SETUP_KEY_RECOVERY,
    CREATE_WALLET,
    PRIMARY_OWNER,
    HONEY_ADD_TAP_SIGNER,
    HONEY_ADD_HARDWARE_KEY_1,
    HONEY_ADD_HARDWARE_KEY_2,
    SETUP_INHERITANCE,
    BYZANTINE_ADD_TAP_SIGNER,
    BYZANTINE_ADD_HARDWARE_KEY_0,
    BYZANTINE_ADD_HARDWARE_KEY_1,
    BYZANTINE_ADD_HARDWARE_KEY_2,
    BYZANTINE_ADD_HARDWARE_KEY_3,
    BYZANTINE_ADD_HARDWARE_KEY_4,
    BYZANTINE_INVITE_MEMBER,
    BYZANTINE_ADD_TAP_SIGNER_1,
}

val MembershipStep.isAddInheritanceKey: Boolean
    get() = this == MembershipStep.BYZANTINE_ADD_TAP_SIGNER || this == MembershipStep.BYZANTINE_ADD_TAP_SIGNER_1 || this == MembershipStep.HONEY_ADD_TAP_SIGNER

fun MembershipStep.toIndex() = when (this) {
    MembershipStep.BYZANTINE_ADD_TAP_SIGNER,
    MembershipStep.HONEY_ADD_TAP_SIGNER,
    MembershipStep.IRON_ADD_HARDWARE_KEY_1, MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0 -> 0
    MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1, MembershipStep.IRON_ADD_HARDWARE_KEY_2, MembershipStep.HONEY_ADD_HARDWARE_KEY_1, MembershipStep.BYZANTINE_ADD_TAP_SIGNER_1 -> 1
    MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2, MembershipStep.HONEY_ADD_HARDWARE_KEY_2 -> 2
    MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3 -> 3
    MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4 -> 4
    else -> throw IllegalArgumentException()
}