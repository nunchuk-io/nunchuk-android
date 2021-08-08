package com.nunchuk.android.wallet.util

import android.widget.TextView
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.wallet.R

fun TextView.bindWalletConfiguration(wallet: Wallet) {
    bindWalletConfiguration(
        totalSigns = wallet.totalRequireSigns,
        assignedSigns = wallet.signers.size
    )
}

fun TextView.bindWalletConfiguration(totalSigns: Int, assignedSigns: Int) {
    text = if (totalSigns == 0 || assignedSigns == 0) {
        context.getString(R.string.nc_wallet_not_configured)
    } else if (totalSigns == 1 && assignedSigns == 1) {
        context.getString(R.string.nc_wallet_single_sig)
    } else {
        val totalRequireSignsValue = "$totalSigns/$assignedSigns ${context.getString(R.string.nc_wallet_multisig)}"
        totalRequireSignsValue
    }
}