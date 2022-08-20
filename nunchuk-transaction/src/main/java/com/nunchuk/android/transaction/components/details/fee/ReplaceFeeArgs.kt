package com.nunchuk.android.transaction.components.details.fee

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue

data class ReplaceFeeArgs(
    val walletId: String,
    val txId: String,
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ReplaceFeeActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_TRANSACTION_ID, txId)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_TRANSACTION_ID = "EXTRA_TRANSACTION_ID"

        fun deserializeFrom(intent: Intent): ReplaceFeeArgs {
            val extras = intent.extras
            return ReplaceFeeArgs(
                walletId = extras.getStringValue(EXTRA_WALLET_ID),
                txId = extras.getStringValue(EXTRA_TRANSACTION_ID),
            )
        }
    }
}