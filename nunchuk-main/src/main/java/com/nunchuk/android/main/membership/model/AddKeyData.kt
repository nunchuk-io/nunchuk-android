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
import com.nunchuk.android.core.util.TAPSIGNER_INHERITANCE_NAME
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

val MembershipStep.resId: Int
    get() {
        return when (this) {
            MembershipStep.ADD_SEVER_KEY -> R.drawable.ic_server_key_dark
            MembershipStep.HONEY_ADD_TAP_SIGNER -> R.drawable.ic_nfc_card
            MembershipStep.IRON_ADD_HARDWARE_KEY_1,
            MembershipStep.IRON_ADD_HARDWARE_KEY_2,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_2 -> R.drawable.ic_hardware_key
            MembershipStep.SETUP_KEY_RECOVERY,
            MembershipStep.SETUP_INHERITANCE,
            MembershipStep.CREATE_WALLET -> throw IllegalArgumentException("Not support")
        }
    }

fun MembershipStep.getLabel(context: Context): String {
    return when (this) {
        MembershipStep.IRON_ADD_HARDWARE_KEY_1 -> "Hardware key"
        MembershipStep.IRON_ADD_HARDWARE_KEY_2 -> "Hardware key #2"
        MembershipStep.ADD_SEVER_KEY -> context.getString(R.string.nc_server_key)
        MembershipStep.HONEY_ADD_TAP_SIGNER -> TAPSIGNER_INHERITANCE_NAME
        MembershipStep.HONEY_ADD_HARDWARE_KEY_1 -> "Hardware key #2"
        MembershipStep.HONEY_ADD_HARDWARE_KEY_2 -> "Hardware key #3"
        MembershipStep.SETUP_KEY_RECOVERY,
        MembershipStep.SETUP_INHERITANCE,
        MembershipStep.CREATE_WALLET -> throw IllegalArgumentException("Not support")
    }
}

fun MembershipStep.getButtonText(context: Context): String {
    return when (this) {
        MembershipStep.ADD_SEVER_KEY -> context.getString(R.string.nc_configure)
        else -> context.getString(R.string.nc_add)
    }
}