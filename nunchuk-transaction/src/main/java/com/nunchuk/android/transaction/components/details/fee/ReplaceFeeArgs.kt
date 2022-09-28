package com.nunchuk.android.transaction.components.details.fee

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.Transaction

data class ReplaceFeeArgs(
    val walletId: String,
    val transaction: Transaction
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ReplaceFeeActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_TRANSACTION, transaction)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "a"
        private const val EXTRA_TRANSACTION = "b"

        fun deserializeFrom(intent: Intent): ReplaceFeeArgs {
            val extras = intent.extras
            return ReplaceFeeArgs(
                walletId = extras.getStringValue(EXTRA_WALLET_ID),
                transaction = extras!!.getParcelable(EXTRA_TRANSACTION)!!,
            )
        }
    }
}