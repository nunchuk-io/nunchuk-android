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
        transactionOption: TransactionOption
    )

    fun openReplaceTransactionFee(
        launcher: ActivityResultLauncher<Intent>, context: Context, walletId: String, transaction: Transaction
    )
}