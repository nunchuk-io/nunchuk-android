/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.signer.satscard

import android.app.Activity
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.model.SatsCardStatus
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SatsCardActivity : BaseNfcActivity<ActivityNavigationBinding>() {
    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHost.navController.navInflater
        val graph = inflater.inflate(R.navigation.satscard_navigation)
        val args: SatsCardArgs = SatsCardArgs.deserializeBundle(intent.extras!!)
        val startDestinationId = if (args.isShowUnseal) {
            R.id.satsCardUnsealSlotFragment
        } else {
            R.id.satsCardSlotFragment
        }
        graph.setStartDestination(startDestinationId)
        navHost.navController.setGraph(graph, intent.extras)
    }

    companion object {
        fun navigate(activity: Activity, status: SatsCardStatus, hasWallet: Boolean, isShowUnseal: Boolean = false) {
            activity.startActivity(SatsCardArgs(status, hasWallet, isShowUnseal).buildIntent(activity))
        }
    }
}