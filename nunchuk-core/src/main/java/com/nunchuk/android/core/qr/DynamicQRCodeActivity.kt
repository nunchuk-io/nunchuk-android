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

package com.nunchuk.android.core.qr

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.core.databinding.ActivityDynamicQrBinding
import com.nunchuk.android.core.util.DELAY_DYNAMIC_QR
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DynamicQRCodeActivity : AppCompatActivity() {

    private val args: DynamicQRCodeArgs by lazy { DynamicQRCodeArgs.deserializeFrom(intent) }
    private val viewModel: DynamicQRCodeViewModel by viewModels()

    private lateinit var bitmaps: List<Bitmap>

    private lateinit var binding: ActivityDynamicQrBinding

    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityDynamicQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        flowObserver(viewModel.getWalletName(args.walletId)) {
            binding.toolbarTitle.text = it
        }
    }

    private fun setupViews() {
        bitmaps = args.values.mapNotNull(String::convertToQRCode)
        binding.btnDoneScan.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
        lifecycleScope.launch {
            repeat(Int.MAX_VALUE) {
                bindQrCodes()
                delay(DELAY_DYNAMIC_QR)
            }
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
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
        fun buildIntent(activityContext: Context, walletId: String, values: List<String>) =
            DynamicQRCodeArgs(walletId, values).buildIntent(
                activityContext
            )
    }

}

