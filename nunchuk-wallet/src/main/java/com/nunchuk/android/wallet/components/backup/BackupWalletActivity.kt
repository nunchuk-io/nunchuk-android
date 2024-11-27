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

package com.nunchuk.android.wallet.components.backup

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.wallet.WalletSecurityArgs
import com.nunchuk.android.core.wallet.WalletSecurityType
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.Failure
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.Success
import com.nunchuk.android.wallet.databinding.ActivityWalletBackupWalletBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BackupWalletActivity : BaseActivity<ActivityWalletBackupWalletBinding>() {

    private val controller: IntentSharingController by lazy { IntentSharingController.from(this) }

    private var isShared: Boolean = false

    private val args: BackupWalletArgs by lazy { BackupWalletArgs.deserializeFrom(intent) }

    private val viewModel: BackupWalletViewModel by viewModels()

    override fun initializeBinding() = ActivityWalletBackupWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init(args.wallet)
    }

    override fun onResume() {
        super.onResume()
        if (isShared) {
            navigateToNextScreen()
            isShared = false
        }
    }

    private fun setupViews() {
        NCToastMessage(this).show(R.string.nc_wallet_has_been_created)
        binding.btnBackup.setOnClickListener { viewModel.handleBackupDescriptorEvent() }
        binding.btnSkipBackup.setOnClickListener {
            navigateToNextScreen()
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun navigateToNextScreen() {
        if (args.isDecoyWallet) {
            navigator.returnToMainScreen(this)
            navigator.openWalletSecuritySettingScreen(
                this, WalletSecurityArgs(
                    type = WalletSecurityType.CREATE_DECOY_SUCCESS
                )
            )
        } else if (args.isQuickWallet) {
            finish()
        } else if (args.wallet.signers.size > 1) {
            navigator.returnToMainScreen(this)
            navigator.openWalletDetailsScreen(this, args.wallet.id)
        } else {
            navigator.returnToMainScreen(this)
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: BackupWalletEvent) {
        when (event) {
            is Success -> shareFile(event)
            is Failure -> NCToastMessage(this).showWarning(event.message)
        }
    }

    private fun shareFile(event: Success) {
        isShared = true
        controller.shareFile(event.filePath)
    }

    companion object {

        fun start(
            activityContext: Context,
            wallet: Wallet,
            isQuickWallet: Boolean,
            isDecoyWallet: Boolean
        ) {
            activityContext.startActivity(
                BackupWalletArgs(
                    wallet = wallet,
                    isQuickWallet = isQuickWallet,
                    isDecoyWallet = isDecoyWallet
                ).buildIntent(activityContext)
            )
        }
    }

}