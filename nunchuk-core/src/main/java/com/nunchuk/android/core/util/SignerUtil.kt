package com.nunchuk.android.core.util

import android.content.Context
import com.nunchuk.android.core.R
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.SignerType.*

fun SignerType.toReadableString(context: Context) = when (this) {
    AIRGAP -> context.getString(R.string.nc_signer_type_air_gapped)
    SOFTWARE -> context.getString(R.string.nc_signer_type_software)
    HARDWARE -> context.getString(R.string.nc_signer_type_hardware)
    FOREIGN_SOFTWARE -> context.getString(R.string.nc_signer_type_foreign_software)
}

fun SignerModel.toReadableSignerType(context: Context) = if (software) {
    context.getString(R.string.nc_signer_type_software)
} else {
    type.toReadableString(context)
}