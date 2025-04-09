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

package com.nunchuk.android.nav.args

import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable

data class ConfigureWalletArgs(
    val walletName: String,
    val walletType: WalletType,
    val addressType: AddressType,
    val decoyPin: String = "",
    val quickWalletParam: QuickWalletParam?
) {

    fun buildBundle() = Bundle().apply {
        putString(EXTRA_WALLET_NAME, walletName)
        putSerializable(EXTRA_WALLET_TYPE, walletType)
        putSerializable(EXTRA_ADDRESS_TYPE, addressType)
        putString(EXTRA_DECOY_PIN, decoyPin)
        putParcelable(EXTRA_QUICK_WALLET_PARAM, quickWalletParam)
    }

    companion object {
        private const val EXTRA_WALLET_NAME = "EXTRA_WALLET_NAME"
        private const val EXTRA_WALLET_TYPE = "EXTRA_WALLET_TYPE"
        private const val EXTRA_ADDRESS_TYPE = "EXTRA_ADDRESS_TYPE"
        private const val EXTRA_DECOY_PIN = "EXTRA_DECOY_PIN"
        private const val EXTRA_QUICK_WALLET_PARAM = "EXTRA_QUICK_WALLET_PARAM"

        fun deserializeFrom(intent: Intent): ConfigureWalletArgs = ConfigureWalletArgs(
            intent.extras?.getString(EXTRA_WALLET_NAME, "").orEmpty(),
            intent.serializable<WalletType>(EXTRA_WALLET_TYPE)!!,
            intent.serializable<AddressType>(EXTRA_ADDRESS_TYPE)!!,
            intent.extras?.getString(EXTRA_DECOY_PIN, "").orEmpty(),
            intent.parcelable<QuickWalletParam>(EXTRA_QUICK_WALLET_PARAM)
        )
    }
}