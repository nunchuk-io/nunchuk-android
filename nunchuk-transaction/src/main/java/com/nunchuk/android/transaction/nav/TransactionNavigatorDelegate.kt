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
        sweepType: SweepType
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
            sweepType = sweepType
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
        slots: List<SatsCardSlot>
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
            slots = slots
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
        slots: List<SatsCardSlot>
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
            slots = slots
        )
    }

    override fun openTransactionDetailsScreen(
        activityContext: Activity,
        walletId: String,
        txId: String,
        initEventId: String,
        roomId: String,
        transaction: Transaction?
    ) {
        activityContext.startActivity(
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
        ImportTransactionActivity.start(
            activityContext = activityContext,
            walletId = walletId,
            transactionOption = transactionOption,
            masterFingerPrint = masterFingerPrint,
            initEventId = initEventId
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