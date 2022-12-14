/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

import androidx.annotation.Keep

@Keep
enum class MembershipPlan {
    NONE, IRON_HAND, HONEY_BADGER
}

fun String?.toMembershipPlan() = when (this) {
    IRON_HAND_PLAN, IRON_HAND_PLAN_TESTNET -> {
        MembershipPlan.IRON_HAND
    }
    HONEY_BADGER_PLAN, HONEY_BADGER_PLAN_TESTNET -> {
        MembershipPlan.HONEY_BADGER
    }
    else -> {
        MembershipPlan.NONE
    }
}

private const val IRON_HAND_PLAN = "iron_hand"
private const val IRON_HAND_PLAN_TESTNET = "iron_hand_testnet"
private const val HONEY_BADGER_PLAN_TESTNET = "honey_badger_testnet"
private const val HONEY_BADGER_PLAN = "honey_badger"
