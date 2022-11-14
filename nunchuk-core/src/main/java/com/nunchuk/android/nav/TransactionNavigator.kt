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

package com.nunchuk.android.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.share.model.TransactionOption

interface TransactionNavigator {

    fun openReceiveTransactionScreen(
        activityContext: Activity,
        walletId: String
    )

    fun openAddressDetailsScreen(
        activityContext: Activity,
        address: String,
        balance: String
    )

    fun openInputAmountScreen(
        activityContext: Activity,
        roomId: String = "",
        walletId: String,
        availableAmount: Double
    )

    fun openAddReceiptScreen(
        activityContext: Activity,
        walletId: String,
        outputAmount: Double,
        availableAmount: Double,
        address: String = "",
        privateNote: String = "",
        subtractFeeFromAmount: Boolean = false,
        slots: List<SatsCardSlot> = emptyList(),
        sweepType: SweepType = SweepType.NONE
    )

    fun openEstimatedFeeScreen(
        activityContext: Activity,
        walletId: String,
        outputAmount: Double,
        availableAmount: Double,
        address: String,
        privateNote: String,
        subtractFeeFromAmount: Boolean = false,
        sweepType: SweepType = SweepType.NONE,
        slots: List<SatsCardSlot> = emptyList()
    )

    fun openTransactionConfirmScreen(
        activityContext: Activity,
        walletId: String,
        outputAmount: Double,
        availableAmount: Double,
        address: String,
        privateNote: String,
        estimatedFee: Double,
        subtractFeeFromAmount: Boolean = false,
        manualFeeRate: Int = 0,
        sweepType: SweepType = SweepType.NONE,
        slots: List<SatsCardSlot> = emptyList()
    )

    fun openTransactionDetailsScreen(
        activityContext: Activity,
        walletId: String,
        txId: String,
        initEventId: String = "",
        roomId: String,
        transaction: Transaction? = null
    )

    fun openTransactionDetailsScreen(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Activity,
        walletId: String,
        txId: String,
        initEventId: String = "",
        roomId: String,
        transaction: Transaction? = null
    )

    fun openImportTransactionScreen(
        activityContext: Activity,
        walletId: String,
        transactionOption: TransactionOption,
        masterFingerPrint: String = "",
        initEventId: String = ""
    )

    fun openReplaceTransactionFee(
        launcher: ActivityResultLauncher<Intent>, context: Context, walletId: String, transaction: Transaction
    )
}