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

package com.nunchuk.android.transaction.components.details.fee

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.Transaction

data class ReplaceFeeArgs(
    val walletId: String,
    val transaction: Transaction
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ReplaceFeeActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_TRANSACTION, transaction)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "a"
        private const val EXTRA_TRANSACTION = "b"

        fun deserializeFrom(intent: Intent): ReplaceFeeArgs {
            val extras = intent.extras
            return ReplaceFeeArgs(
                walletId = extras.getStringValue(EXTRA_WALLET_ID),
                transaction = extras!!.getParcelable(EXTRA_TRANSACTION)!!,
            )
        }
    }
}