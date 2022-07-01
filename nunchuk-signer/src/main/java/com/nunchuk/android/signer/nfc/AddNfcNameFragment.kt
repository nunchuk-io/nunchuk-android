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
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentNfcAddNameKeyBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setMaxLength
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class AddNfcNameFragment : BaseFragment<FragmentNfcAddNameKeyBinding>() {
    private val nfcViewModel by activityViewModels<NfcViewModel>()
    private val viewModel by viewModels<AddNfcNameViewModel>()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNfcAddNameKeyBinding {
        return FragmentNfcAddNameKeyBinding.inflate(inflater, container, false)
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
                nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_ADD_KEY }
                    .collect {
                        viewModel.addNameForNfcKey(
                            IsoDep.get(it.tag),
                            nfcViewModel.inputCvc.orEmpty(),
                            binding.signerName.getEditText()
                        )
                        nfcViewModel.clearScanInfo()
                    }
            }
        }

        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    showOrHideLoading(state is AddNfcNameState.Loading)
                    if (state is AddNfcNameState.Success) {
                        navigator.openSignerInfoScreen(
                            activityContext = requireActivity(),
                            id = state.masterSigner.id,
                            justAdded = true,
                            name = state.masterSigner.name,
                            type = state.masterSigner.type
                        )
                        requireActivity().finish()
                    } else if (state is AddNfcNameState.Error) {
                        NCToastMessage(requireActivity()).showError(getString(R.string.nc_create_signer_failed))
                    }
                }
            }
        }
    }

    private fun initViews() {
        binding.signerName.setMaxLength(20)
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.signerName.addTextChangedCallback {
            binding.nameCounter.text = "${it.length}/$MAX_LENGTH"
        }
        binding.btnContinue.setOnClickListener {
            if (binding.signerName.getEditText().isEmpty()) {
                binding.signerName.setError(getString(R.string.nc_text_required))
                return@setOnClickListener
            }
            binding.signerName.hideError()
            startNfcAddKeyFlow()
        }
    }

    private fun startNfcAddKeyFlow() {
        (requireActivity() as BaseNfcActivity<*>).startNfcFlow(BaseNfcActivity.REQUEST_NFC_ADD_KEY)
    }

    companion object {
        private const val MAX_LENGTH = 20
    }
}