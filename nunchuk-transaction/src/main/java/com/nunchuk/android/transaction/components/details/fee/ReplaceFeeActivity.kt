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

package com.nunchuk.android.transaction.components.details.fee

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.nfc.RbfType
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.databinding.ActivityReplaceByFeeBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReplaceFeeActivity : BaseActivity<ActivityReplaceByFeeBinding>() {

    override fun initializeBinding(): ActivityReplaceByFeeBinding {
        return ActivityReplaceByFeeBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightStatusBar()
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.navigation_replace_fee)
        when (ReplaceFeeArgs.deserializeFrom(intent).rbfType) {
            RbfType.ReplaceFee -> graph.setStartDestination(R.id.replaceFeeFragment)
            RbfType.CancelTransaction -> graph.setStartDestination(R.id.rbfCancelTransactionFragment)
        }
        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    companion object {
        fun start(launcher: ActivityResultLauncher<Intent>, context: Context, walletId: String, transaction: Transaction, type: RbfType) {
            launcher.launch(ReplaceFeeArgs(walletId, transaction, type).buildIntent(context))
        }
    }
}