package com.nunchuk.android.nav.args

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.utils.parcelable

data class ClaimTransactionArgs(
    val transaction: Transaction,
    val masterSignerIds: List<String>,
    val derivationPaths: List<String>,
    val magic: String,
) : ActivityArgs {

    override fun buildIntent(activityContext: Context): Intent {
        return Intent().apply {
            putExtra(EXTRA_TRANSACTION, transaction)
            putStringArrayListExtra(EXTRA_MASTER_SIGNER_IDS, ArrayList(masterSignerIds))
            putStringArrayListExtra(EXTRA_DERIVATION_PATHS, ArrayList(derivationPaths))
            putExtra(EXTRA_MAGIC, magic)
        }
    }

    companion object {
        const val EXTRA_TRANSACTION = "EXTRA_TRANSACTION"
        const val EXTRA_MASTER_SIGNER_IDS = "EXTRA_MASTER_SIGNER_IDS"
        const val EXTRA_DERIVATION_PATHS = "EXTRA_DERIVATION_PATHS"
        const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        const val EXTRA_MAGIC = "EXTRA_MAGIC"

        fun deserializeFrom(intent: Intent): ClaimTransactionArgs {
            val extras = intent.extras
            return ClaimTransactionArgs(
                transaction = extras?.parcelable(EXTRA_TRANSACTION)
                    ?: throw IllegalArgumentException("Transaction is required"),
                masterSignerIds = extras.getStringArrayList(EXTRA_MASTER_SIGNER_IDS).orEmpty(),
                derivationPaths = extras.getStringArrayList(EXTRA_DERIVATION_PATHS).orEmpty(),
                magic = extras.getString(EXTRA_MAGIC).orEmpty(),
            )
        }
    }
}
