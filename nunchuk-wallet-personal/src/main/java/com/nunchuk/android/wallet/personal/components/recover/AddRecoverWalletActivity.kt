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

package com.nunchuk.android.wallet.personal.components.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter.LengthFilter
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.DEFAULT_COLDCARD_WALLET_NAME
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RecoverWalletType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.databinding.ActivityAddRecoverWalletBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddRecoverWalletActivity : BaseActivity<ActivityAddRecoverWalletBinding>() {

    private val viewModel: RecoverWalletViewModel by viewModels()

    private val recoverWalletData: RecoverWalletData
        get() = intent.parcelable(EXTRAS_DATA)!!

    override fun initializeBinding() = ActivityAddRecoverWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleState(state: RecoverWalletState) {
        bindWalletCounter(state.walletName)
    }


    private fun handleEvent(event: RecoverWalletEvent) {
        when (event) {
            is RecoverWalletEvent.ImportWalletErrorEvent -> NCToastMessage(this).showError(event.message)
            is RecoverWalletEvent.ImportWalletSuccessEvent -> handleSuccessRecoverEvent(walletName = event.walletName, walletId = event.walletId)
            is RecoverWalletEvent.UpdateWalletErrorEvent -> NCToastMessage(this).showError(event.message)
            is RecoverWalletEvent.UpdateWalletSuccessEvent -> handleSuccessRecoverEvent(walletName = event.walletName, walletId = event.walletId)
            is RecoverWalletEvent.WalletSetupDoneEvent -> handleWalletSetupDoneEvent()
            RecoverWalletEvent.WalletNameRequiredEvent -> binding.walletName.setError(getString(R.string.nc_text_required))
            is RecoverWalletEvent.ImportGroupWalletSuccessEvent -> {
                navigator.openFreeGroupWalletScreen(this, event.walletId)
            }
        }
    }

    private fun handleWalletSetupDoneEvent() {
        val walletName = viewModel.walletName
        if (recoverWalletData.type == RecoverWalletType.FILE) {
            val filePath = recoverWalletData.filePath
            if (walletName != null && filePath != null) {
                importWallet(walletName, filePath)
            }
        } else if (recoverWalletData.type == RecoverWalletType.GROUP_WALLET) {
            val filePath = recoverWalletData.filePath
            if (walletName != null && filePath != null) {
                viewModel.recoverGroupWallet(
                    name = walletName,
                    filePath = filePath,
                    description = ""
                )
            }
        } else {
            val walletId = recoverWalletData.walletId
            if (walletName != null && walletId != null) {
                updateWallet(walletName, walletId)
            }
        }
    }

    private fun handleSuccessRecoverEvent(walletId: String, walletName: String) {
        NcToastManager.scheduleShowMessage(getString(R.string.nc_txt_import_wallet_success, walletName))
        openWalletConfigScreen(walletId)
    }

    private fun importWallet(walletName: String, filePath: String) {
        viewModel.importWallet(
            filePath = filePath,
            name = walletName,
            description = ""
        )
    }

    private fun updateWallet(name: String, walletId: String) {
        viewModel.updateWallet(
            walletId = walletId,
            walletName = name
        )
    }

    private fun openWalletConfigScreen(walletId: String) {
        navigator.openWalletConfigScreen(this, walletId)
        finish()
    }


    private fun setupViews() {
        binding.walletName.getEditTextView().filters = arrayOf(LengthFilter(MAX_LENGTH))
        binding.walletName.addTextChangedCallback(viewModel::updateWalletName)
        if (recoverWalletData.type == RecoverWalletType.COLDCARD) {
            binding.walletName.getEditTextView().setText(DEFAULT_COLDCARD_WALLET_NAME)
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.btnContinue.setOnClickListener { viewModel.handleContinueEvent() }
    }

    private fun bindWalletCounter(walletName: String) {
        val counter = "${walletName.length}/${MAX_LENGTH}"
        binding.walletNameCounter.text = counter
    }

    override fun onBackPressed() {
        if (recoverWalletData.walletId.isNullOrEmpty().not()) {
            recoverWalletData.walletId?.let {
                openWalletConfigScreen(it)
            }
        }
        super.onBackPressed()
    }

    companion object {
        private const val MAX_LENGTH = 20
        private const val EXTRAS_DATA = "EXTRAS_DATA"

        fun start(activityContext: Context, data: RecoverWalletData) {
            val intent = Intent(activityContext, AddRecoverWalletActivity::class.java).apply {
                putExtra(EXTRAS_DATA, data)
            }
            activityContext.startActivity(intent)
        }
    }
}