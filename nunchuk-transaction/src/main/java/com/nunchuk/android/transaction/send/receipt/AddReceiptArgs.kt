package com.nunchuk.android.transaction.send.receipt

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getDoubleValue
import com.nunchuk.android.core.util.getStringValue

data class AddReceiptArgs(
    val walletId: String,
    val outputAmount: Double,
    val availableAmount: Double
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, AddReceiptActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_OUTPUT_AMOUNT, outputAmount)
        putExtra(EXTRA_AVAILABLE_AMOUNT, availableAmount)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_OUTPUT_AMOUNT = "EXTRA_OUTPUT_AMOUNT"
        private const val EXTRA_AVAILABLE_AMOUNT = "EXTRA_AVAILABLE_AMOUNT"

        fun deserializeFrom(intent: Intent) = AddReceiptArgs(
            intent.extras.getStringValue(EXTRA_WALLET_ID),
            intent.extras.getDoubleValue(EXTRA_OUTPUT_AMOUNT),
            intent.extras.getDoubleValue(EXTRA_AVAILABLE_AMOUNT)
        )
    }
}