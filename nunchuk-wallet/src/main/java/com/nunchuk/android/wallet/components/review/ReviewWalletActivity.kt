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

package com.nunchuk.android.wallet.components.review

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.nav.args.ReviewWalletArgs
import com.nunchuk.android.share.wallet.bindWalletConfiguration
import com.nunchuk.android.utils.viewModelProviderFactoryOf
import com.nunchuk.android.wallet.components.config.SignersViewBinder
import com.nunchuk.android.wallet.components.review.ReviewWalletEvent.CreateWalletErrorEvent
import com.nunchuk.android.wallet.components.review.ReviewWalletEvent.CreateWalletSuccessEvent
import com.nunchuk.android.wallet.components.review.ReviewWalletEvent.SetLoadingEvent
import com.nunchuk.android.wallet.databinding.ActivityReviewWalletBinding
import com.nunchuk.android.wallet.util.isWalletExisted
import com.nunchuk.android.wallet.util.toReadableString
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReviewWalletActivity : BaseActivity<ActivityReviewWalletBinding>() {

    @Inject
    internal lateinit var vmFactory: ReviewWalletViewModel.Factory

    private val viewModel: ReviewWalletViewModel by viewModels {
        viewModelProviderFactoryOf {
            vmFactory.create(args)
        }
    }

    private val args: ReviewWalletArgs by lazy { ReviewWalletArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityReviewWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: ReviewWalletEvent) {
        when (event) {
            is SetLoadingEvent -> showOrHideLoading(event.showLoading)
            is CreateWalletSuccessEvent -> onCreateWalletSuccess(event)
            is CreateWalletErrorEvent -> onCreateWalletError(event)
        }
    }

    private fun onCreateWalletError(event: CreateWalletErrorEvent) {
        showOrHideLoading(false)
        val message = event.message
        NCToastMessage(this).showWarning(message)
        if (message.isWalletExisted()) {
            navigator.openMainScreen(this)
        }
    }

    private fun onCreateWalletSuccess(event: CreateWalletSuccessEvent) {
        showOrHideLoading(false)
        navigator.openBackupWalletScreen(
            activityContext = this,
            wallet = event.wallet,
            isDecoyWallet = args.decoyPin.isNotEmpty()
        )
    }

    private fun setupViews() {
        binding.walletName.text = args.walletName
        val signers = viewModel.mapSigners()
        binding.configuration.bindWalletConfiguration(
            requireSigns = args.totalRequireSigns,
            totalSigns = signers.size
        )

        binding.walletType.text = args.walletType.toReadableString(this)
        binding.addressType.text = args.addressType.toReadableString(this)
        SignersViewBinder(binding.signersContainer, signers).bindItems()

        binding.btnContinue.setOnClickListener {
            viewModel.handleContinueEvent(
                walletName = args.walletName,
                walletType = args.walletType,
                addressType = args.addressType,
                totalRequireSigns = args.totalRequireSigns,
                signers = args.signers,
                decoyPin = args.decoyPin
            )
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    companion object {

        fun start(
            activityContext: Context,
            args: ReviewWalletArgs
        ) {
            activityContext.startActivity(
                Intent(activityContext, ReviewWalletActivity::class.java).apply {
                    putExtras(args.buildBundle())
                }
            )
        }
    }

}