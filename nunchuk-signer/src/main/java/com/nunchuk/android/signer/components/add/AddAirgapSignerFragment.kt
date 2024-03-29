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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.base.BaseCameraFragment
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.isRecommendedPath
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResult
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.AddAirgapSignerErrorEvent
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.AddAirgapSignerSuccessEvent
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.AddSameKey
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.AirgapSignerNameRequiredEvent
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.ErrorMk4TestNet
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.InvalidAirgapSignerSpecEvent
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.LoadingEventAirgap
import com.nunchuk.android.signer.components.add.AddAirgapSignerEvent.ParseKeystoneAirgapSignerSuccess
import com.nunchuk.android.signer.databinding.FragmentAddSignerBinding
import com.nunchuk.android.utils.parcelableArrayList
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.heightExtended
import com.nunchuk.android.widget.util.setMaxLength
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddAirgapSignerFragment : BaseCameraFragment<FragmentAddSignerBinding>(),
    BottomSheetOptionListener {
    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    private val viewModel: AddAirgapSignerViewModel by viewModels()

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
                handleResult(viewModel.updateSigners(keys))
            }
        }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAddSignerBinding {
        return FragmentAddSignerBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        membershipStepManager.updateStep(true)
    }

    override fun onDestroy() {
        membershipStepManager.updateStep(false)
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeEvent()
        viewModel.init(
            (activity as AddAirgapSignerActivity).groupId,
            (activity as AddAirgapSignerActivity).isMembershipFlow
        )
    }

    override fun onOptionClicked(option: SheetOption) {
        viewModel.signers.getOrNull(option.type)?.let {
            binding.signerSpec.getEditTextView().setText(it.descriptor)
        }
    }

    override fun onCameraPermissionGranted(fromUser: Boolean) {
        openScanDynamicQRScreen()
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
                AddSameKey -> showError(getString(R.string.nc_error_add_same_key))
                ErrorMk4TestNet -> NCInfoDialog(requireActivity())
                    .showDialog(
                        title = getString(R.string.nc_invalid_network),
                        message = getString(R.string.nc_error_device_in_testnet_msg)
                    )

                AddAirgapSignerEvent.NewIndexNotMatchException -> {
                    requireActivity().apply {
                        setResult(GlobalResult.RESULT_INDEX_NOT_MATCH)
                        finish()
                    }
                }
                AddAirgapSignerEvent.XfpNotMatchException -> showError(getString(R.string.nc_airgap_xfp_does_not_match))
            }
        }
    }

    private fun onAddAirSignerError(message: String) {
        hideLoading()
        showError(message)
    }

    private fun openSignerInfo(singleSigner: SingleSigner) {
        hideLoading()
        requireActivity().setResult(Activity.RESULT_OK)
        requireActivity().finish()
        if ((requireActivity() as AddAirgapSignerActivity).isMembershipFlow.not()) {
            navigator.openSignerInfoScreen(
                requireContext(),
                isMasterSigner = singleSigner.hasMasterSigner,
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
        val signerTag = (requireActivity() as AddAirgapSignerActivity).signerTag
        val xfp = (requireActivity() as AddAirgapSignerActivity).xfp
        val newIndex = (requireActivity() as AddAirgapSignerActivity).newIndex
        binding.signerName.setMaxLength(MAX_LENGTH)
        updateCounter(0)
        with(isMembershipFlow) {
            binding.signerName.isVisible = this.not()
            binding.signerNameLabel.isVisible = this.not()
            binding.signerNameCounter.isVisible = this.not()
            binding.toolbarTitle.isVisible = this
        }
        if (isMembershipFlow) {
            binding.toolbarTitle.textSize = 12f
            binding.toolbarTitle.text = getString(
                R.string.nc_estimate_remain_time,
                membershipStepManager.remainingTime.value
            )
        }
        binding.signerName.addTextChangedCallback {
            updateCounter(it.length)
        }

        binding.scanContainer.setOnClickListener {
            requestCameraPermissionOrExecuteAction()
        }
        binding.btnImportViaFile.setOnDebounceClickListener {
            importFileLauncher.launch("*/*")
        }
        binding.signerSpec.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_180))
        binding.addSigner.setOnClickListener {
            viewModel.handleAddAirgapSigner(
                signerName = binding.signerName.getEditText(),
                signerSpec = binding.signerSpec.getEditText(),
                isMembershipFlow = isMembershipFlow,
                signerTag = signerTag,
                xfp = xfp,
                newIndex = newIndex,
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
                showSelectKeysDialog(keys)
            }
        }
    }

    private fun bindKey(key: SingleSigner) {
        binding.signerSpec.getEditTextView().setText(key.descriptor)
    }

    private fun showSelectKeysDialog(
        signers: List<SingleSigner>
    ) {
        val fragment = BottomSheetOption.newInstance(signers.mapIndexed { index, singleSigner ->
            SheetOption(
                type = index,
                label = if (singleSigner.derivationPath.isRecommendedPath) "${singleSigner.derivationPath} (${getString(R.string.nc_recommended_for_multisig)})" else singleSigner.derivationPath
            )
        }, title = getString(R.string.nc_signer_select_key_dialog_title))
        fragment.show(childFragmentManager, "BottomSheetOption")
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