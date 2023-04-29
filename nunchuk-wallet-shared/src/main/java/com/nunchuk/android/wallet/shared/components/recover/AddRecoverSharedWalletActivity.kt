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

package com.nunchuk.android.wallet.shared.components.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter.LengthFilter
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.wallet.shared.R
import com.nunchuk.android.wallet.shared.databinding.ActivityAddRecoverSharedWalletBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddRecoverSharedWalletActivity : BaseActivity<ActivityAddRecoverSharedWalletBinding>() {

    private val viewModel: RecoverSharedWalletViewModel by viewModels()

    private val wallet: Wallet
        get() = intent.parcelable(EXTRAS_DATA)!!

    override fun initializeBinding() = ActivityAddRecoverSharedWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        flowObserver(viewModel.event) {
            handleEvent(it)
        }
        flowObserver(viewModel.state) {
            handleState(it)
        }
    }


    private fun handleEvent(event: RecoverSharedWalletEvent) {
        when (event) {
            is RecoverSharedWalletEvent.RecoverSharedWalletSuccess -> {
                val walletName = viewModel.walletName
                navigator.openReviewSharedWalletScreen(
                    activityContext = this,
                    walletName = walletName,
                    walletType = if (event.wallet.escrow) WalletType.ESCROW else WalletType.MULTI_SIG,
                    addressType = event.wallet.addressType,
                    totalSigns = event.wallet.signers.size,
                    requireSigns = event.wallet.totalRequireSigns,
                    signers = event.wallet.signers
                )
            }
            is RecoverSharedWalletEvent.WalletSetupDoneEvent -> handleWalletSetupDoneEvent()
            RecoverSharedWalletEvent.WalletNameRequiredEvent -> binding.walletName.setError(
                getString(
                    R.string.nc_text_required
                )
            )
            is RecoverSharedWalletEvent.ShowError -> NCToastMessage(this).showError(event.message)
        }
    }

    private fun handleState(state: RecoverSharedWalletState) {
        bindWalletCounter(state.walletName)
    }

    private fun handleWalletSetupDoneEvent() {
        val walletName = viewModel.walletName
        navigator.openReviewSharedWalletScreen(
            activityContext = this,
            walletName = walletName,
            walletType = if (wallet.escrow) WalletType.ESCROW else WalletType.MULTI_SIG,
            addressType = wallet.addressType,
            totalSigns = wallet.signers.size,
            requireSigns = wallet.totalRequireSigns,
            signers = wallet.signers
        )
    }

    private fun setupViews() {
        binding.walletName.getEditTextView().filters = arrayOf(LengthFilter(MAX_LENGTH))
        binding.walletName.addTextChangedCallback(viewModel::updateWalletName)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnContinue.setOnClickListener { viewModel.handleContinueEvent() }
    }

    private fun bindWalletCounter(walletName: String) {
        val counter = "${walletName.length}/${MAX_LENGTH}"
        binding.walletNameCounter.text = counter
    }

    companion object {
        private const val MAX_LENGTH = 20
        private const val EXTRAS_DATA = "EXTRAS_WALLET"

        fun start(activityContext: Context, wallet: Wallet) {
            val intent = Intent(activityContext, AddRecoverSharedWalletActivity::class.java).apply {
                putExtra(EXTRAS_DATA, wallet)
            }
            activityContext.startActivity(intent)
        }
    }
}