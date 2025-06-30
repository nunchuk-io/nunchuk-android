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

package com.nunchuk.android.share.wallet

import android.widget.TextView
import com.nunchuk.android.core.R
import com.nunchuk.android.model.Wallet

fun TextView.bindWalletConfiguration(wallet: Wallet, hideWalletDetail: Boolean = false) {
    bindWalletConfiguration(
        requireSigns = wallet.totalRequireSigns,
        totalSigns = wallet.signers.size,
        hideWalletDetail = hideWalletDetail,
        isMiniscript = wallet.miniscript.isNotEmpty()
    )
}

fun TextView.bindWalletConfiguration(totalSigns: Int, requireSigns: Int, isMiniscript: Boolean = false, hideWalletDetail: Boolean = false) {
    text = if (hideWalletDetail) {
        '\u2022'.toString().repeat(6)
    } else if (isMiniscript) {
        context.getString(R.string.nc_miniscript)
    } else if (totalSigns == 0 || requireSigns == 0) {
        context.getString(R.string.nc_wallet_not_configured)
    } else if (totalSigns == 1 && requireSigns == 1) {
        context.getString(R.string.nc_wallet_single_sig)
    } else {
        "$requireSigns/$totalSigns ${context.getString(R.string.nc_wallet_multisig)}"
    }
}