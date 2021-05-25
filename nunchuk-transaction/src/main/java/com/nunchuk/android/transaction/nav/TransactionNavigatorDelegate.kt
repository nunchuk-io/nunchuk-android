package com.nunchuk.android.transaction.nav

import android.app.Activity
import com.nunchuk.android.nav.TransactionNavigator
import com.nunchuk.android.transaction.receive.ReceiveTransactionActivity
import com.nunchuk.android.transaction.receive.address.details.AddressDetailsActivity
import com.nunchuk.android.transaction.send.amount.InputAmountActivity
import com.nunchuk.android.transaction.send.confirmation.TransactionConfirmActivity
import com.nunchuk.android.transaction.send.fee.EstimatedFeeActivity

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

    override fun openEstimatedFeeScreen(activityContext: Activity, walletId: String, amount: Double) {
        EstimatedFeeActivity.start(
            activityContext = activityContext,
            walletId = walletId,
            amount = amount
        )
    }

    override fun openAddReceiptScreen(activityContext: Activity, walletId: String, amount: Double, feeRate: Double) {
        TransactionConfirmActivity.start(activityContext, walletId, amount, feeRate)
    }

}