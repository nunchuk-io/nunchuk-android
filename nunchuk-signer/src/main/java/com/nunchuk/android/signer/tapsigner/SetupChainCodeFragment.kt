package com.nunchuk.android.signer.tapsigner

import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.WindowCompat
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
import com.nunchuk.android.widget.util.heightExtended
import com.nunchuk.android.widget.util.setMaxLength
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

@AndroidEntryPoint
class SetupChainCodeFragment : BaseFragment<FragmentSetupChainCodeBinding>() {
    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    private val viewModel by viewModels<SetupChainCodeViewModel>()
    private val nfcViewModel by activityViewModels<NfcViewModel>()

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSetupChainCodeBinding {
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
    }

    private fun observer() {
        lifecycleScope.launchWhenStarted {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::handleState)
            }
        }
        lifecycleScope.launchWhenStarted {
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
                findNavController().navigate(R.id.changeNfcCvcFragment, ChangeNfcCvcFragment.buildArguments(binding.etChainCode.getEditText()))
            }
        }
    }

    private fun handleSetupSatscard(info: NfcScanInfo) {
        viewModel.setUpSatsCard(IsoDep.get(info.tag), nfcViewModel.inputCvc.orEmpty(), binding.etChainCode.getEditText())
    }

    private fun handleState(state: SetupChainCodeState) {
        binding.etChainCode.getEditTextView().setText(state.chainCode)
        binding.etChainCode.hideError()
    }

    private fun handleEvent(event: SetupChainCodeEvent) {
        when (event) {
            is SetupChainCodeEvent.NfcLoading -> showOrHideNfcLoading(event.isLoading)
            is SetupChainCodeEvent.SetupSatsCardSuccess -> {
                SatsCardActivity.navigate(requireActivity(), event.status, (activity as NfcSetupActivity).hasWallet)
                NcToastManager.scheduleShowMessage(getString(R.string.nc_slot_ready_deposit))
                requireActivity().finish()
            }
            is SetupChainCodeEvent.ShowError -> {
                if (nfcViewModel.handleNfcError(event.e).not()) showError(event.e?.message)
            }
        }
    }
}