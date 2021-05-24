package com.nunchuk.android.transaction.send.confirmation

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getDoubleValue
import com.nunchuk.android.core.util.getStringValue

data class TransactionConfirmArgs(val walletId: String, val amount: Double, val feeRate: Double) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, TransactionConfirmActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_AMOUNT, amount)
        putExtra(EXTRA_FEE_RATE, feeRate)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_AMOUNT = "EXTRA_AMOUNT"
        private const val EXTRA_FEE_RATE = "EXTRA_FEE_RATE"

        fun deserializeFrom(intent: Intent) = TransactionConfirmArgs(
            intent.extras.getStringValue(EXTRA_WALLET_ID),
            intent.extras.getDoubleValue(EXTRA_AMOUNT),
            intent.extras.getDoubleValue(EXTRA_FEE_RATE)
        )
    }
}