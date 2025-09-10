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

package com.nunchuk.android.core.qr

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.core.base.BaseShareSaveFileActivity
import com.nunchuk.android.core.databinding.ActivityDynamicQrBinding
import com.nunchuk.android.core.util.DELAY_DYNAMIC_QR
import com.nunchuk.android.core.util.HIGH_DENSITY
import com.nunchuk.android.core.util.LOW_DENSITY
import com.nunchuk.android.core.util.MEDIUM_DENSITY
import com.nunchuk.android.core.util.ULTRA_DENSITY
import com.nunchuk.android.core.util.densityToLevel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.ExportWalletQRCodeType
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DynamicQRCodeActivity : BaseShareSaveFileActivity<ActivityDynamicQrBinding>() {

    private val viewModel: DynamicQRCodeViewModel by viewModels()

    private lateinit var bitmaps: List<Bitmap>

    override fun initializeBinding(): ActivityDynamicQrBinding {
        return ActivityDynamicQrBinding.inflate(layoutInflater)
    }

    private var index = 0

    private var showQrJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityDynamicQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        flowObserver(viewModel.state) {
            binding.toolbarTitle.text = it.name
            binding.slider.value = it.density.densityToLevel()
            bitmaps = it.bitmaps
            showQr()
        }
        flowObserver(viewModel.event) {
            when (it) {
                is DynamicQRCodeEvent.Error -> {
                    NCToastMessage(this).showError(it.message)
                }

                is DynamicQRCodeEvent.SavePDFSuccess -> {
                    controller.shareFile(it.path)
                }

                is DynamicQRCodeEvent.SaveLocalFile -> showSaveFileState(it.isSuccess)
            }
        }
    }

    private fun setupViews() {
        binding.btnDoneScan.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        val densities = listOf(LOW_DENSITY, MEDIUM_DENSITY, HIGH_DENSITY, ULTRA_DENSITY)
        binding.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                showQrJob?.cancel()
                viewModel.setQrDensity(densities[value.toInt()])
            }
        }

        binding.btnSavePdf.setOnClickListener {
            showSaveShareOption()
        }
        
        // Hide density controls for DESCRIPTOR_QR type
        if (viewModel.type == ExportWalletQRCodeType.DESCRIPTOR_QR) {
            binding.groupDensity.visibility = android.view.View.GONE
        }
    }

    override fun saveFileToLocal() {
        viewModel.saveBitmapToPDF(bitmaps, isSaveLocalFile = true)
    }

    override fun shareFile() {
        viewModel.saveBitmapToPDF(bitmaps, isSaveLocalFile = false)
    }

    private fun showQr() {
        if (bitmaps.isEmpty()) return
        showQrJob?.cancel()
        showQrJob = lifecycleScope.launch {
            repeat(Int.MAX_VALUE) {
                bindQrCodes()
                delay(DELAY_DYNAMIC_QR)
            }
        }
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

    companion object {
        fun buildIntent(activityContext: Context, walletId: String, qrCodeType: Int) =
            DynamicQRCodeArgs(walletId = walletId, qrCodeType = qrCodeType).buildIntent(
                activityContext
            )
    }

}

