/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.wallet.util

import android.content.Context
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.AddressType.*
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.type.WalletType.ESCROW
import com.nunchuk.android.type.WalletType.MULTI_SIG
import com.nunchuk.android.wallet.core.R

fun WalletType.toReadableString(context: Context) = when (this) {
    ESCROW -> context.getString(R.string.nc_wallet_escrow_wallet)
    MULTI_SIG -> context.getString(R.string.nc_wallet_standard_wallet)
    else -> throw UnsupportedWalletTypeException(name)
}

fun AddressType.toReadableString(context: Context) = when (this) {
    NATIVE_SEGWIT -> context.getString(R.string.nc_wallet_native_segwit_wallet)
    NESTED_SEGWIT -> context.getString(R.string.nc_wallet_nested_segwit_wallet)
    LEGACY -> context.getString(R.string.nc_wallet_legacy_wallet)
    TAPROOT -> context.getString(R.string.nc_wallet_taproot_wallet)
    else -> throw UnsupportedAddressTypeException(name)
}

internal class UnsupportedWalletTypeException(message: String) : Exception(message)

internal class UnsupportedAddressTypeException(message: String) : Exception(message)

fun String.isWalletExisted() = this.lowercase().startsWith(WALLET_EXISTED)

internal const val WALLET_EXISTED = "wallet existed"

