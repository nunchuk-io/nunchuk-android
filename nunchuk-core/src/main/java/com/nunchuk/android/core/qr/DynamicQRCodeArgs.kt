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

package com.nunchuk.android.core.qr

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

data class DynamicQRCodeArgs(val walletId: String, val values: List<String>) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, DynamicQRCodeActivity::class.java).apply {
        putExtra(EXTRA_WALLET_VALUES, values.joinToString(separator = ","))
        putExtra(EXTRA_WALLET_ID, walletId)
    }

    companion object {
        private const val EXTRA_WALLET_VALUES = "EXTRA_WALLET_VALUES"
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"

        fun deserializeFrom(intent: Intent): DynamicQRCodeArgs = DynamicQRCodeArgs(
            intent.extras?.getString(EXTRA_WALLET_ID, "").orEmpty(),
            intent.extras?.getString(EXTRA_WALLET_VALUES, "").orEmpty().split(","),
        )
    }
}