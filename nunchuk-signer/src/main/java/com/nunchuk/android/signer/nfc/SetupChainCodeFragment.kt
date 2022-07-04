package com.nunchuk.android.signer.nfc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.CHAIN_CODE_LENGTH
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentSetupChainCodeBinding
import com.nunchuk.android.widget.util.heightExtended
import com.nunchuk.android.widget.util.setMaxLength
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetupChainCodeFragment : BaseFragment<FragmentSetupChainCodeBinding>() {
    private val viewModel by viewModels<SetupChainCodeViewModel>()

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSetupChainCodeBinding {
        return FragmentSetupChainCodeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerEvents()
        initViews()
        observer()
    }

    private fun initViews() {
        binding.etChainCode.setMaxLength(CHAIN_CODE_LENGTH)
        binding.etChainCode.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_120))
        binding.radioAutomatic.isChecked = true
    }

    private fun observer() {
        lifecycleScope.launchWhenStarted {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::handleState)
            }
        }
    }

    private fun registerEvents() {
        val onRadioClickListener = View.OnClickListener {
            binding.radioAdvanced.isChecked = it.id == binding.radioAdvanced.id
            binding.radioAutomatic.isChecked = it.id == binding.radioAutomatic.id

            binding.btnGenerate.isVisible = binding.radioAdvanced.isChecked
            binding.etChainCode.isVisible = binding.radioAdvanced.isChecked
        }
        binding.radioAdvanced.setOnClickListener(onRadioClickListener)
        binding.radioAutomatic.setOnClickListener(onRadioClickListener)
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

            findNavController().navigate(R.id.changeNfcCvcFragment, ChangeNfcCvcFragment.buildArguments(binding.etChainCode.getEditText()))
        }
    }

    private fun handleState(state: SetupChainCodeState) {
        binding.etChainCode.getEditTextView().setText(state.chainCode)
        binding.etChainCode.hideError()
    }
}