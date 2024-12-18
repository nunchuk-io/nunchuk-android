package com.nunchuk.android.main.membership.key

import android.content.Context
import com.nunchuk.android.main.R
import com.nunchuk.android.type.SignerTag

fun SignerTag.toString(context: Context) = when (this) {
    SignerTag.LEDGER -> context.getString(R.string.nc_ledger)
    SignerTag.TREZOR -> context.getString(R.string.nc_trezor)
    SignerTag.COLDCARD -> context.getString(R.string.nc_coldcard)
    SignerTag.BITBOX -> context.getString(R.string.nc_bitbox)
    SignerTag.JADE -> context.getString(R.string.nc_blockstream_jade)
    else -> ""
}