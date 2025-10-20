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

package com.nunchuk.android.wallet.components.upload

import android.content.Context
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.base.BaseWalletConfigActivity
import com.nunchuk.android.wallet.components.details.WalletDetailsActivity
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent.ExportColdcardSuccess
import com.nunchuk.android.wallet.databinding.ActivityWalletUploadConfigurationBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UploadConfigurationActivity : BaseWalletConfigActivity<ActivityWalletUploadConfigurationBinding>() {

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            goToWalletConfigScreen()
        }

    private val sharingController: IntentSharingController by lazy { IntentSharingController.from(this, launcher) }

    private val args: UploadConfigurationArgs by lazy { UploadConfigurationArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityWalletUploadConfigurationBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        sharedViewModel.init(args.walletId)
    }

    private fun setupViews() {
        binding.btnQRCode.setOnDebounceClickListener {
            showExportOptions()
        }
        binding.btnSkipUpload.setOnDebounceClickListener {
            ActivityManager.popUntil(WalletDetailsActivity::class.java)
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun handleSharedEvent(event: UploadConfigurationEvent) {
        super.handleSharedEvent(event)
        when (event) {
            is ExportColdcardSuccess -> {
                shareConfigurationFile(event.filePath)
            }
            is UploadConfigurationEvent.DoneScanQr -> {
                finish()
            }
            else -> {}
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        when (option.type) {
            SheetOptionType.TYPE_EXPORT_QR -> {
                val isMiniscriptWallet = sharedViewModel.getIsMiniscriptWallet()
                val wallet = sharedViewModel.getWallet()
                if (wallet != null) {
                    showExportQRTypeOption(wallet, isMiniscriptWallet)
                }
            }
            SheetOptionType.TYPE_EXPORT_FILE -> {
                showSaveShareOption()
            }
        }
    }

    fun showExportOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_QR,
                    resId = R.drawable.ic_qr,
                    stringId = R.string.nc_export_configuration_qr_code
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_FILE,
                    resId = R.drawable.ic_share,
                    stringId = R.string.nc_wallet_save_configuration
                ),
            )
        ).show(supportFragmentManager, "BottomSheetOption")
    }


    private fun shareConfigurationFile(filePath: String?) {
        if (filePath.isNullOrEmpty().not()) {
            sharingController.shareFile(filePath.orEmpty())
        }
    }

    private fun goToWalletConfigScreen() {
        navigator.openWalletConfigScreen(this, args.walletId)
        ActivityManager.popUntilRoot()
    }

    companion object {
        fun start(activityContext: Context, walletId: String, isOnChainFlow: Boolean = false) {
            activityContext.startActivity(UploadConfigurationArgs(walletId, isOnChainFlow).buildIntent(activityContext))
        }
    }
}