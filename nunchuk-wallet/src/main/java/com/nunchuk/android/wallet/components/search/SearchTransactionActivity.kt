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

package com.nunchuk.android.wallet.components.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.wallet.R
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchTransactionActivity : BaseActivity<ActivityNavigationBinding>() {

    override fun initializeBinding() = ActivityNavigationBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        setLightStatusBar()
        super.onCreate(savedInstanceState)
        if (intent.extras == null) {
            finish()
            return
        }
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navHostFragment.navController.setGraph(
            R.navigation.search_transaction_navigation,
            intent.extras
        )
    }

    companion object {
        fun start(activityContext: Context, walletId: String, roomId: String) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    SearchTransactionActivity::class.java
                ).apply {
                    putExtras(
                        SearchTransactionFragmentArgs(
                            walletId = walletId,
                            roomId = roomId
                        ).toBundle()
                    )
                }
            )
        }
    }
}