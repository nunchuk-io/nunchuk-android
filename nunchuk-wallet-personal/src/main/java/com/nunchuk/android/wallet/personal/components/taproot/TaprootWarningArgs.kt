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

package com.nunchuk.android.wallet.personal.components.taproot

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.parcelable

data class TaprootWarningArgs(
    val walletName: String,
    val walletType: WalletType,
    val addressType: AddressType,
    val decoyPin : String = "",
    val groupSandboxId: String = "",
    val quickWalletParam: QuickWalletParam?
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, TaprootActivity::class.java).apply {
        putExtra(EXTRA_WALLET_NAME, walletName)
        putExtra(EXTRA_WALLET_TYPE, walletType)
        putExtra(EXTRA_ADDRESS_TYPE, addressType)
        putExtra(EXTRA_DECOY_PIN, decoyPin)
        putExtra(EXTRA_GROUP_SANDBOX_ID, groupSandboxId)
        putExtra(EXTRA_QUICK_WALLET_PARAM, quickWalletParam)
    }

    companion object {
        private const val EXTRA_WALLET_NAME = "EXTRA_WALLET_NAME"
        private const val EXTRA_WALLET_TYPE = "EXTRA_WALLET_TYPE"
        private const val EXTRA_ADDRESS_TYPE = "EXTRA_ADDRESS_TYPE"
        private const val EXTRA_DECOY_PIN = "EXTRA_DECOY_PIN"
        private const val EXTRA_GROUP_SANDBOX_ID = "EXTRA_GROUP_SANDBOX_ID"
        private const val EXTRA_QUICK_WALLET_PARAM = "EXTRA_QUICK_WALLET_PARAM"

        fun deserializeFrom(intent: Intent): TaprootWarningArgs = TaprootWarningArgs(
            intent.extras?.getString(EXTRA_WALLET_NAME, "").orEmpty(),
            intent.getSerializableExtra(EXTRA_WALLET_TYPE) as WalletType,
            intent.getSerializableExtra(EXTRA_ADDRESS_TYPE) as AddressType,
            intent.extras?.getString(EXTRA_DECOY_PIN, "").orEmpty(),
            intent.extras?.getString(EXTRA_GROUP_SANDBOX_ID, "").orEmpty(),
            intent.extras?.parcelable<QuickWalletParam>(EXTRA_QUICK_WALLET_PARAM)
        )
    }
}