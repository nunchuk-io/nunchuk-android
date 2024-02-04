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

package com.nunchuk.android.transaction.components.receive.address.details

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.qr.DynamicQRCodeArgs.Companion.EXTRA_WALLET_ID
import com.nunchuk.android.core.util.getStringValue

data class AddressDetailsArgs(
    val address: String,
    val balance: String,
    val walletId: String
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(
        activityContext,
        AddressDetailsActivity::class.java
    ).apply {
        putExtra(EXTRA_ADDRESS, address)
        putExtra(EXTRA_BALANCE, balance)
        putExtra(EXTRA_WALLET_ID, walletId)
    }

    companion object {
        private const val EXTRA_ADDRESS = "EXTRA_ADDRESS"
        private const val EXTRA_BALANCE = "EXTRA_BALANCE"
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"

        fun deserializeFrom(intent: Intent): AddressDetailsArgs = AddressDetailsArgs(
            intent.extras.getStringValue(EXTRA_ADDRESS),
            intent.extras.getStringValue(EXTRA_BALANCE),
            intent.extras.getStringValue(EXTRA_WALLET_ID)
        )
    }
}