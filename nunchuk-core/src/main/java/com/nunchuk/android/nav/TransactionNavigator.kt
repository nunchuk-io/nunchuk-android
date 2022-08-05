package com.nunchuk.android.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
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
        subtractFeeFromAmount: Boolean = false,
    )

    fun openEstimatedFeeScreen(
        activityContext: Activity,
        walletId: String,
        outputAmount: Double,
        availableAmount: Double,
        address: String,
        privateNote: String,
        subtractFeeFromAmount: Boolean = false,
        isReplaceFee: Boolean = false
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
        txId: String,
        initEventId: String,
        roomId: String
    )

    fun openImportTransactionScreen(
        activityContext: Activity,
        walletId: String,
        transactionOption: TransactionOption
    )

    fun openReplaceTransactionFee(
        launcher: ActivityResultLauncher<Intent>, context: Context, walletId: String, txId: String
    )
}