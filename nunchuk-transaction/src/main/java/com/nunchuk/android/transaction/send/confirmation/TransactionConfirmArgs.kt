package com.nunchuk.android.transaction.send.confirmation

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.getDoubleValue
import com.nunchuk.android.core.util.getIntValue
import com.nunchuk.android.core.util.getStringValue

data class TransactionConfirmArgs(
    val walletId: String,
    val outputAmount: Double,
    val availableAmount: Double,
    val address: String,
    val privateNote: String,
    val estimatedFee: Double,
    val subtractFeeFromAmount: Boolean = false,
    val manualFeeRate: Int = 0
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, TransactionConfirmActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_OUTPUT_AMOUNT, outputAmount)
        putExtra(EXTRA_AVAILABLE_AMOUNT, availableAmount)
        putExtra(EXTRA_ADDRESS, address)
        putExtra(EXTRA_PRIVATE_NOTE, privateNote)
        putExtra(EXTRA_ESTIMATE_FEE, estimatedFee)
        putExtra(EXTRA_SUBTRACT_FEE_FROM_AMOUNT, subtractFeeFromAmount)
        putExtra(EXTRA_MANUAL_FEE_RATE, manualFeeRate)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_OUTPUT_AMOUNT = "EXTRA_OUTPUT_AMOUNT"
        private const val EXTRA_AVAILABLE_AMOUNT = "EXTRA_AVAILABLE_AMOUNT"
        private const val EXTRA_ADDRESS = "EXTRA_ADDRESS"
        private const val EXTRA_PRIVATE_NOTE = "EXTRA_PRIVATE_NOTE"
        private const val EXTRA_ESTIMATE_FEE = "EXTRA_ESTIMATE_FEE"
        private const val EXTRA_SUBTRACT_FEE_FROM_AMOUNT = "EXTRA_SUBTRACT_FEE_FROM_AMOUNT"
        private const val EXTRA_MANUAL_FEE_RATE = "EXTRA_MANUAL_FEE_RATE"

        fun deserializeFrom(intent: Intent): TransactionConfirmArgs {
            val extras = intent.extras
            return TransactionConfirmArgs(
                extras.getStringValue(EXTRA_WALLET_ID),
                extras.getDoubleValue(EXTRA_OUTPUT_AMOUNT),
                extras.getDoubleValue(EXTRA_AVAILABLE_AMOUNT),
                extras.getStringValue(EXTRA_ADDRESS),
                extras.getStringValue(EXTRA_PRIVATE_NOTE),
                extras.getDoubleValue(EXTRA_ESTIMATE_FEE),
                extras.getBooleanValue(EXTRA_SUBTRACT_FEE_FROM_AMOUNT),
                extras.getIntValue(EXTRA_MANUAL_FEE_RATE)
            )
        }
    }
}