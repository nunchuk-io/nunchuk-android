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

package com.nunchuk.android.signer.components.add

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.google.zxing.client.android.Intents
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.databinding.ActivityScanDynamicQrBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScanDynamicQRActivity : BaseActivity<ActivityScanDynamicQrBinding>() {

    private val viewModel: AddSignerViewModel by viewModels()

    override fun initializeBinding() = ActivityScanDynamicQrBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observer()
    }

    private fun observer() {
        viewModel.event.observe(this) {
            if (it is AddSignerEvent.ParseKeystoneSignerSuccess) {
                setResult(Activity.RESULT_OK, Intent().apply {
                    putParcelableArrayListExtra(PASSPORT_EXTRA_KEYS, ArrayList(it.signers))
                })
                finish()
            }
        }
    }

    private fun setupViews() {
        val barcodeViewIntent = intent
        barcodeViewIntent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE)
        binding.barcodeView.initializeFromIntent(barcodeViewIntent)
        binding.barcodeView.decodeContinuous { result ->
            viewModel.handAddPassportSigners(result.text)
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
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
        fun start(activityContext: Activity, requestCode: Int) {
            activityContext.startActivityForResult(Intent(activityContext, ScanDynamicQRActivity::class.java), requestCode)
        }
    }

}

