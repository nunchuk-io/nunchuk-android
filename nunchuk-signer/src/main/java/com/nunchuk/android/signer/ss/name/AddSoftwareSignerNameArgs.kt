package com.nunchuk.android.signer.ss.name

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

data class AddSoftwareSignerNameArgs(val mnemonic: String) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, AddSoftwareSignerNameActivity::class.java).apply {
        putExtra(EXTRA_MNEMONIC, mnemonic)
    }

    companion object {
        private const val EXTRA_MNEMONIC = "EXTRA_MNEMONIC"

        fun deserializeFrom(intent: Intent) = AddSoftwareSignerNameArgs(
            intent.extras?.getString(EXTRA_MNEMONIC, "").orEmpty(),
        )
    }

}