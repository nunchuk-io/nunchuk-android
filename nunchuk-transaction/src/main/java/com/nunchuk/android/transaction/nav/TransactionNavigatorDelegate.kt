package com.nunchuk.android.transaction.nav

import android.app.Activity
import com.nunchuk.android.nav.TransactionNavigator
import com.nunchuk.android.transaction.components.details.TransactionDetailsActivity
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
        availableAmount: Double
    ) {
        AddReceiptActivity.start(
            activityContext = activityContext,
            walletId = walletId,
            outputAmount = outputAmount,
            availableAmount = availableAmount
        )
    }

    override fun openEstimatedFeeScreen(
        activityContext: Activity,
        walletId: String,
        outputAmount: Double,
        availableAmount: Double,
        address: String,
        privateNote: String
    ) {
        EstimatedFeeActivity.start(
            activityContext = activityContext,
            walletId = walletId,
            outputAmount = outputAmount,
            availableAmount = availableAmount,
            address = address,
            privateNote = privateNote
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
        manualFeeRate: Int
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
            manualFeeRate = manualFeeRate
        )
    }

    override fun openTransactionDetailsScreen(
        activityContext: Activity,
        walletId: String,
        txId: String
    ) {
        TransactionDetailsActivity.start(
            activityContext = activityContext,
            walletId = walletId,
            txId = txId
        )
    }

    override fun openExportTransactionScreen(
        activityContext: Activity,
        walletId: String,
        txId: String
    ) {
        ExportTransactionActivity.start(
            activityContext = activityContext,
            walletId = walletId,
            txId = txId
        )
    }

    override fun openImportTransactionScreen(
        activityContext: Activity,
        walletId: String
    ) {
        ImportTransactionActivity.start(
            activityContext = activityContext,
            walletId = walletId
        )
    }

}