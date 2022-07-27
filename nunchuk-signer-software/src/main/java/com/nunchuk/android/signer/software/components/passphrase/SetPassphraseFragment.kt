package com.nunchuk.android.signer.software.components.passphrase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showLoading
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.*
import com.nunchuk.android.signer.software.databinding.FragmentSetPassphraseBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.passwordEnabled
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetPassphraseFragment : BaseFragment<FragmentSetPassphraseBinding>() {

    private val viewModel: SetPassphraseViewModel by viewModels()

    private val args: SetPassphraseFragmentArgs by navArgs()

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSetPassphraseBinding {
        return FragmentSetPassphraseBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.init(args.mnemonic, args.signerName)
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
    }

    private fun handleEvent(event: SetPassphraseEvent) {
        when (event) {
            PassPhraseRequiredEvent -> binding.passphrase.setError(getString(R.string.nc_text_required))
            ConfirmPassPhraseRequiredEvent -> binding.confirmPassphrase.setError(getString(R.string.nc_text_required))
            ConfirmPassPhraseNotMatchedEvent -> binding.confirmPassphrase.setError(getString(R.string.nc_text_confirm_passphrase_not_matched))
            is CreateSoftwareSignerCompletedEvent -> onCreateSignerCompleted(event.masterSigner, event.skipPassphrase)
            is CreateSoftwareSignerErrorEvent -> onCreateSignerError(event)
            PassPhraseValidEvent -> removeValidationError()
            is LoadingEvent -> showLoading()
        }
    }

    private fun onCreateSignerError(event: CreateSoftwareSignerErrorEvent) {
        hideLoading()
        NCToastMessage(requireActivity()).showError(event.message)
    }

    private fun removeValidationError() {
        binding.passphrase.hideError()
        binding.confirmPassphrase.hideError()
    }

    private fun onCreateSignerCompleted(masterSigner: MasterSigner, skipPassphrase: Boolean) {
        hideLoading()
        navigator.openSignerInfoScreen(
            activityContext = requireActivity(),
            id = masterSigner.id,
            name = masterSigner.name,
            justAdded = true,
            type = masterSigner.type,
            setPassphrase = !skipPassphrase
        )
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.passphrase.passwordEnabled()
        binding.passphrase.addTextChangedCallback(viewModel::updatePassphrase)

        binding.confirmPassphrase.passwordEnabled()
        binding.confirmPassphrase.addTextChangedCallback(viewModel::updateConfirmPassphrase)
        binding.btnNoPassphrase.setOnClickListener { viewModel.skipPassphraseEvent() }
        binding.btnSetPassphrase.setOnClickListener { viewModel.confirmPassphraseEvent() }
    }
}