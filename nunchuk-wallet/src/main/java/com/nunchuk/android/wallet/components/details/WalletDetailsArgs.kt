package com.nunchuk.android.wallet.components.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.core.util.getStringValue

data class WalletDetailsArgs(val walletId: String, val isSweepBalance: Boolean = false) : FragmentArgs, ActivityArgs {

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
        putExtra(EXTRA_IS_SWEEP_BALANCE, isSweepBalance)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_IS_SWEEP_BALANCE = "EXTRA_IS_SWEEP_BALANCE"

        fun deserializeFrom(bundle: Bundle): WalletDetailsArgs = WalletDetailsArgs(
            bundle.getStringValue(EXTRA_WALLET_ID),
            bundle.getBoolean(EXTRA_IS_SWEEP_BALANCE, false),
        )
    }
}