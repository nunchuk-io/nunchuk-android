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

package com.nunchuk.android.wallet.shared.components.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseCameraActivity
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.openSelectFileChooser
import com.nunchuk.android.wallet.shared.databinding.ActivityRecoverSharedWalletBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecoverSharedWalletActivity : BaseCameraActivity<ActivityRecoverSharedWalletBinding>() {

    override fun initializeBinding() = ActivityRecoverSharedWalletBinding.inflate(layoutInflater)

    private val viewModel: RecoverSharedWalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        flowObserver(viewModel.event) {
            handleEvent(it)
        }
    }

    private fun handleEvent(event: RecoverSharedWalletEvent) {
        when (event) {
            is RecoverSharedWalletEvent.RecoverSharedWalletSuccess -> {
                navigator.openAddRecoverSharedWalletScreen(this, event.wallet)
            }
            else -> Unit
        }
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnRecoverUsingBSMS.setOnClickListener {
            openSelectFileChooser(REQUEST_CODE)
        }
        binding.btnRecoverViaQrCode.setOnDebounceClickListener {
            requestCameraPermissionOrExecuteAction()
        }
    }

    override fun onCameraPermissionGranted(fromUser: Boolean) {
        openScanQRCodeScreen()
    }

    private fun openScanQRCodeScreen() {
        navigator.openRecoverWalletQRCodeScreen(this, true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            val file = intent?.data?.let {
                getFileFromUri(contentResolver, it, cacheDir)
            }
            file?.let {
                viewModel.parseWalletDescriptor(it.readText())
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 10000

        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, RecoverSharedWalletActivity::class.java))
        }
    }

}