package com.nunchuk.android.wallet.components.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.core.util.getStringValue

data class WalletDetailsArgs(val walletId: String, val shouldReloadPendingTx: Boolean = false) : FragmentArgs, ActivityArgs {

    override fun buildBundle(): Bundle {
        return Bundle().apply {
            putString(EXTRA_WALLET_ID, walletId)
        }
    }

    override fun buildIntent(activityContext: Context) = Intent(
        activityContext,
        WalletDetailsActivity::class.java
    ).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_SHOULD_RELOAD_PENDING_TX, shouldReloadPendingTx)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_SHOULD_RELOAD_PENDING_TX = "EXTRA_SHOULD_RELOAD_PENDING_TX"

        fun deserializeFrom(bundle: Bundle): WalletDetailsArgs = WalletDetailsArgs(
            bundle.getStringValue(EXTRA_WALLET_ID),
            bundle.getBoolean(EXTRA_SHOULD_RELOAD_PENDING_TX, false),
        )
    }
}