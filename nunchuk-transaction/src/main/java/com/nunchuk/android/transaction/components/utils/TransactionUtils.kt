package com.nunchuk.android.transaction.components.utils

import android.content.Context
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.transaction.R

fun SweepType.toTitle(context: Context) = when (this) {
    SweepType.NONE -> context.getString(R.string.nc_transaction_new)
    SweepType.SWEEP_TO_NUNCHUK_WALLET,
    SweepType.UNSEAL_SWEEP_TO_NUNCHUK_WALLET -> context.getString(R.string.nc_sweep_to_a_wallet)
    SweepType.SWEEP_TO_EXTERNAL_ADDRESS,
    SweepType.UNSEAL_SWEEP_TO_EXTERNAL_ADDRESS -> context.getString(R.string.nc_sweep_to_an_address)
}