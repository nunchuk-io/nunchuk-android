/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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
import com.nunchuk.android.core.signer.KeyFlow.isPrimaryKeyFlow
import com.nunchuk.android.core.util.getHtmlText
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.ConfirmPassPhraseNotMatchedEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.ConfirmPassPhraseRequiredEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.CreateSoftwareSignerCompletedEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.CreateSoftwareSignerErrorEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.CreateWalletErrorEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.CreateWalletSuccessEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.LoadingEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.PassPhraseRequiredEvent
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.PassPhraseValidEvent
import com.nunchuk.android.signer.software.databinding.FragmentSetPassphraseBinding
import com.nunchuk.android.signer.software.onCreateSignerCompleted
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.addTextChangedCallback
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class SetPassphraseFragment : BaseFragment<FragmentSetPassphraseBinding>() {

    private val viewModel: SetPassphraseViewModel by viewModels()

    private val args: SetPassphraseFragmentArgs by navArgs()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentSetPassphraseBinding {
        return FragmentSetPassphraseBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            is CreateSoftwareSignerCompletedEvent -> requireActivity().onCreateSignerCompleted(
                navigator = navigator,
                masterSigner = event.masterSigner,
                skipPassphrase = event.skipPassphrase,
                keyFlow = args.primaryKeyFlow,
                replacedXfp = args.replacedXfp,
                groupId = args.groupId.orEmpty(),
                passphrase = binding.passphrase.getEditText(),
                mnemonic = args.mnemonic,
                signerName = args.signerName,
            )

            is CreateSoftwareSignerErrorEvent -> onCreateSignerError(event)
            PassPhraseValidEvent -> removeValidationError()
            is LoadingEvent -> showOrHideLoading(loading = event.loading)
            is CreateWalletErrorEvent -> showError(event.message)
            is CreateWalletSuccessEvent -> {
                navigator.openBackupWalletScreen(requireActivity(), event.wallet)
                requireActivity().apply {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }

            is SetPassphraseEvent.ExistingSignerEvent -> {
                NCInfoDialog(requireActivity())
                    .showDialog(
                        message = String.format(
                            getString(R.string.nc_existing_key_change_key_type),
                            event.fingerprint.uppercase(Locale.getDefault())
                        ),
                        btnYes = getString(R.string.nc_text_yes),
                        btnInfo = getString(R.string.nc_text_no),
                        onYesClick = {
                            viewModel.createSoftwareSigner(isReplaceKey = true)
                        },
                        onInfoClick = {}
                    )
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

        // add key to assisted wallet flow
        if (!args.groupId.isNullOrEmpty()) {
            binding.toolbarTitle.text = getString(R.string.nc_set_passphrase_optional)
            binding.note.text =
                getHtmlText(getString(R.string.nc_ssigner_text_set_passphrase_assisted_wallet_note))
        }
    }
}