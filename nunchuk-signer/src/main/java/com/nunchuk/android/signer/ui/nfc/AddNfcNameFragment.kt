package com.nunchuk.android.signer.ui.nfc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentNfcAddNameKeyBinding
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setMaxLength

class AddNfcNameFragment : BaseFragment<FragmentNfcAddNameKeyBinding>() {
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
    }

    private fun initViews() {
        binding.signerName.setMaxLength(20)
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.signerName.addTextChangedCallback {
            binding.nameCounter.text = "${it.length}/${MAX_LENGTH}"
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

                },
                isMaskedInput = true
            )
    }

    companion object {
        private const val MAX_LENGTH = 20
    }
}