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

package com.nunchuk.android.wallet.shared.components.configure

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.bindEnableState
import com.nunchuk.android.share.wallet.bindWalletConfiguration
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.shared.components.configure.ConfigureSharedWalletEvent.ConfigureCompletedEvent
import com.nunchuk.android.wallet.shared.databinding.ActivityConfigureSharedWalletBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfigureSharedWalletActivity : BaseActivity<ActivityConfigureSharedWalletBinding>() {

    private val args: ConfigureSharedWalletArgs by lazy { ConfigureSharedWalletArgs.deserializeFrom(intent) }

    private val viewModel: ConfigureSharedWalletViewModel by viewModels()

    override fun initializeBinding() = ActivityConfigureSharedWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
        observeEvent()

        viewModel.init()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleEvent(event: ConfigureSharedWalletEvent) {
        when (event) {
            is ConfigureCompletedEvent -> openReviewSharedWalletScreen(
                totalSigns = event.totalSigns,
                requireSigns = event.requireSigns
            )
        }
    }

    private fun openReviewSharedWalletScreen(
        totalSigns: Int,
        requireSigns: Int
    ) {
        navigator.openReviewSharedWalletScreen(
            activityContext = this,
            walletName = args.walletName,
            walletType = args.walletType,
            addressType = args.addressType,
            totalSigns = totalSigns,
            requireSigns = requireSigns,
            signers = emptyList()
        )
    }

    private fun handleState(state: ConfigureSharedWalletState) {
        val totalSigns = state.totalSigns
        val requireSigns = state.requireSigns
        bindTotalRequireSigns(totalSigns)
        binding.totalRequireSigns.bindWalletConfiguration(
            totalSigns = totalSigns,
            requireSigns = requireSigns
        )
        binding.totalSingerCounter.text = "$totalSigns"
        binding.requiredSingerCounter.text = "$requireSigns"
        binding.btnContinue.bindEnableState(state.isConfigured)
        binding.totalSignerIconMinus.isClickable = state.canDecreaseTotal
    }

    private fun bindTotalRequireSigns(totalRequireSigns: Int) {
        binding.requiredSingerCounter.text = "$totalRequireSigns"
    }

    private fun setupViews() {
        binding.totalSignerIconPlus.setOnClickListener { viewModel.handleIncreaseTotalSigners() }
        binding.iconPlus.setOnClickListener { viewModel.handleIncreaseRequiredSigners() }
        binding.totalSignerIconMinus.setOnClickListener { viewModel.handleDecreaseTotalSigners() }
        binding.iconMinus.setOnClickListener { viewModel.handleDecreaseRequiredSigners() }
        binding.btnContinue.setOnClickListener { viewModel.handleContinue() }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    companion object {

        fun start(activityContext: Context, walletName: String, walletType: WalletType, addressType: AddressType) {
            activityContext.startActivity(ConfigureSharedWalletArgs(walletName, walletType, addressType).buildIntent(activityContext))
        }
    }

}
