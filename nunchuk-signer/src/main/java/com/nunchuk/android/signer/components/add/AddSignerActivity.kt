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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.add.AddSignerEvent.*
import com.nunchuk.android.signer.databinding.ActivityAddSignerBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddSignerActivity : BaseActivity<ActivityAddSignerBinding>(), BottomSheetOptionListener {

    private val viewModel: AddSignerViewModel by viewModels()

    private val launcher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            viewModel.parseAirgapSigner(it)
        }
    }

    override fun initializeBinding() = ActivityAddSignerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    override fun onOptionClicked(option: SheetOption) {
        viewModel.signers.getOrNull(option.type)?.let {
            binding.signerSpec.getEditTextView().setText(it.descriptor)
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(this) {
            when (it) {
                is AddSignerSuccessEvent -> openSignerInfo(it.singleSigner)
                InvalidSignerSpecEvent -> binding.signerSpec.setError(getString(R.string.nc_error_invalid_signer_spec))
                is AddSignerErrorEvent -> onAddAirSignerError(it.message)
                SignerNameRequiredEvent -> binding.signerName.setError(getString(R.string.nc_text_required))
                is LoadingEvent -> showOrHideLoading(it.isLoading)
                is ParseKeystoneSignerSuccess -> handleResult(it.signers)
                is ParseKeystoneSigner -> openSignerSheet(it.signers)
            }
        }
    }

    private fun openSignerSheet(signers: List<SingleSigner>) {
        if (signers.isNotEmpty()) {
            val fragment = BottomSheetOption.newInstance(signers.mapIndexed { index, singleSigner ->
                SheetOption(
                    type = index,
                    label = singleSigner.derivationPath
                )
            }, title = getString(R.string.nc_signer_select_key_dialog_title))
            fragment.show(supportFragmentManager, "BottomSheetOption")
        }
    }

    private fun onAddAirSignerError(message: String) {
        hideLoading()
        NCToastMessage(this).showWarning(message)
    }

    private fun openSignerInfo(singleSigner: SingleSigner) {
        hideLoading()
        finish()
        navigator.openSignerInfoScreen(
            this,
            id = singleSigner.masterSignerId,
            masterFingerprint = singleSigner.masterFingerprint,
            name = singleSigner.name,
            type = singleSigner.type,
            derivationPath = singleSigner.derivationPath,
            justAdded = true
        )
    }

    private fun setupViews() {
        binding.signerName.setMaxLength(MAX_LENGTH)
        updateCounter(0)
        binding.signerName.addTextChangedCallback {
            updateCounter(it.length)
        }

        binding.scanContainer.setOnClickListener { openScanDynamicQRScreen() }
        binding.btnImportViaFile.setOnDebounceClickListener {
            launcher.launch("*/*")
        }
        binding.signerSpec.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_180))
        binding.addSigner.setOnClickListener {
            viewModel.handleAddSigner(
                binding.signerName.getEditText(), binding.signerSpec.getEditText()
            )
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun openScanDynamicQRScreen() {
        ScanDynamicQRActivity.start(this, PASSPORT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PASSPORT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val keys =
                    data?.getParcelableArrayListExtra<SingleSigner>(PASSPORT_EXTRA_KEYS).orEmpty()
                handleResult(keys)
            }
        }
    }

    private fun handleResult(keys: List<SingleSigner>) {
        if (keys.isNotEmpty()) {
            if (keys.size == 1) {
                bindKey(keys.first())
            } else {
                showSelectKeysDialog(keys, ::bindKey)
            }
        }
    }

    private fun bindKey(key: SingleSigner) {
        binding.signerSpec.getEditTextView().setText(key.descriptor)
    }

    private fun showSelectKeysDialog(
        keys: List<SingleSigner>, onKeySelected: (SingleSigner) -> Unit
    ) {
        SelectKeyBottomSheet.show(fragmentManager = supportFragmentManager, keys)
            .setListener(onKeySelected)
    }

    private fun updateCounter(length: Int) {
        val counterValue = "$length/$MAX_LENGTH"
        binding.signerNameCounter.text = counterValue
    }

    companion object {
        private const val MAX_LENGTH = 20

        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, AddSignerActivity::class.java))
        }
    }
}

internal const val PASSPORT_REQUEST_CODE = 0x1024
internal const val PASSPORT_EXTRA_KEYS = "PASSPORT_EXTRA_KEYS"