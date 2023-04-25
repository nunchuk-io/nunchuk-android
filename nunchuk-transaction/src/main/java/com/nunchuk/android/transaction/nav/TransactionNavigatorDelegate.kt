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

package com.nunchuk.android.transaction.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nav.TransactionNavigator
import com.nunchuk.android.transaction.components.details.TransactionDetailsActivity
import com.nunchuk.android.transaction.components.details.fee.ReplaceFeeActivity
import com.nunchuk.android.transaction.components.export.ExportTransactionActivity
import com.nunchuk.android.transaction.components.imports.ImportTransactionActivity
import com.nunchuk.android.transaction.components.receive.ReceiveTransactionActivity
import com.nunchuk.android.transaction.components.receive.address.details.AddressDetailsActivity
import com.nunchuk.android.transaction.components.send.amount.InputAmountActivity
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmActivity
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeActivity
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptActivity

interface TransactionNavigatorDelegate : TransactionNavigator {

    override fun openReceiveTransactionScreen(
        activityContext: Activity,
        walletId: String
    ) {
        ReceiveTransactionActivity.start(
            activityContext = activityContext,
            walletId = walletId
        )
    }

    override fun openAddressDetailsScreen(
        activityContext: Activity,
        address: String,
        balance: String
    ) {
        AddressDetailsActivity.start(
            activityContext = activityContext,
            address = address,
            balance = balance
        )
    }

    override fun openInputAmountScreen(
        activityContext: Activity,
        roomId: String,
        walletId: String,
        availableAmount: Double,
        inputs: List<UnspentOutput>
    ) {
        InputAmountActivity.start(
            activityContext = activityContext,
            roomId = roomId,
            walletId = walletId,
            availableAmount = availableAmount,
            inputs = inputs,
        )
    }

    override fun openAddReceiptScreen(
        activityContext: Activity,
        walletId: String,
        outputAmount: Double,
        availableAmount: Double,
        address: String,
        privateNote: String,
        subtractFeeFromAmount: Boolean,
        slots: List<SatsCardSlot>,
        sweepType: SweepType,
        masterSignerId: String,
        magicalPhrase: String,
        inputs: List<UnspentOutput>
    ) {
        AddReceiptActivity.start(
            activityContext = activityContext,
            walletId = walletId,
            outputAmount = outputAmount,
            availableAmount = availableAmount,
            subtractFeeFromAmount = subtractFeeFromAmount,
            address = address,
            privateNote = privateNote,
            slots = slots,
            sweepType = sweepType,
            masterSignerId = masterSignerId,
            magicalPhrase = magicalPhrase,
            inputs = inputs
        )
    }

    override fun openEstimatedFeeScreen(
        activityContext: Activity,
        walletId: String,
        outputAmount: Double,
        availableAmount: Double,
        address: String,
        privateNote: String,
        subtractFeeFromAmount: Boolean,
        sweepType: SweepType,
        slots: List<SatsCardSlot>,
        masterSignerId: String,
        magicalPhrase: String,
        inputs: List<UnspentOutput>
    ) {
        EstimatedFeeActivity.start(
            activityContext = activityContext,
            walletId = walletId,
            outputAmount = outputAmount,
            availableAmount = availableAmount,
            address = address,
            privateNote = privateNote,
            subtractFeeFromAmount = subtractFeeFromAmount,
            sweepType = sweepType,
            slots = slots,
            masterSignerId = masterSignerId,
            magicalPhrase = magicalPhrase,
            inputs = inputs,
        )
    }

    override fun openTransactionConfirmScreen(
        activityContext: Activity,
        walletId: String,
        outputAmount: Double,
        availableAmount: Double,
        address: String,
        privateNote: String,
        estimatedFee: Double,
        subtractFeeFromAmount: Boolean,
        manualFeeRate: Int,
        sweepType: SweepType,
        slots: List<SatsCardSlot>,
        masterSignerId: String,
        magicalPhrase: String,
        inputs: List<UnspentOutput>
    ) {
        TransactionConfirmActivity.start(
            activityContext = activityContext,
            walletId = walletId,
            outputAmount = outputAmount,
            availableAmount = availableAmount,
            address = address,
            privateNote = privateNote,
            estimatedFee = estimatedFee,
            subtractFeeFromAmount = subtractFeeFromAmount,
            manualFeeRate = manualFeeRate,
            sweepType = sweepType,
            slots = slots,
            masterSignerId = masterSignerId,
            magicalPhrase = magicalPhrase,
            inputs = inputs
        )
    }

    override fun openTransactionDetailsScreen(
        activityContext: Activity,
        walletId: String,
        txId: String,
        initEventId: String,
        roomId: String,
        transaction: Transaction?,
        isInheritanceClaimingFlow: Boolean
    ) {
        activityContext.startActivity(
            TransactionDetailsActivity.buildIntent(
                activityContext = activityContext,
                walletId = walletId,
                txId = txId,
                initEventId = initEventId,
                roomId = roomId,
                transaction = transaction,
                isInheritanceClaimingFlow = isInheritanceClaimingFlow
            )
        )
    }

    override fun openTransactionDetailsScreen(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Activity,
        walletId: String,
        txId: String,
        initEventId: String,
        roomId: String,
        transaction: Transaction?
    ) {
        launcher.launch(
            TransactionDetailsActivity.buildIntent(
                activityContext = activityContext,
                walletId = walletId,
                txId = txId,
                initEventId = initEventId,
                roomId = roomId,
                transaction = transaction
            )
        )
    }

    override fun openImportTransactionScreen(
        launcher: ActivityResultLauncher<Intent>?,
        activityContext: Activity,
        walletId: String,
        masterFingerPrint: String,
        initEventId: String,
        isDummyTx: Boolean,
        isFinishWhenError: Boolean
    ) {
        val intent = ImportTransactionActivity.buildIntent(
            activityContext = activityContext,
            walletId = walletId,
            masterFingerPrint = masterFingerPrint,
            initEventId = initEventId,
            isDummyTx = isDummyTx,
            isFinishWhenError = isFinishWhenError
        )
        if (launcher != null) {
            launcher.launch(intent)
        } else {
            activityContext.startActivity(intent)
        }
    }

    override fun openExportTransactionScreen(
        launcher: ActivityResultLauncher<Intent>?,
        activityContext: Activity,
        walletId: String,
        txId: String,
        txToSign: String,
        isDummyTx: Boolean
    ) {
        val intent = ExportTransactionActivity.buildIntent(
            activityContext = activityContext,
            walletId = walletId,
            txId = txId,
            txToSign = txToSign,
            isDummyTx = isDummyTx
        )
        if (launcher != null) {
            launcher.launch(intent)
        } else {
            activityContext.startActivity(intent)
        }
    }

    override fun openReplaceTransactionFee(
        launcher: ActivityResultLauncher<Intent>,
        context: Context,
        walletId: String,
        transaction: Transaction
    ) {
        ReplaceFeeActivity.start(launcher, context, walletId, transaction)
    }
}