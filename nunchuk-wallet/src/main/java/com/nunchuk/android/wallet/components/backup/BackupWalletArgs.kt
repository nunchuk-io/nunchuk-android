package com.nunchuk.android.wallet.components.backup

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue

data class BackupWalletArgs(val walletId: String) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, BackupWalletActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"

        fun deserializeFrom(intent: Intent): BackupWalletArgs = BackupWalletArgs(
            walletId = intent.extras.getStringValue(EXTRA_WALLET_ID)
        )
    }
}