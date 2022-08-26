package com.nunchuk.android.signer.software.components.primarykey.chooseusername

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

data class PKeyChooseUsernameArgs(
    val mnemonic: String,
    val passphrase: String,
    val signerName: String
) :
    ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, PKeyChooseUsernameActivity::class.java).apply {
            putExtra(EXTRA_MNEMONIC, mnemonic)
            putExtra(EXTRA_PASSPHRASE, passphrase)
            putExtra(EXTRA_SIGNER_NAME, signerName)
        }

    companion object {
        private const val EXTRA_MNEMONIC = "EXTRA_MNEMONIC"
        private const val EXTRA_PASSPHRASE = "EXTRA_PASSPHRASE"
        private const val EXTRA_SIGNER_NAME = "EXTRA_SIGNER_NAME"

        fun deserializeFrom(intent: Intent) = PKeyChooseUsernameArgs(
            intent.extras?.getString(EXTRA_MNEMONIC, "").orEmpty(),
            intent.extras?.getString(EXTRA_PASSPHRASE, "").orEmpty(),
            intent.extras?.getString(EXTRA_SIGNER_NAME, "").orEmpty(),
        )
    }
}