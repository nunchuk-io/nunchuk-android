package com.nunchuk.android.wallet.components.config

import android.os.Bundle
import com.nunchuk.android.arch.args.FragmentArgs

data class WalletUpdateBottomSheetArgs(val walletName: String) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putString(EXTRA_WALLET_NAME, walletName)
    }

    companion object {
        private const val EXTRA_WALLET_NAME = "EXTRA_WALLET_NAME"

        fun deserializeFrom(data: Bundle?) = WalletUpdateBottomSheetArgs(
            data?.getString(EXTRA_WALLET_NAME).orEmpty()
        )
    }
}