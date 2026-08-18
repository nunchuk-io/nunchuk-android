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

package com.nunchuk.android.signer.components.jade

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.core.base.BaseCameraActivity
import com.nunchuk.android.core.base.ScannerViewComposer
import com.nunchuk.android.core.constants.NativeErrorCode
import com.nunchuk.android.core.domain.ParseQRCodeFromPhotoUseCase
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivityJadeQrUnlockBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Jade QR PIN unlock. Jade shows a PIN-server request as an animated QR; the app scans it, forwards
 * it to Blockstream, then shows the reply for Jade to scan. The device drives however many
 * exchanges it needs, so the screen toggles between scanning and showing until the user is done.
 */
@AndroidEntryPoint
class JadeQrUnlockActivity : BaseCameraActivity<ActivityJadeQrUnlockBinding>() {

    @Inject
    lateinit var parseQRCodeFromPhotoUseCase: ParseQRCodeFromPhotoUseCase

    private val viewModel: JadeQrUnlockViewModel by viewModels()

    override fun initializeBinding() = ActivityJadeQrUnlockBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestCameraPermissionOrExecuteAction()
        setLightStatusBar()
        binding.toolbar.setNavigationOnClickListener { finish() }
        setupComposeView()
        observer()
    }

    private fun setupComposeView() {
        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            JadeUnlockReplyContent(
                qrs = state.replyQrs,
                onScanNextClicked = viewModel::reset,
                onDoneClicked = ::finish,
            )
        }
    }

    private fun observer() {
        flowObserver(viewModel.state) { state ->
            val isShowingReply = state.replyQrs.isNotEmpty()
            binding.composeView.isVisible = isShowingReply
            // Stop decoding while the reply is on screen, otherwise the camera keeps feeding
            // fragments into a session that is already resolved.
            if (isShowingReply) scanner?.stopScanning() else scanner?.resumeScanning()

            binding.progressBar.isVisible = !isShowingReply && state.progress > 0.0
            binding.progressBar.progress = state.progress.roundToInt()
            binding.tvPercentage.isVisible = !isShowingReply && state.progress > 0.0
            binding.tvPercentage.text = "${state.progress.roundToInt()}%"
        }
        flowObserver(viewModel.event) { event ->
            when (event) {
                is JadeQrUnlockEvent.Loading -> if (event.isLoading) showLoading() else hideLoading()
                is JadeQrUnlockEvent.Error -> NCToastMessage(this).showError(
                    if (event.errorCode == NativeErrorCode.JADE_INVALID_PARAMETER) {
                        getString(R.string.nc_jade_qr_unlock_wrong_qr)
                    } else {
                        event.message
                    }
                )
            }
        }
    }

    override fun decodeQRCodeFromUri(uri: Uri) {
        lifecycleScope.launch {
            parseQRCodeFromPhotoUseCase(uri)
                .onSuccess { viewModel.onQrScanned(it) }
                .onFailure { NCToastMessage(this@JadeQrUnlockActivity).showError(it.message.orEmpty()) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.state.value.replyQrs.isEmpty()) scanner?.resumeScanning()
    }

    override fun onPause() {
        super.onPause()
        scanner?.stopScanning()
    }

    override fun onCameraPermissionGranted(fromUser: Boolean) {
        scanner?.startScanning(intent)
    }

    override fun scannerViewComposer() = ScannerViewComposer(
        btnTurnFlash = binding.scannerActionView.btnTurnFlash,
        btnSelectPhoto = binding.scannerActionView.btnSelectImage,
        btnScannerGoogle = binding.scannerActionView.btnGoogleScanner,
        previewView = binding.previewView,
        barcodeView = binding.barcodeView,
    )

    override fun onScannerResult(result: String) {
        viewModel.onQrScanned(result)
    }

    companion object {
        fun buildIntent(activityContext: Context) =
            Intent(activityContext, JadeQrUnlockActivity::class.java)
    }
}
