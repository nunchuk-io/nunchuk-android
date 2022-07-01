package com.nunchuk.android.signer.nfc

import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentNfcChangeCvcBinding
import com.nunchuk.android.widget.NCEditTextView
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setMaxLength
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class ChangeNfcCvcFragment : BaseFragment<FragmentNfcChangeCvcBinding>() {
    private val nfcViewModel by activityViewModels<NfcViewModel>()
    private val viewModel by viewModels<ChangeNfcCvcViewModel>()

    private var isSharingBackUpKey: Boolean = false

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

    override fun onResume() {
        super.onResume()
        if (isSharingBackUpKey) {
            findNavController().navigate(R.id.addNfcNameFragment)
        }
    }

    private fun observer() {
        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_CHANGE_CVC }
                    .collect {
                        if (setUpAction == NfcSetupActivity.SETUP_NFC) {
                            viewModel.setUpCvc(
                                IsoDep.get(it.tag),
                                binding.editExistCvc.getEditText(),
                                binding.editNewCvc.getEditText()
                            )
                        } else if (setUpAction == NfcSetupActivity.CHANGE_CVC) {
                            viewModel.changeCvc(
                                IsoDep.get(it.tag),
                                binding.editExistCvc.getEditText(),
                                binding.editNewCvc.getEditText()
                            )
                        }
                        nfcViewModel.clearScanInfo()
                    }
            }
        }

        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { state ->
                    showOrHideLoading(state is ChangeNfcCvcEvent.Loading)
                    when (state) {
                        is ChangeNfcCvcEvent.ChangeCvcSuccess -> {
                            NCToastMessage(requireActivity()).show(getString(R.string.nc_cvc_has_been_changed))
                            requireActivity().finish()
                        }
                        is ChangeNfcCvcEvent.SetupCvcSuccess -> {
                            IntentSharingController.from(requireActivity()).shareFile(state.backupKeyPath)
                            isSharingBackUpKey = true
                            NCToastMessage(requireActivity()).show(getString(R.string.nc_cvc_has_been_changed))
                            NCToastMessage(requireActivity()).show(getString(R.string.nc_master_private_key_init))
                        }
                        is ChangeNfcCvcEvent.Error -> {
                            NCToastMessage(requireActivity()).showError(getString(R.string.nc_config_cvc_failed))
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun initViews() {
        binding.editExistCvc.makeMaskedInput()
        binding.editExistCvc.setMaxLength(MAX_CVC_LENGTH)
        binding.editNewCvc.makeMaskedInput()
        binding.editNewCvc.setMaxLength(MAX_CVC_LENGTH)
        binding.editConfirmCvc.makeMaskedInput()
        binding.editConfirmCvc.setMaxLength(MAX_CVC_LENGTH)
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.finish()
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
            (requireActivity() as BaseNfcActivity<*>).startNfcFlow(BaseNfcActivity.REQUEST_NFC_CHANGE_CVC)
        }
    }

    private fun isFillInput(ncEditTextView: NCEditTextView): Boolean {
        if (ncEditTextView.getEditText().length < 6) {
            ncEditTextView.setError(getString(R.string.nc_required_minimum_6_characters))
            return false
        }
        return true
    }

    private val setUpAction: Int
        get() = (requireActivity() as NfcSetupActivity).setUpAction

    companion object {
        private const val MAX_CVC_LENGTH = 32
    }
}