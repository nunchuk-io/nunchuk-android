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

package com.nunchuk.android.transaction.components.details

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.utils.parcelable

data class TransactionDetailsArgs(
    val walletId: String,
    val txId: String,
    val initEventId: String = "",
    val roomId: String = "",
    val transaction: Transaction? = null,
    val isInheritanceClaimingFlow: Boolean = false,
    val isCancelBroadcast: Boolean = false,
    val errorMessage: String = "",
    val isRequestSignatureFlow: Boolean = false,
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, TransactionDetailsActivity::class.java).apply {
            putExtra(EXTRA_WALLET_ID, walletId)
            putExtra(EXTRA_TRANSACTION_ID, txId)
            putExtra(EXTRA_INIT_EVENT_ID, initEventId)
            putExtra(EXTRA_ROOM_ID, roomId)
            putExtra(EXTRA_TRANSACTION, transaction)
            putExtra(EXTRA_INHERITANCE_CLAIMING_FLOW, isInheritanceClaimingFlow)
            putExtra(EXTRA_IS_CANCEL_BROADCAST, isCancelBroadcast)
            putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
            putExtra(EXTRA_REQUEST_SIGNATURE_FLOW, isRequestSignatureFlow)
        }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_TRANSACTION_ID = "EXTRA_TRANSACTION_ID"
        private const val EXTRA_INIT_EVENT_ID = "EXTRA_INIT_EVENT_ID"
        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"
        private const val EXTRA_TRANSACTION = "EXTRA_TRANSACTION"
        private const val EXTRA_INHERITANCE_CLAIMING_FLOW = "EXTRA_INHERITANCE_CLAIMING_FLOW"
        private const val EXTRA_IS_CANCEL_BROADCAST = "EXTRA_IS_CANCEL_BROADCAST"
        private const val EXTRA_ERROR_MESSAGE = "EXTRA_ERROR_MESSAGE"
        private const val EXTRA_REQUEST_SIGNATURE_FLOW = "EXTRA_REQUEST_SIGNATURE_FLOW"

        fun deserializeFrom(intent: Intent): TransactionDetailsArgs {
            val extras = intent.extras
            return TransactionDetailsArgs(
                walletId = extras.getStringValue(EXTRA_WALLET_ID),
                txId = extras.getStringValue(EXTRA_TRANSACTION_ID),
                initEventId = extras.getStringValue(EXTRA_INIT_EVENT_ID),
                roomId = extras.getStringValue(EXTRA_ROOM_ID),
                transaction = extras?.parcelable(EXTRA_TRANSACTION),
                isInheritanceClaimingFlow = extras.getBooleanValue(EXTRA_INHERITANCE_CLAIMING_FLOW),
                isCancelBroadcast = extras.getBooleanValue(EXTRA_IS_CANCEL_BROADCAST),
                errorMessage = extras.getStringValue(EXTRA_ERROR_MESSAGE),
                isRequestSignatureFlow = extras.getBooleanValue(EXTRA_REQUEST_SIGNATURE_FLOW),
            )
        }
    }
}