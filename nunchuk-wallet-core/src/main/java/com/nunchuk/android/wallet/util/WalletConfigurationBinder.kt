package com.nunchuk.android.wallet.util

import android.widget.TextView
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.wallet.core.R

fun TextView.bindWalletConfiguration(wallet: Wallet) {
    bindWalletConfiguration(
        requireSigns = wallet.totalRequireSigns,
        totalSigns = wallet.signers.size
    )
}

fun TextView.bindWalletConfiguration(totalSigns: Int, requireSigns: Int) {
    text = if (totalSigns == 0 || requireSigns == 0) {
        context.getString(R.string.nc_wallet_not_configured)
    } else if (totalSigns == 1 && requireSigns == 1) {
        context.getString(R.string.nc_wallet_single_sig)
    } else {
        "$requireSigns/$totalSigns ${context.getString(R.string.nc_wallet_multisig)}"
    }
}