package com.nunchuk.android.wallet.shared.components.config

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

data class SharedWalletConfigArgs(val walletId: String) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, SharedWalletConfigActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"

        fun deserializeFrom(intent: Intent): SharedWalletConfigArgs = SharedWalletConfigArgs(
            intent.extras?.getString(EXTRA_WALLET_ID, "").orEmpty(),
        )
    }
}