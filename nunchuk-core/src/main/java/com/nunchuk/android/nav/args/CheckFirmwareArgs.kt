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
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable

data class CheckFirmwareArgs(
    val signerTag: SignerTag,
    val onChainAddSignerParam: OnChainAddSignerParam? = null,
    val walletId: String = "",
    val groupId: String = ""
) {

    fun buildBundle() = Bundle().apply {
        putSerializable(SIGNER_TAG, signerTag)
        putParcelable(ONCHAIN_ADD_SIGNER_PARAM, onChainAddSignerParam)
        putString(WALLET_ID, walletId)
        putString(GROUP_ID, groupId)
    }

    companion object {
        private const val SIGNER_TAG = "signer_tag"
        private const val ONCHAIN_ADD_SIGNER_PARAM = "onchain_add_signer_param"
        private const val WALLET_ID = "wallet_id"
        private const val GROUP_ID = "group_id"

        fun deserializeFrom(intent: Intent): CheckFirmwareArgs = CheckFirmwareArgs(
            signerTag = intent.extras?.getSerializable(SIGNER_TAG) as? SignerTag 
                ?: throw IllegalArgumentException("SignerTag is required"),
            onChainAddSignerParam = intent.parcelable<OnChainAddSignerParam>(ONCHAIN_ADD_SIGNER_PARAM),
            walletId = intent.extras?.getString(WALLET_ID, "").orEmpty(),
            groupId = intent.extras?.getString(GROUP_ID, "").orEmpty()
        )
    }
}
