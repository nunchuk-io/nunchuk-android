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
import com.nunchuk.android.signer.databinding.FragmentNfcChangeCvcBinding
import com.nunchuk.android.widget.NCEditTextView
import com.nunchuk.android.widget.util.passwordDisabled
import com.nunchuk.android.widget.util.passwordEnabled
import com.nunchuk.android.widget.util.setMaxLength
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class ChangeNfcCvcFragment : BaseFragment<FragmentNfcChangeCvcBinding>() {
    private val nfcViewModel by activityViewModels<NfcViewModel>()

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
                        // TODO Hai
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
            if (!isFillInput(binding.editExistCvc) || !isFillInput(binding.editNewCvc) || !isFillInput(
                    binding.editConfirmCvc
                )
            ) return@setOnClickListener
            if (binding.editNewCvc.getEditText() != binding.editConfirmCvc.getEditText()) {
                binding.editConfirmCvc.setError(getString(R.string.nc_cvc_not_match))
                return@setOnClickListener
            }
            binding.editExistCvc.hideError()
            binding.editNewCvc.hideError()
            binding.editConfirmCvc.hideError()
            (requireActivity() as BaseNfcActivity<*>).startNfcFlow(BaseNfcActivity.REQUEST_NFC_CHANGE_CVC)
        }
    }

    private fun isFillInput(ncEditTextView: NCEditTextView): Boolean {
        if (ncEditTextView.getEditText().isEmpty()) {
            ncEditTextView.setError(getString(R.string.nc_text_required))
            return false
        }
        return true
    }

    companion object {
        private const val MAX_CVC_LENGTH = 32
    }
}