package com.nunchuk.android.signer.nfc

import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcState
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.util.MAX_CVC_LENGTH
import com.nunchuk.android.core.util.isValidCvc
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentNfcChangeCvcBinding
import com.nunchuk.android.widget.NCEditTextView
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setMaxLength
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class ChangeNfcCvcFragment : BaseFragment<FragmentNfcChangeCvcBinding>() {
    private val nfcViewModel by activityViewModels<NfcViewModel>()
    private val viewModel by viewModels<ChangeNfcCvcViewModel>()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNfcChangeCvcBinding {
        return FragmentNfcChangeCvcBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        registerEvents()
        observer()
    }

    private fun observer() {
        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_CHANGE_CVC }
                    .collect {
                        if (setUpAction == NfcSetupActivity.SETUP_NFC) {
                            val chainCode = arguments?.getString(EXTRA_CHAIN_CODE).orEmpty()
                            viewModel.setUpCvc(
                                IsoDep.get(it.tag),
                                binding.editExistCvc.getEditText(),
                                binding.editNewCvc.getEditText(),
                                chainCode
                            )
                        } else if (setUpAction == NfcSetupActivity.CHANGE_CVC) {
                            viewModel.changeCvc(
                                IsoDep.get(it.tag),
                                binding.editExistCvc.getEditText(),
                                binding.editNewCvc.getEditText(),
                                nfcViewModel.masterSignerId
                            )
                        }
                        nfcViewModel.clearScanInfo()
                    }
            }
        }

        lifecycleScope.launchWhenStarted {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                nfcViewModel.event.collect {
                    when (it) {
                        NfcState.WrongCvc -> handleWrongCvc()
                        NfcState.LimitCvcInput -> handleLimitCvcInput()
                    }
                    nfcViewModel.clearEvent()
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { state ->
                    showOrHideLoading(state is ChangeNfcCvcEvent.Loading, message = getString(R.string.nc_keep_holding_nfc))
                    when (state) {
                        is ChangeNfcCvcEvent.ChangeCvcSuccess -> {
                            NCToastMessage(requireActivity()).show(getString(R.string.nc_cvc_has_been_changed))
                            requireActivity().finish()
                        }
                        is ChangeNfcCvcEvent.SetupCvcSuccess -> {
                            IntentSharingController.from(requireActivity()).shareFile(state.backupKeyPath)
                            nfcViewModel.updateMasterSigner(state.masterSigner)
                            findNavController().apply {
                                popBackStack(findNavController().graph.startDestinationId, true)
                                navigate(R.id.nfcKeyRecoverInfoFragment)
                            }
                        }
                        is ChangeNfcCvcEvent.Error -> {
                            if (nfcViewModel.handleNfcError(state.e).not()) {
                                val message = state.e?.message ?: getString(R.string.nc_config_cvc_failed)
                                NCToastMessage(requireActivity()).showError(message)
                            }
                        }
                        else -> {}
                    }
                    viewModel.clearEvent()
                }
            }
        }
    }


    private fun handleLimitCvcInput() {
        binding.editExistCvc.setError(getString(R.string.nc_cvc_incorrect_3_times))
    }

    private fun handleWrongCvc() {
        binding.editExistCvc.setError(getString(R.string.nc_incorrect_cvc_please_try_again))
    }

    private fun initViews() {
        val isChangeCvcFlow = setUpAction == NfcSetupActivity.CHANGE_CVC
        binding.tvHint.isVisible = isChangeCvcFlow.not()
        if (isChangeCvcFlow) {
            binding.toolbarTitle.text = getString(R.string.nc_change_cvc)
            binding.btnContinue.text = getString(R.string.nc_confirm_change_cvc)
        } else {
            binding.toolbarTitle.text = getString(R.string.nc_set_up_cvc)
            binding.btnContinue.text = getString(R.string.nc_text_continue)
        }
        binding.editExistCvc.makeMaskedInput()
        binding.editExistCvc.setMaxLength(MAX_CVC_LENGTH)
        binding.editNewCvc.makeMaskedInput()
        binding.editNewCvc.setMaxLength(MAX_CVC_LENGTH)
        binding.editConfirmCvc.makeMaskedInput()
        binding.editConfirmCvc.setMaxLength(MAX_CVC_LENGTH)
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.btnContinue.setOnClickListener {
            binding.editExistCvc.hideError()
            binding.editNewCvc.hideError()
            binding.editConfirmCvc.hideError()
            if (!isFillInput(binding.editExistCvc) || !isFillInput(binding.editNewCvc) || !isFillInput(
                    binding.editConfirmCvc
                )
            ) return@setOnClickListener
            if (binding.editNewCvc.getEditText() != binding.editConfirmCvc.getEditText()) {
                binding.editConfirmCvc.setError(getString(R.string.nc_cvc_not_match))
                return@setOnClickListener
            }
            if (setUpAction == NfcSetupActivity.SETUP_NFC) {
                NCInfoDialog(requireActivity()).init(
                    message = getString(R.string.nc_set_up_nfc_hint),
                    cancelable = false,
                    onYesClick = {
                        (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_NFC_CHANGE_CVC)
                    }
                ).show()
            } else {
                (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_NFC_CHANGE_CVC)
            }
        }
    }

    private fun isFillInput(ncEditTextView: NCEditTextView): Boolean {
        if (ncEditTextView.getEditText().isValidCvc().not()) {
            ncEditTextView.setError(getString(R.string.nc_required_minimum_6_characters))
            return false
        }
        return true
    }

    private val setUpAction: Int
        get() = (requireActivity() as NfcSetupActivity).setUpAction

    companion object {
        private const val EXTRA_CHAIN_CODE = "EXTRA_CHAIN_CODE"

        fun buildArguments(chainCode: String): Bundle {
            return Bundle().apply {
                putString(EXTRA_CHAIN_CODE, chainCode)
            }
        }
    }
}