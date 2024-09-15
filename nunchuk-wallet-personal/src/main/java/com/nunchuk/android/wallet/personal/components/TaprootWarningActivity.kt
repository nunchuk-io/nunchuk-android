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

package com.nunchuk.android.wallet.personal.components

import android.content.Context
import android.os.Bundle
import androidx.core.text.HtmlCompat
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.nav.args.ConfigureWalletArgs
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.databinding.ActivityTaprootWarningBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaprootWarningActivity : BaseActivity<ActivityTaprootWarningBinding>() {

    private val args: TaprootWarningArgs by lazy { TaprootWarningArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityTaprootWarningBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
    }

    private fun setupViews() {
        binding.withdrawDesc.text = HtmlCompat.fromHtml(
            getString(R.string.nc_wallet_taproot_withdraw_support_desc),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        // TODO Hai
        binding.btnContinue.setOnClickListener {
            finish()
            navigator.openConfigureWalletScreen(
                activityContext = this,
                args = ConfigureWalletArgs(
                    walletName = args.walletName,
                    walletType = args.walletType,
                    addressType = args.addressType,
                ),
            )
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    companion object {
        fun start(activityContext: Context, walletName: String, walletType: WalletType, addressType: AddressType) {
            activityContext.startActivity(TaprootWarningArgs(walletName, walletType, addressType).buildIntent(activityContext))
        }
    }
}
