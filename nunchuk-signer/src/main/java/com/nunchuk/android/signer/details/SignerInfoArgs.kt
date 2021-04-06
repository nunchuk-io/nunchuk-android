package com.nunchuk.android.signer.details

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

data class SignerInfoArgs(
    val signerName: String,
    val signerSpec: String,
    val justAdded: Boolean = false
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, SignerInfoActivity::class.java).apply {
        putExtra(EXTRA_SIGNER_NAME, signerName)
        putExtra(EXTRA_SIGNER_SPEC, signerSpec)
        putExtra(EXTRA_SIGNER_ADDED, justAdded)
    }

    companion object {
        private const val EXTRA_SIGNER_NAME = "EXTRA_SIGNER_NAME"
        private const val EXTRA_SIGNER_SPEC = "EXTRA_SIGNER_SPEC"
        private const val EXTRA_SIGNER_ADDED = "EXTRA_SIGNER_ADDED"

        fun deserializeFrom(intent: Intent): SignerInfoArgs = SignerInfoArgs(
            intent.extras?.getString(EXTRA_SIGNER_NAME, "").orEmpty(),
            intent.extras?.getString(EXTRA_SIGNER_SPEC, "").orEmpty(),
            intent.extras?.getBoolean(EXTRA_SIGNER_ADDED, false) ?: false
        )
    }
}