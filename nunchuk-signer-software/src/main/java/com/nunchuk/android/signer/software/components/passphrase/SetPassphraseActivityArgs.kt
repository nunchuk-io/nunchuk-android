package com.nunchuk.android.signer.software.components.passphrase

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

data class SetPassphraseActivityArgs(
    val mnemonic: String,
    val signerName: String
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, SetPassphraseActivity::class.java).apply {
        putExtra(EXTRA_MNEMONIC, mnemonic)
        putExtra(EXTRA_SIGNER_NAME, signerName)
    }

    companion object {
        private const val EXTRA_MNEMONIC = "EXTRA_MNEMONIC"
        private const val EXTRA_SIGNER_NAME = "EXTRA_SIGNER_NAME"

        fun deserializeFrom(intent: Intent) = SetPassphraseActivityArgs(
            mnemonic = intent.extras?.getString(EXTRA_MNEMONIC, "").orEmpty(),
            signerName = intent.extras?.getString(EXTRA_SIGNER_NAME, "").orEmpty()
        )
    }

}