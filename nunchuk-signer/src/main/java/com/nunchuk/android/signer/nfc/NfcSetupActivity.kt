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

package com.nunchuk.android.signer.nfc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcViewModel.Companion.EXTRA_MASTER_SIGNER_ID
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivityNavigationBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NfcSetupActivity : BaseNfcActivity<ActivityNavigationBinding>() {
    override fun initializeBinding(): ActivityNavigationBinding =
        ActivityNavigationBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightStatusBar()
        initStartDestination()
    }

    private fun initStartDestination() {
        val navHostFragment = (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.nfc_setup_navigation)
        val startDestinationId = when (setUpAction) {
            SETUP_NFC, SETUP_SATSCARD -> R.id.setupChainCodeFragment
            CHANGE_CVC -> R.id.changeNfcCvcFragment
            RECOVER_NFC -> R.id.recoverNfcKeyGuideFragment
            else -> R.id.addNfcNameFragment
        }
        graph.setStartDestination(startDestinationId)
        navHostFragment.navController.graph = graph
    }

    val setUpAction: Int
            by lazy(LazyThreadSafetyMode.NONE) { intent.getIntExtra(EXTRA_ACTION, SETUP_NFC) }

    val hasWallet: Boolean
            by lazy(LazyThreadSafetyMode.NONE) { intent.getBooleanExtra(EXTRA_HAS_WALLET, false) }

    companion object {
        private const val EXTRA_ACTION = "EXTRA_ACTION"
        private const val EXTRA_HAS_WALLET = "EXTRA_HAS_WALLET"
        const val SETUP_NFC = 1
        const val ADD_KEY = 2
        const val CHANGE_CVC = 3
        const val RECOVER_NFC = 4
        const val SETUP_SATSCARD = 5

        fun navigate(activity: Activity, setUpAction: Int, masterSignerId: String? = null, hasWallet: Boolean = false) {
            activity.startActivity(Intent(activity, NfcSetupActivity::class.java).apply {
                putExtra(EXTRA_ACTION, setUpAction)
                putExtra(EXTRA_MASTER_SIGNER_ID, masterSignerId)
                putExtra(EXTRA_HAS_WALLET, hasWallet)
            })
        }
    }
}