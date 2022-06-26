package com.nunchuk.android.signer.nfc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.signer.BaseNfcActivity
import com.nunchuk.android.signer.NfcViewModel
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentNfcAddNameKeyBinding
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setMaxLength
import kotlinx.coroutines.flow.filter

class AddNfcNameFragment : BaseFragment<FragmentNfcAddNameKeyBinding>() {
    private val nfcViewModel by activityViewModels<NfcViewModel>()

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
                        // TODO Hai
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
            showInputCvcDialog()
        }
    }

    private fun showInputCvcDialog() {
        NCInputDialog(requireActivity())
            .showDialog(
                title = "Enter CVC",
                onConfirmed = {
                    (requireActivity() as BaseNfcActivity<*>).startNfcFlow(BaseNfcActivity.REQUEST_NFC_ADD_KEY)
                },
                isMaskedInput = true
            )
    }

    companion object {
        private const val MAX_LENGTH = 20
    }
}