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

package com.nunchuk.android.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.UnspentOutput

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
        availableAmount: Double,
        inputs: List<UnspentOutput> = emptyList()
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
        sweepType: SweepType = SweepType.NONE,
        masterSignerId: String = "",
        magicalPhrase: String = "",
        derivationPath: String = "",
        inputs: List<UnspentOutput> = emptyList(),
    )

    /**
     * @param masterSignerId inheritance claiming flow
     * @param magicalPhrase inheritance claiming flow
     * @param derivationPath inheritance claiming flow
     */
    fun openEstimatedFeeScreen(
        activityContext: Activity,
        walletId: String,
        availableAmount: Double,
        txReceipts: List<TxReceipt>,
        privateNote: String,
        subtractFeeFromAmount: Boolean = false,
        sweepType: SweepType = SweepType.NONE,
        slots: List<SatsCardSlot> = emptyList(),
        masterSignerId: String = "",
        magicalPhrase: String = "",
        derivationPath: String = "",
        inputs: List<UnspentOutput> = emptyList(),
    )

    /**
     * @param masterSignerId inheritance claiming flow
     * @param magicalPhrase inheritance claiming flow
     */
    fun openTransactionConfirmScreen(
        activityContext: Activity,
        walletId: String,
        availableAmount: Double,
        txReceipts: List<TxReceipt>,
        privateNote: String,
        estimatedFee: Double,
        subtractFeeFromAmount: Boolean = false,
        manualFeeRate: Int = 0,
        sweepType: SweepType = SweepType.NONE,
        slots: List<SatsCardSlot> = emptyList(),
        masterSignerId: String = "",
        magicalPhrase: String = "",
        derivationPath: String = "",
        inputs: List<UnspentOutput> = emptyList(),
    )

    /**
     * @param isInheritanceClaimingFlow inheritance claiming flow
     */
    fun openTransactionDetailsScreen(
        activityContext: Activity,
        walletId: String,
        txId: String,
        initEventId: String = "",
        roomId: String = "",
        transaction: Transaction? = null,
        isInheritanceClaimingFlow: Boolean = false
    )

    fun openTransactionDetailsScreen(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Activity,
        walletId: String,
        txId: String,
        initEventId: String = "",
        roomId: String = "",
        transaction: Transaction? = null
    )

    fun openImportTransactionScreen(
        launcher: ActivityResultLauncher<Intent>? = null,
        activityContext: Activity,
        walletId: String,
        masterFingerPrint: String = "",
        initEventId: String = "",
        isDummyTx: Boolean = false,
        isFinishWhenError: Boolean = false
    )

    fun openExportTransactionScreen(
        launcher: ActivityResultLauncher<Intent>? = null,
        activityContext: Activity,
        walletId: String = "",
        txId: String = "",
        txToSign: String = "",
        isDummyTx: Boolean = false
    )

    fun openReplaceTransactionFee(
        launcher: ActivityResultLauncher<Intent>,
        context: Context,
        walletId: String,
        transaction: Transaction
    )

    fun openBatchTransactionScreen(
        activityContext: Activity, roomId: String,
        walletId: String,
        availableAmount: Double,
        inputs: List<UnspentOutput>
    )
}