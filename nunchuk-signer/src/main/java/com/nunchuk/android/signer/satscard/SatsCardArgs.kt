package com.nunchuk.android.signer.satscard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.model.SatsCardStatus

data class SatsCardArgs(val status: SatsCardStatus, val hasWallet: Boolean, val isShowUnseal: Boolean) : ActivityArgs {
    override fun buildIntent(activityContext: Context): Intent {
        return Intent(activityContext, SatsCardActivity::class.java).apply {
            putExtra(EXTRA_SATSCARD_STATUS, status)
            putExtra(EXTRA_HAS_WALLET, hasWallet)
            putExtra(EXTRA_SHOW_UNSEAL_SLOT, isShowUnseal)
        }
    }

    companion object {
        const val EXTRA_SATSCARD_STATUS = "extra_satscard_status"
        private const val EXTRA_HAS_WALLET = "extra_has_wallet"
        private const val EXTRA_SHOW_UNSEAL_SLOT = "EXTRA_SHOW_UNSEAL_SLOT"

        fun deserializeBundle(arguments: Bundle) = SatsCardArgs(
            arguments.getParcelable(EXTRA_SATSCARD_STATUS)!!,
            arguments.getBoolean(EXTRA_HAS_WALLET),
            arguments.getBoolean(EXTRA_SHOW_UNSEAL_SLOT),
        )
    }
}