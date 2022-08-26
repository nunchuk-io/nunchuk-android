package com.nunchuk.android.signer.software.components.primarykey.manuallysignature

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

data class PKeyManuallySignatureArgs(
    val username: String
) :
    ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, PKeyManuallySignatureActivity::class.java).apply {
            putExtra(EXTRA_USERNAME, username)
        }

    companion object {
        private const val EXTRA_USERNAME = "EXTRA_USERNAME"

        fun deserializeFrom(intent: Intent) = PKeyManuallySignatureArgs(
            intent.extras?.getString(EXTRA_USERNAME, "").orEmpty(),
        )
    }
}