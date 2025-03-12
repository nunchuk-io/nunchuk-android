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

package com.nunchuk.android.signer.components.add

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.core.base.BaseCameraActivity
import com.nunchuk.android.core.base.ScannerViewComposer
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivityScanDynamicQrBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class ScanDynamicQRActivity : BaseCameraActivity<ActivityScanDynamicQrBinding>() {

    private val viewModel: AddAirgapSignerViewModel by viewModels()
    private val scanDynamicQRViewModel: ScanDynamicQRViewModel by viewModels()
    private val isJoinGroupWalletFlow: Boolean by lazy {
        intent.getBooleanExtra(
            IS_JOIN_GROUP_WALLET_FLOW,
            false
        )
    }

    override fun decodeQRCodeFromUri(uri: Uri) {
        scanDynamicQRViewModel.decodeQRCodeFromUri(uri)
    }

    override fun initializeBinding() = ActivityScanDynamicQrBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestCameraPermissionOrExecuteAction()
        setLightStatusBar()
        setupViews()
        observer()
    }

    private fun observer() {
        viewModel.event.observe(this) {
            if (it is AddAirgapSignerEvent.ParseKeystoneAirgapSignerSuccess) {
                setResult(Activity.RESULT_OK, Intent().apply {
                    putParcelableArrayListExtra(PASSPORT_EXTRA_KEYS, ArrayList(it.signers))
                })
                finish()
            }
        }
        flowObserver(viewModel.uiState) {
            binding.progressBar.isVisible = it.progress > 0.0
            binding.progressBar.progress = it.progress.roundToInt()
            binding.tvPercentage.isVisible = it.progress > 0.0
            binding.tvPercentage.text = "${it.progress.roundToInt()}%"
        }
        flowObserver(scanDynamicQRViewModel.event) {
            when (it) {
                is ScanDynamicQREvent.JoinGroupWalletSuccess -> {
                    navigator.openFreeGroupWalletScreen(this, groupId = it.groupSandbox.id)
                    finish()
                }

                is ScanDynamicQREvent.Error -> {
                    NCToastMessage(this).showInfo(getString(R.string.nc_unable_access_link))
                }

                is ScanDynamicQREvent.ParseQRCodeSuccess -> {
                    if (isJoinGroupWalletFlow) {
                        scanDynamicQRViewModel.handleJoinGroupWallet(it.content)
                    } else {
                        viewModel.handAddPassportSigners(it.content)
                    }
                }
            }
        }
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        scanner?.resumeScanning()
    }

    override fun onPause() {
        super.onPause()
        scanner?.stopScanning()
    }

    override fun onCameraPermissionGranted(fromUser: Boolean) {
        scanner?.startScanning(intent)
    }

    override fun scannerViewComposer(): ScannerViewComposer? {
        return ScannerViewComposer(
            btnTurnFlash = binding.scannerActionView.btnTurnFlash,
            btnSelectPhoto = binding.scannerActionView.btnSelectImage,
            btnScannerGoogle = binding.scannerActionView.btnGoogleScanner,
            previewView = binding.previewView,
            barcodeView = binding.barcodeView
        )
    }

    override fun onScannerResult(result: String) {
        if (isJoinGroupWalletFlow) {
            scanDynamicQRViewModel.handleJoinGroupWallet(result)
        } else {
            viewModel.handAddPassportSigners(result)
        }
    }

    companion object {
        private const val IS_JOIN_GROUP_WALLET_FLOW = "is_join_group_wallet_flow"
        fun buildIntent(
            activityContext: Context,
            isJoinGroupWalletFlow: Boolean = false,
        ): Intent {
            val intent = Intent(activityContext, ScanDynamicQRActivity::class.java).apply {
                putExtra(IS_JOIN_GROUP_WALLET_FLOW, isJoinGroupWalletFlow)
            }
            return intent
        }
    }
}

