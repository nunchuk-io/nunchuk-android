package com.nunchuk.android.nav

import android.app.Activity

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

    fun openInputAmountScreen(activityContext: Activity, walletId: String)

    fun openEstimatedFeeScreen(activityContext: Activity, walletId: String, amount: Double)

    fun openAddReceiptScreen(activityContext: Activity, walletId: String, amount: Double, feeRate: Double)
}