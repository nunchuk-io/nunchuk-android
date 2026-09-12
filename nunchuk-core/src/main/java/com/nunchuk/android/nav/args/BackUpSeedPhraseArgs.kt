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
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.parcelable

data class BackUpSeedPhraseArgs(
    val type: BackUpSeedPhraseType,
    val signer: SignerModel? = null,
    val groupId: String = "",
    val walletId: String = "",
    val replacedXfp: String? = null,
    /**
     * Which sharing method of an off-chain inheritance key this run is verifying. Set only by the
     * off-chain inheritance flow, where a key can carry two independently verified artifacts;
     * null on the on-chain timelock flow, which has one verification per key.
     */
    val claimOption: ClaimOption? = null,
    /**
     * The wallet being built, which scopes the key types offered when the restored key is re-added.
     * The server advertises an inheritance-key entry per wallet type, so leaving this null lists
     * the same device once for each of them.
     */
    val walletType: WalletType? = null,
) {

    fun buildBundle() = Bundle().apply {
        putSerializable(TYPE, type)
        putParcelable(SIGNER, signer)
        putString(GROUP_ID, groupId)
        putString(WALLET_ID, walletId)
        putString(REPLACED_XFP, replacedXfp)
        putString(CLAIM_OPTION, claimOption?.name)
        putSerializable(WALLET_TYPE, walletType)
    }

    companion object {
        /**
         * The screen confirming that a seed-phrase backup was proven. Which one depends on the
         * flow, and only the caller knows: an off-chain inheritance key names itself on a card
         * ([claimOption] is set), the on-chain timelock key shows the plain confirmation.
         */
        fun verified(
            signer: SignerModel?,
            groupId: String,
            walletId: String,
            claimOption: ClaimOption?,
        ) = BackUpSeedPhraseArgs(
            type = if (claimOption != null) {
                BackUpSeedPhraseType.INHERITANCE_VERIFIED
            } else {
                BackUpSeedPhraseType.SUCCESS
            },
            signer = signer,
            groupId = groupId,
            walletId = walletId,
            claimOption = claimOption,
        )

        private const val TYPE = "type"
        private const val SIGNER = "signer"
        private const val GROUP_ID = "group_id"
        private const val WALLET_ID = "wallet_id"
        private const val REPLACED_XFP = "replaced_xfp"
        private const val CLAIM_OPTION = "claim_option"
        private const val WALLET_TYPE = "wallet_type"

        fun deserializeFrom(intent: Intent): BackUpSeedPhraseArgs = BackUpSeedPhraseArgs(
            type = intent.extras?.getSerializable(TYPE) as? BackUpSeedPhraseType 
                ?: throw IllegalArgumentException("BackUpSeedPhraseType is required"),
            signer = intent.extras?.parcelable(SIGNER),
            groupId = intent.extras?.getString(GROUP_ID, "").orEmpty(),
            walletId = intent.extras?.getString(WALLET_ID, "").orEmpty(),
            replacedXfp = intent.extras?.getString(REPLACED_XFP),
            claimOption = intent.extras?.getString(CLAIM_OPTION)
                ?.let { name -> ClaimOption.entries.firstOrNull { it.name == name } },
            walletType = intent.extras?.getSerializable(WALLET_TYPE) as? WalletType,
        )
    }
}

