package com.nunchuk.android.transaction.receive

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue

data class ReceiveTransactionArgs(val walletId: String) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(
        activityContext,
        ReceiveTransactionActivity::class.java
    ).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"

        fun deserializeFrom(intent: Intent): ReceiveTransactionArgs = ReceiveTransactionArgs(
            intent.extras.getStringValue(EXTRA_WALLET_ID)
        )
    }
}