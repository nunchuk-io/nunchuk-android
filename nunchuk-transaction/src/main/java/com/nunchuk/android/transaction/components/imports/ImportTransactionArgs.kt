package com.nunchuk.android.transaction.components.imports

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.share.model.TransactionOption

data class ImportTransactionArgs(
    val walletId: String,
    val transactionOption: TransactionOption,
    val masterFingerPrint: String,
    val initEventId: String
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ImportTransactionActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_TRANSACTION_OPTION, transactionOption)
        putExtra(EXTRA_MASTER_FINGER_PRINT, masterFingerPrint)
        putExtra(EXTRA_INIT_EVENT_ID, initEventId)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "a"
        private const val EXTRA_TRANSACTION_OPTION = "b"
        private const val EXTRA_MASTER_FINGER_PRINT = "c"
        private const val EXTRA_INIT_EVENT_ID = "d"

        fun deserializeFrom(intent: Intent): ImportTransactionArgs {
            val extras = intent.extras
            return ImportTransactionArgs(
                walletId = extras.getStringValue(EXTRA_WALLET_ID),
                transactionOption = extras?.getSerializable(EXTRA_TRANSACTION_OPTION) as TransactionOption? ?: TransactionOption.IMPORT_KEYSTONE,
                masterFingerPrint = extras.getStringValue(EXTRA_MASTER_FINGER_PRINT),
                initEventId = extras.getStringValue(EXTRA_INIT_EVENT_ID)
            )
        }
    }
}