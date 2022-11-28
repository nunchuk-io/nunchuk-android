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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.*
import com.nunchuk.android.signer.databinding.FragmentAddSignerBinding
import com.nunchuk.android.utils.parcelableArrayList
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.heightExtended
import com.nunchuk.android.widget.util.setMaxLength
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddAirgapSignerFragment : BaseFragment<FragmentAddSignerBinding>(),
    BottomSheetOptionListener {

    private val viewModel: AddAirgapSignerViewModel by viewModels()

    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openScanDynamicQRScreen()
            } else {
                showError(getString(R.string.nc_give_app_permission))
            }
        }

    private val importFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                viewModel.parseAirgapSigner(it)
            }
        }

    private val scanQrLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val keys = it.data?.parcelableArrayList<SingleSigner>(PASSPORT_EXTRA_KEYS).orEmpty()
                handleResult(keys)
            }
        }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAddSignerBinding {
        return FragmentAddSignerBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeEvent()
    }

    override fun onOptionClicked(option: SheetOption) {
        viewModel.signers.getOrNull(option.type)?.let {
            binding.signerSpec.getEditTextView().setText(it.descriptor)
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is AddAirgapSignerSuccessEvent -> openSignerInfo(it.singleSigner)
                InvalidAirgapSignerSpecEvent -> binding.signerSpec.setError(getString(R.string.nc_error_invalid_signer_spec))
                is AddAirgapSignerErrorEvent -> onAddAirSignerError(it.message)
                AirgapSignerNameRequiredEvent -> binding.signerName.setError(getString(R.string.nc_text_required))
                is LoadingEventAirgap -> showOrHideLoading(it.isLoading)
                is ParseKeystoneAirgapSignerSuccess -> handleResult(it.signers)
                is ParseKeystoneAirgapSigner -> openSignerSheet(it.signers)
                AddSameKey -> showError(getString(R.string.nc_error_add_same_key))
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
            fragment.show(childFragmentManager, "BottomSheetOption")
        }
    }

    private fun onAddAirSignerError(message: String) {
        hideLoading()
        showWarning(message)
    }

    private fun openSignerInfo(singleSigner: SingleSigner) {
        hideLoading()
        requireActivity().finish()
        if ((requireActivity() as AddAirgapSignerActivity).isMembershipFlow.not()) {
            navigator.openSignerInfoScreen(
                requireContext(),
                id = singleSigner.masterSignerId,
                masterFingerprint = singleSigner.masterFingerprint,
                name = singleSigner.name,
                type = singleSigner.type,
                derivationPath = singleSigner.derivationPath,
                justAdded = true
            )
        }
    }

    private fun setupViews() {
        val isMembershipFlow = (requireActivity() as AddAirgapSignerActivity).isMembershipFlow
        binding.signerName.setMaxLength(MAX_LENGTH)
        updateCounter(0)
        with(isMembershipFlow) {
            binding.signerName.isVisible = this.not()
            binding.signerNameLabel.isVisible = this.not()
            binding.signerNameCounter.isVisible = this.not()
        }
        binding.signerName.addTextChangedCallback {
            updateCounter(it.length)
        }

        binding.scanContainer.setOnClickListener {
            if (requestPermissionLauncher.checkCameraPermission(requireActivity())) {
                openScanDynamicQRScreen()
            }
        }
        binding.btnImportViaFile.setOnDebounceClickListener {
            importFileLauncher.launch("*/*")
        }
        binding.signerSpec.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_180))
        binding.addSigner.setOnClickListener {
            viewModel.handleAddAirgapSigner(
                signerName = binding.signerName.getEditText(),
                signerSpec = binding.signerSpec.getEditText(),
                isMembershipFlow = isMembershipFlow
            )
        }
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun openScanDynamicQRScreen() {
        scanQrLauncher.launch(ScanDynamicQRActivity.buildIntent(requireActivity()))
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
        SelectKeyBottomSheet.show(fragmentManager = childFragmentManager, keys)
            .setListener(onKeySelected)
    }

    private fun updateCounter(length: Int) {
        val counterValue = "$length/$MAX_LENGTH"
        binding.signerNameCounter.text = counterValue
    }

    companion object {
        private const val MAX_LENGTH = 20
    }
}

internal const val PASSPORT_EXTRA_KEYS = "PASSPORT_EXTRA_KEYS"