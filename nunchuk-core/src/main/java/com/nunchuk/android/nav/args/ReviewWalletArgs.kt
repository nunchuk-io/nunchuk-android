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
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.parcelableArrayList
import com.nunchuk.android.utils.serializable

data class ReviewWalletArgs(
    val walletName: String,
    val walletType: WalletType,
    val addressType: AddressType,
    val totalRequireSigns: Int,
    val masterSigners: List<SingleSigner>,
    val remoteSigners: List<SingleSigner>,
    val isDecoyWallet: Boolean = false
) {

    fun buildBundle() = Bundle().apply {
        putString(EXTRA_WALLET_NAME, walletName)
        putSerializable(EXTRA_WALLET_TYPE, walletType)
        putSerializable(EXTRA_ADDRESS_TYPE, addressType)
        putInt(EXTRA_TOTAL_REQUIRED_SIGNS, totalRequireSigns)
        putParcelableArrayList(EXTRA_MASTER_SIGNERS, ArrayList(masterSigners))
        putParcelableArrayList(EXTRA_REMOTE_SIGNERS, ArrayList(remoteSigners))
    }

    companion object {
        private const val EXTRA_WALLET_NAME = "EXTRA_WALLET_NAME"
        private const val EXTRA_WALLET_TYPE = "EXTRA_WALLET_TYPE"
        private const val EXTRA_ADDRESS_TYPE = "EXTRA_ADDRESS_TYPE"
        private const val EXTRA_TOTAL_REQUIRED_SIGNS = "EXTRA_TOTAL_REQUIRED_SIGNS"
        private const val EXTRA_MASTER_SIGNERS = "EXTRA_MASTER_SIGNERS"
        private const val EXTRA_REMOTE_SIGNERS = "EXTRA_REMOTE_SIGNERS"

        fun deserializeFrom(intent: Intent): ReviewWalletArgs = ReviewWalletArgs(
            intent.extras.getStringValue(EXTRA_WALLET_NAME),
            intent.serializable<WalletType>(EXTRA_WALLET_TYPE)!!,
            intent.serializable<AddressType>(EXTRA_ADDRESS_TYPE)!!,
            intent.getIntExtra(EXTRA_TOTAL_REQUIRED_SIGNS, 0),
            intent.parcelableArrayList<SingleSigner>(EXTRA_MASTER_SIGNERS).orEmpty(),
            intent.parcelableArrayList<SingleSigner>(EXTRA_REMOTE_SIGNERS).orEmpty()
        )
    }

}