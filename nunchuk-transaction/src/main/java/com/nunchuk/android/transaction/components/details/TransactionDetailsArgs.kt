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

package com.nunchuk.android.transaction.components.details

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.Transaction

data class TransactionDetailsArgs(
    val walletId: String,
    val txId: String,
    val initEventId: String,
    val roomId: String,
    val transaction: Transaction?
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, TransactionDetailsActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_TRANSACTION_ID, txId)
        putExtra(EXTRA_INIT_EVENT_ID, initEventId)
        putExtra(EXTRA_ROOM_ID, roomId)
        putExtra(EXTRA_TRANSACTION, transaction)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_TRANSACTION_ID = "EXTRA_TRANSACTION_ID"
        private const val EXTRA_INIT_EVENT_ID = "EXTRA_INIT_EVENT_ID"
        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"
        private const val EXTRA_TRANSACTION = "EXTRA_TRANSACTION"

        fun deserializeFrom(intent: Intent): TransactionDetailsArgs {
            val extras = intent.extras
            return TransactionDetailsArgs(
                walletId = extras.getStringValue(EXTRA_WALLET_ID),
                txId = extras.getStringValue(EXTRA_TRANSACTION_ID),
                initEventId = extras.getStringValue(EXTRA_INIT_EVENT_ID),
                roomId = extras.getStringValue(EXTRA_ROOM_ID),
                transaction = extras?.getParcelable(EXTRA_TRANSACTION)
            )
        }
    }
}