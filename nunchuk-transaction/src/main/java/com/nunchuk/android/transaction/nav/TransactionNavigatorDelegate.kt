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
import com.nunchuk.android.nav.TransactionNavigator
import com.nunchuk.android.share.model.TransactionOption
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
        availableAmount: Double
    ) {
        InputAmountActivity.start(
            activityContext = activityContext,
            roomId = roomId,
            walletId = walletId,
            availableAmount = availableAmount
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
        magicalPhrase: String
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
            magicalPhrase = magicalPhrase
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
        magicalPhrase: String
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
            magicalPhrase = magicalPhrase
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
        magicalPhrase: String
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
            magicalPhrase = magicalPhrase
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
        activityContext: Activity,
        walletId: String,
        transactionOption: TransactionOption,
        masterFingerPrint: String,
        initEventId: String
    ) {
        activityContext.startActivity(
            ImportTransactionActivity.buildIntent(
                activityContext = activityContext,
                walletId = walletId,
                transactionOption = transactionOption,
                masterFingerPrint = masterFingerPrint,
                initEventId = initEventId
            )
        )
    }

    override fun openImportDummyTransactionScreen(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Activity,
        transactionOption: TransactionOption,
        walletId: String,
    ) {
        launcher.launch(
            ImportTransactionActivity.buildIntent(
                activityContext = activityContext,
                transactionOption = transactionOption,
                walletId = walletId,
                isDummyTx = true
            )
        )
    }

    override fun openExportTransactionScreen(
        activityContext: Activity,
        walletId: String,
        txId: String,
        txToSign: String,
        transactionOption: TransactionOption
    ) {
        ExportTransactionActivity.start(
            activityContext = activityContext,
            walletId = walletId,
            txId = txId,
            txToSign = txToSign,
            transactionOption = transactionOption
        )
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