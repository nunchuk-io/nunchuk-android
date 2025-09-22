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

package com.nunchuk.android.main.membership.model

import android.content.Context
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.DEFAULT_KEY_NAME
import com.nunchuk.android.core.util.HARDWARE_KEY_NAME
import com.nunchuk.android.main.R
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.VerifyType

data class AddKeyData(
    val type: MembershipStep,
    val signer: SignerModel? = null,
    val verifyType: VerifyType = VerifyType.NONE
) {
    val isVerifyOrAddKey: Boolean
        get() = signer != null || verifyType != VerifyType.NONE
}

data class AddKeyOnChainData(
    val type: MembershipStep,
    val signers: List<SignerModel>? = null,
    val verifyType: VerifyType = VerifyType.NONE
) {
    val isVerifyOrAddKey: Boolean
        get() = signers != null || verifyType != VerifyType.NONE
}

val MembershipStep.resId: Int
    get() {
        return when (this) {
            MembershipStep.ADD_SEVER_KEY -> R.drawable.ic_server_key_dark
            MembershipStep.TIMELOCK -> R.drawable.ic_timer
            MembershipStep.HONEY_ADD_INHERITANCE_KEY,
            MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY,
            MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4,
            MembershipStep.IRON_ADD_HARDWARE_KEY_1,
            MembershipStep.IRON_ADD_HARDWARE_KEY_2,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_2 -> R.drawable.ic_hardware_key

            else -> 0
        }
    }

fun MembershipStep.getLabel(context: Context, isStandard: Boolean): String {
    val defaultKeyName = if (isStandard) {
        DEFAULT_KEY_NAME
    } else {
        HARDWARE_KEY_NAME
    }
    return when (this) {
        MembershipStep.IRON_ADD_HARDWARE_KEY_1 -> "$defaultKeyName #1"
        MembershipStep.IRON_ADD_HARDWARE_KEY_2 -> "$defaultKeyName #2"
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0 -> "$defaultKeyName #1"
        MembershipStep.ADD_SEVER_KEY -> context.getString(R.string.nc_server_key)
        MembershipStep.TIMELOCK -> context.getString(R.string.nc_timelock)
        MembershipStep.HONEY_ADD_INHERITANCE_KEY, MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY -> "$defaultKeyName #1"
        MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1 -> "$defaultKeyName #2"
        MembershipStep.HONEY_ADD_HARDWARE_KEY_1, MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1 -> "$defaultKeyName #2"
        MembershipStep.HONEY_ADD_HARDWARE_KEY_2, MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2 -> "$defaultKeyName #3"
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3 -> "$defaultKeyName #4"
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4 -> "$defaultKeyName #5"
        else -> ""
    }
}

fun MembershipStep.getButtonText(context: Context): String {
    return when (this) {
        MembershipStep.ADD_SEVER_KEY -> context.getString(R.string.nc_configure)
        else -> context.getString(R.string.nc_add)
    }
}