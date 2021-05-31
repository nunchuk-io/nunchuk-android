package com.nunchuk.android.transaction.nav

import android.app.Activity
import com.nunchuk.android.nav.TransactionNavigator
import com.nunchuk.android.transaction.details.TransactionDetailsActivity
import com.nunchuk.android.transaction.export.ExportTransactionActivity
import com.nunchuk.android.transaction.receive.ReceiveTransactionActivity
import com.nunchuk.android.transaction.receive.address.details.AddressDetailsActivity
import com.nunchuk.android.transaction.send.amount.InputAmountActivity
import com.nunchuk.android.transaction.send.confirmation.TransactionConfirmActivity
import com.nunchuk.android.transaction.send.fee.EstimatedFeeActivity
import com.nunchuk.android.transaction.send.receipt.AddReceiptActivity

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
        walletId: String,
        availableAmount: Double
    ) {
        InputAmountActivity.start(
            activityContext = activityContext,
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
        TransactionDetailsActivity.start(activityContext, walletId, txId)
    }

    override fun openExportTransactionScreen(
        activityContext: Activity,
        walletId: String,
        txId: String
    ) {
        ExportTransactionActivity.start(activityContext, walletId, txId)
    }

}