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

package com.nunchuk.android.wallet.personal.components.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.nav.args.ConfigureWalletArgs
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddWalletActivity : BaseComposeActivity() {
    private val pin: String by lazy(LazyThreadSafetyMode.NONE) {
        intent.getStringExtra(DECOY_PIN).orEmpty()
    }

    private val isEdit: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        intent.getBooleanExtra(IS_EDIT, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AddWalletView(isEdit = isEdit) { walletName, addressType ->
                openAssignSignerScreen(
                    walletName = walletName,
                    addressType = addressType
                )
            }
        }
    }

    private fun openAssignSignerScreen(
        walletName: String,
        addressType: AddressType
    ) {
        if (addressType.isTaproot()) {
            navigator.openTaprootScreen(
                activityContext = this,
                walletName = walletName,
                walletType = WalletType.MULTI_SIG,
                addressType = addressType,
                decoyPin = pin
            )
        } else {
            navigator.openConfigureWalletScreen(
                this,
                args = ConfigureWalletArgs(
                    walletName = walletName,
                    walletType = WalletType.MULTI_SIG,
                    addressType = addressType,
                    decoyPin = pin
                )
            )
        }
    }

    companion object {
        private const val DECOY_PIN = "decoy_wallet"
        private const val IS_EDIT = "is_edit"


        fun start(activityContext: Context, decoyPin: String, isEdit: Boolean) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    AddWalletActivity::class.java
                ).apply {
                    putExtra(DECOY_PIN, decoyPin)
                    putExtra(IS_EDIT, isEdit)
                })
        }
    }

}