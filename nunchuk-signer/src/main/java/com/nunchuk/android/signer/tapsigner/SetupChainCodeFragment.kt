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

package com.nunchuk.android.signer.tapsigner

import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcScanInfo
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.util.CHAIN_CODE_LENGTH
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentSetupChainCodeBinding
import com.nunchuk.android.signer.satscard.SatsCardActivity
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.util.heightExtended
import com.nunchuk.android.widget.util.setMaxLength
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SetupChainCodeFragment : BaseFragment<FragmentSetupChainCodeBinding>() {
    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    private val viewModel by viewModels<SetupChainCodeViewModel>()
    private val nfcViewModel by activityViewModels<NfcViewModel>()
    private val selectedBackground = R.drawable.nc_rounded_12dp_stroke_primary_background
    private val unselectedBackground = R.drawable.nc_rounded_12dp_stroke_border_background

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSetupChainCodeBinding {
        return FragmentSetupChainCodeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        membershipStepManager.updateStep(true)
        registerEvents()
        initViews()
        observer()
    }

    override fun onDestroyView() {
        membershipStepManager.updateStep(false)
        super.onDestroyView()
    }

    private fun initViews() {
        binding.etChainCode.setMaxLength(CHAIN_CODE_LENGTH)
        binding.etChainCode.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_120))
        if ((activity as NfcSetupActivity).fromMembershipFlow) {
            binding.tvTitle.text = if (membershipStepManager.remainingTime.value <= 0) "" else
                getString(
                    R.string.nc_estimate_remain_time,
                    membershipStepManager.remainingTime.value
                )
        } else {
            binding.tvTitle.text = ""
        }
        binding.cardAutomatic.setBackgroundResource(selectedBackground)
        binding.cardAdvanced.setBackgroundResource(unselectedBackground)
    }

    private fun observer() {
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::handleState)
            }
        }
        lifecycleScope.launch {
            nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_SATSCARD_SETUP }
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect(::handleSetupSatscard)
        }
        flowObserver(viewModel.event, ::handleEvent)
    }

    private fun registerEvents() {
        val onRadioClickListener = View.OnClickListener {
            binding.radioAdvanced.isChecked = it.id == binding.cardAdvanced.id
            binding.radioAutomatic.isChecked = it.id == binding.cardAutomatic.id
            binding.cardAdvanced.setBackgroundResource(if (it.id == binding.cardAdvanced.id) selectedBackground else unselectedBackground)
            binding.cardAutomatic.setBackgroundResource(if (it.id == binding.cardAutomatic.id) selectedBackground else unselectedBackground)
        }
        val onCheckChangeListener = CompoundButton.OnCheckedChangeListener { _, _ ->
            binding.btnGenerate.isVisible = binding.radioAdvanced.isChecked
            binding.etChainCode.isVisible = binding.radioAdvanced.isChecked
        }
        binding.radioAdvanced.setOnCheckedChangeListener(onCheckChangeListener)
        binding.radioAutomatic.setOnCheckedChangeListener(onCheckChangeListener)
        binding.cardAdvanced.setOnClickListener(onRadioClickListener)
        binding.cardAutomatic.setOnClickListener(onRadioClickListener)
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.btnGenerate.setOnClickListener {
            viewModel.generateChainCode()
        }
        binding.btnContinue.setOnClickListener {
            if (binding.radioAdvanced.isChecked) {
                if (!viewModel.isValidChainCode(binding.etChainCode.getEditText())) {
                    binding.etChainCode.setError(getString(R.string.nc_invalid_chain_code))
                    return@setOnClickListener
                } else {
                    binding.etChainCode.hideError()
                }
            }

            if ((activity as NfcSetupActivity).setUpAction == NfcSetupActivity.SETUP_SATSCARD) {
                (activity as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_SATSCARD_SETUP)
            } else {
                findNavController().navigate(
                    R.id.changeNfcCvcFragment,
                    ChangeNfcCvcFragment.buildArguments(binding.etChainCode.getEditText())
                )
            }
        }
    }

    private fun handleSetupSatscard(info: NfcScanInfo) {
        viewModel.setUpSatsCard(
            IsoDep.get(info.tag),
            nfcViewModel.inputCvc.orEmpty(),
            binding.etChainCode.getEditText(),
            requireActivity().intent.parcelable(NfcSetupActivity.EXTRA_SATSCARD_SLOT)
        )
    }

    private fun handleState(state: SetupChainCodeState) {
        binding.etChainCode.getEditTextView().setText(state.chainCode)
        binding.etChainCode.hideError()
    }

    private fun handleEvent(event: SetupChainCodeEvent) {
        when (event) {
            is SetupChainCodeEvent.NfcLoading -> showOrHideNfcLoading(event.isLoading)
            is SetupChainCodeEvent.SetupSatsCardSuccess -> {
                SatsCardActivity.navigate(
                    requireActivity(),
                    event.status,
                    (activity as NfcSetupActivity).hasWallet
                )
                NcToastManager.scheduleShowMessage(getString(R.string.nc_slot_ready_deposit))
                requireActivity().finish()
            }

            is SetupChainCodeEvent.ShowError -> {
                if (nfcViewModel.handleNfcError(event.e).not()) showError(event.e?.message)
            }
        }
    }
}