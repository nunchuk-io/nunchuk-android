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
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.core.base.BaseShareSaveFileActivity
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.util.navigateToSelectWallet
import com.nunchuk.android.core.wallet.WalletSecurityArgs
import com.nunchuk.android.core.wallet.WalletSecurityType
import com.nunchuk.android.model.BannerState
import com.nunchuk.android.nav.args.BackUpWalletArgs
import com.nunchuk.android.nav.args.BackUpWalletType
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.Failure
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.Success
import com.nunchuk.android.wallet.databinding.ActivityWalletBackupWalletBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BackupWalletActivity : BaseShareSaveFileActivity<ActivityWalletBackupWalletBinding>() {
    
    private var isShared: Boolean = false

    private val args: BackUpWalletArgs by lazy { BackUpWalletArgs.deserializeFrom(intent) }

    private val viewModel: BackupWalletViewModel by viewModels()

    override fun initializeBinding() = ActivityWalletBackupWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init(args.wallet)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToNextScreen(isBackupSuccessful = false)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (isShared) {
            navigateToNextScreen(isBackupSuccessful = true)
            isShared = false
        }
    }

    private fun setupViews() {
        if (args.isFromWalletDetails) {
            NCToastMessage(this).show(R.string.nc_wallet_has_been_created)
        }
        binding.btnBackup.setOnClickListener { showSaveShareOption() }
        binding.btnSkipBackup.setOnClickListener {
            navigateToNextScreen(isBackupSuccessful = false)
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun navigateToNextScreen(isBackupSuccessful: Boolean = false) {
        // Trigger DismissCurrentAlert event if backup is successful and from alert
        if (isBackupSuccessful && args.isFromAlert) {
            lifecycleScope.launch {
                pushEventManager.push(PushEvent.DismissCurrentAlert)
            }
        }
        
        if (args.backUpWalletType == BackUpWalletType.ASSISTED_CREATED) {
            setResult(RESULT_OK)
            finish()
        } else if (args.isDecoyWallet) {
            navigator.returnToMainScreen(this)
            navigator.openWalletSecuritySettingScreen(
                this, WalletSecurityArgs(
                    type = WalletSecurityType.CREATE_DECOY_SUCCESS,
                    quickWalletParam = args.quickWalletParam
                )
            )
        } else if (args.quickWalletParam != null) {
            navigateToSelectWallet(
                navigator = navigator,
                quickWalletParam = args.quickWalletParam
            )
        } else if (isBackupSuccessful) {
            // Handle banner state logic based on backup success
            val bannerState = viewModel.getBannerState()
            val hasHardwareOrAirgapSigner = viewModel.hasHardwareOrAirgapSigner()
            val walletId = viewModel.getWalletId() ?: args.wallet.id

            when (bannerState) {
                BannerState.BACKUP_AND_REGISTER -> {
                    if (hasHardwareOrAirgapSigner) {
                        // Update banner state to REGISTER_ONLY and open upload configuration screen
                        viewModel.updateBannerState(BannerState.REGISTER_ONLY)
                        navigator.openUploadConfigurationScreen(this, walletId)
                        finish()
                    } else {
                        // Remove banner state and go to wallet details
                        viewModel.removeBannerState()
                        navigator.returnToMainScreen(this)
                        navigator.openWalletDetailsScreen(this, walletId)
                    }
                }
                BannerState.BACKUP_ONLY -> {
                    // Remove banner state and go to wallet details
                    viewModel.removeBannerState()
                    navigator.returnToMainScreen(this)
                    navigator.openWalletDetailsScreen(this, walletId)
                }
                else -> {
                    // No banner state, just proceed normally
                    navigator.returnToMainScreen(this)
                    navigator.openWalletDetailsScreen(this, walletId)
                }
            }
        } else {
            navigator.returnToMainScreen(this)
            navigator.openWalletDetailsScreen(this, args.wallet.id)
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    override fun shareFile() {
        viewModel.handleBackupDescriptorEvent()
    }

    override fun saveFileToLocal() {
        viewModel.saveBSMSToLocal()
    }

    private fun handleEvent(event: BackupWalletEvent) {
        when (event) {
            is Success -> shareFile(event)
            is Failure -> NCToastMessage(this).showWarning(event.message)
            is BackupWalletEvent.SaveLocalFile -> {
                showSaveFileState(event.isSuccess)
                lifecycleScope.launch {
                    delay(1000L)
                    navigateToNextScreen(isBackupSuccessful = event.isSuccess)
                }
            }
        }
    }

    private fun shareFile(event: Success) {
        isShared = true
        controller.shareFile(event.filePath)
    }

    companion object {

        fun start(
            activityContext: Context,
            args: BackUpWalletArgs
        ) {
            activityContext.startActivity(
                buildIntent(
                    activityContext,
                    args
                )
            )
        }

        fun buildIntent(
            activityContext: Context,
            args: BackUpWalletArgs
        ) = args.buildIntent(activityContext).setClass(
            activityContext,
            BackupWalletActivity::class.java
        )
    }
}