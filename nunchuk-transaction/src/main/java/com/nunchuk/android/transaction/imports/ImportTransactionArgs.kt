package com.nunchuk.android.transaction.imports

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue

data class ImportTransactionArgs(val walletId: String) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ImportTransactionActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"

        fun deserializeFrom(intent: Intent): ImportTransactionArgs {
            val extras = intent.extras
            return ImportTransactionArgs(
                extras.getStringValue(EXTRA_WALLET_ID)
            )
        }
    }
}