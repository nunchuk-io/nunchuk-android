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
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.BackUpSeedPhraseType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable

data class BackUpSeedPhraseArgs(
    val type: BackUpSeedPhraseType,
    val signer: SignerModel? = null,
    val groupId: String = "",
    val walletId: String = ""
) {

    fun buildBundle() = Bundle().apply {
        putSerializable(TYPE, type)
        putParcelable(SIGNER, signer)
        putString(GROUP_ID, groupId)
        putString(WALLET_ID, walletId)
    }

    companion object {
        private const val TYPE = "type"
        private const val SIGNER = "signer"
        private const val GROUP_ID = "group_id"
        private const val WALLET_ID = "wallet_id"

        fun deserializeFrom(intent: Intent): BackUpSeedPhraseArgs = BackUpSeedPhraseArgs(
            type = intent.extras?.getSerializable(TYPE) as? BackUpSeedPhraseType 
                ?: throw IllegalArgumentException("BackUpSeedPhraseType is required"),
            signer = intent.extras?.parcelable(SIGNER),
            groupId = intent.extras?.getString(GROUP_ID, "").orEmpty(),
            walletId = intent.extras?.getString(WALLET_ID, "").orEmpty()
        )
    }
}

