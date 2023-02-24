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

package com.nunchuk.android.transaction.components.export

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.util.*
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.export.ExportTransactionEvent.*
import com.nunchuk.android.transaction.databinding.ActivityExportTransactionBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExportTransactionActivity : BaseActivity<ActivityExportTransactionBinding>() {

    private val controller: IntentSharingController by lazy { IntentSharingController.from(this) }

    private val args: ExportTransactionArgs by lazy { ExportTransactionArgs.deserializeFrom(intent) }

    private lateinit var bitmaps: List<Bitmap>

    private var index = 0

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK, it.data)
                finish()
            }
        }

    private val viewModel: ExportTransactionViewModel by viewModels()

    private var showQrJob: Job? = null

    override fun initializeBinding() = ActivityExportTransactionBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init(args)
    }

    private fun bindQrCodes() {
        calculateIndex()
        binding.qrCode.setImageBitmap(bitmaps[index])
    }

    private fun calculateIndex() {
        index++
        if (index >= bitmaps.size) {
            index = 0
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun setupViews() {
        val densities = listOf(LOW_DENSITY, MEDIUM_DENSITY, HIGH_DENSITY)
        binding.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.setQrDensity(densities[value.toInt()])
            }
        }

        binding.toolbarTitle.text = getText(R.string.nc_export_transaction_qr)
        binding.btnExportAsFile.setOnClickListener {
            viewModel.exportTransactionToFile()
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnImportSignature.setOnDebounceClickListener {
            openImportTransactionScreen()
        }
    }

    private fun handleState(state: ExportTransactionState) {
        binding.slider.value = state.density.densityToLevel()
        if (state.qrCodeBitmap.isNotEmpty()) {
            bitmaps = state.qrCodeBitmap
            showQrJob?.cancel()
            showQrJob = lifecycleScope.launch {
                repeat(Int.MAX_VALUE) {
                    bindQrCodes()
                    delay(DELAY_DYNAMIC_QR)
                }
            }
        }
    }

    private fun handleEvent(event: ExportTransactionEvent) {
        when (event) {
            is ExportTransactionError -> {
                hideLoading()
                NCToastMessage(this).showError(event.message)
            }
            is ExportToFileSuccess -> {
                hideLoading()
                shareTransactionFile(event.filePath)
            }
            LoadingEvent -> showLoading()
        }
    }

    private fun shareTransactionFile(filePath: String) {
        controller.shareFile(filePath)
    }

    private fun openImportTransactionScreen() {
        navigator.openImportTransactionScreen(
            launcher = launcher,
            activityContext = this,
            walletId = args.walletId,
            masterFingerPrint = args.masterFingerPrint,
            initEventId = args.initEventId,
            isDummyTx = args.isDummyTx,
            isFinishWhenError = true
        )
    }

    companion object {

        fun buildIntent(
            activityContext: Activity,
            walletId: String,
            txId: String,
            txToSign: String = "",
            initEventId: String = "",
            masterFingerPrint: String = "",
            isDummyTx: Boolean = false
        ): Intent {
            return ExportTransactionArgs(
                walletId = walletId,
                txId = txId,
                txToSign = txToSign,
                initEventId = initEventId,
                masterFingerPrint = masterFingerPrint,
                isDummyTx = isDummyTx
            ).buildIntent(activityContext)
        }
    }
}

