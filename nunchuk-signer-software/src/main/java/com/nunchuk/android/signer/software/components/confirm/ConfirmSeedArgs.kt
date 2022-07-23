package com.nunchuk.android.signer.software.components.confirm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.arch.args.ActivityArgs

data class ConfirmSeedArgs(val mnemonic: String) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ConfirmSeedActivity::class.java).apply {
        putExtra(EXTRA_MNEMONIC, mnemonic)
    }

    companion object {
        private const val EXTRA_MNEMONIC = "EXTRA_MNEMONIC"

        fun deserializeFrom(bundle: Bundle) = ConfirmSeedArgs(
            bundle.getString(EXTRA_MNEMONIC, "").orEmpty(),
        )
    }

}