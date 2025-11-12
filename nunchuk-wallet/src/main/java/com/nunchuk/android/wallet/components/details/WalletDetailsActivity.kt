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

package com.nunchuk.android.wallet.components.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.ActivityWalletDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletDetailsActivity : BaseActivity<ActivityWalletDetailBinding>() {

    override fun initializeBinding() = ActivityWalletDetailBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle WindowInsets properly to ensure keyboard doesn't overlap content
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            
            // Apply system bar insets as padding
            view.setPadding(systemBars.left, 0, systemBars.right,
                if (ime.bottom > 0) 0 else systemBars.bottom)
            
            // When keyboard is shown, adjust bottom padding to account for it
            if (ime.bottom > 0) {
                val extraPadding = (32 * resources.displayMetrics.density).toInt()
                val imeHeight = ime.bottom - systemBars.bottom + extraPadding
                view.setPadding(systemBars.left, 0, systemBars.right, imeHeight)
            }
            
            insets
        }
        
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navHostFragment.navController.setGraph(R.navigation.wallet_detail_navigation, intent.extras)
    }

    companion object {
        fun start(activityContext: Context, walletId: String) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    WalletDetailsActivity::class.java
                ).apply {
                    putExtras(
                        WalletDetailsFragmentArgs(
                            walletId = walletId
                        ).toBundle()
                    )
                }
            )
        }

        fun buildIntent(activityContext: Context, walletId: String): Intent {
            return Intent(
                activityContext,
                WalletDetailsActivity::class.java
            ).apply {
                putExtras(
                    WalletDetailsFragmentArgs(
                        walletId = walletId
                    ).toBundle()
                )
            }
        }
    }
}