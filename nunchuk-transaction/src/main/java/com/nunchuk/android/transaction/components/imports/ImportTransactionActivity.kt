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

package com.nunchuk.android.transaction.components.imports

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.zxing.client.android.Intents
import com.nunchuk.android.core.base.BaseCameraActivity
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionError
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionSuccess
import com.nunchuk.android.transaction.databinding.ActivityImportTransactionBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class ImportTransactionActivity : BaseCameraActivity<ActivityImportTransactionBinding>() {

    private val args: ImportTransactionArgs by lazy { ImportTransactionArgs.deserializeFrom(intent) }

    private val viewModel: ImportTransactionViewModel by viewModels()

    override fun initializeBinding() = ActivityImportTransactionBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestCameraPermissionOrExecuteAction()
        setLightStatusBar()
        viewModel.init(args)
        setupViews()
        observeEvent()
    }

    override fun onCameraPermissionGranted(fromUser: Boolean) {
        if (fromUser) {
            recreate()
        }
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

    @SuppressLint("SetTextI18n")
    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        flowObserver(viewModel.uiState) {
            binding.progressBar.progress = it.progress.roundToInt()
            binding.tvPercentage.isVisible = it.progress > 0.0
            binding.tvPercentage.text = "${it.progress.roundToInt()}%"
        }
    }

    private fun setupViews() {
        val barcodeViewIntent = intent
        barcodeViewIntent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE)
        binding.barcodeView.initializeFromIntent(barcodeViewIntent)
        binding.barcodeView.decodeContinuous {
            viewModel.importTransactionViaQR(it.text)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun handleEvent(event: ImportTransactionEvent) {
        when (event) {
            is ImportTransactionError -> onImportTransactionError(event)
            is ImportTransactionSuccess -> onImportTransactionSuccess(event)
        }
    }

    private fun onImportTransactionSuccess(event: ImportTransactionSuccess) {
        val intent = Intent()
        if (event.transaction != null) {
            intent.apply {
                putExtra(GlobalResultKey.TRANSACTION_EXTRA, event.transaction)
            }
        }
        setResult(Activity.RESULT_OK, intent)
        hideLoading()
        finish()
    }

    private fun onImportTransactionError(event: ImportTransactionError) {
        hideLoading()
        if (args.isFinishWhenError) {
            NcToastManager.scheduleShowMessage(
                message = getString(R.string.nc_transaction_imported_failed) + event.message,
                type = NcToastManager.MessageType.WARING
            )
            finish()
        } else {
            NCToastMessage(this).showWarning(getString(R.string.nc_transaction_imported_failed) + event.message)
        }
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
        fun buildIntent(
            activityContext: Activity,
            walletId: String = "",
            masterFingerPrint: String = "",
            initEventId: String = "",
            isDummyTx: Boolean = false,
            isFinishWhenError: Boolean = false,
            isSignInFlow: Boolean = false
        ): Intent {
            return ImportTransactionArgs(
                walletId = walletId,
                masterFingerPrint = masterFingerPrint,
                initEventId = initEventId,
                isDummyTx = isDummyTx,
                isFinishWhenError = isFinishWhenError,
                isSignInFlow = isSignInFlow
            ).buildIntent(activityContext)
        }
    }
}
