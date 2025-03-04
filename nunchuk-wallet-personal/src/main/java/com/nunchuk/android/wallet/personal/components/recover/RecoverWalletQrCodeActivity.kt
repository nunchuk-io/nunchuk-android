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
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.zxing.client.android.Intents
import com.nunchuk.android.core.base.BaseCameraActivity
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RecoverWalletType
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.databinding.ActivityImportWalletQrcodeBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class RecoverWalletQrCodeActivity : BaseCameraActivity<ActivityImportWalletQrcodeBinding>() {

    private val viewModel: RecoverWalletQrCodeViewModel by viewModels()

    override fun initializeBinding() = ActivityImportWalletQrcodeBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        requestCameraPermissionOrExecuteAction()
        observeEvent()
    }

    override fun onCameraPermissionGranted(fromUser: Boolean) {
        setupViews()
    }

    private fun observeEvent() {
        flowObserver(viewModel.state) {
            binding.progressBar.progress = it.progress.roundToInt()
            binding.tvPercentage.isVisible = it.progress > 0.0
            binding.tvPercentage.text = "${it.progress.roundToInt()}%"
        }
        flowObserver(viewModel.event, collector = ::handleEvent)
    }

    private fun setupViews() {
        val barcodeViewIntent = intent
        barcodeViewIntent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE)
        binding.barcodeView.initializeFromIntent(barcodeViewIntent)
        binding.barcodeView.decodeContinuous { viewModel.updateQRCode(isParseOnly, it.text, "") }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun handleEvent(event: RecoverWalletQrCodeEvent) {
        when (event) {
            is RecoverWalletQrCodeEvent.ImportQRCodeError -> onImportQRCodeError()
            is RecoverWalletQrCodeEvent.ImportQRCodeSuccess -> onImportQRCodeSuccess(event)
            is RecoverWalletQrCodeEvent.ParseQRCodeFromPhotoSuccess -> viewModel.updateQRCode(
                isParseOnly, event.content, ""
            )
        }
    }

    private fun onImportQRCodeSuccess(event: RecoverWalletQrCodeEvent.ImportQRCodeSuccess) {
        hideLoading()
        if (isParseOnly) {
            setResult(RESULT_OK, Intent().apply {
                putExtra(GlobalResultKey.WALLET, event.wallet)
            })
        } else if (isCollaborativeWallet) {
            navigator.openAddRecoverSharedWalletScreen(this, event.wallet)
        } else {
            navigator.openAddRecoverWalletScreen(
                this, RecoverWalletData(
                    type = RecoverWalletType.QR_CODE,
                    walletId = event.wallet.id
                )
            )
        }
        finish()
    }

    override fun btnSelectPhoto(): ImageView {
        return binding.barcodeView.findViewById(R.id.btn_select_image)
    }

    override fun btnTurnFlash(): ImageView {
        return binding.barcodeView.findViewById(R.id.btn_turn_flash)
    }

    override fun decodeQRCodeFromUri(uri: Uri) {
        viewModel.decodeQRCodeFromUri(uri)
    }

    override fun torchState(isOn: Boolean) {
        if (isOn) {
            binding.barcodeView.setTorchOn()
        } else {
            binding.barcodeView.setTorchOff()
        }
    }

    private fun onImportQRCodeError() {
        hideLoading()
        NCToastMessage(this).showWarning(getString(R.string.nc_invalid_qr))
    }

    override fun onResume() {
        super.onResume()
        binding.barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeView.pause()
    }

    private val isCollaborativeWallet: Boolean
        get() = intent.getBooleanExtra(EXTRA_COLLABORATIVE_WALLET, false)

    private val isParseOnly: Boolean
        get() = intent.getBooleanExtra(EXTRA_PARSE_ONLY, false)

    companion object {
        private const val EXTRA_COLLABORATIVE_WALLET = "_a"
        private const val EXTRA_PARSE_ONLY = "_b"
        fun start(activityContext: Context, isCollaborativeWallet: Boolean) {
            activityContext.startActivity(
                buildIntent(
                    activityContext,
                    isCollaborativeWallet,
                    false
                )
            )
        }

        fun buildIntent(
            activityContext: Context,
            isCollaborativeWallet: Boolean,
            isParseOnly: Boolean,
        ): Intent {
            return Intent(activityContext, RecoverWalletQrCodeActivity::class.java).apply {
                putExtra(EXTRA_COLLABORATIVE_WALLET, isCollaborativeWallet)
                putExtra(EXTRA_PARSE_ONLY, isParseOnly)
            }
        }
    }

}

