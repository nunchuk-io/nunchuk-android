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

package com.nunchuk.android.app.wallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.R
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.databinding.ActivityQuickWalletBinding
import com.nunchuk.android.wallet.personal.components.WalletIntermediaryFragmentArgs
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuickWalletActivity : BaseActivity<ActivityQuickWalletBinding>() {
    override fun initializeBinding(): ActivityQuickWalletBinding {
        return ActivityQuickWalletBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightStatusBar()
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navHostFragment.findNavController().setGraph(R.navigation.quick_wallet_navigation, WalletIntermediaryFragmentArgs(isQuickWallet = true).toBundle())
    }

    companion object {
        fun start(launcher: ActivityResultLauncher<Intent>, activityContext: Context) {
            launcher.launch(Intent(activityContext, QuickWalletActivity::class.java))
        }
    }
}