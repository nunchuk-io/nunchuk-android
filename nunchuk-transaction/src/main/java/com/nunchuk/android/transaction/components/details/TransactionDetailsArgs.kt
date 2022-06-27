package com.nunchuk.android.transaction.components.details

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue

data class TransactionDetailsArgs(
    val walletId: String,
    val txId: String,
    val initEventId: String,
    val roomId: String
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, TransactionDetailsActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_TRANSACTION_ID, txId)
        putExtra(EXTRA_INIT_EVENT_ID, initEventId)
        putExtra(EXTRA_ROOM_ID, roomId)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_TRANSACTION_ID = "EXTRA_TRANSACTION_ID"
        private const val EXTRA_INIT_EVENT_ID = "EXTRA_INIT_EVENT_ID"
        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"

        fun deserializeFrom(intent: Intent): TransactionDetailsArgs {
            val extras = intent.extras
            return TransactionDetailsArgs(
                walletId = extras.getStringValue(EXTRA_WALLET_ID),
                txId = extras.getStringValue(EXTRA_TRANSACTION_ID),
                initEventId = extras.getStringValue(EXTRA_INIT_EVENT_ID),
                roomId = extras.getStringValue(EXTRA_ROOM_ID)
            )
        }
    }
}