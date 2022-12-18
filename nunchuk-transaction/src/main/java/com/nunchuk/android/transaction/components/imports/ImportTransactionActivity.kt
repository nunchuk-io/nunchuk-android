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

package com.nunchuk.android.transaction.components.imports

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.google.zxing.client.android.Intents
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionError
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionSuccess
import com.nunchuk.android.transaction.databinding.ActivityImportTransactionBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ImportTransactionActivity : BaseActivity<ActivityImportTransactionBinding>() {

    private val args: ImportTransactionArgs by lazy { ImportTransactionArgs.deserializeFrom(intent) }

    private val viewModel: ImportTransactionViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                recreate()
            } else {
                navigateSystemPermissionSetting()
            }
        }

    private fun navigateSystemPermissionSetting() {
        NCWarningDialog(this).showDialog(
            message = getString(R.string.nc_give_app_permission),
            onYesClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                settingLauncher.launch(intent)
            }
        )
    }

    private val settingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (isPermissionGranted(Manifest.permission.CAMERA)) {
            recreate()
        } else {
            navigateSystemPermissionSetting()
        }
    }

    override fun initializeBinding() = ActivityImportTransactionBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher.checkCameraPermission(this)
        setLightStatusBar()
        viewModel.init(args)
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
        binding.barcodeView.decodeContinuous { viewModel.importTransactionViaQR(it.text) }
        binding.btnImportViaFile.setOnClickListener { openSelectFileChooser() }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun handleEvent(event: ImportTransactionEvent) {
        when (event) {
            is ImportTransactionError -> onImportTransactionError(event)
            is ImportTransactionSuccess -> onImportTransactionSuccess(event)
        }
    }

    private fun onImportTransactionSuccess(event: ImportTransactionSuccess) {
        if (event.signature.isNotEmpty()) {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(GlobalResultKey.SIGNATURE_EXTRA, event.signature)
            })
        }
        hideLoading()
        NcToastManager.scheduleShowMessage(getString(R.string.nc_transaction_imported))
        finish()
    }

    private fun onImportTransactionError(event: ImportTransactionError) {
        hideLoading()
        NCToastMessage(this).showWarning(getString(R.string.nc_transaction_imported_failed) + event.message)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == CHOOSE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            intent?.data?.let {
                getFileFromUri(contentResolver, it, cacheDir)
            }?.absolutePath?.let(viewModel::importTransactionViaFile)
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
            transactionOption: TransactionOption,
            masterFingerPrint: String = "",
            initEventId: String = "",
            signer: SingleSigner? = null,
        ): Intent {
            return ImportTransactionArgs(
                walletId = walletId,
                transactionOption = transactionOption,
                masterFingerPrint = masterFingerPrint,
                initEventId = initEventId,
                signer = signer
            ).buildIntent(activityContext)
        }
    }
}
