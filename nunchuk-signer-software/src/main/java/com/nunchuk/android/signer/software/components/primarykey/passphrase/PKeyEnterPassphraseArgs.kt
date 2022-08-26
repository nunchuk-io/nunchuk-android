package com.nunchuk.android.signer.software.components.primarykey.passphrase

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.signer.PrimaryKeyFlow

data class PKeyEnterPassphraseArgs(
    val mnemonic: String,
    val primaryKeyFlow: Int,
) :
    ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, PKeyEnterPassphraseActivity::class.java).apply {
            putExtra(EXTRA_MNEMONIC, mnemonic)
            putExtra(EXTRA_PRIMARY_KEY_FLOW, primaryKeyFlow)
        }

    companion object {
        private const val EXTRA_MNEMONIC = "EXTRA_MNEMONIC"
        private const val EXTRA_PRIMARY_KEY_FLOW = "EXTRA_PRIMARY_KEY_FLOW"

        fun deserializeFrom(intent: Intent) = PKeyEnterPassphraseArgs(
            mnemonic = intent.extras?.getString(EXTRA_MNEMONIC, "").orEmpty(),
            primaryKeyFlow = intent.extras?.getInt(EXTRA_PRIMARY_KEY_FLOW, PrimaryKeyFlow.NONE)
                ?: PrimaryKeyFlow.NONE,
        )
    }

}