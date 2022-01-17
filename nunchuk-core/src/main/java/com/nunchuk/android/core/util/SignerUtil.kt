package com.nunchuk.android.core.util

import android.content.Context
import androidx.core.content.ContextCompat
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

fun SignerModel.toReadableSignerType(context: Context) = type.toReadableString(context)

fun SignerType.toReadableDrawable(context: Context) = when (this) {
    AIRGAP -> ContextCompat.getDrawable(context,R.drawable.ic_signer_type_air_gapped)
    SOFTWARE -> ContextCompat.getDrawable(context,R.drawable.ic_singer_type_software)
    HARDWARE -> ContextCompat.getDrawable(context,R.drawable.ic_signer_type_wired)
    FOREIGN_SOFTWARE -> ContextCompat.getDrawable(context,R.drawable.ic_singer_type_software)
}

fun SignerModel.toReadableSignerTypeDrawable(context: Context) = type.toReadableDrawable(context)