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

package com.nunchuk.android.nav.args

import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable

data class AddAirSignerArgs(
    val isMembershipFlow: Boolean = false,
    val tag: SignerTag? = null,
    val groupId: String = "",
    val xfp: String? = null,
    val newIndex: Int = 0,
    val replacedXfp: String? = null,
    val walletId: String = "",
    val step: MembershipStep? = null,
    val onChainAddSignerParam: OnChainAddSignerParam? = null,
) {

    fun buildBundle() = Bundle().apply {
        putBoolean(IS_MEMBERSHIP_FLOW, isMembershipFlow)
        putSerializable(SIGNER_TAG, tag)
        putString(GROUP_ID, groupId)
        putString(XFP, xfp)
        putInt(NEW_INDEX, newIndex)
        putString(REPLACED_XFP, replacedXfp)
        putString(WALLET_ID, walletId)
        putSerializable(MEMBERSHIP_STEP, step)
        putParcelable(ONCHAIN_ADD_SIGNER_PARAM, onChainAddSignerParam)
    }

    companion object {
        private const val IS_MEMBERSHIP_FLOW = "is_membership_flow"
        private const val SIGNER_TAG = "signer_tag"
        private const val GROUP_ID = "group_id"
        private const val XFP = "xfp"
        private const val NEW_INDEX = "new_index"
        private const val REPLACED_XFP = "replaced_xfp"
        private const val WALLET_ID = "wallet_id"
        private const val MEMBERSHIP_STEP = "step"
        private const val ONCHAIN_ADD_SIGNER_PARAM = "onchain_add_signer_param"

        fun deserializeFrom(intent: Intent): AddAirSignerArgs = AddAirSignerArgs(
            isMembershipFlow = intent.extras?.getBoolean(IS_MEMBERSHIP_FLOW, false) == true,
            tag = intent.serializable<SignerTag>(SIGNER_TAG),
            groupId = intent.extras?.getString(GROUP_ID, "").orEmpty(),
            xfp = intent.extras?.getString(XFP),
            newIndex = intent.extras?.getInt(NEW_INDEX, 0) ?: 0,
            replacedXfp = intent.extras?.getString(REPLACED_XFP),
            walletId = intent.extras?.getString(WALLET_ID, "").orEmpty(),
            step = intent.serializable<MembershipStep>(MEMBERSHIP_STEP),
            onChainAddSignerParam = intent.parcelable<OnChainAddSignerParam>(ONCHAIN_ADD_SIGNER_PARAM)
        )
    }
}
