package com.nunchuk.android.transaction.components.send.amount

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getDoubleValue
import com.nunchuk.android.core.util.getStringValue

data class InputAmountArgs(
    val walletId: String,
    val availableAmount: Double
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(
        activityContext,
        InputAmountActivity::class.java
    ).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_AVAILABLE_AMOUNT, availableAmount)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_AVAILABLE_AMOUNT = "EXTRA_AVAILABLE_AMOUNT"

        fun deserializeFrom(intent: Intent) = InputAmountArgs(
            intent.extras.getStringValue(EXTRA_WALLET_ID),
            intent.extras.getDoubleValue(EXTRA_AVAILABLE_AMOUNT),

            )
    }
}