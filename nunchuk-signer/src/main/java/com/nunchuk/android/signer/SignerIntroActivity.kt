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

package com.nunchuk.android.signer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.databinding.ActivitySignerIntroBinding
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.signer.tapsigner.SetUpNfcOptionSheet
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignerIntroActivity : BaseActivity<ActivitySignerIntroBinding>(), SetUpNfcOptionSheet.OptionClickListener {

    override fun initializeBinding() = ActivitySignerIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    override fun onOptionClickListener(option: SetUpNfcOptionSheet.SetUpNfcOption) {
        when (option) {
            SetUpNfcOptionSheet.SetUpNfcOption.ADD_NEW -> navigateToSetupTapSigner()
            SetUpNfcOptionSheet.SetUpNfcOption.RECOVER -> NfcSetupActivity.navigate(this, NfcSetupActivity.RECOVER_NFC)
            SetUpNfcOptionSheet.SetUpNfcOption.Mk4 -> {
                navigator.openSetupMk4(this, false)
                finish()
            }
        }
    }

    private fun setupViews() {
        binding.btnAddNFC.setOnClickListener {
            SetUpNfcOptionSheet.newInstance().show(supportFragmentManager, "SetUpNfcOptionSheet")
        }
        binding.btnAddAirSigner.setOnClickListener { openAddAirSignerIntroScreen() }
        binding.btnAddSSigner.setOnClickListener { openAddSoftwareSignerScreen() }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun openAddAirSignerIntroScreen() {
        finish()
        navigator.openAddAirSignerIntroScreen(this)
    }

    private fun openAddSoftwareSignerScreen() {
        finish()
        navigator.openAddSoftwareSignerScreen(this)
    }

    private fun navigateToSetupTapSigner() {
        NfcSetupActivity.navigate(this, NfcSetupActivity.SETUP_TAP_SIGNER)
        finish()
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, SignerIntroActivity::class.java))
        }
    }
}