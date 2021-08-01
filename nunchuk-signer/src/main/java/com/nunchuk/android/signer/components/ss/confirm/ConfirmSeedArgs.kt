package com.nunchuk.android.signer.components.ss.confirm

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

data class ConfirmSeedArgs(val mnemonic: String) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ConfirmSeedActivity::class.java).apply {
        putExtra(EXTRA_MNEMONIC, mnemonic)
    }

    companion object {
        private const val EXTRA_MNEMONIC = "EXTRA_MNEMONIC"

        fun deserializeFrom(intent: Intent) = ConfirmSeedArgs(
            intent.extras?.getString(EXTRA_MNEMONIC, "").orEmpty(),
        )
    }

}