package com.nunchuk.android.wallet.components.backup

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue

data class BackupWalletArgs(val walletId: String, val numberOfSignKey: Int, val isQuickWallet: Boolean) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, BackupWalletActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_WALLET_TOTAL_SIGN, numberOfSignKey)
        putExtra(EXTRA_IS_QUICK_WALLET, isQuickWallet)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_WALLET_TOTAL_SIGN = "EXTRA_WALLET_TOTAL_SIGN"
        private const val EXTRA_IS_QUICK_WALLET = "EXTRA_IS_QUICK_WALLET"

        fun deserializeFrom(intent: Intent): BackupWalletArgs = BackupWalletArgs(
            walletId = intent.extras.getStringValue(EXTRA_WALLET_ID),
            numberOfSignKey = intent.getIntExtra(EXTRA_WALLET_TOTAL_SIGN, 0),
            isQuickWallet = intent.getBooleanExtra(EXTRA_IS_QUICK_WALLET, false),
        )
    }
}