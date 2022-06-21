package com.nunchuk.android.signer.ui.nfc

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentNfcChangeCvcBinding
import com.nunchuk.android.widget.NCEditTextView
import com.nunchuk.android.widget.util.TextWatcherAdapter
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.passwordEnabled
import com.nunchuk.android.widget.util.passwordNumberEnabled

class ChangeNfcCvcFragment : BaseFragment<FragmentNfcChangeCvcBinding>() {
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
    }

    private fun initViews() {
        binding.editExistCvc.passwordNumberEnabled()
        binding.editNewCvc.passwordNumberEnabled()
        binding.editConfirmCvc.passwordNumberEnabled()
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.finish()
        }
        binding.editExistCvc.addTextChangedCallback {
            binding.editExistCvc.hideError()
        }
        binding.editNewCvc.addTextChangedCallback {
            binding.editNewCvc.hideError()
        }
        binding.editConfirmCvc.addTextChangedCallback {
            binding.editConfirmCvc.hideError()
        }
        binding.btnContinue.setOnClickListener {
            if (!isFillInput(binding.editExistCvc) || !isFillInput(binding.editNewCvc) || !isFillInput(binding.editConfirmCvc)) return@setOnClickListener
            if (binding.editNewCvc.getEditText() != binding.editConfirmCvc.getEditText()) {
                binding.editConfirmCvc.setError(getString(R.string.nc_cvc_not_match))
                return@setOnClickListener
            }
            // TODO request NFC
        }
    }

    private fun isFillInput(ncEditTextView: NCEditTextView) : Boolean {
        if (ncEditTextView.getEditText().isEmpty()) {
            ncEditTextView.setError(getString(R.string.nc_please_input_first))
            return false
        }
        return true
    }
}