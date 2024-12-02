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
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaprootActivity : BaseComposeActivity() {

    private val args: TaprootWarningArgs by lazy { TaprootWarningArgs.deserializeFrom(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()

            NavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navController, startDestination = TaprootIntroScreenRoute) {
                taprootIntroScreen {
                    navController.navigateTaprootWarningScreen()
                }
                taprootWarningScreen {

                }
            }
        }
    }
//
//    private fun setupViews() {
//        binding.withdrawDesc.text = HtmlCompat.fromHtml(
//            getString(R.string.nc_wallet_taproot_withdraw_support_desc),
//            HtmlCompat.FROM_HTML_MODE_COMPACT
//        )
//
//        // TODO Hai
//        binding.btnContinue.setOnClickListener {
//            finish()
//            navigator.openConfigureWalletScreen(
//                activityContext = this,
//                args = ConfigureWalletArgs(
//                    walletName = args.walletName,
//                    walletType = args.walletType,
//                    addressType = args.addressType,
//                ),
//            )
//        }
//
//        binding.toolbar.setNavigationOnClickListener {
//            finish()
//        }
//    }

    companion object {
        fun start(activityContext: Context, walletName: String, walletType: WalletType, addressType: AddressType) {
            activityContext.startActivity(TaprootWarningArgs(walletName, walletType, addressType).buildIntent(activityContext))
        }
    }
}
