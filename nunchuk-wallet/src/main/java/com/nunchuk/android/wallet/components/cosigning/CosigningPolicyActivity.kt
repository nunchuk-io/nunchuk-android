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

package com.nunchuk.android.wallet.components.cosigning

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.wallet.R
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CosigningPolicyActivity : BaseActivity<ActivityNavigationBinding>() {
    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        initStartDestination()
    }

    private fun initStartDestination() {
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.cosigning_policy_navigation)
        if (intent.hasExtra("group_id")) {
            graph.setStartDestination(R.id.cosigningGroupPolicyFragment)
        } else {
            graph.setStartDestination(R.id.cosigningPolicyFragment)
        }
        navHostFragment.navController.setGraph(
            graph,
            intent.extras
        )
    }

    companion object {
        fun start(activity: Activity, walletId: String, token: String, keyPolicy: KeyPolicy?, xfp: String) {
            activity.startActivity(Intent(activity, CosigningPolicyActivity::class.java).apply {
                putExtras(
                    CosigningPolicyFragmentArgs(
                        keyPolicy = keyPolicy,
                        xfp = xfp,
                        token = token,
                        walletId = walletId
                    ).toBundle()
                )
            })
        }

        fun start(activity: Activity, walletId: String, token: String, xfp: String, groupId: String) {
            activity.startActivity(Intent(activity, CosigningPolicyActivity::class.java).apply {
                putExtras(
                    CosigningGroupPolicyFragmentArgs(
                        xfp = xfp,
                        token = token,
                        walletId = walletId,
                        groupId = groupId
                    ).toBundle()
                )
            })
        }
    }
}