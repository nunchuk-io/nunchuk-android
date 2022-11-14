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

package com.nunchuk.android.wallet.shared.components.configure

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

data class ConfigureSharedWalletArgs(
    val walletName: String,
    val walletType: WalletType,
    val addressType: AddressType
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ConfigureSharedWalletActivity::class.java).apply {
        putExtra(EXTRA_WALLET_NAME, walletName)
        putExtra(EXTRA_WALLET_TYPE, walletType)
        putExtra(EXTRA_ADDRESS_TYPE, addressType)
    }

    companion object {
        private const val EXTRA_WALLET_NAME = "EXTRA_WALLET_NAME"
        private const val EXTRA_WALLET_TYPE = "EXTRA_WALLET_TYPE"
        private const val EXTRA_ADDRESS_TYPE = "EXTRA_ADDRESS_TYPE"

        fun deserializeFrom(intent: Intent): ConfigureSharedWalletArgs = ConfigureSharedWalletArgs(
            intent.extras?.getString(EXTRA_WALLET_NAME, "").orEmpty(),
            intent.getSerializableExtra(EXTRA_WALLET_TYPE) as WalletType,
            intent.getSerializableExtra(EXTRA_ADDRESS_TYPE) as AddressType
        )
    }
}