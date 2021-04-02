package com.nunchuk.android.main.components.signer

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

data class SignerInfoArgs(val signerName: String, val signerSpec: String) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, SignerInfoActivity::class.java).apply {
        putExtra(EXTRA_SIGNER_NAME, signerName)
        putExtra(EXTRA_SIGNER_SPEC, signerSpec)
    }

    companion object {
        private const val EXTRA_SIGNER_NAME = "EXTRA_SIGNER_NAME"
        private const val EXTRA_SIGNER_SPEC = "EXTRA_SIGNER_SPEC"

        fun deserializeFrom(intent: Intent): SignerInfoArgs = SignerInfoArgs(
            intent.extras?.getString(EXTRA_SIGNER_NAME, "").orEmpty(),
            intent.extras?.getString(EXTRA_SIGNER_SPEC, "").orEmpty()
        )
    }
}