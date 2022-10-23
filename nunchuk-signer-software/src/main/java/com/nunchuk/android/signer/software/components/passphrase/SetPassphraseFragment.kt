package com.nunchuk.android.signer.software.components.passphrase

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.signer.PrimaryKeyFlow.isPrimaryKeyFlow
import com.nunchuk.android.core.signer.PrimaryKeyFlow.isReplaceFlow
import com.nunchuk.android.core.signer.PrimaryKeyFlow.isSignUpFlow
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showLoading
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.*
import com.nunchuk.android.signer.software.databinding.FragmentSetPassphraseBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.addTextChangedCallback
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetPassphraseFragment : BaseFragment<FragmentSetPassphraseBinding>() {

    private val viewModel: SetPassphraseViewModel by viewModels()

    private val args: SetPassphraseFragmentArgs by navArgs()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSetPassphraseBinding {
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
            is CreateSoftwareSignerCompletedEvent -> onCreateSignerCompleted(
                event.masterSigner,
                event.skipPassphrase
            )
            is CreateSoftwareSignerErrorEvent -> onCreateSignerError(event)
            PassPhraseValidEvent -> removeValidationError()
            is LoadingEvent -> showLoading()
            is CreateWalletErrorEvent -> showError(event.message)
            is CreateWalletSuccessEvent -> {
                navigator.openBackupWalletScreen(requireActivity(), event.walletId, 1, true)
                requireActivity().apply {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
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

    private fun onCreateSignerCompleted(masterSigner: MasterSigner?, skipPassphrase: Boolean) {
        hideLoading()
        if (args.primaryKeyFlow.isSignUpFlow()) {
            navigator.openPrimaryKeyChooseUserNameScreen(
                activityContext = requireActivity(),
                mnemonic = args.mnemonic,
                passphrase = binding.passphrase.getEditText(),
                signerName = args.signerName
            )
        } else if (args.primaryKeyFlow.isReplaceFlow()) {
            ActivityManager.popToLevel(2)
            navigator.openSignerInfoScreen(
                activityContext = requireActivity(),
                id = masterSigner!!.id,
                masterFingerprint = masterSigner.device.masterFingerprint,
                name = masterSigner.name,
                type = masterSigner.type,
                justAdded = true,
                setPassphrase = !skipPassphrase,
                isReplacePrimaryKey = true
            )
        } else {
            navigator.returnToMainScreen()
            navigator.openSignerInfoScreen(
                activityContext = requireActivity(),
                id = masterSigner!!.id,
                masterFingerprint = masterSigner.device.masterFingerprint,
                name = masterSigner.name,
                type = masterSigner.type,
                justAdded = true,
                setPassphrase = !skipPassphrase
            )
        }
    }

    private fun showConfirmationDialog() {
        NCWarningDialog(requireActivity()).showDialog(
            title = getString(R.string.nc_confirmation),
            message = getString(R.string.nc_ssigner_text_set_passphrase_skip_confirmation),
            btnYes = getString(R.string.nc_text_confirm),
            btnNo = getString(R.string.nc_text_cancel),
            onYesClick = { viewModel.skipPassphraseEvent() }
        )
    }

    private fun setupViews() {
        binding.primaryKeyNote.isVisible = args.primaryKeyFlow.isPrimaryKeyFlow()
        if (args.primaryKeyFlow.isPrimaryKeyFlow()) {
            binding.btnFirst.text = getString(R.string.nc_ssigner_text_set_passphrase)
            binding.btnSecondary.text = getString(R.string.nc_ssigner_text_dont_set_passphrase)
        } else {
            binding.btnFirst.text = getString(R.string.nc_ssigner_text_dont_set_passphrase)
            binding.btnSecondary.text = getString(R.string.nc_ssigner_text_set_passphrase)
        }
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.passphrase.makeMaskedInput()
        binding.passphrase.addTextChangedCallback(viewModel::updatePassphrase)

        binding.confirmPassphrase.makeMaskedInput()
        binding.confirmPassphrase.addTextChangedCallback(viewModel::updateConfirmPassphrase)
        binding.btnFirst.setOnClickListener {
            if (args.primaryKeyFlow.isPrimaryKeyFlow()) viewModel.confirmPassphraseEvent() else viewModel.skipPassphraseEvent()
        }
        binding.btnSecondary.setOnClickListener {
            if (args.primaryKeyFlow.isPrimaryKeyFlow()) showConfirmationDialog() else viewModel.confirmPassphraseEvent()
        }
    }

}