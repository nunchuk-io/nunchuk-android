package com.nunchuk.android.wallet.components.backup

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue

data class BackupWalletArgs(val walletId: String, val totalRequireSigns: Int) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, BackupWalletActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_WALLET_TOTAL_SIGN, totalRequireSigns)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_WALLET_TOTAL_SIGN = "EXTRA_WALLET_TOTAL_SIGN"

        fun deserializeFrom(intent: Intent): BackupWalletArgs = BackupWalletArgs(
            walletId = intent.extras.getStringValue(EXTRA_WALLET_ID),
            totalRequireSigns = intent.getIntExtra(EXTRA_WALLET_TOTAL_SIGN, 0)
        )
    }
}