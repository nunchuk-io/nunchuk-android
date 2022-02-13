package com.nunchuk.android.transaction.components.export

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.transaction.components.details.TransactionOption

data class ExportTransactionArgs(
    val walletId: String,
    val txId: String,
    val transactionOption: TransactionOption
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ExportTransactionActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_TRANSACTION_ID, txId)
        putExtra(EXTRA_TRANSACTION_OPTION, transactionOption)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_TRANSACTION_ID = "EXTRA_TRANSACTION_ID"
        private const val EXTRA_TRANSACTION_OPTION = "EXTRA_TRANSACTION_OPTION"

        fun deserializeFrom(intent: Intent): ExportTransactionArgs {
            val extras = intent.extras
            return ExportTransactionArgs(
                walletId = extras.getStringValue(EXTRA_WALLET_ID),
                txId = extras.getStringValue(EXTRA_TRANSACTION_ID),
                transactionOption = extras?.getSerializable(EXTRA_TRANSACTION_OPTION) as TransactionOption? ?: TransactionOption.EXPORT
            )
        }
    }
}