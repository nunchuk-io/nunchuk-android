package com.nunchuk.android.transaction.components.imports

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.share.model.TransactionOption

data class ImportTransactionArgs(
    val walletId: String,
    val transactionOption: TransactionOption
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ImportTransactionActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_TRANSACTION_OPTION, transactionOption)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_TRANSACTION_OPTION = "EXTRA_TRANSACTION_OPTION"

        fun deserializeFrom(intent: Intent): ImportTransactionArgs {
            val extras = intent.extras
            return ImportTransactionArgs(
                walletId = extras.getStringValue(EXTRA_WALLET_ID),
                transactionOption = extras?.getSerializable(EXTRA_TRANSACTION_OPTION) as TransactionOption? ?: TransactionOption.IMPORT_KEYSTONE
            )
        }
    }
}