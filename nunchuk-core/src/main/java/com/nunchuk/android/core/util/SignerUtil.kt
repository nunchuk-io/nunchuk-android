/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.core.util

import android.content.Context
import androidx.core.content.ContextCompat
import com.nunchuk.android.core.R
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.SignerType.AIRGAP
import com.nunchuk.android.type.SignerType.COLDCARD_NFC
import com.nunchuk.android.type.SignerType.FOREIGN_SOFTWARE
import com.nunchuk.android.type.SignerType.HARDWARE
import com.nunchuk.android.type.SignerType.NFC
import com.nunchuk.android.type.SignerType.SERVER
import com.nunchuk.android.type.SignerType.SOFTWARE
import com.nunchuk.android.type.SignerType.UNKNOWN

val SignerType.isRemoteSigner: Boolean
    get() = this == AIRGAP || this == COLDCARD_NFC || this == HARDWARE

fun SignerType.toReadableString(context: Context, isPrimaryKey: Boolean): String {
    if (isPrimaryKey) return context.getString(R.string.nc_signer_type_primary_key)
    return when (this) {
        AIRGAP -> context.getString(R.string.nc_signer_type_air_gapped)
        SOFTWARE -> context.getString(R.string.nc_signer_type_software)
        HARDWARE -> context.getString(R.string.nc_signer_type_hardware)
        FOREIGN_SOFTWARE -> context.getString(R.string.nc_signer_type_foreign_software)
        NFC, COLDCARD_NFC -> context.getString(R.string.nc_nfc)
        UNKNOWN -> context.getString(R.string.nc_unknown)
        SERVER -> context.getString(R.string.nc_server_key)
    }
}

fun String?.toSignerType() = SignerType.values().find { it.name == this } ?: UNKNOWN

fun SignerModel.toReadableSignerType(context: Context, isIgnorePrimary: Boolean = false) =
    type.toReadableString(context, if (isIgnorePrimary) false else isPrimaryKey)

private fun toReadableDrawableResId(type: SignerType, tags: List<SignerTag>, isPrimaryKey: Boolean = false): Int {
    if (isPrimaryKey) return R.drawable.ic_signer_type_primary_key_small
    return when {
        type == AIRGAP && tags.contains(SignerTag.JADE) -> R.drawable.ic_air_gapped_jade
        type == AIRGAP && tags.contains(SignerTag.SEEDSIGNER) -> R.drawable.ic_air_gapped_seedsigner
        type == AIRGAP && tags.contains(SignerTag.PASSPORT) -> R.drawable.ic_air_gapped_passport
        type == AIRGAP && tags.contains(SignerTag.KEYSTONE) -> R.drawable.ic_air_gapped_keystone
        type == AIRGAP -> R.drawable.ic_air_gapped_other
        type == COLDCARD_NFC -> R.drawable.ic_coldcard_small
        type == SOFTWARE -> R.drawable.ic_logo_dark_small
        type == HARDWARE -> R.drawable.ic_signer_type_wired
        type == FOREIGN_SOFTWARE -> R.drawable.ic_logo_dark_small
        type == NFC -> R.drawable.ic_nfc_card
        type == SERVER -> R.drawable.ic_server_key_dark
        else -> R.drawable.ic_unknown_key
    }
}

fun SignerModel.toReadableDrawable(context: Context, isPrimaryKey: Boolean = false) = ContextCompat.getDrawable(
        context,
        toReadableDrawableResId(type, tags, isPrimaryKey)
    ) ?: throw NullPointerException("Nunchuk can not get drawable")

fun SignerModel.toReadableDrawableResId(isPrimaryKey: Boolean = false): Int {
    return toReadableDrawableResId(type, tags, isPrimaryKey)
}

fun MasterSigner.toReadableDrawable(context: Context, isPrimaryKey: Boolean = false) = ContextCompat.getDrawable(
    context,
    toReadableDrawableResId(type, tags, isPrimaryKey)
) ?: throw NullPointerException("Nunchuk can not get drawable")

fun SingleSigner.toReadableDrawable(context: Context, isPrimaryKey: Boolean = false) = ContextCompat.getDrawable(
    context,
    toReadableDrawableResId(type, tags, isPrimaryKey)
) ?: throw NullPointerException("Nunchuk can not get drawable")