package com.nunchuk.android.transaction.send.fee

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getDoubleValue
import com.nunchuk.android.core.util.getStringValue

data class EstimatedFeeArgs(val walletId: String, val amount: Double) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, EstimatedFeeActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_AMOUNT, amount)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_AMOUNT = "EXTRA_AMOUNT"

        fun deserializeFrom(intent: Intent) = EstimatedFeeArgs(
            intent.extras.getStringValue(EXTRA_WALLET_ID),
            intent.extras.getDoubleValue(EXTRA_AMOUNT)
        )
    }
}