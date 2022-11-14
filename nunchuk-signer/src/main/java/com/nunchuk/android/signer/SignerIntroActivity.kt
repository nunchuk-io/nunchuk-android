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
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.signer.databinding.ActivitySignerIntroBinding
import com.nunchuk.android.signer.nfc.NfcSetupActivity
import com.nunchuk.android.signer.nfc.SetUpNfcOptionSheet
import com.nunchuk.android.signer.util.handleTapSignerStatus
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class SignerIntroActivity : BaseNfcActivity<ActivitySignerIntroBinding>(), SetUpNfcOptionSheet.OptionClickListener {
    private val viewModel: SignerIntroViewModel by viewModels()

    override fun initializeBinding() = ActivitySignerIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observer()
    }

    override fun onOptionClickListener(option: SetUpNfcOptionSheet.SetUpNfcOption) {
        when (option) {
            SetUpNfcOptionSheet.SetUpNfcOption.ADD_NEW -> startNfcFlow(REQUEST_NFC_STATUS)
            SetUpNfcOptionSheet.SetUpNfcOption.RECOVER -> NfcSetupActivity.navigate(this, NfcSetupActivity.RECOVER_NFC)
            SetUpNfcOptionSheet.SetUpNfcOption.Mk4 -> {
                navigator.openSetupMk4(this)
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

    private fun observer() {
        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_STATUS }
                    .collect {
                        viewModel.getTapSignerStatus(IsoDep.get(it.tag))
                        nfcViewModel.clearScanInfo()
                    }
            }
        }

        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect {
                    showOrHideLoading(it is SignerIntroState.Loading, message = getString(R.string.nc_keep_holding_nfc))
                    when (it) {
                        is SignerIntroState.GetTapSignerStatusSuccess -> handleTapSignerStatus(
                            it.status,
                            onCreateSigner = ::navigateToAddNfcKeySigner,
                            onSetupNfc = ::navigateToSetupNfc
                        )
                        is SignerIntroState.GetTapSignerStatusError -> {
                            val message = it.e?.message.orEmpty()
                            if (message.isNotEmpty()) {
                                NCToastMessage(this@SignerIntroActivity).showError(message)
                            }
                        }
                        else -> {}
                    }
                    viewModel.clearTapSignerStatus()
                }
            }
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

    private fun navigateToSetupNfc() {
        finish()
        NfcSetupActivity.navigate(this, NfcSetupActivity.SETUP_NFC)
    }

    private fun navigateToAddNfcKeySigner() {
        finish()
        NfcSetupActivity.navigate(this, NfcSetupActivity.ADD_KEY)
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, SignerIntroActivity::class.java))
        }
    }
}