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

package com.nunchuk.android.settings.walletsecurity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.wallet.WalletSecurityArgs
import com.nunchuk.android.core.wallet.WalletSecurityType
import com.nunchuk.android.settings.R
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletSecuritySettingActivity : BaseActivity<ActivityNavigationBinding>() {
    val args: WalletSecurityArgs by lazy {
        intent.extras?.let { extras ->
            WalletSecurityArgs.fromBundle(extras)
        } ?: WalletSecurityArgs()
    }

    override fun initializeBinding() = ActivityNavigationBinding.inflate(layoutInflater).also {
        enableEdgeToEdge()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.wallet_security_setting_nav)
        when (args.type) {
            WalletSecurityType.CREATE_DECOY_WALLET -> {
                graph.setStartDestination(R.id.decoyWalletIntroFragment)
            }
            WalletSecurityType.CREATE_DECOY_SUCCESS -> {
                graph.setStartDestination(R.id.decoyWalletSuccessFragment)
            }
            else -> {
                graph.setStartDestination(R.id.walletSecuritySettingFragment)
            }
        }
        navHostFragment.navController.setGraph(graph, intent.extras ?: Bundle())
    }

    companion object {

        fun start(activityContext: Context, args: WalletSecurityArgs) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    WalletSecuritySettingActivity::class.java
                ).apply {
                    putExtras(args.buildBundle())
                }
            )
        }
    }
}