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

    fun openInputAmountScreen(activityContext: Activity, walletId: String, availableAmount: Double)

    fun openAddReceiptScreen(
        activityContext: Activity,
        walletId: String,
        outputAmount: Double,
        availableAmount: Double
    )

    fun openEstimatedFeeScreen(
        activityContext: Activity,
        walletId: String,
        outputAmount: Double,
        availableAmount: Double,
        address: String,
        privateNote: String
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
        manualFeeRate: Int = 0
    )

    fun openTransactionDetailsScreen(
        activityContext: Activity,
        walletId: String,
        txId: String
    )

}