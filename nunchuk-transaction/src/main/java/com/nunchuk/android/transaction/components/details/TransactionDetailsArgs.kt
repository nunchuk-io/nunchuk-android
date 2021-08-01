package com.nunchuk.android.transaction.components.details

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue

data class TransactionDetailsArgs(
    val walletId: String,
    val txId: String
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, TransactionDetailsActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_TRANSACTION_ID, txId)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_TRANSACTION_ID = "EXTRA_TRANSACTION_ID"

        fun deserializeFrom(intent: Intent): TransactionDetailsArgs {
            val extras = intent.extras
            return TransactionDetailsArgs(
                extras.getStringValue(EXTRA_WALLET_ID),
                extras.getStringValue(EXTRA_TRANSACTION_ID)
            )
        }
    }
}