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

package com.nunchuk.android.wallet.personal.components.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.google.zxing.client.android.Intents
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RecoverWalletType
import com.nunchuk.android.wallet.personal.databinding.ActivityImportWalletQrcodeBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecoverWalletQrCodeActivity : BaseActivity<ActivityImportWalletQrcodeBinding>() {

    private val viewModel: RecoverWalletQrCodeViewModel by viewModels()

    override fun initializeBinding() = ActivityImportWalletQrcodeBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun setupViews() {
        val barcodeViewIntent = intent
        barcodeViewIntent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE)
        binding.barcodeView.initializeFromIntent(barcodeViewIntent)
        binding.barcodeView.decodeContinuous { viewModel.updateQRCode(it.text, "") }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun handleEvent(event: RecoverWalletQrCodeEvent) {
        when (event) {
            is RecoverWalletQrCodeEvent.ImportQRCodeError -> onImportQRCodeError(event)
            is RecoverWalletQrCodeEvent.ImportQRCodeSuccess -> onImportQRCodeSuccess(event)
        }
    }

    private fun onImportQRCodeSuccess(event: RecoverWalletQrCodeEvent.ImportQRCodeSuccess) {
        hideLoading()
        navigator.openAddRecoverWalletScreen(
            this, RecoverWalletData(
                type = RecoverWalletType.QR_CODE,
                walletId = event.walletId
            )
        )
        finish()
    }

    private fun onImportQRCodeError(event: RecoverWalletQrCodeEvent.ImportQRCodeError) {
        hideLoading()
        NCToastMessage(this).showWarning(event.message)
    }

    override fun onResume() {
        super.onResume()
        binding.barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeView.pause()
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, RecoverWalletQrCodeActivity::class.java))
        }
    }

}

