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

package com.nunchuk.android.wallet.shared.components.review

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

data class ReviewSharedWalletArgs(
    val walletName: String,
    val walletType: WalletType,
    val addressType: AddressType,
    val totalSigns: Int,
    val requireSigns: Int,
    val signers: List<SingleSigner>
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ReviewSharedWalletActivity::class.java).apply {
        putExtra(EXTRA_WALLET_NAME, walletName)
        putExtra(EXTRA_WALLET_TYPE, walletType)
        putExtra(EXTRA_ADDRESS_TYPE, addressType)
        putExtra(EXTRA_TOTAL_SIGNS, totalSigns)
        putExtra(EXTRA_REQUIRE_SIGNS, requireSigns)
        putParcelableArrayListExtra(EXTRA_SIGNERS, ArrayList(signers))
    }

    companion object {
        private const val EXTRA_WALLET_NAME = "EXTRA_WALLET_NAME"
        private const val EXTRA_WALLET_TYPE = "EXTRA_WALLET_TYPE"
        private const val EXTRA_ADDRESS_TYPE = "EXTRA_ADDRESS_TYPE"
        private const val EXTRA_TOTAL_SIGNS = "EXTRA_TOTAL_SIGNS"
        private const val EXTRA_REQUIRE_SIGNS = "EXTRA_REQUIRE_SIGNS"
        private const val EXTRA_SIGNERS = "EXTRA_SIGNERS"

        fun deserializeFrom(intent: Intent): ReviewSharedWalletArgs = ReviewSharedWalletArgs(
            intent.extras.getStringValue(EXTRA_WALLET_NAME),
            intent.getSerializableExtra(EXTRA_WALLET_TYPE) as WalletType,
            intent.getSerializableExtra(EXTRA_ADDRESS_TYPE) as AddressType,
            intent.getIntExtra(EXTRA_TOTAL_SIGNS, 0),
            intent.getIntExtra(EXTRA_REQUIRE_SIGNS, 0),
            intent.getParcelableArrayListExtra<SingleSigner>(EXTRA_SIGNERS)?.toList().orEmpty()
        )
    }

}