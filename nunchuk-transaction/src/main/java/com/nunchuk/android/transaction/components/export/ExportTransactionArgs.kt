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

package com.nunchuk.android.transaction.components.export

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.utils.serializable

data class ExportTransactionArgs(
    val walletId: String,
    val txId: String,
    val txToSign: String,
    val transactionOption: TransactionOption,
    val initEventId: String,
    val masterFingerPrint: String,
    val isDummyTx: Boolean
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ExportTransactionActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_TRANSACTION_ID, txId)
        putExtra(EXTRA_TX_TO_SIGN, txToSign)
        putExtra(EXTRA_TRANSACTION_OPTION, transactionOption)
        putExtra(EXTRA_INIT_EVENT_ID, initEventId)
        putExtra(EXTRA_MASTER_FINGERPRINT, masterFingerPrint)
        putExtra(EXTRA_IS_DUMMY_TX, isDummyTx)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_TRANSACTION_ID = "EXTRA_TRANSACTION_ID"
        private const val EXTRA_TX_TO_SIGN = "EXTRA_TX_TO_SIGN"
        private const val EXTRA_TRANSACTION_OPTION = "EXTRA_TRANSACTION_OPTION"
        private const val EXTRA_INIT_EVENT_ID = "EXTRA_INIT_EVENT_ID"
        private const val EXTRA_MASTER_FINGERPRINT = "EXTRA_MASTER_FINGERPRINT"
        private const val EXTRA_IS_DUMMY_TX = "EXTRA_IS_DUMMY_TX"

        fun deserializeFrom(intent: Intent): ExportTransactionArgs {
            val extras = intent.extras
            return ExportTransactionArgs(
                walletId = extras.getStringValue(EXTRA_WALLET_ID),
                txId = extras.getStringValue(EXTRA_TRANSACTION_ID),
                txToSign = extras.getStringValue(EXTRA_TX_TO_SIGN),
                transactionOption = extras?.serializable(EXTRA_TRANSACTION_OPTION) ?: TransactionOption.EXPORT_KEYSTONE,
                initEventId = extras.getStringValue(EXTRA_INIT_EVENT_ID),
                masterFingerPrint = extras.getStringValue(EXTRA_MASTER_FINGERPRINT),
                isDummyTx = extras.getBooleanValue(EXTRA_IS_DUMMY_TX)
            )
        }
    }
}